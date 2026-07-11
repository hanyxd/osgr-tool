package dlindustries.vigillant.system.module.modules.crystal;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.KeybindSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

import java.util.function.Predicate;

public final class UtilitySwapper extends Module implements TickListener {

    // Global flag to tell other modules (AutoTotem/Dhand) to pause
    public static boolean isSwapping = false;

    private final NumberSetting slotSwitchDelay = new NumberSetting(EncryptedString.of("Slot Switch Delay"), 0, 20, 0, 1)
            .setDescription(EncryptedString.of("Delay in ticks before switching hotbar slots"));

    private final NumberSetting itemPickupDelay = new NumberSetting(EncryptedString.of("Item Pickup Delay"), 0, 20, 1, 1)
            .setDescription(EncryptedString.of("Delay in ticks before moving items in the inventory"));

    private final NumberSetting utilitySlot = new NumberSetting(EncryptedString.of("Utility Slot"), 1, 9, 9, 1)
            .setDescription(EncryptedString.of("The hotbar slot (1-9) where items will be swapped to"));

    private final KeybindSetting shieldActivation = new KeybindSetting(EncryptedString.of("Shield Activation"), GLFW.GLFW_KEY_UNKNOWN, false)
            .setDescription(EncryptedString.of("Hold to swap to and use a shield"));

    private final KeybindSetting axeSwap = new KeybindSetting(EncryptedString.of("Axe Swap"), GLFW.GLFW_KEY_UNKNOWN, false)
            .setDescription(EncryptedString.of("Press/Hold to ensure an axe is in the utility slot"));

    private int switchTimer = 0;
    private int pickupTimer = 0;

    public UtilitySwapper() {
        super(EncryptedString.of("Utility Swapper"),
                EncryptedString.of("Automatically manages shields and axes in your inventory with coordinated swapping."),
                -1,
                Category.CRYSTAL);
        addSettings(slotSwitchDelay, itemPickupDelay, utilitySlot, shieldActivation, axeSwap);
    }

    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        switchTimer = 0;
        pickupTimer = 0;
        isSwapping = false;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        mc.options.useKey.setPressed(false);
        isSwapping = false;
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        // Safety: If user is typing or in a menu we didn't open, reset state and do nothing.
        if (!isScreenSafe()) {
            isSwapping = false;
            return;
        }

        if (switchTimer > 0) switchTimer--;
        if (pickupTimer > 0) pickupTimer--;

        boolean shieldPressed = isKeyPressed(shieldActivation);
        boolean axePressed = isKeyPressed(axeSwap);

        if (shieldPressed) {
            handleShield();
        } else if (axePressed) {
            // Stop blocking if we were blocking
            if (mc.player.isUsingItem() && mc.player.getActiveItem().getItem() instanceof ShieldItem) {
                mc.options.useKey.setPressed(false);
            }
            handleAxe();
        } else {
            // Reset state
            if (mc.player.getMainHandStack().getItem() instanceof ShieldItem || mc.player.getOffHandStack().getItem() instanceof ShieldItem) {
                mc.options.useKey.setPressed(false);
            }
            isSwapping = false;
        }
    }

    private void handleShield() {
        // 1. Offhand Check
        if (mc.player.getOffHandStack().getItem() == Items.SHIELD) {
            mc.options.useKey.setPressed(true);
            return;
        }

        // 2. Hotbar Check
        int shieldInHotbar = findSlotInHotbar(item -> item.getItem() == Items.SHIELD);

        if (shieldInHotbar != -1) {
            // Switch to hotbar slot
            if (mc.player.getInventory().selectedSlot != shieldInHotbar) {
                // Do not switch if inventory is currently open
                if (mc.currentScreen == null && switchTimer <= 0) {
                    mc.player.getInventory().selectedSlot = shieldInHotbar;
                    switchTimer = slotSwitchDelay.getValueInt();
                }
            } else {
                // Block if screen is closed
                if (mc.currentScreen == null) {
                    mc.options.useKey.setPressed(true);
                }
            }
        } else {
            // 3. Inventory Swap
            performInventorySwap(item -> item.getItem() == Items.SHIELD);
        }
    }

    private void handleAxe() {
        // 1. Hotbar Check
        int axeInHotbar = findSlotInHotbar(item -> item.getItem() instanceof AxeItem);

        if (axeInHotbar != -1) {
            // Axe is already ready. Stop swapping state.
            isSwapping = false;
            return;
        }

        // 2. Inventory Swap
        performInventorySwap(item -> item.getItem() instanceof AxeItem);
    }

    private void performInventorySwap(Predicate<ItemStack> itemFinder) {
        // Step 1: Open Inventory
        if (!(mc.currentScreen instanceof InventoryScreen)) {
            if (!isSwapping) {
                isSwapping = true; // Signal AutoTotem to STOP
                mc.setScreen(new InventoryScreen(mc.player));
                pickupTimer = itemPickupDelay.getValueInt();
            }
            return;
        }

        // Step 2: Wait
        if (pickupTimer > 0) return;

        // Step 3: Find & Swap
        int sourceSlot = -1;
        for (int i = 9; i < 36; i++) {
            if (itemFinder.test(mc.player.getInventory().main.get(i))) {
                sourceSlot = i;
                break;
            }
        }

        if (sourceSlot != -1) {
            int targetHotbarIndex = utilitySlot.getValueInt() - 1;

            mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    sourceSlot,
                    targetHotbarIndex,
                    SlotActionType.SWAP,
                    mc.player
            );

            // Step 4: Close and cleanup
            mc.currentScreen.close();
            isSwapping = false; // Release lock
            switchTimer = slotSwitchDelay.getValueInt();
        } else {
            // Item not found
            mc.currentScreen.close();
            isSwapping = false;
        }
    }

    private int findSlotInHotbar(Predicate<ItemStack> predicate) {
        for (int i = 0; i < 9; i++) {
            if (predicate.test(mc.player.getInventory().getStack(i))) {
                return i;
            }
        }
        return -1;
    }

    private boolean isScreenSafe() {
        if (mc.currentScreen == null) return true;
        // Safe if WE opened the inventory
        if (mc.currentScreen instanceof InventoryScreen && isSwapping) return true;
        // Unsafe if user is typing or in other menus
        if (mc.currentScreen instanceof ChatScreen ||
                mc.currentScreen instanceof AnvilScreen ||
                mc.currentScreen instanceof SignEditScreen ||
                mc.currentScreen instanceof CreativeInventoryScreen) {
            return false;
        }
        return false;
    }

    private boolean isKeyPressed(KeybindSetting setting) {
        int key = setting.getKey();
        if (key == -1 || key == GLFW.GLFW_KEY_UNKNOWN) return false;
        if (key < 8) {
            return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
        }
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
    }
}