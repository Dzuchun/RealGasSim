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
	}

	public double getKineticEnergy() {
		double res = 0;
		for (T p : this) {
//			if (p.kineticEnergy() > 1000.0d) {
//				p=p;
//			}
			res += p.kineticEnergy();
		}
		return res;
//		return parallelStream().mapToDouble(Particle::kineticEnergy).sum();
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
		public V integrate(P parameter, P delta, V current, Iterable<V> derivs);
	}

	@Override
	public void advance(Double dt) {
		double mass;
		LinkedHashMap<T, V> assignedForces = new LinkedHashMap<T, V>(0);
		V force;
		for (T p : this) {
			mass = p.getMass();
			force = zeroPosFactory.get();
			for (T p1 : this) {
				if (p == p1) {
					continue;
				}
				V force1 = p1.getForceOn(p);
				force.add(force1, false);
			}
			force.scale(1 / mass, false);
			assignedForces.put(p, force);
//			p.stepAccelerate(speedPolicy.integrate(time, dt, p.getSpeed(), Arrays.asList(force)));
		}
//		for (T p : this) {
//			p.stepMove(positionPolicy.integrate(time, dt, p.getPosition(), Arrays.asList(p.getSpeed())));
//		}

		// Belong to feature below
		double dt2d2 = (dt * dt) / 2;
		double ddt = 1 / dt;
		for (Entry<T, V> e : assignedForces.entrySet()) {
			/*
			 * Beta-feature: Verlet integration
			 * https://en.wikipedia.org/wiki/Verlet_integration
			 */
			if (Settings.VERLET_INTEGRATION) {
				T p = e.getKey();
				@SuppressWarnings("unchecked")
				V move = (V) p.getLastPos().scale(-1.0d, true).add(p.getPosition(), false)
						.add(e.getValue().scale(dt2d2, false), false);
				p.stepMove(move);
				@SuppressWarnings("unchecked")
				V tmp = (V) p.getSpeed().scale(-1.0d, true).add(move.scale(ddt, false), false);
				p.stepAccelerate(tmp);
			} else {

				e.getKey().stepMove(positionPolicy.integrate(time, dt, e.getKey().getPosition(),
						Arrays.asList(e.getKey().getSpeed(), e.getValue())));
				e.getKey().stepAccelerate(
						speedPolicy.integrate(time, dt, e.getKey().getSpeed(), Arrays.asList(e.getValue())));
			}
		}
		time += dt;
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

	public double getCurrentTime() {
		return time;
	}
}
