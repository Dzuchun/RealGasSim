package dzuchun.sim.simplegas;

import dzuchun.lib.math.GeometricVector3D;
import dzuchun.lib.math.MultiplicativeMatrix;

public class Particle3D extends GeometricVector3D implements Particle<GeometricVector3D> {

	protected GeometricVector3D speed;

	public Particle3D(double x, double y, double z) {
		super(x, y, z);
		speed = new GeometricVector3D();

		// Belong to verlet integration feature
		lastPos = new GeometricVector3D(this);
	}

	public Particle3D(GeometricVector3D posIn, GeometricVector3D velIn) {
		this(posIn.getX(), posIn.getY(), posIn.getZ());
		speed = new GeometricVector3D(velIn);
	}

	@Override
	public GeometricVector3D getSpeed() {
		return new GeometricVector3D(speed);
	}

	public void scaleSpeed(double factor) {
		speed.multiply(factor, false);
	}

	public static final double SIGMA_SQ = Settings.SIGMA * Settings.SIGMA;

	public GeometricVector3D getRelativePos(Particle<GeometricVector3D> particle) {
		return (GeometricVector3D) particle.getPosition().multiply(-1.0d, true).add(getPosition(), false);
	}

	@Override
	public double getPotentialEnergy(Particle<GeometricVector3D> particle) {
		GeometricVector3D position = getRelativePos(particle);
		return getPotentialInternal(position) * particle.getMass();
	}

	protected double getPotentialInternal(GeometricVector3D position) {
		double d = Math.pow(Particle3D.SIGMA_SQ / position.dotProduct(position), 3);
		return 4.0d * Settings.EPSILON * d * (d - 1);
	}

	@Override
	public GeometricVector3D getForceOn(Particle<GeometricVector3D> particle) {
		if (particle == this) {
			return new GeometricVector3D();
		}
		GeometricVector3D position = getRelativePos(particle);
		return (GeometricVector3D) getForceOnInternal(position).multiply(particle.getMass(), false);
	}

	protected GeometricVector3D getForceOnInternal(GeometricVector3D relativePos) {
		double distanceSq = relativePos.dotProduct(relativePos);
		double d = Math.pow(Particle3D.SIGMA_SQ / distanceSq, 3);
		double k = ((24.0d * Settings.EPSILON) / distanceSq) * d * (1 - (2 * d));
		return (GeometricVector3D) relativePos.multiply(k, true);
	}

	@Override
	public MultiplicativeMatrix<Double> getForceJakobi(Particle<GeometricVector3D> particle) {
		if ((particle == this) || !Settings.USE_FORCE_JAKOBI) {
			return MultiplicativeMatrix.getZeroDoubleMatrix(3, 3);
		}
		GeometricVector3D position = getRelativePos(particle);
		return getForceJakobiInternal(position).multiply(particle.getMass(), false);
	}

	protected MultiplicativeMatrix<Double> getForceJakobiInternal(GeometricVector3D relativePos) {
		double distanceSq = relativePos.dotProduct(relativePos);
		double d = Math.pow(Particle3D.SIGMA_SQ / distanceSq, 3);
		MultiplicativeMatrix<Double> res = ((MultiplicativeMatrix<Double>) MultiplicativeMatrix.getUnitDoubleMatrix(3)
				.multiply(d * (1 - (2 * d)), false)
				.add(relativePos.matrixProduct(relativePos).multiply(((26.0 * d) - 7) / distanceSq, false), false))
						.multiply((24.0d * Settings.EPSILON) / distanceSq, false);
		return res;
	}

	@Override
	public double getMass() {
		return Settings.MASS;
	}

	@Override
	public Particle<GeometricVector3D> copy() {
		Particle3D res = new Particle3D(coords().get(0), coords().get(1), coords().get(2));
		res.speed = getSpeed();
		return res;
	}

	@Override
	public GeometricVector3D getPosition() {
		return new GeometricVector3D(this);
	}

	protected GeometricVector3D lastPos;

	@Override
	public GeometricVector3D getLastPos() {
		return new GeometricVector3D(lastPos);
	}

	@Override
	public void stepMove(GeometricVector3D shift) {
		lastPos = new GeometricVector3D(this);
		add(shift, false);
	}

	protected GeometricVector3D lastAcc;

	@Override
	public GeometricVector3D getLastAcc() {
		return new GeometricVector3D(lastAcc);
	}

	@Override
	public void stepAccelerate(GeometricVector3D accStep) {
		speed.add(accStep, false);
		lastAcc = new GeometricVector3D(accStep);
	}

}
