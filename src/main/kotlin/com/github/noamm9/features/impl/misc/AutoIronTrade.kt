package com.github.noamm9.features.impl.misc

//#if CHEAT

import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.config.types.DropdownSetting
import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.Utils.send
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.client.gui.screens.inventory.MerchantScreen
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.MerchantMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.trading.MerchantOffer
import kotlin.time.Duration.Companion.milliseconds

/**
 * Vanilla feature - has nothing to do with Hypixel Skyblock.
 * Scans every villager/wandering trader menu for an iron trade and runs it automatically.
 */
object AutoIronTrade: Feature("Automatically completes iron trades when a villager trading menu is opened.") {
    private val tradeMode by DropdownSetting("Trade Type", 0, listOf("Sell Iron", "Buy Iron", "Any"))
        .withDescription("Sell Iron = iron is the payment. Buy Iron = iron is the reward.")

    private val startDelay by SliderSetting("Start Delay", 300f, 0f, 2000f, 50f, "ms")
        .withDescription("Wait time after the menu opened before the first click.")
    private val clickDelay by SliderSetting("Click Delay", 150f, 50f, 1000f, 10f, "ms")
        .withDescription("Delay between each trade step.")
    private val maxTrades by SliderSetting("Max Trades", 64, 1, 512, 1)
        .withDescription("Stop after this many completed trades.")

    private val useIngot by ToggleSetting("Iron Ingot", true).section("Counts as Iron")
    private val useBlock by ToggleSetting("Iron Block", true)
    private val useNugget by ToggleSetting("Iron Nugget", false)
    private val useRaw by ToggleSetting("Raw Iron", false)

    private val closeAfter by ToggleSetting("Close Menu When Done", true).section("Misc")
    private val announce by ToggleSetting("Chat Feedback", true)

    private const val PAYMENT_1 = 0
    private const val PAYMENT_2 = 1
    private const val RESULT_SLOT = 2

    private var busy = false

    override fun init() {
        register<ContainerEvent.Open> {
            val screen = event.screen
            if (screen !is MerchantScreen) return@register
            if (busy) return@register

            val menu = screen.menu as? MerchantMenu ?: return@register
            busy = true

            scope.launch {
                try { runTrades(menu) }
                catch (e: Exception) { ChatUtils.modMessage("&cAutoIronTrade failed: ${e.message}") }
                finally { busy = false }
            }
        }

        register<ContainerEvent.Close> { busy = false }
    }

    private suspend fun runTrades(menu: MerchantMenu) {
        // offers can arrive a tick or two after the screen was built
        var waited = 0
        while (menu.offers.isEmpty() && waited < 2000) {
            delay(50.milliseconds)
            waited += 50
        }

        if (! isStillOpen(menu)) return

        val index = menu.offers.indexOfFirst { isIronTrade(it) && ! it.isOutOfStock }
        if (index == - 1) {
            if (announce.value) ChatUtils.modMessage("&cNo iron trade found in this menu.")
            return
        }

        if (announce.value) ChatUtils.modMessage("&aIron trade found in slot ${index + 1}, trading...")
        delay(startDelay.value.toLong().milliseconds)

        var completed = 0
        while (completed < maxTrades.value) {
            if (! isStillOpen(menu)) break

            val offer = menu.offers.getOrNull(index) ?: break
            if (offer.isOutOfStock) break

            selectTrade(menu, index)
            delay(clickDelay.value.toLong().milliseconds)

            // not enough materials in the inventory -> payment slots stay empty
            if (menu.getSlot(PAYMENT_1).item.isEmpty && menu.getSlot(PAYMENT_2).item.isEmpty) break
            if (menu.getSlot(RESULT_SLOT).item.isEmpty) break

            takeResult(menu)
            completed ++
            delay(clickDelay.value.toLong().milliseconds)
        }

        if (announce.value) {
            if (completed > 0) ChatUtils.modMessage("&aCompleted &b$completed &atrade(s).")
            else ChatUtils.modMessage("&cCould not trade - out of stock or missing items.")
        }

        if (closeAfter.value && completed > 0) ThreadUtils.runOnMcThread { mc.player?.closeContainer() }
    }

    private fun selectTrade(menu: MerchantMenu, index: Int) = ThreadUtils.runOnMcThread {
        menu.setSelectionHint(index)
        menu.tryMoveItems(index)
        ServerboundSelectTradePacket(index).send()
    }

    private fun takeResult(menu: MerchantMenu) = ThreadUtils.runOnMcThread {
        val player = mc.player ?: return@runOnMcThread
        mc.gameMode?.handleContainerInput(menu.containerId, RESULT_SLOT, 0, ContainerInput.QUICK_MOVE, player)
    }

    private fun isStillOpen(menu: MerchantMenu) = mc.screen is MerchantScreen && mc.player?.containerMenu === menu

    private fun isIronTrade(offer: MerchantOffer) = when (tradeMode.value) {
        0 -> offer.costA.isIron || offer.costB.isIron
        1 -> offer.result.isIron
        else -> offer.costA.isIron || offer.costB.isIron || offer.result.isIron
    }

    private val ItemStack.isIron: Boolean
        get() {
            if (isEmpty) return false
            return (useIngot.value && `is`(Items.IRON_INGOT))
                    || (useBlock.value && `is`(Items.IRON_BLOCK))
                    || (useNugget.value && `is`(Items.IRON_NUGGET))
                    || (useRaw.value && (`is`(Items.RAW_IRON) || `is`(Items.RAW_IRON_BLOCK)))
        }
}

//#endif