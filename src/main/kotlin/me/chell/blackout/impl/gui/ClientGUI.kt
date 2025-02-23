package me.chell.blackout.impl.gui

import com.mojang.blaze3d.systems.RenderSystem
import me.chell.blackout.api.event.EventHandler
import me.chell.blackout.api.event.EventManager
import me.chell.blackout.api.events.InputEvent
import me.chell.blackout.api.feature.Category
import me.chell.blackout.api.util.*
import me.chell.blackout.impl.features.client.GuiFeature
import me.chell.blackout.impl.gui.tabs.CategoryTab
import me.chell.blackout.impl.gui.tabs.FriendsTab
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.client.util.InputUtil
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.lwjgl.glfw.GLFW
import java.awt.Color

object ClientGUI: Screen(Text.literal("$modName GUI")) {

    private val bannerTexture = Identifier(modId, "textures/gui/banner.png")

    private val background: Int = Color(100, 100, 100, 150).rgb
    private val color = Color(161, 0, 255).rgb

    private val tabs = mutableListOf<Tab>()
    lateinit var currentTab: Tab

    private var animationTicks = 0
    private val animationLength = 5
    private var closing = false

    private val x = 0
    private val y = 0
    val uiWidth = 300
    private val uiHeight get() = mc.window.scaledHeight
    val bannerHeight = 75

    var hoveredItem: Description? = null
    private val descX get() = x + Tab.size
    val descY get () = y + uiHeight - (mc.textRenderer.fontHeight * 3) - (descPadding * 2)
    private val descPadding = 5f

    private val updateText = "Version ${Updater.latestVersion} available! Click here to update and restart."

    fun clientInit() {
        var tabY = bannerHeight+1

        for(category in Category.values()) {
            tabs.add(CategoryTab(category, 0, tabY, this))
            tabY += Tab.size
        }

        tabs.add(FriendsTab(0, tabY, this))
        tabY += Tab.size

        tabs.add(Tab(0, tabY, this, Category.Misc.icon)) // waypoints
        tabY += Tab.size
        tabs.add(Tab(0, tabY, this, Category.Misc.icon)) // macros
        tabY += Tab.size

        currentTab = tabs[0]
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // animation linear interpolation
        val scissorWidth = if(animationTicks > 0) {
            val t = if (closing) animationTicks + delta else animationLength - animationTicks + delta
            (lerp(x.toFloat(), x + uiWidth.toFloat(), t) / animationLength).toInt()
        } else {
            uiWidth
        }

        hoveredItem = null

        context.enableScissor(x, y, x + scissorWidth, y + uiHeight)

        // background
        context.fill(x, y, x + uiWidth, y + uiHeight, background)

        // banner
        RenderSystem.setShaderTexture(0, bannerTexture)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.enableDepthTest()
        context.drawTexture(bannerTexture, x, y, 0f, 0f, uiWidth, bannerHeight, uiWidth, bannerHeight)

        // line under banner
        context.drawHorizontalLine(x, x + uiWidth, bannerHeight, color)

        val mouseTab = mouseX <= x + Tab.size

        // tabs
        for(tab in tabs) {
            tab.render(context, mouseX, mouseY, delta)

            if(mouseTab && tab is CategoryTab && mouseY >= tab.y && mouseY <= tab.y + Tab.size)
                hoveredItem = tab.category
        }

        if(!mouseTab) {

        }

        // line next to icons
        context.drawVerticalLine(Tab.size, bannerHeight, y + uiHeight, color)

        context.drawHorizontalLine(descX, x + uiWidth, descY.toInt(), color)

        context.drawTrimmedTextWithShadow(hoveredItem?.description ?: "Hover over an item to see it's description.", descX + descPadding, descY + descPadding, (uiWidth - Tab.size - 1 - descPadding - descPadding).toInt(), -1)

        context.disableScissor()

        Console.render(context, mouseX, mouseY, delta)

        if(Updater.updateAvailable)
            context.drawTextWithShadow(textRenderer, updateText, width - textRenderer.getWidth(updateText) - 2, 2, Rainbow.color.rgb)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if(animationTicks > 0) return false

        if(Console.mouseClicked(mouseX, mouseY, button)) return true

        for(tab in tabs) {
            if(tab.mouseClicked(mouseX, mouseY, button)) return true
        }

        if(Updater.updateAvailable && mouseX >= width - textRenderer.getWidth(updateText) - 2f && mouseY <= textRenderer.fontHeight + 4f)
            Updater.update()

        return false
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if(Console.mouseReleased(mouseX, mouseY, button)) return true

        for(tab in tabs) {
            if(tab.mouseReleased(mouseX, mouseY, button)) return true
        }

        return false
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if(Console.keyPressed(keyCode, scanCode, modifiers)) return true

        for(tab in tabs) {
            if(tab.keyPressed(keyCode, scanCode, modifiers)) return true
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close()
            return true
        }
        return false
    }

    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        if(Console.charTyped(chr, modifiers)) return true

        for(tab in tabs) {
            if(tab.charTyped(chr, modifiers)) return true
        }

        return false
    }

    @EventHandler
    fun onScroll(event: InputEvent.Scroll) {
        if(Console.mouseScrolled(mc.mouse.x, mc.mouse.y, event.amount)) return

        for(tab in tabs) {
            if(tab.mouseScrolled(mc.mouse.x, mc.mouse.y, event.amount)) return
        }
    }

    /*
    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if(Console.mouseScrolled(mouseX, mouseY, verticalAmount)) return true

        for(tab in tabs) {
            if(tab.mouseScrolled(mouseX, mouseY, verticalAmount)) return true
        }

        return false
    }
    */

    override fun init() {
        closing = false
        animationTicks = animationLength
        mc.soundManager.play(PositionedSoundInstance.master(SoundEvents.UI_TOAST_IN, 1.0f, 1.0f))
        EventManager.register(this)
    }

    override fun tick() {
        if(animationTicks > 0) {
            animationTicks--
            if(closing && animationTicks == 0) mc.setScreen(null)
        } else {
            Console.tick()
        }
    }

    override fun close() {
        EventManager.unregister(this)
        for(tab in tabs) {
            tab.onClose()
        }

        if(GuiFeature.mainSetting.value.key.code == GLFW.GLFW_KEY_UNKNOWN)
            GuiFeature.mainSetting.value.setKey(InputUtil.GLFW_KEY_BACKSLASH, InputUtil.Type.KEYSYM)

        closing = true
        animationTicks = animationLength
        mc.soundManager.play(PositionedSoundInstance.master(SoundEvents.UI_TOAST_OUT, 1.0f, 1.0f))
    }

    override fun resize(client: MinecraftClient?, width: Int, height: Int) {
    }

    override fun shouldPause() = false

    private fun lerp(a: Float, b: Float, t: Float) = a * (1 - t) + b * t

    ////////////////////////////////////////////////////////////////////////////////////////

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return super.isMouseOver(mouseX, mouseY)
    }

    override fun shouldCloseOnEsc(): Boolean {
        return super.shouldCloseOnEsc()
    }

    override fun clearAndInit() {
        super.clearAndInit()
    }

    override fun removed() {
        super.removed()
    }

    override fun renderBackground(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        super.renderBackground(context, mouseX, mouseY, delta)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return super.keyReleased(keyCode, scanCode, modifiers)
    }
}