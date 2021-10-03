package dzuchun.sim.simplegas;

import java.util.Collection;

import dzuchun.lib.io.SaveHelper.ResultValue;
import dzuchun.lib.math.PrecisedValue;

public class PrecisedResultValue extends PrecisedValue implements ResultValue {

	public PrecisedResultValue(Collection<Double> collection) {
		super(collection);
	}

	public PrecisedResultValue(double[] collection) {
		super(collection);
	}

	public PrecisedResultValue(double value) {
		super(value);
	}

	@Override
	public String[] asValues() {
		return new String[] { mean + "", deviation + "" };
	}

	@Override
	public int size() {
		return 2;
	}

	public static final String[] COL_NAMES = { "Value", "Deviation" };

	@Override
	public String[] colNames() {
		return PrecisedResultValue.COL_NAMES;
	}
}
