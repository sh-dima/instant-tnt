package io.gitlab.shdima.tnt.listeners

import io.gitlab.shdima.tnt.InstantTnt
import io.gitlab.shdima.tnt.util.center
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent

class InstantTntBreakListener(private val plugin: InstantTnt) : Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    fun onInstantTntBreak(event: BlockBreakEvent) {
        val block = event.block
        val instantTntManager = plugin.instantTntManager

        if (!instantTntManager.isInstantTnt(block)) return

        instantTntManager.removeInstantTnt(event.block)

        val instantTnt = instantTntManager.instantTntItem

        if (event.player.gameMode != GameMode.CREATIVE && event.player.gameMode != GameMode.SPECTATOR) {
            val world = block.world
            world.dropItem(block.center, instantTnt)
        }

        event.isDropItems = false
    }
}
