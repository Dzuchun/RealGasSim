package dzuchun.sim.simplegas.test;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Arrays;

import dzuchun.lib.graph.Point2DGrapher;
import dzuchun.sim.simplegas.ExperimentFrame;

public class ExperimentFrameTest {

	public static void main(String[] args) {
		final ExperimentFrame<> frame = new ExperimentFrame<Point2D>(
				new Point2DGrapher.Properties<Point2D>(() -> Arrays.asList(new Point(50, 50))).setPointColor(Color.red)
						.setSize(5).build(),
				null);
		frame.setVisible(true);
		new Thread(() -> {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			frame.pointGrapher.drawFrame();
			frame.pointGrapher.repaint();
			System.out.println("Repainted");
		}).start();
	}

}
