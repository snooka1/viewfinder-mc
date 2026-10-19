package com.example.viewfinder;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Shared camera maths so capture and placement use the exact same ray grid. */
public final class Projection {
	public static final int W = 96;
	public static final int H = 54;
	public static final double TAN_H = Math.tan(Math.toRadians(35)); // ~70 degree horizontal FOV
	public static final double TAN_V = TAN_H * H / W;
	public static final double RANGE = 64.0;

	private Projection() {}

	/** Unit direction of grid cell (i, j) for the player's current view. */
	public static Vec3 dir(Player p, int i, int j) {
		Vec3 fwd = p.getViewVector(1.0f);
		Vec3 right = fwd.cross(new Vec3(0, 1, 0));
		if (right.lengthSqr() < 1.0e-6) {
			// Looking straight up/down: derive "right" from yaw instead.
			double yaw = Math.toRadians(p.getYRot());
			right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
		}
		right = right.normalize();
		Vec3 up = right.cross(fwd).normalize();
		double u = ((i + 0.5) / W * 2.0 - 1.0) * TAN_H;
		double v = ((j + 0.5) / H * 2.0 - 1.0) * TAN_V;
		return fwd.add(right.scale(u)).add(up.scale(-v)).normalize();
	}
}
