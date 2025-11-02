package io.gitlab.shdima.tnt.util

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.util.Vector

val Block.center: Location
    get() = location.center

val Location.center: Location
    get() = clone().add(0.5, 0.5, 0.5)

val Vector.center: Vector
    get() = clone().add(Vector(0.5, 0.5, 0.5))
