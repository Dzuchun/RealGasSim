package dzuchun.sim.simplegas.test;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.WindowConstants;

import dzuchun.lib.graph.Point2DGrapher;
import dzuchun.lib.math.GeometricVector2D;
import dzuchun.lib.sim.Simulator;
import dzuchun.sim.simplegas.ParticleSystem;
import dzuchun.sim.simplegas.ParticleSystem.SimpleChecker;
import dzuchun.sim.simplegas.Settings;

public class ExperimentFrame<T extends Particle2D> extends JFrame {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private static final Boolean SAVING_VELDIST = true;

	@SuppressWarnings("unused")
	private final Simulator<ParticleSystem<GeometricVector2D, T>, ?, SimpleChecker<GeometricVector2D, T>> simulator;
	public final Point2DGrapher<T> pointGrapher;
	private static final String[] BUTTON_STATES = new String[] { "ON", "OFF" };
	public JToggleButton toggleButton;

	private double initialEnergy;
	private boolean energySet = false;
	private JLabel energyLabel;
	private JLabel energyDistLabel;

	private double[] initialMomentum = new double[3];
	private boolean momentumSet = false;
	private JLabel momentumLabel;

	private JLabel timeLabel;

	public DistributeFrame distributionFrame;

	public ExperimentFrame(Point2DGrapher<T> pointGrapherIn,
			Simulator<ParticleSystem<GeometricVector2D, T>, ?, SimpleChecker<GeometricVector2D, T>> simulatorIn) {
		this.simulator = simulatorIn;
		this.pointGrapher = pointGrapherIn;
		this.setSize(1500, 1000);
		setLayout(new BorderLayout(1, 1));
		this.add(this.pointGrapher, BorderLayout.CENTER);
		this.add(new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5)) {
			/**
			 *
			 */
			private static final long serialVersionUID = 1L;

			{
//				this.setBackground(Color.BLUE);
				JButton stopButton;
				this.add(stopButton = new JButton("Terminate"));
				stopButton.addActionListener(e -> {
					simulatorIn.endSimulation();
					if (Settings.ENABLE_FILESAVE) {
						SimpleGas.closeAndSave();
					}
				});
				this.add(toggleButton = new JToggleButton("ON"));
//				toggleButton.setSelected(true);
				toggleButton.addItemListener(e -> {
					toggleButton.setText(
							keepWorking() ? ExperimentFrame.BUTTON_STATES[0] : ExperimentFrame.BUTTON_STATES[1]);
					if (keepWorking()) {
						simulatorIn.resumeSimulation();
					}
				});
				JButton stepButton = new JButton("Make step");
				this.add(stepButton);
				stepButton.addActionListener(e -> {
					simulatorIn.resumeSimulation();
				});
				this.add(energyLabel = new JLabel());
				this.add(energyDistLabel = new JLabel());
				this.add(momentumLabel = new JLabel());
				this.add(timeLabel = new JLabel());
				this.add(stopButton = new JButton("Show distribution"));

				distributionFrame = new DistributeFrame();

				stopButton.addActionListener(e -> {
					distributionFrame.setVisible(true);
					distributionFrame.grapher.drawFrame();
					distributionFrame.grapher.repaint();
				});

				JButton reportButton = new JButton("Report");
				this.add(reportButton);
				reportButton.addActionListener(e -> {
					System.out.println(simulatorIn.getLastReport());
				});
			}
		}, BorderLayout.PAGE_END);
		this.pointGrapher.repaint();
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}

	public boolean keepWorking() {
		return toggleButton.isSelected();
	}

	public static final DecimalFormat ENERGY_FORMAT = new DecimalFormat("###0.00");
	public static final DecimalFormat ENERGYDIST_FORMAT = new DecimalFormat("###0.00");

	public void setDisplayedEnergy(double energy, double potential) {
		if (!energySet) {
			energySet = true;
			initialEnergy = energy;
			System.out.println("Initial energy is set to " + energy);
		}
		this.energyLabel.setText("Energy:" + ExperimentFrame.ENERGY_FORMAT.format(energy / initialEnergy));
		this.energyDistLabel.setText("Dist: " + ExperimentFrame.ENERGYDIST_FORMAT.format(potential / energy));
	}

	public static final DecimalFormat MOMENTUM_FORMAT = new DecimalFormat("###0.00");

	public void setDisplayedMomentum(double x, double y, double z) {
		if (!momentumSet) {
			momentumSet = true;
			this.initialMomentum[0] = x;
			this.initialMomentum[1] = y;
			this.initialMomentum[2] = z;
			System.out.println(
					String.format("Initial momentum is set to (%s;%s;%s)", ExperimentFrame.MOMENTUM_FORMAT.format(x),
							ExperimentFrame.MOMENTUM_FORMAT.format(y), ExperimentFrame.MOMENTUM_FORMAT.format(z)));
		}
		this.momentumLabel.setText(
				String.format("Momentum: (%s;%s;%s)", ExperimentFrame.MOMENTUM_FORMAT.format(x / initialMomentum[0]),
						ExperimentFrame.MOMENTUM_FORMAT.format(y / initialMomentum[1]),
						ExperimentFrame.MOMENTUM_FORMAT.format(z / initialMomentum[2])));
	}

	public static final DecimalFormat TIME_FORMAT = new DecimalFormat("###0.0000");

	public void setDisplayedTime(double time, double dt) {
		this.timeLabel.setText(
				"Time: " + ExperimentFrame.TIME_FORMAT.format(time) + "+" + ExperimentFrame.TIME_FORMAT.format(dt));
	}

	private double[] gaussianValues;
	private int lastDistSize = 0;
	private double lastDistSigma = 0;
	private double lastSpeedStep = 0;

	private void updateGaussianValues(int distSize, double sigma, double speedStep) {
		if ((distSize != lastDistSize) || (sigma != lastDistSigma) || (speedStep != lastSpeedStep)) {
			lastDistSize = distSize;
			lastDistSigma = sigma;
			gaussianValues = new double[distSize];
			for (int i = 0; i < distSize; i++) {
				gaussianValues[i] = Math.exp(-(Math.pow((i * speedStep) / sigma, 2)) / 2)
						/ (Math.sqrt(2 * Math.PI) * sigma);
			}
		}
	}

	@SuppressWarnings("null")
	public void redestributeVelocity() {
		if (!distributionFrame.isVisible() && !ExperimentFrame.SAVING_VELDIST) {
			return;
		}
		final double maxSpeed = Settings.MAX_SPEED;
		final int distSize = Settings.DISTRIBUTION_SIZE;
		Integer[] distribution = null;
		if (Settings.ENABLE_GAUSSIAN_MEAN) {
			Integer[] meanedDestribution = new Integer[distSize];
			updateGaussianValues(distSize, Settings.GAUSSIAN_SIGMA, maxSpeed / distSize);
			for (int i = 0; i < distSize; i++) {
				for (int j = 0; j < distSize; j++) {
					meanedDestribution[j] += (int) (distribution[i] * gaussianValues[Math.abs(i - j)] * 10);
				}
			}
			distribution = meanedDestribution;
		}
		distributionFrame.updateDistribution(distribution);
	}

	static class DistributeFrame extends JFrame {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private ArrayList<GeometricVector2D> distribution;
		private static final GeometricVector2D FALLBACK = new GeometricVector2D(0, 4);
		private final Point2DGrapher<GeometricVector2D> grapher;

		public DistributeFrame() {
			this.setSize(400, 300);
			distribution = new ArrayList<GeometricVector2D>(0);
			for (int i = 0; i < Settings.DISTRIBUTION_SIZE; i++) {
				distribution.add(new GeometricVector2D(i, 0));
			}
			this.add(grapher = new Point2DGrapher.Properties<GeometricVector2D>(() -> distribution)
					.setTransform((g, s) -> {
						double maxY = distribution.stream().max((p1, p2) -> (int) (p1.getY() - p2.getY()))
								.orElse(DistributeFrame.FALLBACK).getY() + 1;
						g.translate(0, s.height);
						g.scale(s.width / (Settings.DISTRIBUTION_SIZE - 1), -s.height / maxY);
					}).setPointRenderer((g, p, s) -> {
						g.drawRect((int) (p.getX()), (int) (p.getY() - 1), 1, 1);
					}).build());
		}

		public void updateDistribution(Integer[] distributionIn) {
			boolean flag = false;
			for (int i = 0; i < Settings.DISTRIBUTION_SIZE; i++) {
				if (distribution.get(i).getY() != distributionIn[i]) {
					distribution.get(i).setY(distributionIn[i]);
					flag = true;
				}
			}
			if (flag) {
				grapher.drawFrame();
				grapher.repaint();
			}
//			for (int i : distributionIn) {
//				System.out.print(i + " ");
//			}
//			System.out.println();
		}

		public ArrayList<Double> getDistribution() {
			ArrayList<Double> res = new ArrayList<Double>(0);
			for (GeometricVector2D point : distribution) {
				res.add(point.getY());
			}
			return res;
		}
	}

}
