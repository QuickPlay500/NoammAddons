package com.github.noamm9.features.impl.general

//#if CHEAT

import com.github.noamm9.event.impl.ContainerFullyOpenedEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.features.impl.general.ProtectItem
import com.github.noamm9.features.impl.general.ProtectItem.ProtectType
import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.GuiUtils
import com.github.noamm9.utils.items.ItemUtils.customData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.world.inventory.ChestMenu
import kotlin.collections.setOf
import kotlin.jvm.optionals.getOrDefault
import kotlin.time.Duration.Companion.milliseconds

object AutoSell : Feature("Automatic Item Sell to Trades Menu") {

    private val firstClickDelay by SliderSetting("First CLick Delay",500f,100f,1000f,5f).withDescription("Recommended Minimum: 400 - ping")
    private val delayMs by SliderSetting("Delay", 500f, 100f, 1000f, 5f).withDescription("Delay between each click in ms.")

    private val sellSkeletonMasterCp by ToggleSetting("Skeleton Master Chestplates", false).section("Items to Sell")
    private val sellReviveStones by ToggleSetting("Revive Stones", false)
    private val sellRecombedItems by ToggleSetting("Recombulated Items", false)
    private val sell50percentItems by ToggleSetting("50/50 Items",false)
    private val sellRunes by ToggleSetting("Runes",false)
    private val sellDragonFragments by ToggleSetting("Dragon Fragments",false)
    private val sellMobItems by ToggleSetting("Mob Items", false).withDescription("Armor, Bows, etc.")
    private val sellCandycombs by ToggleSetting("Candycombs", false)
    private val sellUselessOtherStuff by ToggleSetting("Useless Other Stuff").withDescription("Buttons, Levers, Defuse Kits, Signs, etc.")
    private val sellOtherSackItems by ToggleSetting("Other Sack Items", false)


    private val setSkeletonMasterChestplate = setOf("skeleton master chestplate")
    private val setReviveStones = setOf("Revive Stone")
    private val setRunes = setOf("Rune")
    private val setDragonFragments = setOf("young dragon fragment","superior dragon fragment","holy dragon fragment","strong dragon fragment","unstable dragon fragment")
    private val setMobItems = setOf("rotten", "skeleton master helmet", "skeleton master leggings", "skeleton master boots", "skeleton grunt", "cutlass", "skeleton lord", "skeleton soldier", "zombie soldier", "zombie knight", "zombie commander", "zombie lord", "skeletor", "super heavy", "heavy", "sniper helmet", "dreadlord", "earth shard", "zombie commander whip", "machine gun", "sniper bow", "soulstealer bow", "silent death")
    private val setUselessOtherStuff = setOf("defuse kit", "optical lens", "tripwire hook", "button", "carpet", "lever", "diamond atom", "healing viii splash potion", "healing 8 splash potion","training weight","premium flesh")
    private val setCandycombs = setOf("candycomb")
    private val setOtherSackItems = setOf("mimic fragment, enchanted rotten flesh","trap","enchanted bone", "defuse kit", "enchanted ice")


    val sellList: MutableSet<String> = mutableSetOf()
    fun configureSellList(){
        sellList.clear()
        if (sellSkeletonMasterCp.value) sellList.addAll(setSkeletonMasterChestplate)
        if (sellReviveStones.value) sellList.addAll(setReviveStones)
        if (sellDragonFragments.value) sellList.addAll(setDragonFragments)
        if (sellMobItems.value) sellList.addAll(setMobItems)
        if (sellRunes.value) sellList.addAll(setRunes)
        if (sellUselessOtherStuff.value) sellList.addAll(setUselessOtherStuff)
        if (sellCandycombs.value) sellList.addAll(setCandycombs)
        if (sellOtherSackItems.value) sellList.addAll(setOtherSackItems)
    }


    private var awaitingTrades = false

    override fun init() {
        register<ContainerFullyOpenedEvent> {
            if (!awaitingTrades) return@register
            if (!event.title.unformattedText.equals("Trades", true)) return@register
            awaitingTrades = false

            if (!ProtectItem.isSellMenu()){
                ChatUtils.modMessage("No Cookie Buff active")
                mc.player?.closeContainer()
                return@register
            }

            scope.launch {
                val container = mc.player?.containerMenu as? ChestMenu ?: return@launch

                delay(firstClickDelay.value.toLong().milliseconds)
                sellItems(container)
                mc.player?.closeContainer()
                ChatUtils.modMessage("&aDone selling!")
            }
        }
    }

    fun startSelling() {
        if(!enabled) return ChatUtils.modMessage("AutoSell not enabled")

        configureSellList()

        if (sellList.isEmpty()) return ChatUtils.modMessage("&cSell list is empty!")

        awaitingTrades = true
        ChatUtils.sendCommand("trades")

        scope.launch {
            delay(5000.milliseconds)
            if (awaitingTrades) {
                awaitingTrades = false
                ChatUtils.modMessage("&cTrades menu did not open!")
            }
        }
    }
    private suspend fun sellItems(container: ChestMenu){
        var found = true
        while (found) {
            found = false
            val slot = container.slots
                .takeLast(36)
                .firstOrNull { slot ->
                    if (slot.item.isEmpty) return@firstOrNull false
                    if (ProtectItem.getProtectType(slot.item)  != ProtectType.None) return@firstOrNull false
                    if (!sellRecombedItems.value && (slot.item.customData.getInt("rarity_upgrades").getOrDefault(0)>0)) return@firstOrNull false
                    if (!sell50percentItems.value && (slot.item.customData.getInt("baseStatBoostPercentage").getOrDefault(0)==50)) return@firstOrNull false
                    sellList.any { slot.item.hoverName.string.lowercase().contains(it.lowercase()) }
                }

            if (slot != null) {
                GuiUtils.clickSlot(slot.index, GuiUtils.ButtonType.LEFT)
                found = true
                delay(delayMs.value.toLong().milliseconds)
            }
        }
    }
}
//#endif