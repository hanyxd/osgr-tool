package dlindustries.vigillant.system.module.modules.sword;

import dlindustries.vigillant.system.event.events.HudListener;
import dlindustries.vigillant.system.event.events.MouseMoveListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.MinMaxSetting;
import dlindustries.vigillant.system.module.setting.ModeSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.module.setting.StringSetting;
import dlindustries.vigillant.system.utils.*;
import dlindustries.vigillant.system.utils.rotation.Rotation;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

public final class AimAssist extends Module implements HudListener, MouseMoveListener {

    public enum WeaponMode {
        MACE_ONLY("Mace Only"),
        WEAPONS_ONLY("Weapons Only"),
        MACE_AND_WEAPONS("Mace and Weapons"),
        ONLY_AXE("Only Axe"),
        MACE_AND_AXE("Mace and Axe"),
        ALL("All Items");

        private final String name;

        WeaponMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final BooleanSetting aimSpear = new BooleanSetting(EncryptedString.of("Aim Spear"), false)
            .setDescription(EncryptedString.of("Aims at nearest player within 15 blocks when right-clicking a netherite sword or mace"));

    private final StringSetting spearItemName = new StringSetting(EncryptedString.of("Spear Item Name"), "1.21.11 Netherite Spear")
            .setDescription(EncryptedString.of("Only activates spear aim when the held netherite sword's name matches this"));

    private final BooleanSetting stickyAim = new BooleanSetting(EncryptedString.of("Sticky Aim"), false)
            .setDescription(EncryptedString.of("Aims at the last attacked player"));

    private final ModeSetting<WeaponMode> weaponMode = new ModeSetting<>(
            EncryptedString.of("Filter"),
            WeaponMode.WEAPONS_ONLY,
            WeaponMode.class
    ).setDescription(EncryptedString.of("Which items trigger aim assist"));

    private final BooleanSetting onLeftClick = new BooleanSetting(EncryptedString.of("On Left Click"), false)
            .setDescription(EncryptedString.of("Only gets triggered if holding down left click"));
    private final ModeSetting<AimMode> aimAt = new ModeSetting<>(EncryptedString.of("Aim At"), AimMode.Head, AimMode.class);

    private final BooleanSetting stopAtTargetVertical = new BooleanSetting(EncryptedString.of("Stop at Target Vert"), true)
            .setDescription(EncryptedString.of("Stops vertically assisting if already aiming at the entity, helps bypass anti-cheat"));

    private final BooleanSetting stopAtTargetHorizontal = new BooleanSetting(EncryptedString.of("Stop at Target Horiz"), false)
            .setDescription(EncryptedString.of("Stops horizontally assisting if already aiming at the entity, helps bypass anti-cheat"));

    private final NumberSetting radius = new NumberSetting(EncryptedString.of("Radius"), 0.1, 6, 5, 0.1);

    private final BooleanSetting seeOnly = new BooleanSetting(EncryptedString.of("See Only"), true);
    private final BooleanSetting lookAtNearest = new BooleanSetting(EncryptedString.of("Look at Nearest"), false);

    private final NumberSetting fov = new NumberSetting(EncryptedString.of("FOV"), 5, 360, 100, 1);

    private final MinMaxSetting pitchSpeed = new MinMaxSetting(EncryptedString.of("Vertical Speed"), 0, 10, 0.1, 2, 4);
    private final MinMaxSetting yawSpeed = new MinMaxSetting(EncryptedString.of("Horizontal Speed"), 0, 10, 0.1, 2, 4);

    private final NumberSetting speedChange = new NumberSetting(EncryptedString.of("Speed Delay"), 0, 1000, 250, 1)
            .setDescription(EncryptedString.of("Time in milliseconds to wait after resetting random speed"));

    private final NumberSetting randomization = new NumberSetting(EncryptedString.of("Chance"), 0, 100, 50, 1);

    private final BooleanSetting yawAssist = new BooleanSetting(EncryptedString.of("Horizontal"), true);
    private final BooleanSetting pitchAssist = new BooleanSetting(EncryptedString.of("Vertical"), true);

    private final NumberSetting waitFor = new NumberSetting(EncryptedString.of("Wait on Move"), 0, 1000, 0, 1)
            .setDescription(EncryptedString.of("After you move your mouse aim assist will stop working for the selected amount of time"));

    private final NumberSetting above = new NumberSetting(EncryptedString.of("Above"), 0, 6, 0, 0.1)
            .setDescription(EncryptedString.of("Aim assist will not run if your feet are within this many blocks of the ground"));

    private final ModeSetting<LerpMode> lerp = new ModeSetting<>(EncryptedString.of("Lerp"), LerpMode.Normal, LerpMode.class)
            .setDescription(EncryptedString.of("Linear interpolation to use to rotate"));

    private final ModeSetting<PosMode> posMode = new ModeSetting<>(EncryptedString.of("Pos mode"), PosMode.Normal, PosMode.class)
            .setDescription(EncryptedString.of("Precision of the target position"));

    private final TimerUtils timer = new TimerUtils();
    private final TimerUtils resetSpeed = new TimerUtils();
    private boolean move;
    private float pitch, yaw;

    @SuppressWarnings("unused")
    public enum PosMode {
        Normal, Lerped
    }

    public enum AimMode {
        Head, Chest, Legs
    }

    public enum LerpMode {
        Normal, Smoothstep, EaseOut
    }

    public AimAssist() {
        super(EncryptedString.of("Aim Assist"),
                EncryptedString.of("Automatically aims at players for you"),
                -1,
                Category.sword);

        addSettings(aimSpear, spearItemName, stickyAim, weaponMode, onLeftClick, aimAt, stopAtTargetVertical, stopAtTargetHorizontal,
                radius, seeOnly, lookAtNearest, fov, pitchSpeed, yawSpeed, speedChange,
                randomization, yawAssist, pitchAssist, waitFor, above, lerp, posMode);
    }

    @Override
    public void onEnable() {
        move = true;
        pitch = pitchSpeed.getRandomValueFloat();
        yaw = yawSpeed.getRandomValueFloat();

        eventManager.add(HudListener.class, this);
        eventManager.add(MouseMoveListener.class, this);

        timer.reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(HudListener.class, this);
        eventManager.remove(MouseMoveListener.class, this);
        super.onDisable();
    }

    @Override
    public void onRenderHud(HudEvent event) {
        if (timer.delay(waitFor.getValueFloat()) && !move) {
            move = true;
            timer.reset();
        }

        if (mc.player == null || mc.currentScreen != null)
            return;

        if (mc.world == null)
            return;

        if (above.getValue() > 0) {
            Vec3d start = mc.player.getPos();
            Vec3d end = start.add(0.0, -above.getValue(), 0.0);
            var groundCheck = mc.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
            if (groundCheck != null && groundCheck.getType() != net.minecraft.util.hit.HitResult.Type.MISS) {
                return;
            }
        }

        ItemStack heldStack = mc.player.getMainHandStack();
        Item heldItem = heldStack.getItem();

        boolean isSpearActive = aimSpear.getValue()
                && (
                (heldItem == Items.NETHERITE_SWORD && heldStack.getName().getString().strip().equals(spearItemName.getValue().strip()))
                        || heldItem instanceof MaceItem
        )
                && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        if (!isSpearActive) {
            switch (weaponMode.getMode()) {
                case MACE_ONLY:
                    if (!(heldItem instanceof MaceItem)) return;
                    break;
                case WEAPONS_ONLY:
                    if (!(heldItem instanceof SwordItem || heldItem instanceof AxeItem))
                        return;
                    break;
                case MACE_AND_WEAPONS:
                    if (!(heldItem instanceof SwordItem || heldItem instanceof AxeItem || heldItem instanceof MaceItem))
                        return;
                    break;
                case MACE_AND_AXE:
                    if (!(heldItem instanceof MaceItem || heldItem instanceof AxeItem))
                        return;
                    break;
                case ONLY_AXE:
                    if (!(heldItem instanceof AxeItem))
                        return;

                    boolean hasMace = false;
                    for (int i = 0; i < 9; i++) {
                        if (mc.player.getInventory().getStack(i).getItem() instanceof MaceItem) {
                            hasMace = true;
                            break;
                        }
                    }

                    if (!hasMace) return;
                    break;
                case ALL:
                    break;
            }

            if (onLeftClick.getValue() && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS)
                return;
        }

        PlayerEntity target;

        if (isSpearActive) {
            target = WorldUtils.findNearestPlayer(mc.player, 15.0f, seeOnly.getValue(), true);
        } else {
            target = WorldUtils.findNearestPlayer(mc.player, radius.getValueFloat(), seeOnly.getValue(), true);
            if (stickyAim.getValue() && mc.player.getAttacking() instanceof PlayerEntity player && player.distanceTo(mc.player) < radius.getValue())
                target = player;
        }

        if (target == null)
            return;

        if(resetSpeed.delay(speedChange.getValueFloat())) {
            pitch = pitchSpeed.getRandomValueFloat();
            yaw = yawSpeed.getRandomValueFloat();
            resetSpeed.reset();
        }

        // 1. Get the base position
        Vec3d basePos = posMode.isMode(PosMode.Normal) ? target.getPos() : target.getLerpedPos(RenderTickCounter.ONE.getTickDelta(true));

        float eyeHeight = target.getEyeHeight(target.getPose());
        float targetHeight = target.getHeight();

        double xOffset = 0;
        double yOffset = 0;
        double zOffset = 0;

        // 2. Adjust dynamically based on whether they are standing tall or flying/swimming horizontally
        if (targetHeight < 1.0f) {
            // Player is horizontal (Flying, Swimming, Crawling)
            // Get the horizontal direction they are facing to shift our aim along their body
            float yawRad = target.getYaw() * 0.017453292F;
            double lookX = -MathHelper.sin(yawRad);
            double lookZ = MathHelper.cos(yawRad);

            if (aimAt.isMode(AimMode.Head)) {
                // Shift aim to the front edge of the bounding box
                xOffset = lookX * 0.25;
                zOffset = lookZ * 0.25;
                yOffset = 0;
            }
            else if (aimAt.isMode(AimMode.Chest)) {
                // Keep dead center
                yOffset = -(eyeHeight - (targetHeight / 2.0f));
            }
            else if (aimAt.isMode(AimMode.Legs)) {
                // Shift aim to the back edge where the visual legs trail
                xOffset = -lookX * 0.25;
                zOffset = -lookZ * 0.25;
                yOffset = 0; // Removing the steep drop stops it from aiming at the ground
            }
        } else {
            // Player is standing vertically (Standard Math)
            if (aimAt.isMode(AimMode.Head)) {
                yOffset = 0;
            }
            else if (aimAt.isMode(AimMode.Chest)) {
                yOffset = -(eyeHeight - (targetHeight / 2.0f));
            }
            else if (aimAt.isMode(AimMode.Legs)) {
                yOffset = -(eyeHeight - (targetHeight * 0.2f));
            }
        }

        // Apply all dynamic offsets
        Vec3d targetPos = basePos.add(xOffset, yOffset, zOffset);

        Rotation rotation = RotationUtils.getDirection(mc.player, targetPos);

        double angleToRotation = RotationUtils.getAngleToRotation(rotation);
        if (angleToRotation > (double) fov.getValueInt() / 2)
            return;

        float yawStrength = yaw / 50;
        float pitchStrength = pitch / 50;

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        if (lerp.isMode(LerpMode.Smoothstep)) {
            currentYaw = (float) smoothStepLerp(yawStrength, mc.player.getYaw(), (float) rotation.yaw());
            currentPitch = (float) smoothStepLerp(pitchStrength, mc.player.getPitch(), (float) rotation.pitch());
        }
        if (lerp.isMode(LerpMode.Normal)) {
            currentYaw = lerp(yawStrength, mc.player.getYaw(), (float) (rotation.yaw()));
            currentPitch = lerp(pitchStrength, mc.player.getPitch(), (float) (rotation.pitch()));
        }

        if (lerp.isMode(LerpMode.EaseOut)) {
            currentYaw = (float) easeOutBackDegrees(mc.player.getYaw(), rotation.yaw(), yawStrength * RenderTickCounter.ONE.getLastFrameDuration());
            currentPitch = (float) easeOutBackDegrees(mc.player.getPitch(), rotation.pitch(), pitchStrength * RenderTickCounter.ONE.getLastFrameDuration());
        }

        if (MathUtils.randomInt(1, 100) <= randomization.getValueInt()) {
            if (move) {
                if (yawAssist.getValue()) {
                    if(stopAtTargetHorizontal.getValue() && WorldUtils.getHitResult(radius.getValue()) instanceof EntityHitResult hitResult && hitResult.getEntity() == target)
                        return;

                    mc.player.setYaw(currentYaw);
                }

                if (pitchAssist.getValue()) {
                    if(stopAtTargetVertical.getValue() && WorldUtils.getHitResult(radius.getValue()) instanceof EntityHitResult hitResult && hitResult.getEntity() == target)
                        return;

                    mc.player.setPitch(currentPitch);
                }
            }
        }
    }

    public float lerp(float delta, float start, float end) {
        return start + (MathHelper.wrapDegrees(end - start) * delta);
    }

    public static double easeOutBackDegrees(double start, double end, float speed) {
        double c1 = 1.70158;
        double c3 = 2.70158;
        double x = 1 - Math.pow(1 - (double) speed, 3);

        return start + MathHelper.wrapDegrees(end - start) * (1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2));
    }

    public double smoothStepLerp(double delta, double start, double end) {
        double value;
        delta = Math.max(0, Math.min(1, delta));

        double t = delta * delta * (3 - 2 * delta);

        value = start + MathHelper.wrapDegrees(end - start) * t;
        return value;
    }

    @Override
    public void onMouseMove(MouseMoveEvent event) {
        move = false;
        timer.reset();
    }
}