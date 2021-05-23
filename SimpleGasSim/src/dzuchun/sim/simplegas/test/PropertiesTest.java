package dzuchun.sim.simplegas.test;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class PropertiesTest {
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException, ParseException {// First Employee
		JSONObject employeeDetails = new JSONObject();
		employeeDetails.put("firstName", "Lokesh");
		employeeDetails.put("lastName", 0.02);
		employeeDetails.put("website", 2);
		JSONObject o = new JSONObject();
		o.put("s", employeeDetails);
		JSONArray a = new JSONArray();
		a.add(o);
//        a.add(o);
		// Write JSON file
		try (FileWriter file = new FileWriter("employees.json")) {
			// We can write any JSONArray or JSONObject instance to the file
			file.write(a.toJSONString());
			file.flush();

		} catch (IOException e) {
			e.printStackTrace();
		}
		// read

		JSONParser parser = new JSONParser();
		FileReader fr = new FileReader("employees.json");
		JSONArray list = (JSONArray) parser.parse(fr);
		System.out.println(((JSONObject) (((JSONObject) list.get(0)).get("s"))).get("website"));
	}
}
