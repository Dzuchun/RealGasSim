package dzuchun.sim.simplegas;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import dzuchun.lib.graph.Point2DGrapher;
import dzuchun.lib.math.GeometricVector;
import dzuchun.lib.math.GeometricVector2D;
import dzuchun.lib.sim.Simulator;
import dzuchun.sim.simplegas.ParticleSystem.SimpleChecker;
import dzuchun.sim.simplegas.SimpleParticle2D.ContinumParticle2D;

public class SimpleGas {
	private static File outputFile;
	private static OutputStream outputStream;
	private static Workbook workbook;
	private static Sheet sheet;
	private static short lastRow = 0;
	private static double lastSave = 0.0d;

	private static ExperimentFrame<ContinumParticle2D> frame;
	private static Simulator<ParticleSystem<GeometricVector2D, ContinumParticle2D>, Double, SimpleChecker<GeometricVector2D, ContinumParticle2D>> simulator;
	private static long lastTimeRepainted;
	private static double lastCallback;

	public static void main(String[] args) {
		if (Settings.ENABLE_FILESAVE) {
			try {
				SimpleGas.outputFile = new File(Settings.SAVES_FORDER.concat(String.format(Settings.FILENAME_FORMAT)));
				SimpleGas.outputFile.createNewFile();
				SimpleGas.outputStream = new FileOutputStream(SimpleGas.outputFile);
				SimpleGas.workbook = new HSSFWorkbook();
				SimpleGas.sheet = SimpleGas.workbook.createSheet("Rawdata");
				Row row = SimpleGas.sheet.createRow(SimpleGas.lastRow++);
				SimpleGas.generateTableHeader(row);
//				saveToFile();
			} catch (IOException e) {
				System.err.println("Error on save file setup:");
				e.printStackTrace();
			}
		}
		final ArrayList<ContinumParticle2D> particles = new ArrayList<ContinumParticle2D>(0);
		Random random = new Random();
//		Supplier<Double> velocityDistributor =() -> 10 * (random.nextDouble() - 0.5);
		Supplier<Double> velocityDistributor = new Supplier<Double>() {
			private final double ABS = 10;

			private boolean x = true;
			private double ang;

			@Override
			public java.lang.Double get() {
				if (x) {
					x = false;
					ang = random.nextDouble() * Math.PI * 2;
					return ABS * Math.cos(ang);
				} else {
					x = true;
					return ABS * Math.sin(ang);
				}

			}
		};
		Stream.generate(ContinumParticle2D.creator(() -> 2 * Settings.CONTINUUM_MAX_Y * (random.nextDouble() - 0.5),
				velocityDistributor)).limit(Settings.PARTICLES).peek(particle -> particles.add(particle)).count();
		System.out.println("Particles at main:" + particles.toString());
		Point2DGrapher<ContinumParticle2D> grapher = new Point2DGrapher.Properties<ContinumParticle2D>(
				() -> SimpleGas.simulator.getSimulation()).setBackColor(Color.WHITE).setPointColor(Color.green)
						.setSize(1).setTransform((g, c) -> {
//							Dimension canvasSize = c.getSize();
							g.scale(c.getWidth() / Settings.CONTINUUM_MAX_X, -c.getHeight() / Settings.CONTINUUM_MAX_Y);
							g.translate(Settings.CONTINUUM_MAX_X_HALVED, -Settings.CONTINUUM_MAX_Y_HALVED);
						}).setPointRenderer((graphics, point, size) -> {
							int sizeDoubled = size * 2;
//							Color color = graphics.getColor();
//							graphics.setColor(Color.red);
//							graphics.drawLine((int) point.x, (int) point.y,
//									(int) (point.x + point.lastAcc.x * size * 1000),
//									(int) (point.y + point.lastAcc.y * size * 1000));
//							graphics.setColor(color);
							graphics.fillOval((int) point.getX() - size, (int) point.getY() - size, sizeDoubled,
									sizeDoubled);
							graphics.drawLine((int) point.getX(), (int) point.getY(),
									(int) (point.getX() + (point.getSpeed().getCoord(0) * size * 2)),
									(int) (point.getY() + (point.getSpeed().getCoord(1) * size * 2)));
						}).setSize(2).build();

		SimpleChecker<GeometricVector2D, ContinumParticle2D> checker = new SimpleChecker<GeometricVector2D, ContinumParticle2D>() {

			@Override
			public Double check(ParticleSystem<GeometricVector2D, ContinumParticle2D> base,
					ParticleSystem<GeometricVector2D, ContinumParticle2D> candidate, Double dt)
					throws dzuchun.lib.sim.ISimulatable.Checker.CannotProceedException {
				long current = System.currentTimeMillis();
				if ((current - SimpleGas.lastTimeRepainted) > Settings.MIN_REPAINT_TIME) {
					grapher.drawFrame();
					grapher.repaint();
					SimpleGas.lastTimeRepainted = current;
				}
				double baseEnergy = base.getTotalEnergy();
				double candidateEnergy = candidate.getTotalEnergy();
				GeometricVector2D baseMomentum = base.getMomentum();
				GeometricVector2D candidateMomentum = candidate.getMomentum();
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
//						System.out.println("Warning! Could not handle energy/momentum change");
						if (Settings.PAUSE_ON_CRITICAL_ENERGY && (Math.abs(Math.log10((candidate.getTotalEnergy()
								/ base.getTotalEnergy()))) > Settings.CRITICAL_ENERGY_CHANGE)) {
							System.out.println("Critical energy change got, pausing");
							SimpleGas.frame.toggleButton.doClick();
							System.out.println(SimpleGas.simulator.getLastReport());
						}
					}
					return null;
				}
			}
		};
		ParticleSystem<GeometricVector2D, ContinumParticle2D> system = new ParticleSystem<GeometricVector2D, ContinumParticle2D>(
				particles, (p, v, t, dt) -> new GeometricVector2D(v.scale(dt / 2.0d, true)),
				(v, a, t, dt) -> new GeometricVector2D(a.scale(dt, true)), GeometricVector2D::new);
		SimpleGas.simulator = new Simulator<ParticleSystem<GeometricVector2D, ContinumParticle2D>, Double, ParticleSystem.SimpleChecker<GeometricVector2D, ContinumParticle2D>>(
				system, Settings.DT, checker, () -> SimpleGas.frame.keepWorking(), sim -> {
					ParticleSystem<GeometricVector2D, ContinumParticle2D> systemState = sim.getSimulation();
					double energy = systemState.getTotalEnergy();
					double uEnergy = systemState.getUEnergy();
					@SuppressWarnings("unused")
					double kEnergy = systemState.getKineticEnergy();
					double currentTime = systemState.getCurrentTime();
					double dt = currentTime - SimpleGas.lastCallback;
					SimpleGas.lastCallback = currentTime;
					GeometricVector2D momentum = systemState.getMomentum();
					SimpleGas.frame.setDisplayedEnergy(energy, uEnergy);
					SimpleGas.frame.setDisplayedMomentum(momentum.getX(), momentum.getY(), 0);
					SimpleGas.frame.setDisplayedTime(currentTime, dt);
					SimpleGas.frame.redestributeVelocity();
					if (Settings.ENABLE_FILESAVE) {
						if ((currentTime - SimpleGas.lastSave) >= Settings.SAVE_INTERVAL) {
//							System.out.println("Saving state...");
							SimpleGas.lastSave = currentTime;
							Map<String, Object> state = new LinkedHashMap<String, Object>();
							state.put("time", currentTime);
							state.put("energy", energy);
							state.put("veldist", SimpleGas.frame.distributionFrame.getDistribution());

							Row row = SimpleGas.sheet.createRow(SimpleGas.lastRow++);
							SimpleGas.writeStateToRow(row, state);
//							saveToFile();
						}
					}
				});
		SimpleGas.frame = new ExperimentFrame<ContinumParticle2D>(grapher, SimpleGas.simulator);
		SimpleGas.frame.setVisible(true);
		SimpleGas.simulator.start();
	}

	public static final DecimalFormat VELDIST_FORMAT = new DecimalFormat("###0.0000");

	public static void generateTableHeader(Row row) {
		int currentCell = 0;
		for (String param : Settings.SAVE_FILE_FORMAT) {
			if (param.equals("veldist")) {
				Cell cell = row.createCell(currentCell++);
				cell.setCellValue("Veldist:");
				for (int i = 0; i < Settings.DISTRIBUTION_SIZE; i++) {
					cell = row.createCell(currentCell);
					cell.setCellValue(
							String.format("v=[%s,%s]", SimpleGas.VELDIST_FORMAT.format(i * Settings.DISTRIBUTION_STEP),
									SimpleGas.VELDIST_FORMAT.format((i + 1) * Settings.DISTRIBUTION_STEP)));
					SimpleGas.sheet.autoSizeColumn(currentCell);
					currentCell++;
				}
			} else {
				Cell cell = row.createCell(currentCell);
				cell.setCellValue(param);
				SimpleGas.sheet.autoSizeColumn(currentCell);
				currentCell++;
			}
		}

	}

	public static final DecimalFormat TIME_FORMAT = new DecimalFormat("###0.0");

	@SuppressWarnings("unchecked")
	public static void writeStateToRow(Row row, Map<String, Object> state) {
		int currentCell = 0;
		for (String param : Settings.SAVE_FILE_FORMAT) {
			if (param.equals("veldist")) {
				currentCell++;
				ArrayList<Double> velDist = (ArrayList<java.lang.Double>) state.get(param);
				for (double d : velDist) {
					row.createCell(currentCell++).setCellValue(d);
				}
			} else {
				Object o = state.get(param);
				row.createCell(currentCell++).setCellValue(o == null ? "" : o.toString());
			}
		}
	}

	public static void closeAndSave() {
		try {
			SimpleGas.workbook.write(SimpleGas.outputStream);
			SimpleGas.workbook.close();
			SimpleGas.outputStream.flush();
		} catch (IOException e) {
			System.err.println("Error on workbook close");
			e.printStackTrace();
		}
	}

}

class SimpleParticle2D extends Particle2D {

	public SimpleParticle2D(double x, double y, double vx, double vy) {
		super(x, y);
		speed = new GeometricVector2D(vx, vy);
	}

	@Override
	public double getX() {
		return coords.get(0);
	}

	@Override
	public double getY() {
		return coords.get(1);
	}

	@Override
	public GeometricVector2D getForce(Particle<GeometricVector2D> particle) {
		GeometricVector position = scale(-1.0d, true).add(particle.getPosition(), false).scale(-1.0d, false);
		double distanceSq = position.dotProduct(position);
		double d = SimpleParticle2D.SIGMA_SQ / distanceSq;
		double k = ((24.0d * Settings.EPSILON) / Math.sqrt(distanceSq)) * Math.pow(d, 3) * (1 - (2 * Math.pow(d, 3)));
		return new GeometricVector2D(position.scale(k, false));
	}

	protected static final double SIGMA_SQ = Settings.SIGMA * Settings.SIGMA;

	/**
	 * Lennard-Jones potential
	 */
	@Override
	public double getPotential(Particle<GeometricVector2D> particle) {
		GeometricVector position = scale(-1.0d, true).add(particle.getPosition(), false).scale(-1.0d, false);
		double d = Math.pow(SimpleParticle2D.SIGMA_SQ / position.dotProduct(position), 3);
		return 4.0d * Settings.EPSILON * d * (d - 1);
	}

	@Override
	public double getMass() {
		return Settings.MASS;
	}

	@Override
	public SimpleParticle2D copy() {
		return new SimpleParticle2D(getX(), getY(), speed.getCoord(0), speed.getCoord(1));
	}

	@Override
	public String toString() {
		return String.format("[Particle at (%s,%s), with speed (%s,%s)]", getX(), getY(), speed.getCoord(0),
				speed.getCoord(1));
	}

	static class ContinumParticle2D extends SimpleParticle2D {

		public static Supplier<ContinumParticle2D> creator(Supplier<java.lang.Double> posDistribution,
				Supplier<java.lang.Double> velocityDistribution) {
			return () -> new ContinumParticle2D(posDistribution.get(), posDistribution.get(),
					velocityDistribution.get(), velocityDistribution.get());
		}

		public ContinumParticle2D(double x, double y, double vx, double vy) {
			super(x, y, vx, vy);
		}

		@Override
		public void stepMove(GeometricVector2D step) {
			super.stepMove(step);
			if (Math.abs(coords.get(0)) > Settings.CONTINUUM_MAX_X_HALVED) {
				if (coords.get(0) < 0.0d) {
					addToCoord(0, Settings.CONTINUUM_MAX_X);
				} else {
					addToCoord(0, -Settings.CONTINUUM_MAX_X);
				}
			}
			if (Math.abs(coords.get(1)) > Settings.CONTINUUM_MAX_Y_HALVED) {
				if (coords.get(1) < 0.0d) {
					addToCoord(1, Settings.CONTINUUM_MAX_Y);
				} else {
					addToCoord(1, -Settings.CONTINUUM_MAX_Y);
				}
			}
//			System.out.println(String.format("I'm not out-of-continuum now: (%s,%s)", x, y));
		}

		@Override
		public ContinumParticle2D copy() {
			return new ContinumParticle2D(getX(), getY(), speed.getCoord(0), speed.getCoord(1));
		}

//		@Override
//		public double distanceSq(Point2D pt) {
//			double d1 = Math.abs(pt.getX() - this.getX());
//			if (d1 > SimpleGas.CONTINUUM_MAX_X_HALVED)
//				d1 = SimpleGas.CONTINUUM_MAX_X - d1;
//			double d2 = Math.abs(pt.getY() - this.getY());
//			if (d2 > SimpleGas.CONTINUUM_MAX_X_HALVED)
//				d1 = SimpleGas.CONTINUUM_MAX_X - d2;
//			return d1 * d1 + d2 * d2;
//		}

	}

}
