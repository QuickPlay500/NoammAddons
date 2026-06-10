package com.github.noamm9.features.impl.dungeon

//#if CHEAT

import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.PlayerUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.enums.DungeonClass
import com.github.noamm9.utils.location.LocationUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AutoLeap: Feature("Automated Leap") {
    private val witherLeapSetting by ToggleSetting("Leap after key pick up", true)
        .withDescription("Auto Leap to a player after key pick up.")
    private val witherLeapTargets = listOf(
        DungeonClass.Healer,
        DungeonClass.Mage,
        DungeonClass.Berserk,
        DungeonClass.Archer,
        DungeonClass.Tank
    )
    private val targetLeapClass by DropdownSetting("Leap Target", 0, witherLeapTargets.map { it.name })

    override fun init() {
        register<ChatMessageEvent> {
            if (!witherLeapSetting.value) return@register
            if (!LocationUtils.inDungeon) return@register
            if ("Wither Key" !in event.unformattedText) return@register
            scope.launch { performLeap() }
        }
    }

    private suspend fun performLeap() {
        val prevSlot = mc.player?.inventory?.selectedSlot ?: return
        val aliveTeammates = DungeonListener.dungeonTeammatesNoSelf.filterNot { it.isDead }
        val preferredClass = witherLeapTargets[targetLeapClass.value]
        val target = aliveTeammates.find { it.clazz == preferredClass } ?: return
        PlayerUtils.leapAction(target)
        delay(50)
        PlayerUtils.swapToSlot(prevSlot)
    }
}

//#endif
