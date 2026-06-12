package com.github.noamm9.features.impl.floor7

import com.github.noamm9.features.Feature
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.MouseClickEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.network.protocol.game.ClientboundSoundPacket

object CooldownTracker: Feature("M7 Arch/Bers Ability Cooldown Tracker") {
    private val m7only by ToggleSetting("only show in m7 drags",true)

    val rag = ItemStats("RAGNAROCK_AXE",400,200,false,400)
    val tuba = ItemStats("WEIRDER_TUBA",400,600,false,400)
    val badHealth = ItemStats("SWORD_OF_BAD_HEALTH",100,100,false,100)

    override fun init() {

        register<WorldChangeEvent> {
            rag.reset()
            tuba.reset()
            badHealth.reset()
        }

        register<MouseClickEvent>{
            if (show()) return@register

            if (event.button != 1 || event.action != 1) return@register
            val item = mc.player?.mainHandItem ?: return@register
            when (item.skyblockId) {
                tuba.skyblockId -> {
                    tuba.trigger()
                }
                badHealth.skyblockId -> {
                    badHealth.trigger()
                }
                else -> {
                    return@register
                }
            }
        }

        register<MainThreadPacketReceivedEvent.Pre>{
            if (show()) return@register

            if (event.packet !is ClientboundSoundPacket) return@register
            if (event.packet.sound.value().location().path != "entity.wolf.death") return@register
            if (event.packet.pitch.toDouble() == 1.4920635) return@register
            val item = mc.player?.mainHandItem ?: return@register
            if (item.skyblockId != rag.skyblockId) return@register

            rag.trigger()
        }

        register<TickEvent.Start> {
            if (show()) return@register

            rag.countAbilityTicks()
            tuba.countAbilityTicks()
            badHealth.countAbilityTicks()
        }

        hudElement("Ability Cooldown", shouldDraw = { true }) { ctx, example ->
            val text = if(example) "example" else "current"
            return@hudElement 0f to 0f
        }
    }

    fun show(): Boolean{
        return !((!m7only.value) || (LocationUtils.F7Phase == 5 && LocationUtils.inBoss && LocationUtils.dungeonFloorNumber == 7))
    }
}

data class ItemStats(
    val skyblockId: String,
    val cooldownTicks: Int,
    val abilityTicks: Int,
    var onCooldown: Boolean,
    var lastUse: Int
){
    fun trigger(){
        lastUse = 0
        onCooldown = true
    }
    fun countAbilityTicks(){
        if(onCooldown){
            lastUse++
            if (lastUse >= cooldownTicks){
                reset()
            }
        }
    }
    fun reset(){
        lastUse = 0
        onCooldown = false
    }
}
