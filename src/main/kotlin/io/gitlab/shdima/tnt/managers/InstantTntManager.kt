package io.gitlab.shdima.tnt.managers

import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import io.gitlab.shdima.tnt.InstantTnt
import io.gitlab.shdima.tnt.util.center
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import java.io.*
import kotlin.math.floor

class InstantTntManager(private val plugin: InstantTnt) {
    var blocksToDetonate = mutableSetOf<Vector>()

    private val instantTntDataFile = File(plugin.dataFolder, "data/instant-tnt-blocks.json")
    private val instantTntBlocks = mutableListOf<Vector>()

    init {
        loadInstantTntData()
    }

    fun addInstantTnt(blockCoordinates: Vector) {
        instantTntBlocks.add(blockCoordinates)
    }

    fun addInstantTnt(block: Block) {
        addInstantTnt(block.location.toVector())
    }

    fun removeInstantTnt(blockCoordinates: Vector) {
        instantTntBlocks.remove(blockCoordinates)
    }

    fun removeInstantTnt(block: Block) {
        removeInstantTnt(block.location.toVector())
    }

    fun isInstantTnt(blockCoordinates: Vector): Boolean {
        return instantTntBlocks.contains(blockCoordinates)
    }

    fun isInstantTnt(block: Block?): Boolean {
        return block?.type == Material.TNT && isInstantTnt(block.location.toVector())
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

        removeInstantTnt(instantTnt)
    }

    private fun chainDetonateInstantTnt(startingTnt: Block): MutableSet<Vector> {
        val radius = plugin.config.spreadRadiusBlocks
        val radiusSquared = radius * radius

        val remaining = instantTntBlocks.toMutableSet()
        val result = mutableSetOf<Vector>()
        val queue = ArrayDeque<Vector>()

        val start = startingTnt.location.toVector()
        queue.add(start)
        result.add(start)
        remaining.remove(start)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()

            val iterator = remaining.iterator()
            while (iterator.hasNext()) {
                val candidate = iterator.next()
                if (current.distanceSquared(candidate) <= radiusSquared) {
                    iterator.remove()
                    result.add(candidate)
                    queue.add(candidate)
                }
            }
        }

        return result
    }

    fun chainDetonateInstantTnt(startingTnt: Block, cause: Entity?) {
        blocksToDetonate = chainDetonateInstantTnt(startingTnt)

        val explosionOrigin = startingTnt.center.toVector()
        val world = startingTnt.world

        val blocksPerTickDelay = plugin.config.blocksPerTickDelay

        for (explosionLocation in blocksToDetonate) {
            val distance = explosionOrigin.distance(explosionLocation)

            val tickDelay = if (blocksPerTickDelay == 0.0) 0 else floor(distance / blocksPerTickDelay).toInt()

            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                detonateInstantTnt(
                    world.getBlockAt(Location(world, explosionLocation.x, explosionLocation.y, explosionLocation.z)),
                    cause
                )
            }, tickDelay.toLong())
        }
    }

    fun loadInstantTntData() {
        try {
            val reader = FileReader(instantTntDataFile)

            val gson = Gson()

            val locations = gson.fromJson<List<*>>(
                reader,
                MutableList::class.java
            ) as List<LinkedTreeMap<String, Double>>

            reader.close()

            for (location in locations) {
                instantTntBlocks.add(
                    Vector(
                        location["x"]!!,
                        location["y"]!!,
                        location["z"]!!
                    )
                )
            }
        } catch (exception: IOException) {
            throw RuntimeException(exception)
        }
    }

    fun saveInstantTntData() {
        try {
            val writer = FileWriter(instantTntDataFile)

            val gson = Gson()

            gson.toJson(instantTntBlocks, writer)

            writer.flush()
            writer.close()
        } catch (exception: IOException) {
            throw RuntimeException(exception)
        }
    }
}
