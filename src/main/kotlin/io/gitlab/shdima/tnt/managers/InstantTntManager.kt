package io.gitlab.shdima.tnt.managers

import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import io.gitlab.shdima.tnt.InstantTnt
import io.gitlab.shdima.tnt.data.BlockLocation
import io.gitlab.shdima.tnt.util.blockLocation
import io.gitlab.shdima.tnt.util.center
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.io.*
import kotlin.math.sqrt

class InstantTntManager(private val plugin: InstantTnt) {
    private val instantTntDataFile = File(plugin.dataFolder, "data/instant-tnt-blocks.json")
    val instantTntBlocks = mutableMapOf<BlockLocation, Pair<Long, Entity?>?>()

    init {
        loadInstantTntData()

        plugin.server.scheduler.runTaskTimer(plugin, { ->
            instantTntBlocks.toMutableMap().forEach {
                val pair = it.value ?: return@forEach
                val time = pair.first - 1

                if (time <= 0L) {
                    detonateInstantTnt(it.key.block, pair.second)
                    instantTntBlocks.remove(it.key)
                } else {
                    instantTntBlocks[it.key] = Pair(time, pair.second)
                }
            }
        }, 0L, 1L)
    }

    fun addInstantTnt(blockCoordinates: Location) {
        instantTntBlocks[blockCoordinates.blockLocation] = null
    }

    fun addInstantTnt(block: Block) {
        addInstantTnt(block.location)
    }

    fun removeInstantTnt(blockCoordinates: Location) {
        instantTntBlocks.remove(blockCoordinates.blockLocation)
    }

    fun removeInstantTnt(block: Block) {
        removeInstantTnt(block.location)
    }

    fun isInstantTnt(blockCoordinates: Location): Boolean {
        return instantTntBlocks.contains(blockCoordinates.blockLocation)
    }

    fun isInstantTnt(block: Block?): Boolean {
        return block?.type == Material.TNT && isInstantTnt(block.location)
    }

    fun isInstantTnt(itemStack: ItemStack): Boolean {
        val meta = itemStack.itemMeta
        val dataContainer = meta.persistentDataContainer
        return dataContainer.get(plugin.instantTntKey, PersistentDataType.BOOLEAN) == true
    }

    val instantTntItem: ItemStack
        get() {
            val instantTnt = ItemStack(Material.TNT)

            val meta = instantTnt.itemMeta

            val dataContainer = meta.persistentDataContainer
            val instantTntKey = plugin.instantTntKey
            dataContainer.set(instantTntKey, PersistentDataType.BOOLEAN, true)

            instantTnt.setItemMeta(meta)

            return instantTnt
        }

    fun shouldInstantTntDetonate(instantTnt: Block, cause: Entity, locationOverride: Location = cause.location): Boolean {
        val blockCenterLocation = instantTnt.center

        val tntX = blockCenterLocation.x
        val tntY = blockCenterLocation.y
        val tntZ = blockCenterLocation.z

        val minTntY = tntY - 0.5
        val maxTntY = tntY + 0.5

        val minTntX = tntX - 0.5
        val maxTntX = tntX + 0.5

        val minTntZ = tntZ - 0.5
        val maxTntZ = tntZ + 0.5

        val entityVelocity = plugin.playerVelocityManager.getVelocity(cause)

        val velocityX = entityVelocity.x
        val velocityY = entityVelocity.y
        val velocityZ = entityVelocity.z

        var significantValue = 0.0

        val locationDifference = locationOverride.clone().subtract(cause.location).toVector()

        val boundingBox = cause.boundingBox

        val minEntityY = boundingBox.minY + locationDifference.y
        val maxEntityY = boundingBox.maxY + locationDifference.y

        val maxEntityX = boundingBox.maxX + locationDifference.x
        val minEntityX = boundingBox.minX + locationDifference.x

        val maxEntityZ = boundingBox.maxZ + locationDifference.z
        val minEntityZ = boundingBox.minZ + locationDifference.z

        if (maxEntityY <= minTntY) {
            significantValue = velocityY
        } else if (minEntityY >= maxTntY) {
            significantValue = -velocityY
        } else if (maxEntityX <= minTntX) {
            significantValue = velocityX
        } else if (minEntityX >= maxTntX) {
            significantValue = -velocityX
        } else if (maxEntityZ <= minTntZ) {
            significantValue = velocityZ
        } else if (minEntityZ >= maxTntZ) {
            significantValue = -velocityZ
        }

        return significantValue > plugin.config.minimumCollisionDetonationSpeed / 20.0
    }

    fun detonateInstantTnt(instantTnt: Block, cause: Entity?) {
        instantTnt.type = Material.AIR
        removeInstantTnt(instantTnt)

        val power = plugin.config.power.toFloat()
        val shouldSetFire = plugin.config.setsFire
        val shouldBreakBlocks = plugin.config.breaksBlocks

        instantTnt.world.createExplosion(
            cause,
            instantTnt.center,
            power,
            shouldSetFire,
            shouldBreakBlocks,
            false
        )
    }

    fun chainDetonateInstantTnt(startingTnt: Block, entity: Entity?) {
        val radius = plugin.config.spreadRadiusBlocks
        val radiusSquared = radius * radius

        val tickDelayPerBlock = plugin.config.tickDelayPerBlock

        val toCheck = instantTntBlocks.keys.toMutableSet()
        val toExplode = mutableMapOf<BlockLocation, Long>()
        val queue = ArrayDeque<Pair<BlockLocation, Long>>()

        val start = startingTnt.location.blockLocation
        queue.add(Pair(start, 0))
        toExplode[start] = 0
        toCheck.remove(start)

        while (queue.isNotEmpty()) {
            val checking = queue.removeFirst()

            val toCheckIterator = toCheck.iterator()
            while (toCheckIterator.hasNext()) {
                val candidate = toCheckIterator.next()
                val distanceSquared = checking.first.location.distanceSquared(candidate.location)

                if (distanceSquared <= radiusSquared) {
                    val delay = (checking.second + tickDelayPerBlock * sqrt(distanceSquared)).toLong()

                    toExplode[candidate] = delay
                    queue.add(Pair(candidate, delay))

                    toCheckIterator.remove()
                }
            }
        }

        plugin.logger.info("Set ${toExplode.size} instant TNTs to explode!")

        toExplode.forEach {
            val existing = instantTntBlocks[it.key]?.first

            if (existing == null || existing > it.value) {
                instantTntBlocks[it.key] = Pair(it.value, entity)
            }
        }
    }

    fun loadInstantTntData() {
        try {
            val reader = FileReader(instantTntDataFile)

            val gson = Gson()

            val locations = gson.fromJson<List<BlockLocation>>(
                reader,
                MutableList::class.java
            ) as List<LinkedTreeMap<String, Any>>

            reader.close()

            for (location in locations) {
                val x = (location["x"] as Double).toInt()
                val y = (location["y"] as Double).toInt()
                val z = (location["z"] as Double).toInt()

                val world = location["world"] as String

                instantTntBlocks[
                    BlockLocation(x,y,z, world)
                ] = null
            }
        } catch (exception: IOException) {
            throw RuntimeException(exception)
        }
    }

    fun saveInstantTntData() {
        try {
            val writer = FileWriter(instantTntDataFile)

            val gson = Gson()

            gson.toJson(instantTntBlocks.keys, writer)

            writer.flush()
            writer.close()
        } catch (exception: IOException) {
            throw RuntimeException(exception)
        }
    }
}
