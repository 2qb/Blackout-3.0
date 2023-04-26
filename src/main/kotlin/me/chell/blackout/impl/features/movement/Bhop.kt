package me.chell.blackout.impl.features.movement

import me.chell.blackout.api.event.EventHandler
import me.chell.blackout.api.event.EventManager
import me.chell.blackout.api.events.PlayerTickEvent
import me.chell.blackout.api.feature.Category
import me.chell.blackout.api.feature.Feature
import me.chell.blackout.api.setting.Bind
import me.chell.blackout.api.setting.Setting
import me.chell.blackout.api.util.mc
import me.chell.blackout.api.util.player

object Bhop: Feature("Auto Jump", Category.Movement) {

    override var description = "Jump when you're moving"

    override val mainSetting = Setting("Enabled", Bind.Toggle(onEnable = { onEnable() }, onDisable = { onDisable() }))

    private fun onEnable() {
        EventManager.register(this)
    }

    private fun onDisable() {
        EventManager.unregister(this)
    }

    @EventHandler
    fun onPlayerTick(event: PlayerTickEvent) {
        if((player.forwardSpeed != 0f || player.sidewaysSpeed != 0f) && player.isOnGround && !mc.options.jumpKey.isPressed && !player.isRiding && !player.isTouchingWater && !player.isSubmergedInWater)
            player.jump()
    }

}