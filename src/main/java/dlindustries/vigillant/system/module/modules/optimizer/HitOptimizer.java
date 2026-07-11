package dlindustries.vigillant.system.module.modules.optimizer;

import dlindustries.vigillant.system.event.events.AttackListener;
import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.module.setting.StringSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.InventoryUtils;
import dlindustries.vigillant.system.utils.MouseSimulation;
import dlindustries.vigillant.system.utils.WorldUtils;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public final class HitOptimizer extends Module implements AttackListener, TickListener {

    private final BooleanSetting requireSword = new BooleanSetting(EncryptedString.of("Require Sword"), true)
            .setDescription(EncryptedString.of("Only switch if sword is available"));

    private final BooleanSetting switchBack = new BooleanSetting(EncryptedString.of("Switch Back"), true)
            .setDescription(EncryptedString.of("Switch back to original slot after attack"));

    private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay"), 0, 5, 1, 1)
            .setDescription(EncryptedString.of("Delay after attacking before switching back"));

    private final StringSetting spearItemName = new StringSetting(EncryptedString.of("Spear Item Name"), "1.21.11 Netherite Spear")
            .setDescription(EncryptedString.of("Only activates mid-air mace swap when the held netherite sword's name matches this"));

    private final BooleanSetting autoMace = new BooleanSetting(EncryptedString.of("Auto Mace"), false)
            .setDescription(EncryptedString.of("Attacks once immediately after swapping to mace if vanilla hit requirements are met"));

    private final NumberSetting mSwitchDelay = new NumberSetting(EncryptedString.of("MSwitchdelay"), 0, 1, 0, 1)
            .setDescription(EncryptedString.of("Delay (ticks) before swapping to mace in the air-mace swap logic"));

    private final NumberSetting range = new NumberSetting(EncryptedString.of("Range"), 1, 4.4, 3, 0.1)
            .setDescription(EncryptedString.of("Target distance for air-mace swap"));

    private boolean shouldSwitchBack;
    private int originalSlot = -1;
    private int switchTimer;
    private int pendingMaceSwitchTicks;
    private int pendingMaceSlot = -1;
    private boolean pendingMaceAutoAttack;
    private static volatile UUID pendingShieldDisableTarget = null;
    private static volatile boolean pendingShieldDisableUseMace = false;
    private static volatile int pendingShieldDisableMaceSlot = -1;
    private static volatile boolean pendingShieldDisableIsNormalStun = false;

    public static synchronized void requestShieldDisable(UUID target, boolean useMace, int maceSlot, boolean isNormalStun) {
        pendingShieldDisableTarget = target;
        pendingShieldDisableUseMace = useMace;
        pendingShieldDisableMaceSlot = maceSlot;
        pendingShieldDisableIsNormalStun = isNormalStun;
    }

    public static synchronized void clearShieldDisableRequest() {
        pendingShieldDisableTarget = null;
        pendingShieldDisableUseMace = false;
        pendingShieldDisableMaceSlot = -1;
        pendingShieldDisableIsNormalStun = false;
    }

    public HitOptimizer() {
        super(EncryptedString.of("Hit Optimizer"),
                EncryptedString.of("Automatically switches to sword for attacks / make sure they take KB"),
                -1,
                Category.optimizer);
        addSettings(requireSword, switchBack, switchDelay, spearItemName, autoMace, mSwitchDelay, range);
    }

    @Override
    public void onEnable() {
        eventManager.add(AttackListener.class, this);
        eventManager.add(TickListener.class, this);
        resetState();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(AttackListener.class, this);
        eventManager.remove(TickListener.class, this);
        if (shouldSwitchBack && originalSlot != -1) {
            mc.player.getInventory().selectedSlot = originalSlot;
        }
        clearShieldDisableRequest();
        super.onDisable();
    }

    @Override
    public void onAttack(AttackEvent event) {
        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.ENTITY) return;

        Entity target = ((EntityHitResult) mc.crosshairTarget).getEntity();
        if (target == null) return;

        // ADDED: If target is an End Crystal, ignore auto-switch logic
        if (target instanceof EndCrystalEntity) return;

        if (pendingShieldDisableTarget != null && pendingShieldDisableTarget.equals(target.getUuid())) {
            performShieldDisableSequence(target);
            return;
        }

        if (hasMaceAndShieldInHotbar()) {
            resetState();
            return;
        }

        ItemStack currentStack = mc.player.getMainHandStack();
        Item currentItem = currentStack.getItem();

        if (isWeapon(currentItem)) return;

        if (shouldSwitchBack) {
            switchTimer = 0;
            return;
        }

        if (requireSword.getValue()) {
            int swordSlot = findSwordSlot();
            if (swordSlot == -1) return;

            if (switchBack.getValue() && originalSlot == -1) {
                originalSlot = mc.player.getInventory().selectedSlot;
            }

            mc.player.getInventory().selectedSlot = swordSlot;

            if (switchBack.getValue()) {
                shouldSwitchBack = true;
                switchTimer = 0;
            }
        }
    }

    private void performShieldDisableSequence(Entity target) {
        if (switchBack.getValue() && originalSlot == -1) {
            originalSlot = mc.player.getInventory().selectedSlot;
        }
        if (pendingShieldDisableIsNormalStun) {
            performNormalStun(target);
        } else {
            performStunSlam(target);
        }
        if (switchBack.getValue()) {
            shouldSwitchBack = true;
            switchTimer = 0;
        }
        clearShieldDisableRequest();
    }

    private void performNormalStun(Entity target) {
        if (InventoryUtils.selectAxe()) {
            MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);
            WorldUtils.hitEntity(target, true);
        }
        int swordSlot = findSwordSlot();
        if (swordSlot != -1) {
            mc.player.getInventory().selectedSlot = swordSlot;
        }
    }

    private void performStunSlam(Entity target) {
        if (InventoryUtils.selectAxe()) {
            MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);
            WorldUtils.hitEntity(target, true);
        }
        if (pendingShieldDisableMaceSlot >= 1 && pendingShieldDisableMaceSlot <= 9) {
            mc.player.getInventory().selectedSlot = pendingShieldDisableMaceSlot - 1;
        } else {
            int maceSlot = InventoryUtils.getMaceSlot();
            if (maceSlot != -1) {
                mc.player.getInventory().selectedSlot = maceSlot;
            }
        }

        MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        WorldUtils.hitEntity(target, true);
    }

    @Override
    public void onTick() {
        if (pendingMaceSlot != -1) {
            if (pendingMaceSwitchTicks > 0) {
                pendingMaceSwitchTicks--;
            } else {
                mc.player.getInventory().selectedSlot = pendingMaceSlot;
                if (pendingMaceAutoAttack) {
                    attemptAutoMaceAttack();
                }
                pendingMaceSlot = -1;
                pendingMaceAutoAttack = false;
                resetState();
            }
        }

        // Handle the mid-air mace swap logic
        handleAirMaceSwap();

        if (!shouldSwitchBack || originalSlot == -1) return;

        if (hasMaceAndShieldInHotbar()) {
            resetState();
            return;
        }

        if (switchTimer < switchDelay.getValueInt()) {
            switchTimer++;
            return;
        }

        mc.player.getInventory().selectedSlot = originalSlot;
        resetState();
    }

    private void handleAirMaceSwap() {
        if (mc.player == null || mc.world == null) return;

        if (pendingMaceSlot != -1) return;

        // Condition 1: Must be holding right-click
        if (!mc.options.useKey.isPressed()) return;

        // Condition 2: Must be in the air
        if (mc.player.isOnGround()) return;

        // Condition 3: Must be holding a Netherite Sword
        ItemStack heldStack = mc.player.getMainHandStack();
        if (heldStack.getItem() != Items.NETHERITE_SWORD) return;
        if (!heldStack.getName().getString().strip().equals(spearItemName.getValue().strip())) return;

        // Condition 4: Crosshair must be on a Player entity
        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.ENTITY) return;
        Entity target = ((EntityHitResult) mc.crosshairTarget).getEntity();
        if (!(target instanceof PlayerEntity)) return;

        PlayerEntity targetPlayer = (PlayerEntity) target;

        if (!targetPlayer.isOnGround()) {
            Vec3d start = targetPlayer.getPos();
            Vec3d end = start.add(0.0, -1.1, 0.0);
            HitResult groundCheck = mc.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, targetPlayer));
            if (groundCheck == null || groundCheck.getType() == HitResult.Type.MISS) {
                return;
            }
        }

        float targetDistance = mc.player.distanceTo((PlayerEntity) target);
        if (targetDistance > range.getValue()) return;

        Item equippedChestItem = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem();
        boolean isWearingChestplate = equippedChestItem instanceof ArmorItem armor && armor.getSlotType() == EquipmentSlot.CHEST;
        boolean isWearingElytra = equippedChestItem instanceof ElytraItem;

        if (!isWearingChestplate || isWearingElytra) {
            int chestplateSlot = -1;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                Item item = stack.getItem();
                if (item instanceof ArmorItem armor && armor.getSlotType() == EquipmentSlot.CHEST && !(item instanceof ElytraItem)) {
                    chestplateSlot = i;
                    break;
                }
            }

            if (chestplateSlot == -1) return;

            int originalSelected = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = chestplateSlot;
            mc.interactionManager.interactItem(mc.player, net.minecraft.util.Hand.MAIN_HAND);
            mc.player.getInventory().selectedSlot = originalSelected;

            Item afterEquipChestItem = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem();
            boolean afterIsChestplate = afterEquipChestItem instanceof ArmorItem armor && armor.getSlotType() == EquipmentSlot.CHEST;
            boolean afterIsElytra = afterEquipChestItem instanceof ElytraItem;
            if (!afterIsChestplate || afterIsElytra) return;
        }

        if (targetDistance > range.getValue()) return;

        // Find Mace in Hotbar (Slots 0-8)
        int maceSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof MaceItem) {
                maceSlot = i;
                break;
            }
        }

        // If Mace is found, switch to it immediately and DO NOT switch back
        if (maceSlot != -1) {
            int delayTicks = Math.max(0, Math.min(1, mSwitchDelay.getValueInt()));
            if (delayTicks == 0) {
                mc.player.getInventory().selectedSlot = maceSlot;
                if (autoMace.getValue()) {
                    attemptAutoMaceAttack();
                }
                resetState(); // Clear any existing switch-back queue to ensure it stays on the mace
            } else {
                pendingMaceSlot = maceSlot;
                pendingMaceSwitchTicks = delayTicks;
                pendingMaceAutoAttack = autoMace.getValue();
            }
        }
    }

    private void attemptAutoMaceAttack() {
        if (mc.player == null || mc.world == null) return;
        if (!(mc.player.getMainHandStack().getItem() instanceof MaceItem)) return;
        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.ENTITY) return;
        if (!(mc.crosshairTarget instanceof EntityHitResult hit)) return;
        if (!(hit.getEntity() instanceof PlayerEntity targetPlayer)) return;
        if (mc.player.distanceTo(targetPlayer) > 3.0) return;

        if (mc.player.getAttackCooldownProgress(0.0f) < 1.0f) return;

        WorldUtils.hitEntity(targetPlayer, true);
    }

    private void resetState() {
        shouldSwitchBack = false;
        originalSlot = -1;
        switchTimer = 0;
        pendingMaceSwitchTicks = 0;
        pendingMaceSlot = -1;
        pendingMaceAutoAttack = false;
    }

    private boolean hasMaceAndShieldInHotbar() {
        if (mc.player == null) return false;

        boolean hasMace = false;
        boolean hasShield = false;

        for (int i = 0; i < 9; i++) {
            Item item = mc.player.getInventory().getStack(i).getItem();
            if (item instanceof MaceItem) hasMace = true;
            if (item instanceof ShieldItem) hasShield = true;
            if (hasMace && hasShield) return true;
        }

        return false;
    }

    private boolean isWeapon(Item item) {
        return item instanceof SwordItem ||
                item instanceof AxeItem ||
                item instanceof MaceItem ||
                item instanceof ElytraItem ||
                (item instanceof ArmorItem && ((ArmorItem) item).getSlotType() == EquipmentSlot.CHEST);
    }

    private int findSwordSlot() {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (stack.getItem() instanceof SwordItem) {
                return slot;
            }
        }
        return -1;
    }
}