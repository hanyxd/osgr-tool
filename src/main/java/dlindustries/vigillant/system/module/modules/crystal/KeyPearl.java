package dlindustries.vigillant.system.module.modules.crystal;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.KeybindSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.InventoryUtils;
import dlindustries.vigillant.system.utils.KeyUtils;
import dlindustries.vigillant.system.utils.MathUtils;
import dlindustries.vigillant.system.utils.WorldUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.MaceItem;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;


public final class KeyPearl extends Module implements TickListener {

    private final KeybindSetting activateKey = new KeybindSetting(EncryptedString.of("Activate Key"), -1, false);
    private final NumberSetting delay = new NumberSetting(EncryptedString.of("Delay"), 0, 20, 1, 1);
    private final BooleanSetting switchBack = new BooleanSetting(EncryptedString.of("Switch Back"), true);
    private final NumberSetting switchslot = new NumberSetting(EncryptedString.of("Switch Slot"), 1, 9, 1, 1)
            .setDescription(EncryptedString.of("The slot it returns to after pearling (e.g., your sword slot)"));
    private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay"), 1, 20, 1, 1)
            .setDescription(EncryptedString.of("Delay after throwing pearl before switching back"));
    private final NumberSetting boostMultiplier = new NumberSetting(
            EncryptedString.of("Pearl Boost Multiplier"),
            1.0, 5.0, 1.5, 0.05
    ).setDescription(EncryptedString.of("How much faster pearls travel when thrown by this module (1.0 = normal)"));

    private final BooleanSetting boostFirstTickOnly = new BooleanSetting(
            EncryptedString.of("Only First Tick"), false
    ).setDescription(EncryptedString.of("Boost only applies on the first tick (recommended)"));

    private final KeybindSetting aggroPearlKey = new KeybindSetting(EncryptedString.of("Aggro Pearl Key"), -1, false);
    private final NumberSetting aggroLandingOffset = new NumberSetting(
            EncryptedString.of("Landing Offset"), -3.0, 3.0, -2.5, 0.1
    ).setDescription(EncryptedString.of("use -3 to -2"));

    private boolean active, hasActivated;
    private int clock, previousSlot, switchClock;
    private boolean aggroActive;
    private PlayerEntity aggroTarget;

    public KeyPearl() {
        super(EncryptedString.of("Pearl Optimizer"), EncryptedString.of("Optimizes and throws pearl for you"), -1, Category.CRYSTAL);
        addSettings(activateKey, aggroPearlKey, aggroLandingOffset, delay, switchBack, switchslot, switchDelay, boostMultiplier, boostFirstTickOnly);
    }

    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        PearlBoostAccessor.INSTANCE.reset();
        reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        PearlBoostAccessor.INSTANCE.reset();
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (mc.currentScreen != null) return;

        if (KeyUtils.isKeyPressed(activateKey.getKey())) {
            active = true;
        }

        if (KeyUtils.isKeyPressed(aggroPearlKey.getKey()) && !active) {
            PlayerEntity target = WorldUtils.findNearestPlayer(mc.player, 15f, true, true);
            if (target != null) {
                aggroTarget = target;
                aggroActive = true;
                active = true;
            }
        }

        if (!active) return;

        if (aggroActive && aggroTarget != null && !hasActivated) {
            updateAggroRotation();
        }

        if (previousSlot == -1)
            previousSlot = mc.player.getInventory().selectedSlot;
        applyBoost();

        InventoryUtils.selectItemFromHotbar(Items.ENDER_PEARL);

        if (clock < delay.getValueInt()) {
            clock++;
            return;
        }

        if (!hasActivated) {
            ActionResult result = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            if (result.isAccepted() && result.shouldSwingHand())
                mc.player.swingHand(Hand.MAIN_HAND);

            hasActivated = true;
        }

        if (switchBack.getValue() && !hasMaceAndShieldInHotbar())
            switchBack();
        else
            reset();
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

    private void applyBoost() {
        PearlBoostAccessor accessor = PearlBoostAccessor.INSTANCE;
        accessor.enabled = true;
        accessor.multiplier = boostMultiplier.getValue();
        accessor.firstTickOnly = boostFirstTickOnly.getValue();
    }

    private void switchBack() {
        if (switchClock < switchDelay.getValueInt()) {
            switchClock++;
            return;
        }

        int slot = Math.max(1, Math.min(9, switchslot.getValueInt())) - 1;
        InventoryUtils.setInvSlot(slot);
        reset();
    }

    private void updateAggroRotation() {
        if (aggroTarget == null || !aggroTarget.isAlive()) return;

        Vec3d playerPos = mc.player.getPos();
        Vec3d targetPos = aggroTarget.getPos();
        Vec3d targetVel = aggroTarget.getVelocity();

        double effectiveSpeed = 1.5 * boostMultiplier.getValue();
        double eyeHeight = 1.52; // vanilla pearl spawns at getEyeY() - 0.1
        double landingOffset = aggroLandingOffset.getValue();

        double dx = targetPos.x - playerPos.x;
        double dz = targetPos.z - playerPos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist < 1.0) return;

        // Direction from player to target (kept constant — player aims manually for lateral offset)
        double dirX = dx / dist;
        double dirZ = dz / dist;

        // Target radial velocity: positive = moving away, negative = moving towards
        double targetRadialVel = targetVel.x * dirX + targetVel.z * dirZ;

        // First pass: compute angles to current distance to estimate flight time
        double landingDist = Math.max(0.5, dist - landingOffset);
        double landX = dirX * landingDist;
        double landZ = dirZ * landingDist;

        // No player velocity passed — sprint boost adds naturally via vanilla pearl physics
        double[] angles = MathUtils.findPearlAngles(landX, landZ, 0, 0, eyeHeight, effectiveSpeed);
        if (angles == null) return;

        // Estimate flight time from first-pass simulation
        double[] sim = MathUtils.simulatePearlTrajectory(angles[1], angles[0], 0, 0, eyeHeight, effectiveSpeed);
        int flightTicks = (int) sim[2];

        // Predict target radial distance after flight time
        double predictedDist = dist + targetRadialVel * flightTicks;

        if (predictedDist < 1.0) return;

        // Recalculate landing position along same direction with predicted distance
        landingDist = Math.max(0.5, predictedDist - landingOffset);
        landX = dirX * landingDist;
        landZ = dirZ * landingDist;

        // Final angle computation — pitch adjusts for predicted distance
        angles = MathUtils.findPearlAngles(landX, landZ, 0, 0, eyeHeight, effectiveSpeed);
        if (angles == null) return;

        mc.player.setYaw((float) angles[0]);
        mc.player.setPitch((float) angles[1]);
    }

    private void reset() {
        PearlBoostAccessor.INSTANCE.reset();
        previousSlot = -1;
        clock = 0;
        switchClock = 0;
        active = false;
        hasActivated = false;
        aggroActive = false;
        aggroTarget = null;
    }


    public static final class PearlBoostAccessor {
        public static final PearlBoostAccessor INSTANCE = new PearlBoostAccessor();
        public boolean enabled = false;
        public double multiplier = 1.0;
        public boolean firstTickOnly = true;

        private PearlBoostAccessor() {}

        public void reset() {
            enabled = false;
            multiplier = 1.0;
            firstTickOnly = true;
        }
    }
}
