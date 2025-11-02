package io.gitlab.shdima.tnt.listeners

import io.gitlab.shdima.tnt.InstantTnt
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent

class InstantTntPlaceListener(private val plugin: InstantTnt) : Listener {
    @EventHandler
    fun onTntPlace(event: BlockPlaceEvent) {
        val instantTntManager = plugin.instantTntManager

        if (!instantTntManager.isInstantTnt(event.itemInHand)) return

        instantTntManager.addInstantTnt(event.block)
    }
}
