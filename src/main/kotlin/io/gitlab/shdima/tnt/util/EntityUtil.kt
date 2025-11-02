package io.gitlab.shdima.tnt.util

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import kotlin.math.floor

fun Entity.getTouchedBlocks(locationOverride: Location = this.location): List<Block> {
    val box = boundingBox
        box.expand(0.01)

    val locationDifference = locationOverride.clone().subtract(location)

    val minX = box.minX + locationDifference.x
    val maxX = box.maxX + locationDifference.x

    val minY = box.minY + locationDifference.y
    val maxY = box.maxY + locationDifference.y

    val minZ = box.minZ + locationDifference.z
    val maxZ = box.maxZ + locationDifference.z

    val touchedBlocks = mutableListOf<Block>()

    var x = floor(minX).toInt()
    while (x <= floor(maxX)) {
        var y = floor(minY).toInt()
        while (y <= floor(maxY)) {
            var z = floor(minZ).toInt()
            while (z <= floor(maxZ)) {
                touchedBlocks.add(world.getBlockAt(x, y, z))
                z++
            }
            y++
        }
        x++
    }

    return touchedBlocks
}
