package io.gitlab.shdima.tnt.listeners

import io.gitlab.shdima.tnt.InstantTnt
import io.gitlab.shdima.tnt.util.getTouchedBlocks
import io.papermc.paper.event.entity.EntityMoveEvent
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

class InstantTntCollideListener(private val plugin: InstantTnt) : Listener {
    @EventHandler
    @Suppress("UnstableApiUsage")
    fun onInstantTntCollide(event: PlayerMoveEvent) {
        onInstantTntCollide(EntityMoveEvent(event.player, event.from, event.to))
    }

    @EventHandler
    fun onInstantTntCollide(event: EntityMoveEvent) {
        val entity = event.entity
        val touchedBlocks = entity.getTouchedBlocks(event.to)

        val instantTntManager = plugin.instantTntManager
        val instantTnts = touchedBlocks.filter { instantTntManager.isInstantTnt(it) && instantTntManager.shouldInstantTntDetonate(it, entity, event.to) } as MutableList<Block>
        if (instantTnts.isEmpty()) return

        instantTntManager.chainDetonateInstantTnt(instantTnts.removeFirst(), entity)
        instantTnts.forEach {
            if (instantTntManager.blocksToDetonate.contains(it.location.toVector())) return@forEach

            instantTntManager.detonateInstantTnt(it, entity)
        }
    }
}
