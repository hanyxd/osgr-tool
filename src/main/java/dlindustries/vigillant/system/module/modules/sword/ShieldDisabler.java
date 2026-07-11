package dlindustries.vigillant.system.module.modules.sword;



import dlindustries.vigillant.system.event.events.AttackListener;

import dlindustries.vigillant.system.event.events.TickListener;

import dlindustries.vigillant.system.module.Category;

import dlindustries.vigillant.system.module.Module;

import dlindustries.vigillant.system.module.setting.BooleanSetting;

import dlindustries.vigillant.system.module.setting.NumberSetting;

import dlindustries.vigillant.system.utils.EncryptedString;

import dlindustries.vigillant.system.utils.InventoryUtils;

import dlindustries.vigillant.system.utils.MouseSimulation;

import dlindustries.vigillant.system.utils.WorldUtils;

import dlindustries.vigillant.system.module.modules.optimizer.HitOptimizer;

import net.minecraft.entity.Entity;

import net.minecraft.entity.player.PlayerEntity;

import net.minecraft.item.AxeItem;

import net.minecraft.item.Items;

import net.minecraft.item.SwordItem;

import net.minecraft.util.hit.EntityHitResult;

import org.lwjgl.glfw.GLFW;



import java.util.HashMap;

import java.util.Map;

import java.util.UUID;



public final class ShieldDisabler extends Module implements TickListener, AttackListener {

	private final NumberSetting hitDelay = new NumberSetting(EncryptedString.of("Hit Delay"), 0, 20, 0, 1);

	private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay"), 0, 20, 0, 1);

	private final BooleanSetting switchBack = new BooleanSetting(EncryptedString.of("Switch Back"), true); private final BooleanSetting stun = new BooleanSetting(EncryptedString.of("Stun"), false)

			.setDescription(EncryptedString.of("swaps back to last used slot"));

	private final BooleanSetting stunSlam = new BooleanSetting(EncryptedString.of("Stun Slam"), true)

			.setDescription(EncryptedString.of("Axe->Mace->Back "));



	private final BooleanSetting clickSimulate = new BooleanSetting(EncryptedString.of("Click Simulation"), true);

	private final NumberSetting minShieldHold = new NumberSetting(EncryptedString.of("Min Shield Hold"), 50, 500, 300, 5)

			.setDescription(EncryptedString.of("Minimum time opponent must hold shield (ms) - only for normal stun")); private final BooleanSetting syncWithHitOptimizer = new BooleanSetting(EncryptedString.of("Sync with Hit Optimizer"), false)

			.setDescription(EncryptedString.of("When enabled, coordinate with HitOptimizer (requires manual click)"));



	private int originalSlot = -1;

	private int hitClock, switchClock;

	private boolean inStunSequence;

	private int stunStep;

	private final Map<UUID, Long> shieldStartTimes = new HashMap<>();

	private PlayerEntity currentTarget;

	private boolean isBreaking;

	private boolean isStunSlam; private boolean cancelNextSwordHit;

	public ShieldDisabler() {

		super(EncryptedString.of("Shield Disabler"),

				EncryptedString.of("Disables shield with stun and stun slam techniques"),

				-1,

				Category.sword);



		addSettings(

				switchDelay, hitDelay, switchBack, stun, stunSlam,

				clickSimulate, minShieldHold, syncWithHitOptimizer

		);

	}



	@Override

	public void onEnable() {

		eventManager.add(TickListener.class, this);

		eventManager.add(AttackListener.class, this);



		resetState();

		super.onEnable();

	}



	@Override

	public void onDisable() {

		eventManager.remove(TickListener.class, this);

		eventManager.remove(AttackListener.class, this);

		restoreOriginalSlot();

		shieldStartTimes.clear(); if (syncWithHitOptimizer.getValue()) {

			HitOptimizer.clearShieldDisableRequest();

		}



		super.onDisable();

	}



	private void resetState() {

		hitClock = hitDelay.getValueInt();

		switchClock = switchDelay.getValueInt();

		originalSlot = -1;

		inStunSequence = false;

		stunStep = 0;

		currentTarget = null;

		isBreaking = false;

		isStunSlam = false;

		cancelNextSwordHit = false;

	}



	@Override

	public void onTick() {

		if (shouldSkipTick()) {

			if (isBreaking) abortBreaking();

			return;

		}



		if (inStunSequence) {

			handleStunSequence();

			return;

		} Entity targetEntity = null;

		PlayerEntity playerTarget = null;



		if (mc.crosshairTarget instanceof EntityHitResult entityHit) {

			Entity entity = entityHit.getEntity();

			if (entity != null && entity != mc.player && entity.isAlive()) {

				targetEntity = entity;

				if (entity instanceof PlayerEntity player) {
					playerTarget = player;
				}

			}

		} updateShieldTracking(playerTarget);



		if (isBreaking && currentTarget != null) {

			handleBreakingSequence();

			return;

		} if (targetEntity != null && shouldPerformTechnique(playerTarget)) {

			currentTarget = (targetEntity instanceof PlayerEntity) ? (PlayerEntity) targetEntity : null;



			if (syncWithHitOptimizer.getValue()) { boolean useMace = isStunSlam;

				int maceSlot = useMace ? InventoryUtils.getMaceSlot() + 1 : -1;

				UUID targetId = (currentTarget != null) ? currentTarget.getUuid() : null;

				if (targetId != null) {

					HitOptimizer.requestShieldDisable(targetId, useMace, maceSlot, !useMace);

				} if (currentTarget != null) {

					shieldStartTimes.remove(currentTarget.getUuid());

				}

				resetState();

			} else {

				startBreakingSequence(currentTarget);

			}

		}

	}



	private boolean shouldSkipTick() {

		return mc.currentScreen != null ||

				mc.player == null ||

				mc.player.isUsingItem();

	}



	private void updateShieldTracking(PlayerEntity target) {

		currentTarget = target; for (PlayerEntity player : mc.world.getPlayers()) {

			if (player == mc.player) continue;



			UUID uuid = player.getUuid();

			boolean isBlocking = player.isHolding(Items.SHIELD) && player.isBlocking();



			if (isBlocking) {

				if (!shieldStartTimes.containsKey(uuid)) {

					shieldStartTimes.put(uuid, System.currentTimeMillis());

				}

			} else {

				shieldStartTimes.remove(uuid);

			}

		}

	}



	private boolean shouldPerformTechnique(PlayerEntity target) {

		if (target == null) return false; boolean holdingAxe = mc.player.getMainHandStack().getItem() instanceof AxeItem;

		boolean holdingSword = mc.player.getMainHandStack().getItem() instanceof SwordItem;

		boolean hasMace = InventoryUtils.hasMaceInHotbar();

		boolean hasAxe = InventoryUtils.getAxeSlot() != -1;

		if ((holdingAxe || holdingSword) && stunSlam.getValue() && hasAxe && hasMace) {
			isStunSlam = true;
			return true;
		}

		if (holdingSword && stun.getValue() && hasAxe) {

			UUID uuid = target.getUuid();

			if (!shieldStartTimes.containsKey(uuid)) return false;




			long holdTime = System.currentTimeMillis() - shieldStartTimes.get(uuid);

			if (holdTime < minShieldHold.getValue()) return false;

			isStunSlam = false;

			return true;

		}

		return false;

	}



	private void startBreakingSequence(PlayerEntity target) {

		isBreaking = true;

		currentTarget = target;

		if (originalSlot == -1) {

			originalSlot = mc.player.getInventory().selectedSlot;

		}

		switchClock = switchDelay.getValueInt(); if (!isStunSlam) {

			cancelNextSwordHit = true;

		}

	}



	private void handleBreakingSequence() {

		if (currentTarget == null) {

			abortBreaking();

			return;

		} if (switchClock > 0) {

			switchClock--;

			return;

		} if (!InventoryUtils.selectAxe()) {

			abortBreaking();

			return;

		} if (hitClock > 0) {

			hitClock--;

			return;

		} if (clickSimulate.getValue()) {

			MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);

		} if (mc.crosshairTarget instanceof EntityHitResult hit) {

			Entity entity = hit.getEntity();

			if (entity != null) {

				WorldUtils.hitEntity(entity, true);

			}

		} if (currentTarget != null) {

			shieldStartTimes.remove(currentTarget.getUuid());

		} inStunSequence = true;

		stunStep = 1;

		isBreaking = false;

		hitClock = hitDelay.getValueInt();

	}



	private void abortBreaking() {

		restoreOriginalSlot();

		resetState();

	}



	private void handleStunSequence() {

		if (mc.player == null || currentTarget == null) {

			resetStunSequence();

			return;

		}



		switch (stunStep) {

			case 1: if (hitClock > 0) {

				hitClock--;

				return;

			}

				stunStep = 2;

				break;



			case 2: if (isStunSlam) { int maceSlot = InventoryUtils.getMaceSlot();

				if (maceSlot != -1) {

					mc.player.getInventory().selectedSlot = maceSlot;

				} else { resetStunSequence();

					return;

				}

			} else { int swordSlot = InventoryUtils.getSwordSlot();

				if (swordSlot != -1) {

					mc.player.getInventory().selectedSlot = swordSlot;

				} else { resetStunSequence();

					return;

				}

			}

				stunStep = 3;

				break;



			case 3: if (isStunSlam) { if (mc.crosshairTarget instanceof EntityHitResult entityHit) {

				Entity entity = entityHit.getEntity();

				if (entity != null) {

					if (clickSimulate.getValue()) {

						MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);

					}

					WorldUtils.hitEntity(entity, true);

				}

			}

				stunStep = 4;

			} else { resetStunSequence();

			}

				break;



			case 4: if (isStunSlam && switchBack.getValue()) {

				restoreOriginalSlot();

			}

				resetStunSequence();

				break;

		}

	}



	private void resetStunSequence() {

		inStunSequence = false;

		stunStep = 0;

		resetState();

	}



	private void restoreOriginalSlot() {

		if (switchBack.getValue() && originalSlot != -1) {

			if (switchDelay.getValueInt() > 0) {

				if (switchClock > 0) {

					switchClock--;

				} else {

					mc.player.getInventory().selectedSlot = originalSlot;

					originalSlot = -1;

				}

			} else {

				mc.player.getInventory().selectedSlot = originalSlot;

				originalSlot = -1;

			}

		}

	}



	@Override

	public void onAttack(AttackEvent event) { if (cancelNextSwordHit && mc.player.getMainHandStack().getItem() instanceof SwordItem) {

		event.cancel();

		cancelNextSwordHit = false;

		return;

	} if (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {

		event.cancel();

	}

	}

}