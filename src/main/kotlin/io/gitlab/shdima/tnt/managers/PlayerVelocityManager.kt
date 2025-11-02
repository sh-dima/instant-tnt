package io.gitlab.shdima.tnt.managers

import io.gitlab.shdima.tnt.InstantTnt
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector

class PlayerVelocityManager(private val plugin: InstantTnt) : BukkitRunnable() {
    private val playerVelocityMap = mutableMapOf<Player, Vector>()
    private val playerPositionMap = mutableMapOf<Player, Vector>()

    fun getPlayerVelocity(player: Player): Vector? {
        return playerVelocityMap[player]
    }

    fun getVelocity(entity: Entity): Vector {
        if (entity is Player) {
            return getPlayerVelocity(entity) ?: Vector()
        }

        return entity.velocity
    }

    fun updatePlayerVelocityData(player: Player) {
        var oldPosition = playerPositionMap[player]
        val newPosition = player.location.toVector()

        if (oldPosition == null) {
            oldPosition = newPosition
        }

        val velocity = newPosition.clone().subtract(oldPosition)

        playerVelocityMap[player] = velocity
        playerPositionMap[player] = newPosition
    }

    fun updateVelocityData() {
        val players = plugin.server.onlinePlayers.stream().toList()

        for (player in players) {
            updatePlayerVelocityData(player)
        }
    }

    override fun run() {
        updateVelocityData()
    }
}
