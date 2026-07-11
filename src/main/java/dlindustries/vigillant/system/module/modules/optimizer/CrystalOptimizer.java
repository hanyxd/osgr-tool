package dlindustries.vigillant.system.module.modules.optimizer;

import dlindustries.vigillant.system.event.events.PacketSendListener;
import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.WorldUtils;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class CrystalOptimizer extends Module implements PacketSendListener, TickListener {
	private final Map<UUID, Integer> pendingCrystals = new HashMap<>();

	public CrystalOptimizer() {
		super(EncryptedString.of("Crystal Optimizer"),
				EncryptedString.of("Marlowww based crystal optimizer - crystals faster"),
				-1,
				Category.optimizer);
	}

	@Override
	public void onEnable() {
		eventManager.add(PacketSendListener.class, this);
		eventManager.add(TickListener.class, this);
		pendingCrystals.clear();
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(PacketSendListener.class, this);
		eventManager.remove(TickListener.class, this);
		pendingCrystals.clear();
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.world == null) return;
		if (pendingCrystals.isEmpty()) return;

		Iterator<Map.Entry<UUID, Integer>> it = pendingCrystals.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Integer> entry = it.next();
			int ticks = entry.getValue();
			if (ticks <= 0) {
				it.remove();
				continue;
			}
			entry.setValue(ticks - 1);
		}

		if (mc.crosshairTarget instanceof EntityHitResult hit
				&& hit.getEntity() instanceof EndCrystalEntity crystal
				&& pendingCrystals.containsKey(crystal.getUuid())) {
			Vec3d cameraPosVec = mc.player.getCameraPosVec(RenderTickCounter.ONE.getTickDelta(true));
			Vec3d rotationVec = WorldUtils.getPlayerLookVec(mc.player);
			Vec3d range = cameraPosVec.add(rotationVec.x * 4.5, rotationVec.y * 4.5, rotationVec.z * 4.5);
			BlockHitResult blockHit = mc.world.raycast(new RaycastContext(cameraPosVec, range, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
			if (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK) {
				mc.crosshairTarget = blockHit;
			}
		}
	}

	@Override
	public void onPacketSend(PacketSendEvent event) {
		if (event.packet instanceof PlayerInteractEntityC2SPacket interactPacket) {
			interactPacket.handle(new PlayerInteractEntityC2SPacket.Handler() {
				@Override
				public void interact(Hand hand) {

				}

				@Override
				public void interactAt(Hand hand, Vec3d pos) {

				}

				@Override
				public void attack() {

					if (mc.crosshairTarget == null)
						return;

					if (mc.crosshairTarget.getType() == HitResult.Type.ENTITY && mc.crosshairTarget instanceof EntityHitResult hit) {
						if (hit.getEntity() instanceof EndCrystalEntity crystal) {
							StatusEffectInstance weakness = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
							StatusEffectInstance strength = mc.player.getStatusEffect(StatusEffects.STRENGTH);
							if (!(weakness == null || strength != null && strength.getAmplifier() > weakness.getAmplifier() || WorldUtils.isTool(mc.player.getMainHandStack())))
								return;

							pendingCrystals.put(crystal.getUuid(), 4);
						}
					}
				}
			});
		}
	}
}
