package com.github.noamm9.features.impl.floor7

import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.ButtonSetting
import com.github.noamm9.ui.clickgui.components.impl.CategorySetting
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.SeparatorSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.SoundSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.enums.DungeonClass
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.sounds.SoundEvents
import kotlin.math.sqrt

object LeapCounter: Feature(description = "Displays a counter for how many players have leaped to you. (Requires Leap Messages)") {

    private val earlyEnterClasses = listOf(
        DungeonClass.Archer,
        DungeonClass.Mage,
        DungeonClass.Berserk,
        DungeonClass.Healer,
        DungeonClass.Tank
    )

    private data class EarlyEnterPosition(
        val x: Double,
        val y: Double,
        val z: Double,
        val distance: Double
    )

    private data class EarlyEnter(
        val enabled: ToggleSetting,
        val onlyOneClass: ToggleSetting,
        val playerClass: DropdownSetting,
        val playerCount: SliderSetting<Int>,
        var count: Int = 0,
        val position: EarlyEnterPosition
    )

    private fun newEarlyEnter(
        name: String,
        defaultEnabled: Boolean,
        defaultPlayerClass: Int,
        defaultPlayerCount: Int,
        position: EarlyEnterPosition
    ): EarlyEnter {
        val enabled = ToggleSetting(name, defaultEnabled)

        if (configSettings.isNotEmpty()) configSettings.add(SeparatorSetting())
        configSettings.add(CategorySetting(name))
        configSettings.add(enabled)

        val onlyOneClass = ToggleSetting("OnlyOneClass", true).showIf { enabled.value }
        val playerClass = DropdownSetting(
            "Class",
            defaultPlayerClass,
            earlyEnterClasses.map { it.name }).showIf { enabled.value && onlyOneClass.value }
        val playerCount = SliderSetting("Count", defaultPlayerCount, 1, 4, 1).showIf { enabled.value }

        configSettings.addAll(listOf(onlyOneClass, playerClass, playerCount))

        return EarlyEnter(
            enabled = enabled,
            onlyOneClass = onlyOneClass,
            playerClass = playerClass,
            playerCount = playerCount,
            position = position
        )
    }

    private fun currentEarlyEnter(): EarlyEnter? {
        val pos = mc.player?.position() ?: return null
        return earlyEnters.filter { it.enabled.value }.firstOrNull { earlyEnter ->
            if (earlyEnter.onlyOneClass.value && DungeonListener.thePlayer?.clazz != earlyEnterClasses[earlyEnter.playerClass.value]) return null
            val p = earlyEnter.position
            val dist = sqrt(
                (pos.x - p.x) * (pos.x - p.x) +
                        (pos.y - p.y) * (pos.y - p.y) +
                        (pos.z - p.z) * (pos.z - p.z)
            )
            dist <= p.distance
        }
    }

    private val playSound by ToggleSetting("Sound after everyone leaped", false).section("Settings")
    private val sound by SoundSetting("Sound", SoundEvents.EXPERIENCE_ORB_PICKUP).showIf { playSound.value }
    private val volume by SliderSetting("Volume", 0.5f, 0f, 1f, 0.1f).showIf { playSound.value }
    private val pitch by SliderSetting("Pitch", 1f, 0f, 2f, 0.1f).showIf { playSound.value }
    private val testSound by ButtonSetting(
        "Test Sound",
        false
    ) {
        mc.soundManager.play(
            SimpleSoundInstance.forUI(
                sound.value,
                pitch.value,
                volume.value
            )
        )
    }.showIf { playSound.value }

    private val ss = newEarlyEnter("ss", false, 3, 3, EarlyEnterPosition(108.0, 120.0, 94.0, 2.0))
    private val hee2 = newEarlyEnter("hee2", true, 1, 4, EarlyEnterPosition(60.5, 132.0, 139.5, 3.0))
    private val lee2 = newEarlyEnter("lee2", false, 0, 4, EarlyEnterPosition(58.0, 109.0, 131.0, 1.5))
    private val ee3 = newEarlyEnter("ee3", true, 3, 3, EarlyEnterPosition(2.0, 109.0, 102.0, 4.0))
    private val hee3 = newEarlyEnter("hee3", false, 3, 3, EarlyEnterPosition(18.5, 121.5, 91.5, 1.5))
    private val core = newEarlyEnter("core", true, 1, 4, EarlyEnterPosition(54.5, 115.0, 51.5, 2.0))
    private val p5 = newEarlyEnter("p5", true, 3, 4, EarlyEnterPosition(54.5, 5.0, 76.5, 3.0))

    private val earlyEnters = listOf(ss, hee2, lee2, ee3, hee3, core, p5)

    override fun init() {
        register<WorldChangeEvent> {
            earlyEnters.forEach { it.count = 0 }
        }
        register<ChatMessageEvent> {
            if (!LocationUtils.inDungeon || LocationUtils.dungeonFloorNumber != 7) return@register
            // still need to filter out own player messages
            if (!event.unformattedText.contains(mc.user.name)) return@register
            val match = currentEarlyEnter() ?: return@register
            match.count++
            if (playSound.value && match.count >= match.playerCount.value) {
                mc.soundManager.play(SimpleSoundInstance.forUI(sound.value, pitch.value, volume.value))
            }
        }
        hudElement(
            name = "Leap Counter",
            shouldDraw = { LocationUtils.inDungeon && LocationUtils.dungeonFloorNumber == 7 && currentEarlyEnter() != null },
        ) { ctx, example ->
            if (example) {
                Render2D.drawString(ctx, "0/4", 0, 0)
                return@hudElement "0/4".width().toFloat() to 9f
            }
            val match = currentEarlyEnter() ?: return@hudElement 0f to 0f
            val text = "${match.count}/${match.playerCount.value}"
            Render2D.drawString(ctx, text, 0, 0)
            text.width().toFloat() to 9f
        }
    }
}