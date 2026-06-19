package com.github.noamm9.features.impl.misc

import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils
import net.minecraft.network.protocol.game.ClientboundSoundPacket

object SoundDebug: Feature("prints the exact sound and pitch in chat") {
    override fun init() {
        register<MainThreadPacketReceivedEvent.Pre> {
            if (event.packet !is ClientboundSoundPacket) return@register
            ChatUtils.modMessage("Sound: ${event.packet.sound.value().location().path} | Pitch: ${event.packet.pitch}")
        }
    }
}