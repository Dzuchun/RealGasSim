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

	public static Supplier<Double> getContinuumProperty(int n) {
		return (n == 0) ? ContinuumParticle3D.CONTINUUM_X
				: (n == 1) ? ContinuumParticle3D.CONTINUUM_Y : ContinuumParticle3D.CONTINUUM_Z;
	}

	public ContinuumParticle3D(double x, double y, double z) {
		this(x, y, z, 0, 0, 0);
	}

	public ContinuumParticle3D(double x, double y, double z, double vx, double vy, double vz) {
		super(x, y, z);
		speed = new GeometricVector3D(vx, vy, vz);
		reactBounds();

		// Belong to verlet integration feature
		lastPos = (GeometricVector3D) speed.scale(-Settings.DT, true).add(this, false);
	}

	@Override
	public GeometricVector3D getRelativePos(Particle<GeometricVector3D> particle) {
		GeometricVector3D relativePos = super.getRelativePos(particle);
		relativePos.forEachCoord((num, c) -> {
			double prop = ContinuumParticle3D.getContinuumProperty(num).get() / 2;
			if (c > prop) {
				c -= 2 * prop;
			} else if (c < -prop) {
				c += 2 * prop;
			}
		});
		return relativePos;
	}

	@Override
	public GeometricVector3D getForceOn(Particle<GeometricVector3D> particle) {
		if (particle == this) {
			return new GeometricVector3D();
		}
		if (Settings.CONTINUUM_ATTRACTION) {
			GeometricVector3D basePos = getRelativePos(particle);
			GeometricVector3D res = new GeometricVector3D();
			int iterations = Settings.CONTINUUM_ATTRACTION_ITERATIONS;
			double[] continuumProps = { CONTINUUM_X.get(), CONTINUUM_Y.get(), CONTINUUM_Z.get() };
			int specCoords[] = { -iterations, -iterations, -iterations };
			for (; specCoords[0] <= iterations; specCoords[0]++) {
				specCoords[1] = -iterations;
				for (; specCoords[1] <= iterations; specCoords[1]++) {
					specCoords[2] = -iterations;
					for (; specCoords[2] <= iterations; specCoords[2]++) {
						GeometricVector3D currentPos = (GeometricVector3D) basePos.createClone();
						for (int i = 0; i < 3; i++) {
							currentPos.addToCoord(i, specCoords[i] * continuumProps[i]);
						}
						res.add(getForceOnInternal(currentPos), false);
					}
				}
			}
			return (GeometricVector3D) res.scale(particle.getMass(), false);
		} else {
			return super.getForceOn(particle);
		}
	}

	@Override
	public double getPotentialEnergy(Particle<GeometricVector3D> particle) {
		if (particle == this) {
			return 0.0d;
		}
		if (Settings.CONTINUUM_ATTRACTION) {
			GeometricVector3D basePos = getRelativePos(particle);
			double res = 0.0d;
			int iterations = Settings.CONTINUUM_ATTRACTION_ITERATIONS;
			double[] continuumProps = { CONTINUUM_X.get(), CONTINUUM_Y.get(), CONTINUUM_Z.get() };
			int specCoords[] = { -iterations, -iterations, -iterations };
			for (; specCoords[0] <= iterations; specCoords[0]++) {
				specCoords[1] = -iterations;
				for (; specCoords[1] <= iterations; specCoords[1]++) {
					specCoords[2] = -iterations;
					for (; specCoords[2] <= iterations; specCoords[2]++) {
						GeometricVector3D currentPos = (GeometricVector3D) basePos.createClone();
						for (int i = 0; i < 3; i++) {
							currentPos.addToCoord(i, specCoords[i] * continuumProps[i]);
						}
						res += getPotentialInternal(currentPos);
					}
				}
			}
			return res*particle.getMass();
		} else {
			return super.getPotentialEnergy(particle);
		}
	}

	protected void reactBounds() {
		double v, c;
		for (int i = 0; i < 3; i++) {
			v = this.getCoord(i);
			c = getContinuumProperty(i).get();
			if (v < 0) {
				addToCoord(i, c);
				lastPos.addToCoord(i, c);
			} else {
				if (v > c) {
					addToCoord(i, -c);
					lastPos.addToCoord(i, -c);
				}
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

		// Belong to Verlet integration
		if (Settings.VERLET_INTEGRATION) {
			res.lastPos = getLastPos();
		}
		return res;
	}

}
