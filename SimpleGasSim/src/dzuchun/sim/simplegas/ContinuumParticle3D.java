package dzuchun.sim.simplegas;

import java.util.function.Supplier;

import dzuchun.lib.math.GeometricVector3D;

public class ContinuumParticle3D extends Particle3D {

	public static Supplier<ContinuumParticle3D> creator(Supplier<GeometricVector3D> pos,
			Supplier<GeometricVector3D> vel) {
		return () -> {
			GeometricVector3D position = pos.get();
			GeometricVector3D velocity = vel.get();
			return new ContinuumParticle3D(position.getX(), position.getY(), position.getZ(), velocity.getX(),
					velocity.getY(), velocity.getZ());
		};
	}

	public static Supplier<Double> CONTINUUM_X = () -> Settings.CONTINUUM_MAX_X;
	public static Supplier<Double> CONTINUUM_Y = () -> Settings.CONTINUUM_MAX_Y;
	public static Supplier<Double> CONTINUUM_Z = () -> Settings.CONTINUUM_MAX_Z;

	public ContinuumParticle3D(double x, double y, double z) {
		super(x, y, z);
		reactBounds();
	}

	public ContinuumParticle3D(double x, double y, double z, double vx, double vy, double vz) {
		super(x, y, z);
		speed = new GeometricVector3D(vx, vy, vz);
		reactBounds();
	}

	protected void reactBounds() {
		double v = getX(), c = ContinuumParticle3D.CONTINUUM_X.get();
		if (v < 0) {
			addToCoord(0, c);
		} else {
			if (v > c) {
				addToCoord(0, -c);
			}
		}

		v = getY();
		c = ContinuumParticle3D.CONTINUUM_Y.get();
		if (v < 0) {
			addToCoord(1, c);
		} else {
			if (v > c) {
				addToCoord(1, -c);
			}
		}

		v = getZ();
		c = ContinuumParticle3D.CONTINUUM_Z.get();
		if (v < 0) {
			addToCoord(2, c);
		} else {
			if (v > c) {
				addToCoord(2, -c);
			}
		}
	}

	@Override
	public void stepMove(GeometricVector3D shift) {
		super.stepMove(shift);
		reactBounds();
	}

	@Override
	public ContinuumParticle3D copy() {
		ContinuumParticle3D res = new ContinuumParticle3D(coords.get(0), coords.get(1), coords.get(2));
		res.speed = getSpeed();
		return res;
	}

}
