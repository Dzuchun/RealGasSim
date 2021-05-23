package dzuchun.sim.simplegas;

import dzuchun.lib.math.GeometricVector2D;

public abstract class Particle2D extends GeometricVector2D implements Particle<GeometricVector2D> {

	protected GeometricVector2D speed = new GeometricVector2D();

	public Particle2D(double x, double y) {
		super(x, y);
	}

	@Override
	public GeometricVector2D getPosition() {
		return new GeometricVector2D(this);
	}

	@Override
	public void stepMove(GeometricVector2D shift) {
		add(shift, false);
	}

	@Override
	public GeometricVector2D getSpeed() {
		return new GeometricVector2D(speed);
	}

	private GeometricVector2D lastAcc;

	@Override
	public void stepAccelerate(GeometricVector2D accStep) {
		speed.add(accStep, false);
		lastAcc = accStep;
	}

	@Override
	public GeometricVector2D getLastAcc() {
		return new GeometricVector2D(lastAcc);
	}
}