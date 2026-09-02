package com.github.noamm9.features.impl.visual

import com.github.noamm9.config.types.ColorSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.dungeons.enums.Blessing
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.RenderHelper.width
import gg.essential.universal.ChatColor

object BlessingDisplay: Feature("Displays the current active blessings in the dungeon.") {
    private val power by ToggleSetting("Power Blessing", true).section("Blessings")
    private val time by ToggleSetting("Time Blessing", true)
    private val wisdom by ToggleSetting("Wisdom Blessing", false)
    private val life by ToggleSetting("Life Blessing", false)
    private val stone by ToggleSetting("Stone Blessing", false)

    private val timeToPower by ToggleSetting("Time to Power", false)

    private val powerColor by ColorSetting("Power Color", ChatColor.DARK_RED.color !!).showIf { power.value }.section("Colors")
    private val timeColor by ColorSetting("Time Color", ChatColor.DARK_PURPLE.color !!).showIf { time.value }
    private val wisdomColor by ColorSetting("Wisdom Color", ChatColor.AQUA.color !!).showIf { wisdom.value }
    private val lifeColor by ColorSetting("Red Color", ChatColor.RED.color !!).showIf { life.value }
    private val stoneColor by ColorSetting("Stone Color", ChatColor.GRAY.color !!).showIf { stone.value }

    private fun getBlessingConfig(type: Blessing) = when (type) {
        Blessing.POWER -> power.value to powerColor.value
        Blessing.TIME -> time.value to timeColor.value
        Blessing.STONE -> stone.value to stoneColor.value
        Blessing.LIFE -> life.value to lifeColor.value
        Blessing.WISDOM -> wisdom.value to wisdomColor.value
    }

    override fun init() {
        hudElement("BlessingDisplay") { context, example ->
            var maxWidth = 0
            var currentY = 0

            Blessing.entries.forEach { blessing ->
                val (enabled, color) = getBlessingConfig(blessing)

                if (blessing == Blessing.TIME && timeToPower.value) return@forEach

                val rawValue = if (example) 5 else blessing.current

                // Add time blessings to power blessings
                val value: Float = if (blessing == Blessing.POWER && timeToPower.value) {
                    val timeValue = if (example) 5 else Blessing.TIME.current
                    rawValue + timeValue * 0.5f
                } else {
                    rawValue.toFloat()
                }

                if (!enabled || value <= 0f) return@forEach
                val valueString = if (value % 1 == 0f) value.toInt().toString() else value.toString()
                val text = "${blessing.displayString} §f$valueString"


                context.drawString(text, 0, currentY, color)

                maxWidth = maxOf(maxWidth, text.width())
                currentY += 9
            }

            return@hudElement maxWidth to currentY
        }
    }
}