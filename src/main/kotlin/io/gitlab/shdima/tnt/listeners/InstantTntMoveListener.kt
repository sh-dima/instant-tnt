package io.gitlab.shdima.tnt.listeners

import io.gitlab.shdima.tnt.InstantTnt
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent

class InstantTntMoveListener(private val plugin: InstantTnt) : Listener {
    @EventHandler
    private fun onPistonPush(event: BlockPistonExtendEvent) {
        val piston = event.block
        val direction = event.direction

        val pushed = piston.getRelative(direction)
        val manager = plugin.instantTntManager

        if (!manager.isInstantTnt(pushed)) return

        val newBlock = pushed.getRelative(direction)

        manager.removeInstantTnt(pushed)
        manager.addInstantTnt(newBlock)
    }

    @EventHandler
    private fun onPistonPull(event: BlockPistonRetractEvent) {
        if (!event.isSticky) return

        val piston = event.block
        val direction = event.direction.oppositeFace

        val newBlock = piston.getRelative(direction)
        val manager = plugin.instantTntManager

        val pulled = newBlock.getRelative(direction)

        if (!manager.isInstantTnt(pulled)) return

        manager.removeInstantTnt(pulled)
        manager.addInstantTnt(newBlock)
    }
}
