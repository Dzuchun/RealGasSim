package dzuchun.sim.simplegas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

import dzuchun.lib.math.GeometricVector;
import dzuchun.lib.sim.ISimulatable;

public class ParticleSystem<V extends GeometricVector, T extends Particle<V>> extends ArrayList<T>
		implements ISimulatable<Double> {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private final IntegrationPolicy<V, Double> positionPolicy;
	private final IntegrationPolicy<V, Double> speedPolicy;
	private final Supplier<V> zeroPosFactory;
	private double time;

	/**
	 *
	 * @param particlesIn
	 * @param positionPolicyIn
	 * @param speedPolicyIn
	 * @param graphicalSizeIn  optional
	 */
	public ParticleSystem(Collection<T> particlesIn, IntegrationPolicy<V, Double> positionPolicyIn,
			IntegrationPolicy<V, Double> speedPolicyIn, Supplier<V> zeroPosFactoryIn) {
		super(particlesIn);
		this.positionPolicy = positionPolicyIn;
		this.speedPolicy = speedPolicyIn;
		this.zeroPosFactory = zeroPosFactoryIn;
	}

	public double getKineticEnergy() {
		return parallelStream().mapToDouble(Particle::kineticEnergy).sum();
	}

	public double getUEnergy() {
		double res = 0;
		for (T p1 : this) {
			for (T p2 : this) {
				if (p1 != p2) {
					res += p1.getPotential(p2) / 2.0d;
				}
			}
		}
		return res / 2.0d;
	}

	public double getTotalEnergy() {
		return getKineticEnergy() + getUEnergy();
	}

	public V getMomentum() {
		V res = this.zeroPosFactory.get();
		for (T p : this) {
			res.add(p.getSpeed().scale(p.getMass(), true), false);
		}
		return res;
	}

	@SuppressWarnings("unchecked")
	@Override
	public ParticleSystem<V, T> createCopy() {
		ParticleSystem<V, T> res = new ParticleSystem<V, T>(Arrays.asList(), positionPolicy, speedPolicy,
				zeroPosFactory);
		for (T p : this) {
			res.add((T) p.copy());
		}
		res.time = this.time;
		return res;
	}

	public static class SimpleChecker<V extends GeometricVector, T extends Particle<V>>
			implements ISimulatable.Checker<ParticleSystem<V, T>, Double> {

		@Override
		public Double check(ParticleSystem<V, T> base, ParticleSystem<V, T> candidate, Double dt)
				throws CannotProceedException {
			return null;
		}

	}

	@FunctionalInterface
	public static interface IntegrationPolicy<V, P> {
		/**
		 * Defines policy for integration
		 *
		 * @param current   Current value.
		 * @param deriv     Value derivative.
		 * @param parameter Current (absolute) parameter value.
		 * @param delta     Parameter delta
		 * @return Value change.
		 */
		public V integrate(V current, V deriv, P parameter, P delta);
	}

	@Override
	public void advance(Double dt) {
		double mass;
		V force;
		for (T p : this) {
			mass = p.getMass();
			force = zeroPosFactory.get();
			for (T p1 : this) {
				if (p == p1) {
					continue;
				}
				V force1 = p1.getForce(p);
				force.add(force1, false);
			}
			force.scale(1 / mass, false);
			p.stepAccelerate(speedPolicy.integrate(p.getSpeed(), force, time, dt));
		}
		for (T p : this) {
			p.stepMove(positionPolicy.integrate(p.getPosition(), p.getSpeed(), time, dt));
		}
		time += dt;
	}

	public int[] distributeVelocity(Function<Double, Integer> distributor, int distLength) {
		int[] res = new int[distLength];
		for (T p : this) {
			res[distributor.apply(p.getAbsSpeed())]++;
		}
		return res;
	}

	public int[] distributeParameter(Function<Double, Integer> distributor, int distLength,
			Function<T, Double> parameterGetterIn) {
		int[] res = new int[distLength];
		for (T p : this) {
			res[distributor.apply(parameterGetterIn.apply(p))]++;
		}
		return res;
	}

	@Override
	public String simulationState() {
		return String.format("Name:%s, Particles:%s", this.getClass().getSimpleName(), toString());
	}

	public double getCurrentTime() {
		return time;
	}
}
