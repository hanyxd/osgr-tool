package dlindustries.vigillant.system.utils;

import java.util.Random;

public final class MathUtils {
	public static Random random = new Random(System.currentTimeMillis());

	public static double roundToDecimal(double n, double point) {
		return point * Math.round(n / point);
	}

	public static int randomInt(int start, int bound) {
		return random.nextInt(start, bound);
	}

	public static double smoothStepLerp(double delta, double start, double end) {
		double value;
		delta = Math.max(0, Math.min(1, delta));

		double t = delta * delta * (3 - 2 * delta);

		value = start + (end - start) * t;
		return value;
	}

	public static double goodLerp(float delta, double start, double end) {
		int step = (int) Math.ceil(Math.abs(end - start) * delta);
		if (start < end) return Math.min(start + step, end);
		else return Math.max(start - step, end);
	}

	private static final double PEARL_GRAVITY = 0.03;
	private static final double PEARL_DRAG = 0.99;

	/**
	 * Simulates an ender pearl trajectory tick-by-tick using Minecraft projectile physics.
	 * Position is updated first, then drag, then gravity — matching vanilla order.
	 *
	 * @param pitchDeg    throw pitch in degrees (negative = looking up)
	 * @param yawDeg      throw yaw in degrees
	 * @param playerVelX  thrower X velocity (added to pearl initial velocity)
	 * @param playerVelZ  thrower Z velocity (added to pearl initial velocity)
	 * @param eyeHeight   eye height above feet level (1.62 for players)
	 * @param speed       effective pearl launch speed (base 1.5)
	 * @return {relativeX, relativeZ, flightTicks}
	 */
	public static double[] simulatePearlTrajectory(double pitchDeg, double yawDeg,
			double playerVelX, double playerVelZ, double eyeHeight, double speed) {
		double pitchRad = Math.toRadians(pitchDeg);
		double yawRad = Math.toRadians(yawDeg);

		double vx = -Math.sin(yawRad) * Math.cos(pitchRad) * speed + playerVelX;
		double vy = -Math.sin(pitchRad) * speed;
		double vz =  Math.cos(yawRad) * Math.cos(pitchRad) * speed + playerVelZ;

		double x = 0, y = eyeHeight, z = 0;

		for (int tick = 1; tick <= 500; tick++) {
			x += vx;
			y += vy;
			z += vz;

			vx *= PEARL_DRAG;
			vy *= PEARL_DRAG;
			vz *= PEARL_DRAG;
			vy -= PEARL_GRAVITY;

			if (y <= 0) {
				return new double[]{x, z, tick};
			}
		}
		return new double[]{x, z, 500};
	}

	/**
	 * Finds the yaw and pitch required to throw an ender pearl so it lands at
	 * the given (landX, landZ) offset from the thrower, accounting for the
	 * thrower's horizontal velocity.
	 *
	 * @return {yaw, pitch} in degrees, or null if unreachable
	 */
	public static double[] findPearlAngles(double landX, double landZ,
			double playerVelX, double playerVelZ, double eyeHeight, double speed) {
		double targetDist = Math.sqrt(landX * landX + landZ * landZ);
		if (targetDist < 0.5) return null;

		double baseYaw = Math.toDegrees(Math.atan2(-landX, landZ));

		// --- Phase 1: coarse scan (1-degree steps) ---
		double bestPitch = 0;
		double bestError = Double.MAX_VALUE;

		for (double p = -45; p <= 70; p += 1.0) {
			double[] r = simulatePearlTrajectory(p, baseYaw, playerVelX, playerVelZ, eyeHeight, speed);
			double d = Math.sqrt(r[0] * r[0] + r[1] * r[1]);
			double err = Math.abs(d - targetDist);
			if (err < bestError) {
				bestError = err;
				bestPitch = p;
			}
		}

		// --- Phase 2: fine-tune (0.05-degree steps) ---
		double lo = bestPitch - 1.5;
		double hi = bestPitch + 1.5;
		for (double p = lo; p <= hi; p += 0.05) {
			double[] r = simulatePearlTrajectory(p, baseYaw, playerVelX, playerVelZ, eyeHeight, speed);
			double d = Math.sqrt(r[0] * r[0] + r[1] * r[1]);
			double err = Math.abs(d - targetDist);
			if (err < bestError) {
				bestError = err;
				bestPitch = p;
			}
		}

		// --- Phase 3: yaw correction for velocity-induced lateral drift ---
		double[] sim = simulatePearlTrajectory(bestPitch, baseYaw, playerVelX, playerVelZ, eyeHeight, speed);
		double actualAngle = Math.toDegrees(Math.atan2(-sim[0], sim[1]));
		double correctedYaw = baseYaw + (baseYaw - actualAngle);

		// re-tune pitch with corrected yaw
		bestError = Double.MAX_VALUE;
		for (double p = lo; p <= hi; p += 0.05) {
			double[] r = simulatePearlTrajectory(p, correctedYaw, playerVelX, playerVelZ, eyeHeight, speed);
			double d = Math.sqrt(r[0] * r[0] + r[1] * r[1]);
			double err = Math.abs(d - targetDist);
			if (err < bestError) {
				bestError = err;
				bestPitch = p;
			}
		}

		return new double[]{correctedYaw, bestPitch};
	}
}
