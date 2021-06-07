package dzuchun.sim.simplegas;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Settings {
	// JSON
	private static final JSONParser JSON_PARSER = new JSONParser();
	private static FileReader JSON_FILE_READER;
	private static JSONObject PROPERTIES_OBJECT;
	static {
		try {
			Settings.JSON_FILE_READER = new FileReader("settings.json");
			Settings.PROPERTIES_OBJECT = (JSONObject) Settings.JSON_PARSER.parse(Settings.JSON_FILE_READER);
		} catch (FileNotFoundException e) {
			System.err.println("Could not find \"settings.json\" file. All values will be default.");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			System.err.println("Settings file might be damaged:");
			e.printStackTrace();
		}
	}
	// Graph
	private static final JSONObject GRAPHICS_PROPERTIES = (JSONObject) Settings.PROPERTIES_OBJECT.get("graph");
	public static final long MIN_REPAINT_TIME = (long) Settings.GRAPHICS_PROPERTIES.get("min_repaint_time");
	public static final int DISTRIBUTION_SIZE = (int) (long) Settings.GRAPHICS_PROPERTIES.get("distribution_size");
	public static final double MAX_SPEED = (double) Settings.GRAPHICS_PROPERTIES.get("maximum_speed");
	public static final double DISTRIBUTION_STEP = Settings.MAX_SPEED / Settings.DISTRIBUTION_SIZE;
	public static final boolean ENABLE_GAUSSIAN_MEAN = (boolean) Settings.GRAPHICS_PROPERTIES
			.get("enable_Gaussian_mean");
	public static final double GAUSSIAN_SIGMA = (double) Settings.GRAPHICS_PROPERTIES.get("gaussian_sigma");
	// Mech
	private static final JSONObject MECH_PROPERTIES = (JSONObject) Settings.PROPERTIES_OBJECT.get("mech");
	public static final double MASS = (double) Settings.MECH_PROPERTIES.get("mass");
	public static final double DT = (double) Settings.MECH_PROPERTIES.get("time_unit");
	public static final double MAX_RELATIVE_ENERGY_CHANGE = (double) Settings.MECH_PROPERTIES
			.get("max_relative_energy_change");
	public static final double CRITICAL_ENERGY_CHANGE = (double) Settings.MECH_PROPERTIES
			.get("critical_relative_energy_change");
	public static final double MAX_RELATIVE_MOMENTUM_CHANGE = (double) Settings.MECH_PROPERTIES
			.get("max_relative_momentum_change");
	public static final double MIN_DT = (double) Settings.MECH_PROPERTIES.get("time_quant");
	public static final double CONTINUUM_MAX_X = (double) Settings.MECH_PROPERTIES.get("continuum_width");
	public static final double CONTINUUM_MAX_Y = (double) Settings.MECH_PROPERTIES.get("continuum_height");
	public static final double CONTINUUM_MAX_Z = (double) Settings.MECH_PROPERTIES.get("continuum_applicate");
	public static final double CONTINUUM_MAX_X_HALVED = Settings.CONTINUUM_MAX_X / 2;
	public static final double CONTINUUM_MAX_Y_HALVED = Settings.CONTINUUM_MAX_Y / 2;
	public static final double CONTINUUM_MAX_Z_HALVED = Settings.CONTINUUM_MAX_Z / 2;
	// Force
	private static final JSONObject FORCE_PROPERTIES = (JSONObject) Settings.PROPERTIES_OBJECT.get("force");
	public static final double EPSILON = (double) Settings.FORCE_PROPERTIES.get("epsilon");
	public static final double SIGMA = (double) Settings.FORCE_PROPERTIES.get("sigma");
	// Data
	private static final JSONObject DATA_PROPERTIES = (JSONObject) Settings.PROPERTIES_OBJECT.get("data");
	public static final Boolean ENABLE_FILESAVE = (Boolean) Settings.DATA_PROPERTIES.get("enable_filesave");
	public static final String SAVES_FORDER = (String) Settings.DATA_PROPERTIES.get("saves_folder");
	public static final String FILENAME_FORMAT = (String) Settings.DATA_PROPERTIES.get("filename_format");
	public static final double SAVE_INTERVAL = (double) Settings.DATA_PROPERTIES.get("save_interval");
	// Debug
	private static final JSONObject DEBUG_PROPERTIES = (JSONObject) Settings.PROPERTIES_OBJECT.get("debug");
	public static final boolean PAUSE_ON_CRITICAL_ENERGY = (boolean) Settings.DEBUG_PROPERTIES
			.get("pause_on_critical_energy");
	// Temporary
	private static final JSONObject TEMPORARY_PROPERTIES = (JSONObject) Settings.PROPERTIES_OBJECT.get("temporary");
	public static final int PARTICLES = (int) (long) Settings.TEMPORARY_PROPERTIES.get("particles");
	// GUI
	private static final JSONObject GUI_PROPERTIES = (JSONObject) Settings.PROPERTIES_OBJECT.get("gui");
	public static final double DEFAULT_MAX_TIME = (double) Settings.GUI_PROPERTIES.get("default_max_time");
	public static final double DEFAULT_MAX_ENERGY = (double) Settings.GUI_PROPERTIES.get("default_max_energy");
	public static final int DEFAULT_SIMULATIONS = (int) (long) Settings.GUI_PROPERTIES.get("default_simulations");
	// Features
	private static final JSONObject FEATURES = (JSONObject) Settings.PROPERTIES_OBJECT.get("features");
	public static final boolean DELETE_PROXIMATE = (boolean) Settings.FEATURES.get("delete_proximate");
	public static final boolean SCALE_VELOCITY = (boolean) Settings.FEATURES.get("scale_velocity");
	public static final boolean CONTINUUM_ATTRACTION = (boolean) Settings.FEATURES.get("continuum_attration");
	public static final int CONTINUUM_ATTRACTION_ITERATIONS = (int) (long) Settings.FEATURES
			.get("continuum_attraction_iterations");
	public static final boolean VERLET_INTEGRATION = (boolean) Settings.FEATURES.get("verlet_integration");
	public static final boolean SEQUENCED_REACTION = (boolean) Settings.FEATURES.get("sequenced_reaction");
	// Start conditions
	private static final JSONObject START_CONDITIONS = (JSONObject) Settings.PROPERTIES_OBJECT.get("start_conditions");
	public static final boolean FIXED_SPEED = (boolean) Settings.START_CONDITIONS.get("fixed_speed");
	public static final double FIXED_SPEED_LENGTH = (double) Settings.START_CONDITIONS.get("fixed_speed_length");
	public static final boolean RANDOM_SPEED = (boolean) Settings.START_CONDITIONS.get("random_speed");
	public static final double MAX_RANDOM_SPEED = (double) Settings.START_CONDITIONS.get("max_random_speed");
}
