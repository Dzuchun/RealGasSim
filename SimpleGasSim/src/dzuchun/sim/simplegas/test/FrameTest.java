package dzuchun.sim.simplegas.test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;

import javax.swing.JFrame;

import dzuchun.lib.graph.Point2DGrapher;
import dzuchun.lib.graph.Point2DGrapher.Properties;
import dzuchun.lib.math.GeometricVector2D;

public class FrameTest extends JFrame {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public static void main(String[] args) {
		new FrameTest().setVisible(true);
	}

	public FrameTest() {
		Point2DGrapher<GeometricVector2D> grapher = new Properties<GeometricVector2D>(
				() -> Arrays.asList(new GeometricVector2D(50, 50))).setBackColor(Color.red).setPointColor(Color.blue)
						.setSize(5).build();
		this.setSize(400, 300);
		setLayout(new BorderLayout(5, 5));
		this.add(grapher, BorderLayout.CENTER);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("Frame test");
		grapher.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
				grapher.drawFrame();
				grapher.repaint();
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

		});
		grapher.repaint();
	}
}
