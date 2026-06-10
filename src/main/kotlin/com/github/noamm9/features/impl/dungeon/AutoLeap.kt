package com.github.noamm9.features.impl.dungeon

//#if CHEAT

import com.github.noamm9.event.impl.BossBarUpdateEvent
import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.EntityUnloadEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.PlayerUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.enums.DungeonClass
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.location.LocationUtils.dungeonFloorNumber
import com.github.noamm9.utils.location.LocationUtils.inBoss
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.world.entity.decoration.ArmorStand

object AutoLeap: Feature("Automated Leap") {
    private val leapTargets = listOf(
        DungeonClass.Archer,
        DungeonClass.Mage,
        DungeonClass.Berserk,
        DungeonClass.Healer,
        DungeonClass.Tank
    )

    private val witherLeapSetting by ToggleSetting("Leap after a wither key pick up", false)
    private val targetWitherKeyLeap by DropdownSetting("Leap Target", 4, leapTargets.map { it.name })

    private val maxorDeadLeapSetting by ToggleSetting("Leap after Maxor died",false)
    private val targetMaxorDeadLeap by DropdownSetting("Leap Target",3, leapTargets.map { it.name })
    private var maxorDead = false

    private val stormCrushLeapSetting by ToggleSetting("Leap after first Storm crush",false)
    private val targetStormCrushLeap by DropdownSetting("Leap Target",4, leapTargets.map { it.name })

    private val stormEnragedLeapSetting by ToggleSetting("Leap after first pillar",false)
    private val targetStormEnragedLeap by DropdownSetting("Leap Target",2, leapTargets.map { it.name })

    private val stormDeadLeapSetting by ToggleSetting("Leap after Storm died",false)
    private val targetStormDeadLeap by DropdownSetting("Leap Target",4, leapTargets.map { it.name })


    override fun init() {

        // Wither Key
        register<EntityUnloadEvent> {
            if (!witherLeapSetting.value) return@register
            if (!LocationUtils.inDungeon || inBoss) return@register

            val entity = event.entity
            if (entity is ArmorStand && entity.customName?.unformattedText == "Wither Key") {
                scope.launch { performLeap(targetWitherKeyLeap.value) }
            }
        }

        // Maxor
        register<BossBarUpdateEvent> {
            if (!maxorDeadLeapSetting.value) return@register
            if (dungeonFloorNumber != 7 && !inBoss) return@register
            if (event.progress > 0f) return@register
            val name = event.name.unformattedText
            val entry = DungeonListener.bossEntryTime?.ticks ?: return@register

            if (name.contains("Maxor") && !maxorDead &&DungeonListener.currentTime - entry > 120) {
                maxorDead = true
                scope.launch { performLeap(targetMaxorDeadLeap.value) }
            }
        }

        // Storm
        register<ChatMessageEvent> {
            if (!stormDeadLeapSetting.value || !stormEnragedLeapSetting.value || !stormCrushLeapSetting.value || dungeonFloorNumber != 7 || !inBoss) return@register
            when (event.unformattedText) {
                "[BOSS] Storm: Oof", "[BOSS] Storm: Ouch, that hurt!" -> if(stormCrushLeapSetting.value){scope.launch { performLeap(targetStormCrushLeap.value)}}
                "[BOSS] Storm: I should have known that I stood no chance." -> if(stormEnragedLeapSetting.value){scope.launch { performLeap(targetStormDeadLeap.value)}}
                "[BOSS] Storm: BEGONE PILLAR!","[BOSS] Storm: This factory is too small for me!","[BOSS] Storm: Slowing me down will be your greatest accomplishment!","[BOSS] Storm: THAT WAS ONLY IN MY WAY!" -> if (stormEnragedLeapSetting.value){scope.launch { performLeap(targetStormEnragedLeap.value) }}
            }
        }

    }

    private suspend fun performLeap(value: Int) {
        val prevSlot = mc.player?.inventory?.selectedSlot ?: return
        val aliveTeammates = DungeonListener.dungeonTeammatesNoSelf.filterNot { it.isDead }
        val preferredClass = leapTargets[value]
        val target = aliveTeammates.find { it.clazz == preferredClass } ?: return
        PlayerUtils.leapAction(target)
        delay(50)
        PlayerUtils.swapToSlot(prevSlot)
    }
}

//#endif
