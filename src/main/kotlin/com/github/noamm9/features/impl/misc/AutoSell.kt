package com.github.noamm9.features.impl.misc

import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils

object AutoSell: Feature("Automatic Item Sell to Trades Menu") {

    fun startSelling(){
        ChatUtils.modMessage("Command autosell triggered.")
    }
}