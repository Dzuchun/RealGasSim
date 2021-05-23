package dzuchun.sim.simplegas.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class SaveTest {

	public static void main(String[] args) {
		try {
			File out = new File("saves/" + System.currentTimeMillis() + ".xlsx");
			out.createNewFile();
			Workbook workbook = new HSSFWorkbook();
			Sheet sheet = workbook.createSheet();
			Row row = sheet.createRow(0);
			Cell cell = row.createCell(0);
			cell.setCellValue("g");

			workbook.write(new FileOutputStream(out));
			workbook.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
