package com.github.noamm9.features.impl.floor7

import com.github.noamm9.features.Feature
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundSoundPacket

object M7CooldownTracker: Feature("M7 Arch/Bers Ability Cooldown Tracker") {
    private val m7only by ToggleSetting("only show in m7 drags",true)

    val rag = ItemStats("RAGNAROCK_AXE","entity.wolf.death",1.4920635,400,200,false,400)
    val tuba = ItemStats("WEIRDER_TUBA","entity.wolf.death",1.0,400,600,false,400)
    val badHealth = ItemStats("SWORD_OF_BAD_HEALTH ","entity.generic.eat",1.0,100,100,false,100)

    override fun init() {

        register<WorldChangeEvent> {
            rag.onCooldown = false
            tuba.onCooldown = false
            badHealth.onCooldown = false
        }

        register<MainThreadPacketReceivedEvent.Pre> {
            detectAbilityUsage(rag,event.packet)
            detectAbilityUsage(tuba, event.packet)
            detectAbilityUsage(badHealth,event.packet)
        }

        register<TickEvent.Start> {
            countAbilityTicks(rag)
            countAbilityTicks(tuba)
            countAbilityTicks(badHealth)
        }

        hudElement("Ability Cooldown", shouldDraw = { show() }) { ctx, example ->
            val text = if(example) "example" else "current"
            return@hudElement 0f to 0f
        }
    }

    fun detectAbilityUsage(item: ItemStats,eventPacket: Packet<*>){
        if (eventPacket !is ClientboundSoundPacket) return
        if (eventPacket.sound.value().location().path != item.abilitySound) return
        if (eventPacket.pitch.toDouble() == item.abilityPitch) return
        val heldItem = mc.player?.mainHandItem ?: return
        if (heldItem.skyblockId != item.skyblockId) return

        item.lastUse = 0
        item.onCooldown = true
    }

    fun countAbilityTicks(item: ItemStats){
        if (item.onCooldown){
            item.lastUse++
            if (item.lastUse >= item.cooldownTicks){
                item.onCooldown = false
            }
        }
    }

    fun show(): Boolean{
        return m7only.value || (LocationUtils.inBoss || LocationUtils.dungeonFloorNumber == 7 || LocationUtils.F7Phase == 5)
    }
}
data class ItemStats(
    val skyblockId: String,
    val abilitySound: String,
    val abilityPitch: Double,
    val cooldownTicks: Int,
    val abilityTicks: Int,
    var onCooldown: Boolean,
    var lastUse: Int
)