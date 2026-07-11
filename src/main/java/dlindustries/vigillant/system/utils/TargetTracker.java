package dlindustries.vigillant.system.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

import java.util.UUID;

/**
 * Damage tick util
 */
public final class TargetTracker {
    private static volatile UUID lastAttackedUuid = null;
    private static volatile long lastAttackTimeMs = 0L;

    // Timestamp the client received the server hurt packet for the tracked UUID
    private static volatile long lastServerHurtTimeMs = 0L;

    private static final long ATTACK_TRACK_TIMEOUT_MS = 4000L;
    private static final long DAMAGE_TICK_MS = 500L;
    private static final long DAMAGE_TICK_BUFFER_MS = 120L;

    private TargetTracker() {}

    public static void setLastAttacked(UUID uuid) {
        lastAttackedUuid = uuid;
        lastAttackTimeMs = System.currentTimeMillis();
        lastServerHurtTimeMs = 0L;
    }

    public static void onEntityHurt(UUID uuid) {
        if (uuid == null) return;

        if (lastAttackedUuid != null && lastAttackedUuid.equals(uuid)) {
            lastServerHurtTimeMs = System.currentTimeMillis();
        }
    }

    public static PlayerEntity getTrackedPlayer() {
        if (lastAttackedUuid == null) return null;
        if (System.currentTimeMillis() - lastAttackTimeMs > ATTACK_TRACK_TIMEOUT_MS) return null;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return null;
        PlayerEntity p = mc.world.getPlayerByUuid(lastAttackedUuid);
        if (p == null || !p.isAlive()) return null;
        if (mc.player == null) return null;
        double sq = p.squaredDistanceTo(mc.player);
        if (sq > 36) return null;
        return p;
    }

    public static void clear() {
        lastAttackedUuid = null;
        lastAttackTimeMs = 0L;
        lastServerHurtTimeMs = 0L;
    }

    public static boolean isWithinPredictedDamageWindow() {
        if (lastAttackedUuid == null) return false;
        long elapsed = System.currentTimeMillis() - lastAttackTimeMs;
        return elapsed >= 0 && elapsed <= (DAMAGE_TICK_MS + DAMAGE_TICK_BUFFER_MS);
    }

    public static boolean isWithinServerHurtWindow() {
        if (lastServerHurtTimeMs == 0L) return false;
        long elapsed = System.currentTimeMillis() - lastServerHurtTimeMs;
        return elapsed >= 0 && elapsed <= (DAMAGE_TICK_MS + DAMAGE_TICK_BUFFER_MS);
    }

    public static long predictedDamageWindowRemainingMs() {
        if (lastAttackedUuid == null) return 0L;
        long remaining = (DAMAGE_TICK_MS + DAMAGE_TICK_BUFFER_MS) - (System.currentTimeMillis() - lastAttackTimeMs);
        return Math.max(0L, remaining);
    }

    public static long serverHurtWindowRemainingMs() {
        if (lastServerHurtTimeMs == 0L) return 0L;
        long remaining = (DAMAGE_TICK_MS + DAMAGE_TICK_BUFFER_MS) - (System.currentTimeMillis() - lastServerHurtTimeMs);
        return Math.max(0L, remaining);
    }
}