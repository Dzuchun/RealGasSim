package dzuchun.sim.simplegas;

import java.util.Collection;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;

import dzuchun.lib.io.SpreadsheetHelper;
import dzuchun.lib.io.SpreadsheetHelper.ResultValue;
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
	public void writeTo(Row rowIn, int cellNo, CellStyle cellStyleIn) {
		SpreadsheetHelper.addCellWithValue(rowIn, cellNo++, mean, cellStyleIn);
		SpreadsheetHelper.addCellWithValue(rowIn, cellNo, deviation, cellStyleIn);
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
