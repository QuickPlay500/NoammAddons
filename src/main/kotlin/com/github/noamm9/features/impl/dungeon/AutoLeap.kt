package com.github.noamm9.features.impl.dungeon

//#if CHEAT

import com.github.noamm9.event.impl.BossBarUpdateEvent
import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.EntityUnloadEvent
import com.github.noamm9.event.impl.WorldChangeEvent
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
import kotlin.time.Duration.Companion.milliseconds

private var maxorDead = false
private var goldorDead = false
private var necronDead = false
private var goldorStart = false
private var necronStart = false

object AutoLeap : Feature("Auto Leap") {
    private val leapTargets = listOf(
        DungeonClass.Archer,
        DungeonClass.Mage,
        DungeonClass.Berserk,
        DungeonClass.Healer,
        DungeonClass.Tank
    )
    private var stormCrushed = false
    private var stormEnraged = false
    private var stormDead = false

    private val witherLeapSetting by ToggleSetting("Wither Key", false)
    private val targetWitherKeyLeap by DropdownSetting("Target", 3, leapTargets.map { it.name }).showIf { witherLeapSetting.value }

    private val maxorDeadLeapSetting by ToggleSetting("Maxor dead", false)
    private val targetMaxorDeadLeap by DropdownSetting("Target", 2, leapTargets.map { it.name }).showIf { maxorDeadLeapSetting.value }

    private val stormCrushLeapSetting by ToggleSetting("Storm Crushed", false)
    private val targetStormCrushLeap by DropdownSetting("Target", 3, leapTargets.map { it.name }).showIf { stormCrushLeapSetting.value }

    private val stormEnragedLeapSetting by ToggleSetting("Storm Enraged", false)
    private val targetStormEnragedLeap by DropdownSetting("Target", 1, leapTargets.map { it.name }).showIf { stormEnragedLeapSetting.value }

    private val stormDeadLeapSetting by ToggleSetting("Storm dead", false)
    private val targetStormDeadLeap by DropdownSetting("Target", 3, leapTargets.map { it.name }).showIf { stormDeadLeapSetting.value }

    private val goldorDeadLeapSetting by ToggleSetting("Goldor dead",false)
    private val targetGoldorDeadLeap by DropdownSetting("Target",3,leapTargets.map { it.name }).showIf { goldorDeadLeapSetting.value }

    private val necronDeadLeapSetting by ToggleSetting("Necron dead",false)
    private val targetNecronDeadLeap by DropdownSetting("Target",3,leapTargets.map { it.name }).showIf { necronDeadLeapSetting.value }

    override fun init() {

        register<WorldChangeEvent> {
            stormCrushed = false
            stormEnraged = false
            stormDead = false
            maxorDead = false
            goldorStart = false
            goldorDead = false
            necronStart = false
            necronStart = false
        }

        register<EntityUnloadEvent> {
            if (!witherLeapSetting.value) return@register
            if (!LocationUtils.inDungeon || inBoss) return@register

            val entity = event.entity
            if (entity is ArmorStand && entity.customName?.unformattedText == "Wither Key") {
                scope.launch { performLeap(targetWitherKeyLeap.value) }
            }
        }

        register<BossBarUpdateEvent> {
            if (!maxorDeadLeapSetting.value && !goldorDeadLeapSetting.value && !necronDeadLeapSetting.value) return@register
            if (dungeonFloorNumber != 7 || !inBoss) return@register
            if (event.progress > 0f) return@register
            val name = event.name.unformattedText
            val entry = DungeonListener.bossEntryTime?.ticks ?: return@register

            if (name.contains("Maxor") && !maxorDead && DungeonListener.currentTime - entry > 120) {
                maxorDead = true
                scope.launch { performLeap(targetMaxorDeadLeap.value) }
            }
            else if (name.contains("Goldor") && !goldorDead && goldorStart) {
                goldorDead = true
                scope.launch { performLeap(targetGoldorDeadLeap.value) }
            }
            else if (name.contains("Necron") && !necronDead && necronStart) {
                necronDead = true
                scope.launch { performLeap(targetNecronDeadLeap.value) }
            }
        }

        register<ChatMessageEvent> {
            if ((!stormDeadLeapSetting.value && !stormEnragedLeapSetting.value && !stormCrushLeapSetting.value && !goldorDeadLeapSetting.value && !necronDeadLeapSetting.value) || !inBoss || dungeonFloorNumber != 7) return@register
            when (event.unformattedText) {
                "[BOSS] Storm: Oof", "[BOSS] Storm: Ouch, that hurt!" -> if (stormCrushLeapSetting.value && !stormCrushed) {
                    stormCrushed = true; scope.launch { performLeap(targetStormCrushLeap.value) }
                }

                "[BOSS] Storm: I should have known that I stood no chance.", " [BOSS] Storm: At least my son died by your hands. " -> if (stormDeadLeapSetting.value && !stormDead) {
                    stormDead = true; scope.launch { performLeap(targetStormDeadLeap.value) }
                }

                "[BOSS] Storm: BEGONE PILLAR!", "[BOSS] Storm: This factory is too small for me!", "[BOSS] Storm: Slowing me down will be your greatest accomplishment!", "[BOSS] Storm: THAT WAS ONLY IN MY WAY!" -> if (stormEnragedLeapSetting.value && !stormEnraged) {
                    stormEnraged = true; scope.launch { performLeap(targetStormEnragedLeap.value) }
                }
                "The Core entrance is opening!" -> goldorStart = true
                "[BOSS] Necron: ARGH!" -> necronStart = true
            }
        }
    }

    private suspend fun performLeap(value: Int) {
        val prevSlot = mc.player?.inventory?.selectedSlot ?: return
        val aliveTeammates = DungeonListener.dungeonTeammatesNoSelf.filterNot { it.isDead }
        val preferredClass = leapTargets[value]
        val target = aliveTeammates.find { it.clazz == preferredClass } ?: return
        PlayerUtils.leapAction(target)
        delay(50.milliseconds)
        PlayerUtils.swapToSlot(prevSlot)
    }
}

//#endif
