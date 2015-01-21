package fr.limsi.tools.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;


public class CustomOptions extends Options {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	private final static int MAX_POSITIVE_INT_SHIFT = 30;
	
	protected String programName;
	// command line from option parsing
	protected CommandLine line;
	// Properties
	private TypedProperties properties;

	public CustomOptions(String programName) {
	    super();
	    this.programName = programName;
        this.properties = new TypedProperties();
        this.addOption("h", "help", false, "Get help");
	}

	
	@Override
	public Options addOption(Option option) {
		if (option.getArgName().equals("h")) {
			throw new RuntimeException("Can't create a new option '-h', already existing in " + this.getClass().getName());
		} else {
			return super.addOption(option);
		}
	}
	
	public String[] parseOptions(String[] args) throws IOException, ParseException {
		// create the command line parser
		CommandLineParser parser = new PosixParser();
		// parse the command line arguments
		this.line = parser.parse(this, args);
		// Help
		if (this.line.hasOption('h')) {
			this.printHelp();
			System.exit(0);
		}
		return this.line.getArgs();
	}
	
	public void printHelp() {
		printHelp(false);
	}
	
	public void printHelp(boolean quit) {
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(this.programName, this);
		if (quit) {
			System.exit(1);
		}
	}

	public String[] getArgs() {
		return this.line.getArgs();
	}
	
	public String getOptionValue(String key) {
//	    if (this.line == null) {
//	        throw new RuntimeException("Parse options before exploiting them!");
//	    }
		String value = null;
		if (this.line != null) {
			value = this.line.getOptionValue(key);
		}
		if (value == null) {
			return this.getProperty(key);
		} else {
			return value;
		}
	}
	
	public boolean hasOptionValue(String key) {
	    if (this.line == null) {
	        throw new RuntimeException("Parse options before exploiting them!");
	    }
		return this.line.hasOption(key);
	}
	
	public TypedProperties getProperties() {
		return this.properties;
	}
	
	public void setProperty(String key, String value) {
		this.properties.setProperty(key, value);
	}
	
	public String getProperty(String key) {
		return this.properties.getProperty(key);
	}
	

	public int getIntProperty(String key) throws TypedPropertyException {
		return this.properties.getIntProperty(key);
	}
	
	public void setBooleanProperty(String key, boolean value) {
		this.properties.setBooleanProperty(key, value);
	}
	
    /**
     * Returns a boolean value corresponding to the key Property.
     * Value <code>true</code> is returned if the value of the property is "1" or "true"
     * <code>false</code> if the value is "0" or "false" (case-insensitive).
     *
     * @param key the key of the property
     * @throws TypedPropertyException if the property has no boolean value representation
     * @return a boolean value corresponding to the key Property, <code>false</code> if the 
     * property is not found.
     */
    public boolean getBooleanProperty(String key) throws TypedPropertyException{        
        return this.properties.getBooleanProperty(key);
    }
    
    public boolean getBooleanProperty(String key, boolean defaultValue) throws TypedPropertyException{
    	return this.properties.getBooleanProperty(key, defaultValue);
    }

//    public void addProperties(Path file) throws FileNotFoundException, IOException {
//        Properties prop = new Properties();
//        prop.load(Files.newInputStream(file));
//        this.properties.putAll(prop);        
//    }
    
    public void addProperties(File file) throws FileNotFoundException, IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream(file));
        this.properties.putAll(prop);        
    }

	public static boolean validOption(int option, int options) {
		if (options <= 0) {
			return true;
		} else {
			return ((option & options) > 0);
		}
	}
	
	public static int getAllOptionValue() {
		return ((1 << MAX_POSITIVE_INT_SHIFT) *2 -1);
	}
	
	public static int buildOptionValueByAddition(int[] options) {
		int optionValue = 0;
		double log;
		for (int option : options) {
			log = Math.log(option)/Math.log(2);
			if (log != Math.floor(log)) {
				throw new RuntimeException("Option " + option + " is not a power of 2 !");
			}
			optionValue |= option;
		}
		return optionValue;
	}
	
	public static int buildOptionValueBySubstraction(int option) {
		return (getAllOptionValue() ^ option);
	}
	
	public static int buildOptionValueBySubstraction(int[] options) {
//		double log = Math.log(1 << MAX_POSITIVE_INT_SHIFT)/Math.log(2);
//		if (log != Math.floor(log)) {
//			throw new RuntimeException("Option " + maxOptionValue + " is not a power of 2 !");
//		}
		int allOptions = getAllOptionValue();
		double log;
		for (int option : options) {
			log = Math.log(option)/Math.log(2);
			if (log != Math.floor(log)) {
				throw new RuntimeException("Option " + option + " is not a power of 2 !");
			}
			allOptions ^= option;
		}
		return allOptions;
	}
}
