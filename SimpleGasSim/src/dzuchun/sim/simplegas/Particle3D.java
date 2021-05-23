package dzuchun.sim.simplegas;

import dzuchun.lib.math.GeometricVector;
import dzuchun.lib.math.GeometricVector3D;

public class Particle3D extends GeometricVector3D implements Particle<GeometricVector3D> {

	protected GeometricVector3D speed;

	public Particle3D(double x, double y, double z) {
		super(x, y, z);
		speed = new GeometricVector3D();
	}

	public Particle3D(GeometricVector3D posIn, GeometricVector3D velIn) {
		this(posIn.getX(), posIn.getY(), posIn.getZ());
		speed = new GeometricVector3D(velIn);
	}

	@Override
	public GeometricVector3D getSpeed() {
		return new GeometricVector3D(speed);
	}

	public static final double SIGMA_SQ = Settings.SIGMA * Settings.SIGMA;

	@Override
	public GeometricVector3D getForce(Particle<GeometricVector3D> particle) {
		GeometricVector position = scale(-1.0d, true).add(particle.getPosition(), false).scale(-1.0d, false);
		double distanceSq = position.dotProduct(position);
		double d = Particle3D.SIGMA_SQ / distanceSq;
		double k = ((24.0d * Settings.EPSILON) / Math.sqrt(distanceSq)) * Math.pow(d, 3) * (1 - (2 * Math.pow(d, 3)));
		return new GeometricVector3D(position.scale(k, false));
	}

	@Override
	public double getPotential(Particle<GeometricVector3D> particle) {
		GeometricVector position = scale(-1.0d, true).add(particle.getPosition(), false).scale(-1.0d, false);
		double d = Math.pow(Particle3D.SIGMA_SQ / position.dotProduct(position), 3);
		return 4.0d * Settings.EPSILON * d * (d - 1);
	}

	@Override
	public double getMass() {
		return Settings.MASS;
	}

	@Override
	public Particle<GeometricVector3D> copy() {
		Particle3D res = new Particle3D(coords.get(0), coords.get(1), coords.get(2));
		res.speed = getSpeed();
		return res;
	}

	@Override
	public GeometricVector3D getPosition() {
		return new GeometricVector3D(this);
	}

	@Override
	public void stepMove(GeometricVector3D shift) {
		add(shift, false);
	}

	private GeometricVector3D lastAcc;

	@Override
	public GeometricVector3D getLastAcc() {
		return new GeometricVector3D(lastAcc);
	}

	@Override
	public void stepAccelerate(GeometricVector3D accStep) {
		speed.add(accStep, false);
		lastAcc = accStep;
	}

}
