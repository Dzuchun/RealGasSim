package dzuchun.sim.simplegas.test;

import dzuchun.lib.sim.Simulator;

public class ThreadTest {
	private static boolean b = false;

	public static void main(String[] args) {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Simulator simulator = new Simulator(null, null, null, () -> ThreadTest.b, o -> {
		});
		simulator.start();
		try {
			Thread.currentThread();
			Thread.sleep(1000);
			simulator.resumeSimulation();
			Thread.currentThread();
			Thread.sleep(1000);
			simulator.endSimulation();
		} catch (Exception e) {
		}
	}
}
