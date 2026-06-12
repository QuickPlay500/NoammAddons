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
import net.minecraft.network.chat.Component

object CooldownTracker: Feature("M7 Arch/Bers Ability Cooldown Tracker") {
    private val m7only by ToggleSetting("only show in m7 drags",true)

    val rag = ItemStats("RAGNAROCK_AXE",400,200)
    val tuba = ItemStats("WEIRDER_TUBA",400,600)
    val badHealth = ItemStats("SWORD_OF_BAD_HEALTH",100,100)

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

            rag.countTicks()
            tuba.countTicks()
            badHealth.countTicks()
        }

        hudElement(
            name = "Ability Cooldown",
            shouldDraw = { show() }
        ) { ctx, example ->
            val col0 = 0
            val col1 = 70
            val col2 = 120
            val rowHeight = mc.font.lineHeight + 2

            // Header
            ctx.drawString(mc.font, Component.literal("Buff"), col1, 0, 0xFFAAAAAA.toInt(), true)
            ctx.drawString(mc.font, Component.literal("CD"),   col2, 0, 0xFFAAAAAA.toInt(), true)

            data class Row(val label: String, val buff: String, val cd: String)

            val rows = if (example) {
                listOf(
                    Row("Ragnarock",  "10.0s", "20.0s"),
                    Row("Tuba",       "30.0s", "20.0s"),
                    Row("Bad Health",  "5.0s",  "5.0s"),
                )
            } else {
                buildList {
                    add(Row("Ragnarock",  if (rag.onAbility)       "${"%.1f".format((rag.abilityTicks       - rag.lastUse)       / 20f)}s" else "0s", if (rag.onCooldown)       "${"%.1f".format((rag.cooldownTicks       - rag.lastUse)       / 20f)}s" else "0s"))
                    add(Row("Tuba",       if (tuba.onAbility)      "${"%.1f".format((tuba.abilityTicks      - tuba.lastUse)      / 20f)}s" else "0s", if (tuba.onCooldown)      "${"%.1f".format((tuba.cooldownTicks      - tuba.lastUse)      / 20f)}s" else "0s"))
                    add(Row("Bad Health", if (badHealth.onAbility) "${"%.1f".format((badHealth.abilityTicks - badHealth.lastUse) / 20f)}s" else "0s", if (badHealth.onCooldown) "${"%.1f".format((badHealth.cooldownTicks - badHealth.lastUse) / 20f)}s" else "0s"))
                }
            }

            rows.forEachIndexed { i, row ->
                val y = (i + 1) * rowHeight
                ctx.drawString(mc.font, Component.literal(row.label), col0, y, 0xFFFFFFFF.toInt(), true)
                ctx.drawString(mc.font, Component.literal(row.buff),  col1, y, 0xFF55FF55.toInt(), true)
                ctx.drawString(mc.font, Component.literal(row.cd),    col2, y, 0xFFFFAA00.toInt(), true)
            }

            val width = col2 + 40f
            val height = (rows.size + 1) * rowHeight.toFloat()
            width to height
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
    var onCooldown: Boolean = false,
    var onAbility: Boolean = false,
    var lastUse: Int = 0
){
    fun trigger(){
        lastUse = 0
        onCooldown = true
        onAbility = true
    }
    fun countTicks(){
        if(onCooldown || onAbility){
            lastUse++
            if(lastUse >= cooldownTicks && lastUse >= abilityTicks){
                reset()
            }
            else if (lastUse >= cooldownTicks){
                onCooldown = false
            } else if (lastUse >= abilityTicks){
                onAbility = false
            }
        }
    }
    fun reset(){
        lastUse = 0
        onCooldown = false
        onAbility = false
    }
}
