package dzuchun.sim.simplegas;
// TODO write data if needed

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import org.apache.poi.ss.usermodel.Cell;

import dzuchun.lib.io.DoubleResult;
import dzuchun.lib.io.SpreadsheetHelper;
import dzuchun.lib.io.SpreadsheetHelper.Result;
import dzuchun.lib.io.StringResult;
import dzuchun.lib.math.GeometricVector3D;
import dzuchun.lib.math.MathUtil;
import dzuchun.lib.sim.DistributionResultGenerator;
import dzuchun.lib.sim.DistributionResultGenerator.DistributionResult;
import dzuchun.lib.sim.Simulator;
import dzuchun.sim.simplegas.ParticleSystem.SimpleChecker;

public class RealGasFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	private static final Dimension SMALL_SIZE = new Dimension(50, 20);
	private static final Dimension GRAPH_SIZE = new Dimension(200, 100);
	private static JLabel tmpLabel;

	// Bottom panel elements
	private JButton startButton;
	private static final String[] START_BUTTON_MESSAGES = { "Start new", "Pause", "Resume" };
	private JButton endButton;
	private JButton saveButton;
	private StabializingNote simSpeedField;
	private FormattedNote<Double> currentTime;
	private FormattedNote<Double> energyRatio;

	// RightPanel elements
	private JTextField timeField;
	private double maxTime;
	private JTextField energyField;
	private double maxEnergyDelta;
	private double beginEnergy;
	private JTextField simulationsField;
	private int totalSim;
	private int currentSim;
	private Supplier<Simulator<ParticleSystem<GeometricVector3D, ContinuumParticle3D>, Double, SimpleChecker<GeometricVector3D, ContinuumParticle3D>>> simulators;
	private Simulator<ParticleSystem<GeometricVector3D, ContinuumParticle3D>, Double, SimpleChecker<GeometricVector3D, ContinuumParticle3D>> sim;
	private State state = State.IDLE;
	private Clicker beginBatch;
	private boolean batch = false;

	// Savedata fields
	private static final String[] PARAMETER_NAMES = { "Time", "Kinetic Energy", "Potential Energy", "Total Energy",
			"Simulation №", "Begin Energy", "End Reason", "Velocity x", "Velocity y", "Velocity z", "Particles",
			"Velocity Distribution", "Min distance", "Velocity Distribution (Meaned)", "Mean Distance" };
	private Result result;
	private ArrayList<Result> tmpResult;

	/**
	 *
	 * @param simulatorsIn New simulations supplier. Be aware of supplying used
	 *                     simulations.
	 */
	public RealGasFrame(
			Supplier<Simulator<ParticleSystem<GeometricVector3D, ContinuumParticle3D>, Double, SimpleChecker<GeometricVector3D, ContinuumParticle3D>>> simulatorsIn) {
		setTitle("Real Gas");
//		this.setMinimumSize(new Dimension(200, 400));
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		Container pane = getContentPane();

		pane.setLayout(new BorderLayout(5, 5));
		pane.add(new JPanel() {
			private static final long serialVersionUID = 1L;
			{
				setBackground(new Color(0xCCCCFF));
				setLayout(new FlowLayout());
				this.add(startButton = new Clicker(RealGasFrame.START_BUTTON_MESSAGES[0], e -> {
					switch (state) {
					case IDLE:
						// on start
						batch = false;
						onStart();
						break;
					case PAUSED:
						// on pause
						onResume();
						break;
					case RUNNING:
						onPause();
						break;
					}
				}));
				this.add(endButton = new Clicker("Stop", e -> {
					onEnd();
				}));
				endButton.setEnabled(false);
				this.add(saveButton = new Clicker("Save", e -> {
					// Saving
					if (batch) {
						mergeBatchResults();
					}
					SpreadsheetHelper.saveResToFile(result, Settings.SAVES_FORDER + Settings.FILENAME_FORMAT);
					saveButton.setEnabled(false);
				}) {
					private static final long serialVersionUID = 1L;

					@Override
					public void setEnabled(boolean enabled) {
						super.setEnabled(enabled && Settings.ENABLE_FILESAVE);
					}
				});
				saveButton.setEnabled(false);
				this.add(simSpeedField = new StabializingNote("%.2f t/m", 10));
				this.add(currentTime = new FormattedNote<Double>("%.2f t", 0.0d));
				this.add(energyRatio = new FormattedNote<Double>("%.4f", 1.0d));
			}
		}, BorderLayout.PAGE_END);
		pane.add(new JPanel() {
			private static final long serialVersionUID = 1L;
			{
				setBackground(new Color(0xFFCCCC));
				setLayout(new GridLayout(2, 2, 5, 5));
				this.add(new JPanel() {
					private static final long serialVersionUID = 1L;
					{
						setLayout(new BorderLayout());
						this.add(new Note("Vx"), BorderLayout.PAGE_START);
						this.add(RealGasFrame.tmpLabel = new Note("Graph"), BorderLayout.CENTER);
						RealGasFrame.tmpLabel.setPreferredSize(RealGasFrame.GRAPH_SIZE);
						RealGasFrame.tmpLabel.setBackground(new Color(0xAAFFFF));
					}
				});
				this.add(new JPanel() {
					private static final long serialVersionUID = 1L;
					{
						setLayout(new BorderLayout());
						this.add(new Note("Vy"), BorderLayout.PAGE_START);
						this.add(RealGasFrame.tmpLabel = new Note("Graph"), BorderLayout.CENTER);
						RealGasFrame.tmpLabel.setPreferredSize(RealGasFrame.GRAPH_SIZE);
						RealGasFrame.tmpLabel.setBackground(new Color(0xAAFFFF));
					}
				});
				this.add(new JPanel() {
					private static final long serialVersionUID = 1L;
					{
						setLayout(new BorderLayout());
						this.add(new Note("Vz"), BorderLayout.PAGE_START);
						this.add(RealGasFrame.tmpLabel = new Note("Graph"), BorderLayout.CENTER);
						RealGasFrame.tmpLabel.setPreferredSize(RealGasFrame.GRAPH_SIZE);
						RealGasFrame.tmpLabel.setBackground(new Color(0xAAFFFF));
					}
				});
				this.add(new JPanel() {
					private static final long serialVersionUID = 1L;
					{
						setLayout(new BorderLayout());
						this.add(new Note("|V|"), BorderLayout.PAGE_START);
						this.add(RealGasFrame.tmpLabel = new Note("Graph"), BorderLayout.CENTER);
						RealGasFrame.tmpLabel.setPreferredSize(RealGasFrame.GRAPH_SIZE);
						RealGasFrame.tmpLabel.setBackground(new Color(0xAAFFFF));
					}
				});
			}
		}, BorderLayout.CENTER);
		pane.add(new JPanel() {
			private static final long serialVersionUID = 1L;
			{
				setBackground(new Color(0xFFFFAA));
				setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

				// Adding max time setting
				this.add(new Note("Max Time:"));
				this.add(timeField = new JTextField(Settings.DEFAULT_MAX_TIME + ""));

				// Adding batch simulation controller
				this.add(new Note("Max energy delta"));
				this.add(energyField = new JTextField(Settings.DEFAULT_MAX_ENERGY + ""));
				// Number of silmulations
				this.add(new Note("Simulations number:"));
				this.add(simulationsField = new JTextField(Settings.DEFAULT_SIMULATIONS + ""));
				this.add(beginBatch = new Clicker("Begin experiment", e -> {
					// Start batch simulation
					batch = true;
					maxTime = Double.parseDouble(timeField.getText());
					maxEnergyDelta = Double.parseDouble(energyField.getText());
					totalSim = Integer.parseInt(simulationsField.getText());
					tmpResult = new ArrayList<Result>(0);
					System.out.println(
							String.format("Starting batch. Max time: %s, Max energy delta: %s, Simulations: %s",
									maxTime, maxEnergyDelta, totalSim));
					currentSim = 1;
					onStart();
					beginBatch.setEnabled(false);
				}) {
					private static final long serialVersionUID = 1L;
					{
						setFont(getFont().deriveFont(Font.BOLD));
					}
				});

				this.add(new JCheckBox("Update graphs") {
					private static final long serialVersionUID = 1L;
					{
						setSelected(true);
					}
				});
			}
		}, BorderLayout.LINE_END);
		pack();

		simulators = simulatorsIn;
	}

	private static class Clicker extends JButton {
		private static final long serialVersionUID = 1L;

		public Clicker(String s, Consumer<ActionEvent> action) {
			super(s);
			addActionListener(e -> action.accept(e));
		}
	}

	private static class Note extends JLabel {
		private static final long serialVersionUID = 1L;

		public Note(String s) {
			super(s);
			setHorizontalAlignment(SwingConstants.CENTER);
			setVerticalAlignment(SwingConstants.CENTER);
			setOpaque(true);
			setBackground(new Color(0xCCFFCC));
			setPreferredSize(RealGasFrame.SMALL_SIZE);
		}
	}

	private static class StabializingNote extends Note {
		private static final long serialVersionUID = 1L;

		private String format;
		private double[] buffer;
		private int index;

		public StabializingNote(String formatIn, int bufferSizeIn) {
			super(String.format(formatIn, 0.0d));
			format = formatIn;
			buffer = new double[bufferSizeIn];
			for (int i = 1; i < bufferSizeIn; i++) {
				buffer[i] = Double.NaN;
			}
			index = 0;
		}

		public void addValue(double value) {
			buffer[index] = value;
			index = (index + 1) % buffer.length;
			updateDisplay();
		}

		public void clearBuffer() {
			buffer = new double[buffer.length];
			for (int i = 1; i < buffer.length; i++) {
				buffer[i] = Double.NaN;
			}
			updateDisplay();
		}

		public void updateDisplay() {
			double res = 0;
			int n = 0;
			for (double d : buffer) {
				if (!Double.isNaN(d)) {
					res += d;
					n++;
				}
			}
			res /= n;
			setText(String.format(format, res));
		}

	}

	public class FormattedNote<T> extends Note {
		private static final long serialVersionUID = 1L;

		private String format;

		public FormattedNote(String formatIn, T firstInput) {
			super(String.format(formatIn, firstInput));
			this.format = formatIn;
		}

		public void setValue(T value) {
			this.setText(String.format(format, value));
		}

	}

	public enum State {
		IDLE, RUNNING, PAUSED

	}

	private void onStart() {
		sim = simulators.get();
//		System.out.println("peek");
		startButton.setText(RealGasFrame.START_BUTTON_MESSAGES[1]);
		endButton.setEnabled(true);
		state = State.RUNNING;
		saveButton.setEnabled(false);
		timeField.setEnabled(false);
		energyField.setEnabled(false);
		simulationsField.setEnabled(false);
		beginBatch.setEnabled(false);
		beginEnergy = sim.getSimulation().getTotalEnergy();
		if (Settings.ENABLE_FILESAVE) {
			lastSave = -Settings.SAVE_INTERVAL;
			if (batch) {
				tmpResult.add(new Result());
			} else {
				result = new Result();
			}
		}
		stepCallback(sim.getSimulation());
//		System.out.println("peek: " + Thread.currentThread().getStackTrace()[2]);
		try {
			sim.start();
		} catch (IllegalThreadStateException e) {
			System.out.println("Cought illegal state exception:");
			e.printStackTrace();
		}
	}

	private void onResume() {
		sim.resumeSimulation();
		startButton.setText(RealGasFrame.START_BUTTON_MESSAGES[1]);
		state = State.RUNNING;
		saveButton.setEnabled(false);
	}

	private void onPause() {
		sim.pauseSimulaton();
		startButton.setText(RealGasFrame.START_BUTTON_MESSAGES[2]);
		state = State.PAUSED;
		saveButton.setEnabled(true);
		System.out.println("Simulated time: " + sim.getSimulation().getCurrentTime());
		if (batch) {
			System.out.println("Now running simulation " + currentSim + " / " + totalSim);
		}
	}

	private void onEnd() {
		sim.endSimulation();
		endButton.setEnabled(false);
		startButton.setText(RealGasFrame.START_BUTTON_MESSAGES[0]);
		state = State.IDLE;
		saveButton.setEnabled(true);
		timeField.setEnabled(true);
		energyField.setEnabled(true);
		simulationsField.setEnabled(true);
		System.out.println("Simulated time: " + sim.getSimulation().getCurrentTime());
		if (batch) {
//			batch = false;
			System.out.println("Finished sims: " + currentSim + " / " + totalSim);
		}
//		else {
//			stepCallback(sim.getSimulation());
//		}
		beginBatch.setEnabled(true);
		simSpeedField.clearBuffer();
		currentTime.setValue(0.0d);
		energyRatio.setValue(1.0d);
	}

	private double lastSaveRealTime = System.currentTimeMillis();
	private double lastSave;

	private DistributionResultGenerator<Integer> veldistGenerator = new DistributionResultGenerator<Integer>(
			Settings.DISTRIBUTION_SIZE,
			n -> String.format("v=[%.2f, %.2f]", n * Settings.DISTRIBUTION_STEP, (n + 1) * Settings.DISTRIBUTION_STEP));
	private DistributionResultGenerator<Double> veldistGeneratorD = new DistributionResultGenerator<Double>(
			Settings.DISTRIBUTION_SIZE,
			n -> String.format("v=[%.2f, %.2f]", n * Settings.DISTRIBUTION_STEP, (n + 1) * Settings.DISTRIBUTION_STEP));
	private BiConsumer<Integer, Cell> writeFunctionIntegers = (v, c) -> c.setCellValue((double) v);
	private BiConsumer<Double, Cell> writeFunctionDouble = (v, c) -> c.setCellValue(v);

	public void stepCallback(ParticleSystem<GeometricVector3D, ContinuumParticle3D> stateIn) {
		// Saving
		if (Settings.ENABLE_FILESAVE) {
			// Checking for interval passed
			double currentTime = stateIn.getCurrentTime();
			if (((currentTime - lastSave) >= Settings.SAVE_INTERVAL)) {

				// Updating simulation speed
				double currentRealTime = System.currentTimeMillis();
				double simS = ((currentTime - lastSave) / (currentRealTime - lastSaveRealTime)) * 60000.0d;
				simSpeedField.addValue(simS);
				this.currentTime.setValue(currentTime);
				energyRatio.setValue(stateIn.getTotalEnergy() / beginEnergy);
				lastSaveRealTime = currentRealTime;
				lastSave = currentTime;

				if (batch) {
					Result resu = tmpResult.get(currentSim - 1);
					resu.append(RealGasFrame.PARAMETER_NAMES[0], new DoubleResult(currentTime));
					double kEnergy, uEnergy;
					resu.append(RealGasFrame.PARAMETER_NAMES[1],
							new DoubleResult(kEnergy = stateIn.getKineticEnergy()));
					resu.append(RealGasFrame.PARAMETER_NAMES[2], new DoubleResult(uEnergy = stateIn.getUEnergy()));
					resu.append(RealGasFrame.PARAMETER_NAMES[3], new DoubleResult(kEnergy + uEnergy));
					resu.append(RealGasFrame.PARAMETER_NAMES[7],
							new DoubleResult(stateIn.calculateParameter(p -> p.speed.getX(), (r, d) -> r + d, 0)));
					resu.append(RealGasFrame.PARAMETER_NAMES[8],
							new DoubleResult(stateIn.calculateParameter(p -> p.speed.getY(), (r, d) -> r + d, 0)));
					resu.append(RealGasFrame.PARAMETER_NAMES[9],
							new DoubleResult(stateIn.calculateParameter(p -> p.speed.getZ(), (r, d) -> r + d, 0)));
					// Adding veldist
					int[] distrib = stateIn.distributeVelocity(d -> (int) (d / Settings.DISTRIBUTION_STEP),
							Settings.DISTRIBUTION_SIZE);
					Integer[] distribIntegers = new Integer[distrib.length];
					for (int i = 0; i < distrib.length; i++) {
						distribIntegers[i] = distrib[i];
					}
					resu.append(RealGasFrame.PARAMETER_NAMES[11],
							veldistGenerator.generate(distribIntegers, writeFunctionIntegers));
					// Adding minimum distance
					final double maxDistance = 10 * Settings.CONTINUUM_MAX_X;
					resu.append(RealGasFrame.PARAMETER_NAMES[12], new DoubleResult(stateIn.calculateParameter(p1 -> {
						int size = stateIn.size();
						double res = maxDistance;
						for (int i = 0; i < size; i++) {
							ContinuumParticle3D p2 = stateIn.get(i);
							if (p1.equals(p2)) {
								continue;
							}
							double dist = p1.getRelativePos(p2).getLength();
							if (dist < res) {
								res = dist;
							}
							return res;
						}
						return 0.0d;
					}, Math::min, maxDistance)));
				} else {
					result.append(RealGasFrame.PARAMETER_NAMES[0], new DoubleResult(currentTime));
					// Adding particles
					result.append(RealGasFrame.PARAMETER_NAMES[10], new DoubleResult((double) stateIn.size()));
					// Adding energies
					double kEnergy, uEnergy;
					result.append(RealGasFrame.PARAMETER_NAMES[1],
							new DoubleResult(kEnergy = stateIn.getKineticEnergy()));
					result.append(RealGasFrame.PARAMETER_NAMES[2], new DoubleResult(uEnergy = stateIn.getUEnergy()));
					result.append(RealGasFrame.PARAMETER_NAMES[3], new DoubleResult(kEnergy + uEnergy));
					// Adding minimum distance
					final double maxDistance = 10 * Settings.CONTINUUM_MAX_X;
					result.append(RealGasFrame.PARAMETER_NAMES[12], new DoubleResult(stateIn.calculateParameter(p1 -> {
						int size = stateIn.size();
						double res = maxDistance;
						for (int i = 0; i < size; i++) {
							ContinuumParticle3D p2 = stateIn.get(i);
							if (p1.equals(p2)) {
								continue;
							}
							double dist = p1.getRelativePos(p2).getLength();
							if (dist < res) {
								res = dist;
							}
						}
						return res;
					}, Math::min, maxDistance)));
					// Adding mean distance
					result.append(RealGasFrame.PARAMETER_NAMES[14], new DoubleResult(stateIn.calculateParameter(p1 -> {
						int size = stateIn.size();
						double res = 0.0d;
						for (int i = 0; i < size; i++) {
							ContinuumParticle3D p2 = stateIn.get(i);
							if (p1.equals(p2)) {
								continue;
							}
							res += p1.getRelativePos(p2).getLength();
						}
						return res / size;
					}, Double::sum, 0.0d) / stateIn.size()));
					// Adding veldist
					int[] distrib = stateIn.distributeVelocity(d -> (int) (d / Settings.DISTRIBUTION_STEP),
							Settings.DISTRIBUTION_SIZE);
					Integer[] distribIntegers = new Integer[distrib.length];
					for (int i = 0; i < distrib.length; i++) {
						distribIntegers[i] = distrib[i];
					}
					@SuppressWarnings("rawtypes")
					DistributionResult velDist = veldistGenerator.generate(distribIntegers, writeFunctionIntegers);
					if (Settings.ENABLE_GAUSSIAN_MEAN) {
						// Applying gaussian blur
						velDist = veldistGeneratorD
								.generate(
										MathUtil.applyGaussianBlur(velDist.data(), Settings.SIGMA,
												d -> (double) (int) d, d -> d, new Double[Settings.DISTRIBUTION_SIZE]),
										writeFunctionDouble);
						result.append(RealGasFrame.PARAMETER_NAMES[13], velDist);
					} else {
						result.append(RealGasFrame.PARAMETER_NAMES[11], velDist);
					}
				}
			}

			if (batch) {
				// batch simulation
//			System.out.println("Callback at " + stateIn.getCurrentTime());
				boolean time = stateIn.getCurrentTime() > maxTime;
				boolean energy = Math.abs((stateIn.getTotalEnergy() / beginEnergy) - 1) > maxEnergyDelta;
				if (time || energy) {
					System.out.print("Simulation " + currentSim + " / " + totalSim + " was ended. Reason: ");
					if (energy) {
						System.out.println(String.format("energy (b:%s, e:%s)", beginEnergy, stateIn.getTotalEnergy()));
						if (Settings.ENABLE_FILESAVE) {
							tmpResult.get(currentSim - 1).append(RealGasFrame.PARAMETER_NAMES[6],
									new StringResult("energy"));
						}
					} else if (time) {
						System.out.println("time");
						if (Settings.ENABLE_FILESAVE) {
							tmpResult.get(currentSim - 1).append(RealGasFrame.PARAMETER_NAMES[6],
									new StringResult("time"));
						}
					}
					onEnd();
					currentSim++;
					System.out.println("------------------------------");
					if (currentSim <= totalSim) {
						onStart();
					} else {
						System.out.println("Ended batch");
						beginBatch.setEnabled(true);
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void mergeBatchResults() {
		result = new Result();
		ArrayList<DoubleResult> doubleParameter;
		ArrayList<DistributionResultGenerator<Integer>.DistributionResult> distributionParameter;
		double simno = 1;
		for (Result res : tmpResult) {
			result.append(RealGasFrame.PARAMETER_NAMES[4], new DoubleResult(simno));
			// Adding Time
			doubleParameter = (ArrayList<DoubleResult>) res.getValuesFor(RealGasFrame.PARAMETER_NAMES[0]);
			result.append(RealGasFrame.PARAMETER_NAMES[0], doubleParameter.get(doubleParameter.size() - 1));
			// Adding energies
			doubleParameter = (ArrayList<DoubleResult>) res.getValuesFor(RealGasFrame.PARAMETER_NAMES[3]);
			result.append(RealGasFrame.PARAMETER_NAMES[5], doubleParameter.get(0));
			result.append(RealGasFrame.PARAMETER_NAMES[3], doubleParameter.get(doubleParameter.size() - 1));
			// Adding end reason
			try {
				result.append(RealGasFrame.PARAMETER_NAMES[6],
						res.getValuesFor(RealGasFrame.PARAMETER_NAMES[6]).get(0));
			} catch (NullPointerException e) {
				System.out.println(String.format("Simulation №%s had no \"End Reason\" parameter", simno));
			}
			// Adding vx
			doubleParameter = (ArrayList<DoubleResult>) res.getValuesFor(RealGasFrame.PARAMETER_NAMES[7]);
			result.append(RealGasFrame.PARAMETER_NAMES[7], doubleParameter.get(doubleParameter.size() - 1));
			// Adding vy
			doubleParameter = (ArrayList<DoubleResult>) res.getValuesFor(RealGasFrame.PARAMETER_NAMES[8]);
			result.append(RealGasFrame.PARAMETER_NAMES[8], doubleParameter.get(doubleParameter.size() - 1));
			// Adding vz
			doubleParameter = (ArrayList<DoubleResult>) res.getValuesFor(RealGasFrame.PARAMETER_NAMES[9]);
			result.append(RealGasFrame.PARAMETER_NAMES[9], doubleParameter.get(doubleParameter.size() - 1));
			// Adding minimum distance
			doubleParameter = (ArrayList<DoubleResult>) res.getValuesFor(RealGasFrame.PARAMETER_NAMES[12]);
			result.append(RealGasFrame.PARAMETER_NAMES[12], doubleParameter.get(doubleParameter.size() - 1));
			// Adding veldist
			distributionParameter = (ArrayList<DistributionResultGenerator<Integer>.DistributionResult>) res
					.getValuesFor(RealGasFrame.PARAMETER_NAMES[11]);
			// Adding veldist
			@SuppressWarnings("rawtypes")
			DistributionResult velDist = distributionParameter.get(distributionParameter.size() - 1);
			if (Settings.ENABLE_GAUSSIAN_MEAN) {
				velDist = veldistGeneratorD.generate(MathUtil.applyGaussianBlur(velDist.data(), Settings.SIGMA,
						d -> (double) (int) d, d -> d, new Double[Settings.DISTRIBUTION_SIZE]), writeFunctionDouble);
				result.append(RealGasFrame.PARAMETER_NAMES[13], velDist);
			} else {
				result.append(RealGasFrame.PARAMETER_NAMES[11], velDist);
			}

			simno++;
		}
	}
}
