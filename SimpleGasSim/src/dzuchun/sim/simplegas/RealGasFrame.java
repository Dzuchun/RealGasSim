package dzuchun.sim.simplegas;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
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

import org.apache.poi.hssf.record.chart.BeginRecord;

import dzuchun.lib.io.DoubleResult;
import dzuchun.lib.io.SpreadsheetHelper;
import dzuchun.lib.io.SpreadsheetHelper.Result;
import dzuchun.lib.math.GeometricVector3D;
import dzuchun.lib.math.PrecisedValue;
import dzuchun.lib.sim.Simulator;
import dzuchun.sim.simplegas.ParticleSystem.SimpleChecker;

public class RealGasFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	private static final Dimension SMALL_SIZE = new Dimension(50, 20);
	private static final Dimension GRAPH_SIZE = new Dimension(200, 100);
	private static JLabel tmpLabel;

	private JButton startButton;
	private static final String[] START_BUTTON_MESSAGES = { "Start new", "Pause", "Resume" };
	private JButton endButton;
	private JButton saveButton;
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
	private static final String[] PARAMETER_NAMES = { "Time", "Kinetic_Energy", "Potential_Energy", "Total_Energy" };
	private Result result;
	private Clicker beginBatch;
	private boolean batch = false;

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
					// TODO write data if needed
					onEnd();
				}));
				endButton.setEnabled(false);
				this.add(saveButton = new Clicker("Save", e -> {
					// TODO save
					SpreadsheetHelper.saveResToFile(result, Settings.FILENAME_FORMAT);
					saveButton.setEnabled(false);
				}) {
					private static final long serialVersionUID = 1L;

					@Override
					public void setEnabled(boolean enabled) {
						super.setEnabled(enabled && Settings.ENABLE_FILESAVE);
					}
				});
				saveButton.setEnabled(false);
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
				this.add(beginBatch = new Clicker("Begin experiment", (e) -> {
					// TODO start simulation
					batch = true;
					maxTime = Double.parseDouble(timeField.getText());
					maxEnergyDelta = Double.parseDouble(energyField.getText());
					totalSim = Integer.parseInt(simulationsField.getText());
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

	public enum State {
		IDLE, RUNNING, PAUSED
	}

	private void onStart() {
		sim = simulators.get();
		startButton.setText(RealGasFrame.START_BUTTON_MESSAGES[1]);
		endButton.setEnabled(true);
		state = State.RUNNING;
		saveButton.setEnabled(false);
		timeField.setEnabled(false);
		energyField.setEnabled(false);
		simulationsField.setEnabled(false);
		if (Settings.ENABLE_FILESAVE) {
			result = new Result();
		}
		stepCallback(sim.getSimulation());
		beginEnergy = sim.getSimulation().getTotalEnergy();
//		System.out.println("peek: " + Thread.currentThread().getStackTrace()[2]);
		sim.start();
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
			batch = false;
			System.out.println("Finished sims: " + currentSim + " / " + totalSim);
		}
	}

	public void stepCallback(ParticleSystem<GeometricVector3D, ContinuumParticle3D> stateIn) {
		// Saving
		if (Settings.ENABLE_FILESAVE) {
			result.append(RealGasFrame.PARAMETER_NAMES[0], new DoubleResult(stateIn.getCurrentTime()));
			double kEnergy, uEnergy;
			result.append(RealGasFrame.PARAMETER_NAMES[1], new DoubleResult(kEnergy = stateIn.getKineticEnergy()));
			result.append(RealGasFrame.PARAMETER_NAMES[2], new DoubleResult(uEnergy = stateIn.getUEnergy()));
			result.append(RealGasFrame.PARAMETER_NAMES[3], new DoubleResult(kEnergy + uEnergy));
		}
		if (batch) {
			// TODO batch simulation
			boolean time = stateIn.getCurrentTime() > maxTime;
			boolean energy = Math.abs((stateIn.getTotalEnergy() / beginEnergy) - 1) > maxEnergyDelta;
			if (time || energy) {
				System.out.print("Simulation " + currentSim + " / " + totalSim + " was ended. Reason: ");
				if (energy)
					System.out.println("energy");
				if (time)
					System.out.println("time");
				onEnd();
				// TODO save data from it.

				currentSim++;
				if (currentSim < totalSim) {
					onStart();
				} else {
					System.out.println("Ended batch");
					batch = false;
					beginBatch.setEnabled(true);
					// TODO save data from batch
				}
			}
		}
	}
}
