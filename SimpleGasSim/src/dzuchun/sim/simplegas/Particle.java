package dzuchun.sim.simplegas;

import java.util.function.Supplier;

import dzuchun.lib.math.GeometricVector;
import dzuchun.lib.math.MultiplicativeMatrix;

/**
 *
 * @author dzu
 *
 */
public interface Particle<T extends GeometricVector> {

	static final Supplier<Long> ID_SUPPLIER = new Supplier<Long>() {
		private long lastId = 1;

		@Override
		public Long get() {
			return lastId++;
		}
	};
	public final long id = Particle.ID_SUPPLIER.get();

	public abstract double getPotentialEnergy(Particle<T> particle);

	/**
	 * Defines force function for particle
	 *
	 * @param r
	 * @return Force applied to provided particle
	 */
	public abstract T getForceOn(Particle<T> particle);

	public default MultiplicativeMatrix<Double> getForceJakobi(Particle<T> particle) {
		int size = particle.getPosition().dimLength;
		return MultiplicativeMatrix.getZeroDoubleMatrix(size, size);
	}

	public abstract double getMass();

	public abstract Particle<T> copy();

	public abstract T getPosition();

	public abstract void stepMove(T shift);

	public abstract T getSpeed();

	public default double getAbsSpeed() {
		return this.getSpeed().getLength();
	}

	public T getLastAcc();

	public T getLastPos();

	public abstract void stepAccelerate(T accStep);

	public default double kineticEnergy() {
		T speed = this.getSpeed();
		return (this.getMass() * speed.dotProduct(speed)) / 2.0d;
	}

}
