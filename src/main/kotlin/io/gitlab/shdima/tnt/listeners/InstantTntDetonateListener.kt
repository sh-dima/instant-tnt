package io.gitlab.shdima.tnt.listeners

import io.gitlab.shdima.tnt.InstantTnt
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.entity.TNTPrimed
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.TNTPrimeEvent

class InstantTntDetonateListener(private val plugin: InstantTnt) : Listener {
    @EventHandler
    fun onInstantTntDetonate(event: TNTPrimeEvent) {
        val tnt = event.block

        val manager = plugin.instantTntManager
        if (!manager.isInstantTnt(tnt)) return

        if (event.cause == TNTPrimeEvent.PrimeCause.EXPLOSION && event.primingEntity !is TNTPrimed && event.primingBlock == null) { // Will be handled by instant TNT manager
            event.isCancelled = true; return
        }

        event.isCancelled = true

        val entity = event.primingEntity
        manager.chainDetonateInstantTnt(
            tnt,
            entity
        )

        if (entity !is Player) return

        val heldItem = entity.inventory.itemInMainHand

        if (entity.gameMode == GameMode.CREATIVE || entity.gameMode == GameMode.SPECTATOR) return

        if (heldItem.type == Material.FLINT_AND_STEEL) heldItem.damage(1, entity)
        else if (heldItem.type == Material.FIRE_CHARGE) heldItem.amount--
    }
}
