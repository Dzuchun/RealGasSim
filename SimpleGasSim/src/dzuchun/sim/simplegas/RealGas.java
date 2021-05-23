package dzuchun.sim.simplegas;

import java.util.ArrayList;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.swing.SwingUtilities;

import dzuchun.lib.math.GeometricVector3D;
import dzuchun.lib.sim.Simulator;
import dzuchun.sim.simplegas.ParticleSystem.SimpleChecker;

public class RealGas {

//	private static Simulator<ParticleSystem<GeometricVector3D, ContinuumParticle3D>, Double, SimpleChecker<GeometricVector3D, ContinuumParticle3D>> simulator;
	private static RealGasFrame frame;

	public static void main(String[] args) {

		// Creating checker - object that determines system validity and next argument
		// change
		SimpleChecker<GeometricVector3D, ContinuumParticle3D> checker = new SimpleChecker<GeometricVector3D, ContinuumParticle3D>() {

			@Override
			public Double check(ParticleSystem<GeometricVector3D, ContinuumParticle3D> base,
					ParticleSystem<GeometricVector3D, ContinuumParticle3D> candidate, Double dt) {
				long current = System.currentTimeMillis();
				double baseEnergy = base.getTotalEnergy();
				double candidateEnergy = candidate.getTotalEnergy();
				GeometricVector3D baseMomentum = base.getMomentum();
				GeometricVector3D candidateMomentum = candidate.getMomentum();
				double energyChange = (candidateEnergy / baseEnergy) - 1.0d;
				double momentumXChange = (candidateMomentum.getX() / baseMomentum.getX()) - 1.0d;
				double momentumYChange = (candidateMomentum.getY() / baseMomentum.getY()) - 1.0d;
				double allowedEnergyChange = (Settings.MAX_RELATIVE_ENERGY_CHANGE * dt) / Settings.DT;
				double allowedMomentumChange = (Settings.MAX_RELATIVE_MOMENTUM_CHANGE * dt) / Settings.DT;
				if ((dt > Settings.MIN_DT) && ((Math.abs(energyChange) > allowedEnergyChange)
						|| (Math.abs(momentumXChange) > allowedMomentumChange)
						|| (Math.abs(momentumYChange) > allowedMomentumChange))) {
					return Math.min(Settings.MIN_DT, dt / 2.0d);
				} else {
					// will return null
					if (dt <= Settings.MIN_DT) {
//					System.out.println("Warning! Could not handle energy/momentum change");
						if (Settings.PAUSE_ON_CRITICAL_ENERGY && (Math.abs(Math.log10((candidate.getTotalEnergy()
								/ base.getTotalEnergy()))) > Settings.CRITICAL_ENERGY_CHANGE)) {
							System.out.println("Critical energy change got, pausing");
							// TODO pause 3D
						}
					}

					RealGas.frame.stepCallback(candidate);
					return null;
				}
			}
		};

		final Random random = new Random();
		final Supplier<GeometricVector3D> position = GeometricVector3D.generatorBounded(GeometricVector3D.ZERO,
				new GeometricVector3D(Settings.CONTINUUM_MAX_X, Settings.CONTINUUM_MAX_Y, Settings.CONTINUUM_MAX_Z),
				random::nextDouble);
		final Supplier<Double> declination = () -> Math.acos((random.nextDouble() * 2) - 1);
		final Supplier<GeometricVector3D> velocity = GeometricVector3D.generatorSetLength(1.0d, random::nextDouble,
				declination);
		final Supplier<ContinuumParticle3D> particle = ContinuumParticle3D.creator(position, velocity);

		final Supplier<Simulator<ParticleSystem<GeometricVector3D, ContinuumParticle3D>, Double, SimpleChecker<GeometricVector3D, ContinuumParticle3D>>> sup = () -> {
			// Creating array and filling it with particles
			ArrayList<ContinuumParticle3D> particles = new ArrayList<ContinuumParticle3D>(0);
			Stream.generate(particle).limit(Settings.PARTICLES).peek(p -> particles.add(p)).count();

			// Creating system - object that holds all particles and defines usefull methods
			// to interact with system
			ParticleSystem<GeometricVector3D, ContinuumParticle3D> system = new ParticleSystem<GeometricVector3D, ContinuumParticle3D>(
					particles, (p, v, t, dt) -> new GeometricVector3D(v.scale(dt / 2.0d, true)),
					(v, a, t, dt) -> new GeometricVector3D(a.scale(dt, true)), GeometricVector3D::new);

			// Creating simulator - thread that performs simulation logic and callbacks
			// TODO add keepworking
			return new Simulator<ParticleSystem<GeometricVector3D, ContinuumParticle3D>, Double, ParticleSystem.SimpleChecker<GeometricVector3D, ContinuumParticle3D>>(
					system, Settings.DT, checker, () -> true, s -> {
					});
		};
//		simulator.start(); // simulator is daemon, so JVM shuts down if no other thread is active

		SwingUtilities.invokeLater(() -> {
			RealGas.frame = new RealGasFrame(sup);
			RealGas.frame.setVisible(true);
		});
	}

}
