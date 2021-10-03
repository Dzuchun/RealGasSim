package dzuchun.sim.simplegas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import dzuchun.lib.math.GeometricVector;
import dzuchun.lib.math.MultiplicativeMatrix;
import dzuchun.lib.sim.ISimulatable;

public class ParticleSystem<V extends GeometricVector, T extends Particle<V>> extends ArrayList<T>
		implements ISimulatable<Double> {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private final IntegrationPolicy<Double, V> positionPolicy;
	private final IntegrationPolicy<Double, V> speedPolicy;
	private final Supplier<V> zeroPosFactory;
	private double time;

	/**
	 *
	 * @param particlesIn
	 * @param positionPolicyIn
	 * @param speedPolicyIn
	 * @param graphicalSizeIn  optional
	 */
	public ParticleSystem(Collection<T> particlesIn, IntegrationPolicy<Double, V> positionPolicyIn,
			IntegrationPolicy<Double, V> speedPolicyIn, Supplier<V> zeroPosFactoryIn) {
		super(particlesIn);
		this.positionPolicy = positionPolicyIn;
		this.speedPolicy = speedPolicyIn;
		this.zeroPosFactory = zeroPosFactoryIn;
		// TODO add check if V is correctly implementing vector operations
	}

	public double getKineticEnergy() {
		double res = 0;
		for (T p : this) {
			res += p.kineticEnergy();
		}
		return res;
//		V mom = this.getMomentum();
//		double mass = 0.0d;
//		for (T p : this) {
//			mass += p.getMass();
//		}
//		return mom.dotProduct(mom) / (2.0d * mass);
	}

	public double getUEnergy() {
		double res = 0;
		int size = size();
		for (int i = 0; i < size; i++) {
			T p1 = get(i);
			for (int j = 0; j < size; j++) {
				if (i != j) {
					T p2 = get(j);
					// TODO figure out, if I should divide!
					res += p1.getPotentialEnergy(p2);
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
			res.add(p.getSpeed().multiply(p.getMass(), true), false);
		}
		return res;
	}

	@SuppressWarnings("unchecked")
	public V getForceFor(T particle) {
		V force = zeroPosFactory.get();
		for (T p1 : this) {
			// Here was condition such that particles should not be the same, but it was
			// deleted to allow even crazier systems
			force.add(p1.getForceOn(particle), false);
		}
		return (V) force.multiply(1.0d / particle.getMass(), false);
	}

	public MultiplicativeMatrix<Double> getJakobiFor(T particle) {
		MultiplicativeMatrix<Double> res = get(0).getForceJakobi(particle);
		for (int i = 1; i < size(); i++) {
			res.add(get(i).getForceJakobi(particle), false);
		}
		return res.multiply(1.0d / particle.getMass(), false);
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
	public static interface IntegrationPolicy<P, V> {
		/**
		 * Defines policy for integration
		 *
		 * @param current   Current value.
		 * @param deriv     Value derivative.
		 * @param parameter Current (absolute) parameter value.
		 * @param delta     Parameter delta
		 * @return Value change.
		 */
		// TODO change to suit Verlet
		public V integrate(P parameter, P delta, V current, Iterable<V> derivs);
	}

	@Override
	public void advance(Double dt) {
		@SuppressWarnings("unused")
		double mass;
		LinkedHashMap<T, V> assignedForces = new LinkedHashMap<T, V>(0);
		LinkedHashMap<T, MultiplicativeMatrix<Double>> assignedJakobi = new LinkedHashMap<T, MultiplicativeMatrix<Double>>(
				0);
		V force;
		MultiplicativeMatrix<Double> jakobi;
		for (T p : this) {
			force = getForceFor(p);
			jakobi = getJakobiFor(p);
			if (Settings.SEQUENCED_REACTION) {
				reactToForce(p, dt, force, jakobi);
			} else {
				assignedForces.put(p, force);
				assignedJakobi.put(p, jakobi);
			}
		}
		if (!Settings.SEQUENCED_REACTION) {

			// Belong to feature below
//			double dt2d2 = (dt * dt) / 2;
//			double ddt = 1 / dt;
//			:(

			for (Entry<T, V> e : assignedForces.entrySet()) {
				/*
				 * Beta-feature: Verlet integration
				 * https://en.wikipedia.org/wiki/Verlet_integration
				 */
				T p = e.getKey();
				reactToForce(p, dt, e.getValue(), assignedJakobi.get(p));
			}
		}
		time += dt;
	}

	private void reactToForce(T particle, double dt, V force, MultiplicativeMatrix<Double> jakobi) {
		if (Settings.VERLET_INTEGRATION) {
			@SuppressWarnings("unchecked")
			V move = (V) particle.getLastPos().multiply(-1.0d, true).add(particle.getPosition(), false)
					.add(force.multiply((dt * dt) / 2 / particle.getMass(), false), false);
			particle.stepMove(move);
			@SuppressWarnings("unchecked")
			V tmp = (V) particle.getSpeed().multiply(-1.0d, true).add(move.multiply(1 / dt, false), false);
			particle.stepAccelerate(tmp);
		} else {
			@SuppressWarnings("unchecked")
			V dFdt = (V) particle.getSpeed().multiplyRight(jakobi, true);
			particle.stepMove(positionPolicy.integrate(time, dt, particle.getPosition(),
					Arrays.asList(particle.getSpeed(), force, dFdt)));
			particle.stepAccelerate(speedPolicy.integrate(time, dt, particle.getSpeed(), Arrays.asList(force, dFdt)));
		}
	}

	public int[] distributeVelocity(Function<Double, Integer> distributor, int distLength) {
		int[] res = new int[distLength];
		for (T p : this) {
			res[Math.min(distributor.apply(p.getAbsSpeed()), distLength - 1)]++;
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

	public double calculateParameter(Function<T, Double> parameterGetterIn, BiFunction<Double, Double, Double> combiner,
			double beginValue) {
		double res = beginValue;
		for (T p : this) {
			res = combiner.apply(res, parameterGetterIn.apply(p));
		}
		return res;
	}

	@Override
	public String simulationState() {
		return String.format("Name:%s, Particles:%s", this.getClass().getSimpleName(), toString());
	}

	@Override
	public Double getCurrentParameter() {
		return time;
	}
}
