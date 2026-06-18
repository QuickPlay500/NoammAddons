package com.github.noamm9.features.impl.floor7

import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.CategorySetting
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.SeparatorSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.SoundSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.dungeons.enums.DungeonClass
import net.minecraft.sounds.SoundEvents

object LeapCounter : Feature(description = "Displays a counter for how many players have leaped to you. (Requires Leap Messages)") {

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
        val count: Int = 0,
        val position: EarlyEnterPosition
    )
    private fun newEarlyEnter(
        name: String,
        defaultEnabled: Boolean,
        defaultPlayerClass: Int,
        defaultPlayerCount: Int,
        position: EarlyEnterPosition
    ): EarlyEnter{
        val enabled = ToggleSetting(name,defaultEnabled).section(name)

        if (configSettings.isNotEmpty()) configSettings.add(SeparatorSetting())
        configSettings.add(CategorySetting(name))
        configSettings.add(enabled)

        val onlyOneClass = ToggleSetting("OnlyOneClass",true).showIf { enabled.value }
        val playerClass = DropdownSetting("Class", defaultPlayerClass, earlyEnterClasses.map { it.name }).showIf {enabled.value && onlyOneClass.value}
        val playerCount = SliderSetting("Count",defaultPlayerCount,1,4,1).showIf { enabled.value }

        configSettings.addAll(listOf(onlyOneClass,playerClass,playerCount))

        return EarlyEnter(
            enabled = enabled,
            onlyOneClass = onlyOneClass,
            playerClass = playerClass,
            playerCount = playerCount,
            position = position
        )
    }

    private val playSound by ToggleSetting("Sound after everyone leaped", false).section("Settings")
    private val sound by SoundSetting("Sound", SoundEvents.EXPERIENCE_ORB_PICKUP).withDescription("The internal Minecraft sound key to play.").showIf { playSound.value }

    private val hee2 = newEarlyEnter("hee2",true,1,4, EarlyEnterPosition(0.0,0.0,0.0,0.0))
    private val lee2 = newEarlyEnter("lee2",false,0,4,EarlyEnterPosition(0.0,0.0,0.0,0.0))
    private val ee3 = newEarlyEnter("ee3",true,3,3,EarlyEnterPosition(0.0,0.0,0.0,0.0))
    private val hee3 = newEarlyEnter("hee3",false,3,3,EarlyEnterPosition(0.0,0.0,0.0,0.0))
    private val core = newEarlyEnter("core",true,1,4,EarlyEnterPosition(0.0,0.0,0.0,0.0))
    private val p5 = newEarlyEnter("p5",true,3,4,EarlyEnterPosition(54.5,5.0,76.5,3.0))


    private val playerName = mc.player?.name


    override fun init() {
        register<WorldChangeEvent> {

        }
        register<ChatMessageEvent> {

        }
        /*
        hudElement(
            name = "Leap Counter",

        )
        */
    }
}