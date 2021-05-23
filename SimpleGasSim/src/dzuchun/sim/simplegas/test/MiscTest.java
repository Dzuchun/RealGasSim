package dzuchun.sim.simplegas.test;

import java.util.ArrayList;

public class MiscTest {

	public static void main(String[] args) {
		ArrayList<Double> a = new ArrayList<Double>();
		a.add(2.0d);
		@SuppressWarnings("unchecked")
		ArrayList<Double> b = (ArrayList<Double>) a.clone();
		a.set(0, 5.0d);
		System.out.println(a.get(0));
		System.out.println(b.get(0));
	}

}
