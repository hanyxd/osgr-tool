package dlindustries.vigillant.system.module.modules.crystal;

import dlindustries.vigillant.system.event.events.ItemUseListener;
import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.KeybindSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

public final class AnchorMacro extends Module implements TickListener, ItemUseListener {
    private final BooleanSetting whileUse = new BooleanSetting(EncryptedString.of("While Use"), false).setDescription(EncryptedString.of("If it should trigger while eating/using shield"));
    private final BooleanSetting LootProtect = new BooleanSetting(EncryptedString.of("loot protect"), false).setDescription(EncryptedString.of("Doesn't anchor if body nearby"));
    private final BooleanSetting clickSimulation = new BooleanSetting(EncryptedString.of("Click Simulation"), true).setDescription(EncryptedString.of("Makes the CPS hud think you're legit"));
    private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay"), 0, 20, 1, 1);
    private final NumberSetting switchChance = new NumberSetting(EncryptedString.of("Switch Chance"), 0, 100, 100, 1);
    private final NumberSetting placeChance = new NumberSetting(EncryptedString.of("Place Chance"), 0, 100, 100, 1).setDescription(EncryptedString.of("Randomization"));
    private final NumberSetting glowstoneDelay = new NumberSetting(EncryptedString.of("Glowstone Delay"), 0, 20, 0, 1);
    private final NumberSetting glowstoneChance = new NumberSetting(EncryptedString.of("Glowstone Chance"), 0, 100, 100, 1);

    // NEW: Double Glowstone Chance Setting
    private final NumberSetting doubleGlowstoneChance = new NumberSetting(EncryptedString.of("Double Glowstone Chance"), 0, 100, 5, 1).setDescription(EncryptedString.of("Chance to use two glowstone instead of one"));

    private final NumberSetting explodeDelay = new NumberSetting(EncryptedString.of("Explode Delay"), 0, 20, 1, 1);
    private final NumberSetting explodeChance = new NumberSetting(EncryptedString.of("Explode Chance"), 0, 100, 100, 1);
    private final NumberSetting explodeSlot = new NumberSetting(EncryptedString.of("Explode Slot"), 1, 9, 9, 1);
    private final BooleanSetting onlyOwn = new BooleanSetting(EncryptedString.of("Only Own"), false);
    private final BooleanSetting onlyCharge = new BooleanSetting(EncryptedString.of("Only Charge"), false);
    private final KeybindSetting activateKey = new KeybindSetting(EncryptedString.of("Activate Key"), GLFW.GLFW_MOUSE_BUTTON_RIGHT, false)
            .setDescription(EncryptedString.of("While held, attempt to use a Respawn Anchor hotbar slot to explode - for non airplace anchors ofcourse. make sure key repition is set to 0 so tick tracking works"));

    private int switchClock = 0;
    private int glowstoneClock = 0;
    private int explodeClock = 0;
    private final Set<BlockPos> ownedAnchors = new HashSet<>();
    private final Set<BlockPos> airplacedAnchors = new HashSet<>();
    private BlockPos lastExplodedPos = null;
    private long lastExplodeTime = 0;

    // New flag to track if we should force fast delay
    private boolean forceFastGlowstone = false;

    public AnchorMacro() {
        super(EncryptedString.of("Anchor Macro"),
                EncryptedString.of("Automatically blows up respawn anchors for you"),
                -1,
                Category.CRYSTAL);
        // Added doubleGlowstoneChance to the settings registration
        addSettings(whileUse, LootProtect, clickSimulation, placeChance, switchDelay, switchChance, glowstoneDelay, glowstoneChance, doubleGlowstoneChance, explodeDelay, explodeChance, explodeSlot, onlyOwn, onlyCharge, activateKey);
    }

    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        eventManager.add(ItemUseListener.class, this);
        switchClock = 0;
        glowstoneClock = 0;
        explodeClock = 0;
        lastExplodedPos = null;
        lastExplodeTime = 0;
        airplacedAnchors.clear();
        ownedAnchors.clear();
        forceFastGlowstone = false;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        eventManager.remove(ItemUseListener.class, this);
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (mc.currentScreen != null) return;

        if (!whileUse.getValue() && (mc.player.isUsingItem() ||
                (mc.player.getActiveItem().getItem().getComponents().contains(DataComponentTypes.FOOD) ||
                        mc.player.getActiveItem().getItem() instanceof ShieldItem))) {
            return;
        }

        if (LootProtect.getValue() && (WorldUtils.isDeadBodyNearby() || WorldUtils.isValuableLootNearby())) {
            return;
        }

        if (KeyUtils.isKeyPressed(GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
            if (mc.crosshairTarget instanceof BlockHitResult hit) {
                BlockPos targetPos = hit.getBlockPos();

                if (BlockUtils.isBlock(targetPos, Blocks.RESPAWN_ANCHOR)) {

                    if (onlyOwn.getValue()) {
                        boolean isOwned = ownedAnchors.contains(targetPos);
                        boolean isLastExploded = (lastExplodedPos != null && targetPos.equals(lastExplodedPos));
                        if (!isOwned && !isLastExploded) {
                            return;
                        }
                    }

                    mc.options.useKey.setPressed(false);

                    // --- CHARGING LOGIC ---
                    if (BlockUtils.isAnchorNotCharged(targetPos)) {
                        int randomInt = MathUtils.randomInt(1, 100);

                        if (randomInt <= placeChance.getValueInt()) {
                            if (!mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
                                if (switchClock != switchDelay.getValueInt()) {
                                    switchClock++;
                                    return;
                                }

                                if (MathUtils.randomInt(1, 100) <= switchChance.getValueInt()) {
                                    switchClock = 0;
                                    InventoryUtils.selectItemFromHotbar(Items.GLOWSTONE);
                                }
                            }

                            if (mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
                                // Logic: If forceFastGlowstone is active, we use delay 1, otherwise use setting
                                int currentDelay = forceFastGlowstone ? 1 : glowstoneDelay.getValueInt();

                                if (glowstoneClock != currentDelay) {
                                    glowstoneClock++;
                                    return;
                                }

                                if (MathUtils.randomInt(1, 100) <= glowstoneChance.getValueInt()) {
                                    glowstoneClock = 0;

                                    // Click Simulation works for charging normally
                                    if (clickSimulation.getValue())
                                        MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_RIGHT);

                                    WorldUtils.placeBlock(hit, true);

                                    // NEW: Double Glowstone Logic
                                    if (MathUtils.randomInt(1, 100) <= doubleGlowstoneChance.getValueInt()) {
                                        if (clickSimulation.getValue()) {
                                            MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
                                        }
                                        WorldUtils.placeBlock(hit, true);
                                    }

                                    // Reset the flag after successfully charging "the next anchor"
                                    if (forceFastGlowstone) {
                                        forceFastGlowstone = false;
                                    }
                                }
                            }
                        }
                    }

                    // --- EXPLODING LOGIC ---
                    if (BlockUtils.isAnchorCharged(targetPos)) {
                        int configuredSlot = explodeSlot.getValueInt() - 1;
                        boolean holdingAnchorInMainHand = mc.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR);

                        // !!! FIX: Check tracking set so 1st airplace is detected !!!
                        boolean isAirplaced = isAirplacedBySurroundings(targetPos) || airplacedAnchors.contains(targetPos);

                        boolean activateHeld = (activateKey.getKey() != -1 && KeyUtils.isKeyPressed(activateKey.getKey()));
                        int targetSlot = configuredSlot;

                        if (activateHeld && !isAirplaced) {
                            int respawnHotbarSlot = -1;
                            for (int i = 0; i < 9; i++) {
                                if (mc.player.getInventory().getStack(i).isOf(Items.RESPAWN_ANCHOR)) {
                                    respawnHotbarSlot = i;
                                    break;
                                }
                            }
                            if (respawnHotbarSlot != -1) {
                                targetSlot = respawnHotbarSlot;
                            }
                        }

                        if (isAirplaced && holdingAnchorInMainHand) {
                            holdingAnchorInMainHand = false;
                        }

                        if (!holdingAnchorInMainHand && mc.player.getInventory().selectedSlot != targetSlot) {
                            if (switchClock != switchDelay.getValueInt()) {
                                switchClock++;
                                return;
                            }
                            if (MathUtils.randomInt(1, 100) <= switchChance.getValueInt()) {
                                switchClock = 0;
                                mc.player.getInventory().selectedSlot = targetSlot;
                            } else {
                                return;
                            }
                        }

                        if (holdingAnchorInMainHand || mc.player.getInventory().selectedSlot == targetSlot) {
                            PlayerEntity target = TargetTracker.getTrackedPlayer();

                            if (target == null) {
                                target = mc.world.getClosestPlayer(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 10, false);
                                if (target == mc.player) target = null;
                            }

                            int currentExplodeDelay;

                            if (isAirplaced) {
                                boolean recentlyExploded = (System.currentTimeMillis() - lastExplodeTime) < 550;

                                if (recentlyExploded) {
                                    explodeClock = 0;
                                    return;
                                }

                                if (target != null && target.isAlive()) {
                                    boolean clientHurt = target.hurtTime > 0;
                                    boolean serverNotifiedHurt = TargetTracker.isWithinServerHurtWindow();
                                    boolean predictedHurt = TargetTracker.isWithinPredictedDamageWindow();

                                    // This now correctly pauses for the 1st airplace anchor
                                    if (clientHurt || serverNotifiedHurt || predictedHurt) {
                                        explodeClock = 0;
                                        return;
                                    }
                                }

                                currentExplodeDelay = 0;
                            } else {
                                currentExplodeDelay = explodeDelay.getValueInt();
                            }

                            if (currentExplodeDelay > 0) {
                                if (explodeClock != currentExplodeDelay) {
                                    explodeClock++;
                                    return;
                                }
                            } else {
                                explodeClock = 0;
                            }

                            if (MathUtils.randomInt(1, 100) <= explodeChance.getValueInt()) {
                                explodeClock = 0;

                                if (!onlyCharge.getValue()) {
                                    boolean shouldSimulateClick = clickSimulation.getValue() && !isAirplaced;

                                    if (shouldSimulateClick &&
                                            !(mc.player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING) ||
                                                    mc.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR))) {
                                        shouldSimulateClick = false;
                                    }

                                    if (shouldSimulateClick)
                                        MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_RIGHT);

                                    WorldUtils.placeBlock(hit, true);

                                    lastExplodedPos = targetPos;
                                    lastExplodeTime = System.currentTimeMillis();

                                    ownedAnchors.remove(targetPos);
                                    airplacedAnchors.remove(targetPos);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onItemUse(ItemUseEvent event) {
        if (mc.crosshairTarget instanceof BlockHitResult hitResult && hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hitResult.getBlockPos();

            // Detect normal Anchor placement logic
            if (mc.player.getMainHandStack().getItem() == Items.RESPAWN_ANCHOR) {
                Direction dir = hitResult.getSide();
                BlockPos placedPos;

                if (!mc.world.getBlockState(pos).isReplaceable()) {
                    placedPos = pos.offset(dir);
                } else {
                    placedPos = pos;
                }

                ownedAnchors.add(placedPos);

                // This logic identifies if the new anchor is in the "crater" of the last explosion
                if (isAirplacedBySurroundings(placedPos) || (lastExplodedPos != null && placedPos.equals(lastExplodedPos))) {
                    airplacedAnchors.add(placedPos);
                } else {
                    airplacedAnchors.remove(placedPos);
                }
            }
            // Logic: if glowstone is placed on anything that is NOT a respawn anchor, trigger fast delay
            else if (mc.player.getMainHandStack().getItem() == Items.GLOWSTONE) {
                if (!BlockUtils.isBlock(pos, Blocks.RESPAWN_ANCHOR)) {
                    forceFastGlowstone = true;
                }
            }
        }
    }

    private boolean isAirplacedBySurroundings(BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos np = pos.add(dx, dy, dz);
                    BlockState bs = mc.world.getBlockState(np);
                    if (bs.isAir()) continue;
                    if (bs.isOf(Blocks.OBSIDIAN) ||
                            bs.isOf(Blocks.BEDROCK) ||
                            bs.isOf(Blocks.REINFORCED_DEEPSLATE) ||
                            bs.isOf(Blocks.NETHERITE_BLOCK)) {
                        continue;
                    }
                    return false;
                }
            }
        }
        return true;
    }
}