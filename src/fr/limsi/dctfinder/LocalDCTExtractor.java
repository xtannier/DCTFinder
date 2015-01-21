package fr.limsi.dctfinder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.limsi.tools.classification.ClassificationException;
import fr.limsi.tools.classification.FeatureException;
import fr.limsi.tools.classification.RecordList;
import fr.limsi.tools.classification.crf.CRFRecordFactory;
import fr.limsi.tools.common.CustomOptions;
import fr.limsi.tools.common.ListTools;
import fr.limsi.tools.common.files.HtmlFileFilter;



public class LocalDCTExtractor {

	protected final static String WAPITI_MODEL_FILE = "WAPITI_MODEL_FILE";
	protected final static String DATE_IN_URL_PATTERNS_FILE_NAME = "DATE_IN_URL_PATTERNS_FILE_NAME";
	protected final static String VOCABULARY_DIR_NAME = "VOCABULARY_DIR_NAME";
	protected final static String VOCABULARY_FILE_LIST_NAME = "VOCABULARY_FILE_LIST_NAME";
	protected final static String WAPITI_BINARY_PATH = "WAPITI_BINARY_PATH";
	protected final static String DATA_DIR_PARAMETER = "DATA_DIR";
	protected final static String ENCODING = "ENCODING";
	protected final static String TIME_RELATED_PATTERNS_FILE_NAME = "TIME_RELATED_PATTERNS_FILE_NAME";
	protected final static String TITLE_PATTERNS_FILE_NAME = "TITLE_PATTERNS_FILE_NAME";
	protected final static String TITLE_ANTI_PATTERNS_FILE_NAME = "TITLE_ANTI_PATTERNS_FILE_NAME";


	private static final String DATE_ELEM = "date";

	private static HashMap<Locale, LocalDCTExtractor> extractors = new HashMap<Locale, LocalDCTExtractor>();

	//	private final static String WAPITI_MODEL_FILE_NAME = "wapiti-model.txt";


	/********************
	 * Learning infos
	 ********************/
	// HTML file filter
	private final static FileFilter HTML_FILTER = new HtmlFileFilter();
	// record factory
	private DCTExtractorRecordFactory factory;
	// File separation pattern in CRF file
	Pattern fileSeparationPattern = Pattern.compile(DCTExtractorRecordFactory.DCTFINDER_FILE_SEPARATOR + " ([^\\s]+)\\s.*");


	/********************
	 * File infos
	 ********************/
	private File testOutFileDataset;
	private File resultFile;

	/********************
	 * URL patterns
	 ********************/
	private HashMap<Pattern, String> urlPatterns;

	//    private Logger logger;
	private boolean verbose;
	private Locale locale;

	private DateParser dateParser;

	protected LocalDCTExtractor(Locale locale, Properties properties) throws DCTExtractorException {
		this(locale, properties, false);
	}

	protected LocalDCTExtractor(Locale locale, Properties properties, boolean evalMode) throws DCTExtractorException {
		this(locale, properties, evalMode, false);
	}

	protected LocalDCTExtractor(Locale locale, Properties properties, boolean evalMode, boolean verbose) throws DCTExtractorException {
		//        this.logger = logger;
		this.verbose = verbose;
		try {
			this.testOutFileDataset = File.createTempFile("test", ".crf"); 
			this.testOutFileDataset.deleteOnExit();
			this.resultFile = File.createTempFile("result", ".crf"); 
			this.resultFile.deleteOnExit();
		} catch (IOException e) {
			throw new DCTExtractorException(e);         
		}

		HashMap<Pattern, String> dateRules = new HashMap<>();
		HashMap<String, HashMap<Pattern, String>> allRules = new HashMap<>();

		/*************************
		 * Get vocabulary
		 ************************/
		String vocabularyFileListPath = properties.getProperty(DATA_DIR_PARAMETER) + "/" + locale.toString() + "/" + properties.getProperty(VOCABULARY_FILE_LIST_NAME);
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(vocabularyFileListPath);
		if (is == null) {
			throw new DCTExtractorException("Could not find resource file " + vocabularyFileListPath);
		}

		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String ruleFileName;
		String line;
		String fields[];
		String regex;
		String vocabularyFilePath;

		try {
			// Parse all files in vocabulary directory
			while ((ruleFileName=br.readLine())!=null){
				vocabularyFilePath = properties.getProperty(DATA_DIR_PARAMETER) + "/" + locale.toString() + "/" + properties.getProperty(VOCABULARY_DIR_NAME) + "/" + ruleFileName;                
				InputStream ruleIps = this.getClass().getClassLoader().getResourceAsStream(vocabularyFilePath);
				if (ruleIps == null) {
					throw new DCTExtractorException("Could not find resource file " + vocabularyFilePath + ", please compile the rules again.");
				}
				BufferedReader ruleBr = new BufferedReader(new InputStreamReader(ruleIps, "UTF-8"));

				HashMap<Pattern, String> allPatterns = new HashMap<>();
				if (ruleFileName.startsWith(DATE_ELEM)) {
					while ((line=ruleBr.readLine())!=null){
						line.replaceAll("#.*", "");
						if (line.trim().length() == 0)
							continue;
						fields = line.split("\t");
						if (fields.length == 2) {
							regex = fields[0];
							if (!regex.contains("(")) {
								regex = "(" + regex + ")";
							}
							dateRules.put(Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE), fields[1]);
							allPatterns.put(Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE), fields[1]);
						} else {
							ruleBr.close();
							throw new DCTExtractorException("Bad format in list " + vocabularyFilePath + " : " + line);
						}
					}
					ruleBr.close();
				} else {
					while ((line=ruleBr.readLine())!=null){
						line.replaceAll("#.*", "");
						if (line.trim().length() == 0)
							continue;
						allPatterns.put(Pattern.compile(line, Pattern.CASE_INSENSITIVE), "");
					}
				}
				allRules.put(ruleFileName.substring(0, ruleFileName.length() - 4), allPatterns);
			}
			br.close();
		} catch (IOException e) {
			throw new DCTExtractorException("Couln't create rules from resource " + vocabularyFileListPath);
		}


		/***********************
		 * URL patterns for finding date
		 ***********************/
		String urlPatternsFilePath = properties.getProperty(DATA_DIR_PARAMETER) + "/" + locale.toString() + "/" + properties.getProperty(DATE_IN_URL_PATTERNS_FILE_NAME);
		is = this.getClass().getClassLoader().getResourceAsStream(urlPatternsFilePath);
		if (is == null) {
			throw new DCTExtractorException("Could not find resource file " + urlPatternsFilePath);
		}

		this.urlPatterns = new HashMap<Pattern, String>();
		try {
			isr = new InputStreamReader(is, "UTF-8");
			br = new BufferedReader(isr);

			while ((line=br.readLine())!=null){
				if (line.trim().length() == 0)
					continue;
				line.replaceAll("#.*", "");
				fields = line.split("\t");
				if (fields.length == 2) {
					regex = fields[0];
					if (!regex.contains("(")) {
						regex = "(" + regex + ")";
					}
					this.urlPatterns.put(Pattern.compile(regex), fields[1]);
				} else {
					br.close();
					throw new DCTExtractorException("Bad format in list " + properties.getProperty(DATE_IN_URL_PATTERNS_FILE_NAME) + " : " + line);
				}
			}    
			br.close();
		} catch (IOException e) {
			throw new DCTExtractorException("Couln't create URL patterns from resource " + properties.getProperty(DATE_IN_URL_PATTERNS_FILE_NAME));
		}

		/*****************
		 * Title & time-related patterns
		 *****************/
		// All patterns for finding title-related tags
		ArrayList<Pattern> titleTagRelatedPatterns = new ArrayList<>(); 
		// All patterns for discarding title-related tags
		ArrayList<Pattern> titleTagRelatedAntiPatterns = new ArrayList<>();
		// All patterns for finding date-related tags
		ArrayList<Pattern> timeTagRelatedPatterns = new ArrayList<>();

		// Time-related tag patterns
		String path = properties.getProperty(DATA_DIR_PARAMETER) + "/" + locale.toString() + "/" + properties.getProperty(TIME_RELATED_PATTERNS_FILE_NAME);       
		is = this.getClass().getClassLoader().getResourceAsStream(path);
		if (is == null) {
			throw new DCTExtractorException("Could not find resource file " + path);
		}

		try {
			isr = new InputStreamReader(is, "UTF-8");
			br = new BufferedReader(isr);

			while ((line=br.readLine())!=null){
				if (line.trim().length() == 0)
					continue;
				line.replaceAll("#.*", "");
				timeTagRelatedPatterns.add(Pattern.compile(line, Pattern.CASE_INSENSITIVE));  
			}   
			br.close();
		} catch (IOException e) {
			throw new DCTExtractorException("Couln't create URL patterns from resource " + properties.getProperty(DATE_IN_URL_PATTERNS_FILE_NAME));
		}

		// Title tag patterns
		path = properties.getProperty(DATA_DIR_PARAMETER) + "/" + locale.toString() + "/" + properties.getProperty(TITLE_PATTERNS_FILE_NAME);       
		is = this.getClass().getClassLoader().getResourceAsStream(path);
		if (is == null) {
			throw new DCTExtractorException("Could not find resource file " + path);
		}

		try {
			isr = new InputStreamReader(is, "UTF-8");
			br = new BufferedReader(isr);

			while ((line=br.readLine())!=null){
				if (line.trim().length() == 0)
					continue;
				line.replaceAll("#.*", "");
				titleTagRelatedPatterns.add(Pattern.compile(line, Pattern.CASE_INSENSITIVE));  
			} 
			br.close();
		} catch (IOException e) {
			throw new DCTExtractorException("Couln't create URL patterns from resource " + properties.getProperty(DATE_IN_URL_PATTERNS_FILE_NAME));
		}

		path = properties.getProperty(DATA_DIR_PARAMETER) + "/" + locale.toString() + "/" + properties.getProperty(TITLE_ANTI_PATTERNS_FILE_NAME);       
		is = this.getClass().getClassLoader().getResourceAsStream(path);
		if (is == null) {
			throw new DCTExtractorException("Could not find resource file " + path);
		}

		try {
			isr = new InputStreamReader(is, "UTF-8");
			br = new BufferedReader(isr);

			while ((line=br.readLine())!=null){
				if (line.trim().length() == 0)
					continue;
				line.replaceAll("#.*", "");
				titleTagRelatedAntiPatterns.add(Pattern.compile(line, Pattern.CASE_INSENSITIVE));  
			} 
			br.close();
		} catch (IOException e) {
			throw new DCTExtractorException("Couln't create URL patterns from resource " + properties.getProperty(DATE_IN_URL_PATTERNS_FILE_NAME));
		}

		/*********************
		 * English Locale patch
		 *********************
		 * Ideally a distinction is made between en_US and en_UK (locale == Locale.US or locale == Locale.UK)
		 * (date format is not the same in both languages).
		 *  If the language is english but the country is not provided (locale == Locale.ENGLISH),
		 *  then default is Locale.US
		 *********************/
		if (locale == Locale.ENGLISH) {
			this.locale = Locale.US;
		} else {
			this.locale = locale;
		}
		try {
			this.factory = new DCTExtractorRecordFactory(this.locale, allRules, titleTagRelatedPatterns, titleTagRelatedAntiPatterns, timeTagRelatedPatterns, properties, evalMode, verbose);
			this.dateParser = new DateParser(this.locale, dateRules);
		} catch (IOException | FeatureException e) {
			throw new DCTExtractorException(e);
		}
	}

	/**
	 * Return the extractor Locale
	 * @return the extractor Locale
	 */
	public Locale getLocale() {
		return this.locale;
	}

	/**
	 * Train Wapiti model
	 * @param trainOutFileDataset the training set file 
	 * @param devOutFileDataset the development set file
	 * @param logger the Logger
	 * @return the Wapiti exit value
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws DCTExtractorException 
	 */
	protected static int wapitiTrain(File trainOutFileDataset, File devOutFileDataset, File wapitiTemplateFile, String wapitiModelFilePath, File wapitiBinaryFile, final boolean verbose) throws IOException, InterruptedException, DCTExtractorException {
		// Building Wapiti command for learning
		int cores = Runtime.getRuntime().availableProcessors() - 1;

		String command = wapitiBinaryFile.getAbsolutePath() + " train -t " + cores + " -p " + wapitiTemplateFile.getAbsolutePath() + " -d " + devOutFileDataset.getAbsolutePath() + " " +
				trainOutFileDataset.getAbsolutePath() + " " + wapitiModelFilePath;

		if (verbose) {
			System.out.println("Training command: " + command);
		}

		// Lauching process
		final Process proc = Runtime.getRuntime().exec(command);	

		// Get wapiti standard output
		// in java output must be consumed even if it's not used
		Thread stdThread = new Thread() {
			public void run() {
				try {
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(proc.getInputStream()));
					String line = "";
					try {
						while ((line = reader.readLine()) != null) {
							if (verbose) {
								System.out.println(line);
							}
						}
					} finally {
						reader.close();
					}
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		};
		// Get wapiti error output
		Thread errThread = new Thread() {
			public void run() {
				try {
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(proc.getErrorStream()));
					String line = "";
					try {
						while ((line = reader.readLine()) != null) {
							if (verbose) {
								System.out.println(line);
							}
						}
					} finally {
						reader.close();
					}
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		};

		stdThread.start();
		errThread.start();
		stdThread.join();
		errThread.join();
		proc.waitFor();
		return proc.exitValue();
	}

	protected static int wapitiTest(File testOutFileDataset, File resultOutFile, String wapitiModelFilePath, File wapitiBinaryFile, boolean verbose) throws IOException, InterruptedException, DCTExtractorException {
		return wapitiTest(testOutFileDataset, resultOutFile, wapitiModelFilePath, wapitiBinaryFile, false, verbose);
	}

	protected static int wapitiTest(File testOutFileDataset, File resultOutFile, String wapitiModelFilePath, File wapitiBinaryFile, boolean append, final boolean verbose) throws IOException, InterruptedException, DCTExtractorException {
		// Building Wapiti command for testing
		//		String command = wapitiBinaryPath + " label -s -n 2 -m " + wapitiModelFileName + " " + testOutFileDataset.getAbsolutePath() + " " + resultOutFile.getAbsolutePath();
		File resultFile;

		if (!resultOutFile.getParentFile().isDirectory()) {
			resultOutFile.getParentFile().mkdirs();
		}

		if (append) {
			resultFile = File.createTempFile("WAPITI", ".crf");
			resultFile.deleteOnExit();
		} else {
			resultFile = resultOutFile;
		}

		String command = wapitiBinaryFile.getAbsolutePath() + " label -s -p -m " + wapitiModelFilePath + " " + testOutFileDataset.getAbsolutePath() + " " + resultFile.getAbsolutePath();

		if (verbose) {
			System.out.println("Labeling command: " + command);
		}

		// Lauching process
		final Process proc = Runtime.getRuntime().exec(command);	

		// Get wapiti standard output
		// in java output must be consumed even if it's not used
		Thread stdThread = new Thread() {
			public void run() {
				try {
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(proc.getInputStream()));
					String line = "";
					try {
						while ((line = reader.readLine()) != null) {
							if (verbose) {
								System.out.println(line);
							}
						}
					} finally {
						reader.close();
					}
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		};
		// Get wapiti error output
		Thread errThread = new Thread() {
			public void run() {
				try {
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(proc.getErrorStream()));
					String line = "";
					try {
						while ((line = reader.readLine()) != null) {
							if (verbose) {
								System.out.println(line);
							}
						}
					} finally {
						reader.close();
					}
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		};

		stdThread.start();
		errThread.start();
		stdThread.join();
		errThread.join();
		proc.waitFor();

		if (append) {
			InputStream ips = new FileInputStream(resultFile); 
			InputStreamReader ipsr = new InputStreamReader(ips);
			BufferedReader br = new BufferedReader(ipsr);

			FileWriter fw = new FileWriter(resultOutFile, true);
			BufferedWriter output = new BufferedWriter(fw);

			String line;
			while ((line = br.readLine())!=null){
				output.write(line + "\n");
			}
			br.close();
			output.close();			
		}

		return proc.exitValue();
	}


	private HashMap<String, PageInfo> getDCTFromWapitiResult(File crfFile, Calendar downloadDate, 
			HashMap<String, PageInfo> infos, 
			double scoreThreshold, boolean getDCTByScores) throws IOException, FeatureException, DCTExtractorException {
		InputStream ips = new FileInputStream(crfFile); 
		InputStreamReader ipsr = new InputStreamReader(ips);
		BufferedReader br = new BufferedReader(ipsr);
		String recordLine;
		String[] fields;
		String[] classFields;
		String text; String previousText = null;
		String predictedClassValue;
		String currentDateString = "";
		String currentFileName = null;
		Double score;
		double candidateScore = 0;
		ArrayList<String> candidateDates = new ArrayList<String>();
		ArrayList<Double> candidateScores = new ArrayList<Double>();
		ArrayList<String> secondChanceCandidateDates = new ArrayList<String>();
		if (infos == null) {
			infos = new HashMap<String, PageInfo>();
		}
		boolean firstChance = false;
		while ((recordLine = br.readLine())!=null){
			// If new file section opening
			if (recordLine.startsWith(DCTExtractorRecordFactory.DCTFINDER_FILE_SEPARATOR)) {
				// If we were already in a file section
				if (currentFileName != null) {
					PageInfo info = infos.get(currentFileName);
					if (info == null) {
						info = new PageInfo();
					} 
					PageInfo newInfo;
					if (getDCTByScores) {
						newInfo = this.getDCTFromCandidatesByScores(candidateDates, secondChanceCandidateDates, candidateScores, downloadDate);
					} else {
						newInfo = this.getDCTFromCandidatesByAgeHeuristic(candidateDates, secondChanceCandidateDates, downloadDate);
					}

					info.setDateString(newInfo.getDateString());
					info.setDCT(newInfo.getDCT());
					infos.put(currentFileName, info);
					candidateDates.clear();
					secondChanceCandidateDates.clear();
				}
				// get new file name
				Matcher matcher = fileSeparationPattern.matcher(recordLine);
				if (matcher.matches()) {
					currentFileName = matcher.group(1);
				} else {
					br.close();
					throw new DCTExtractorException("Bad format in CRF output file, property " + DCTExtractorRecordFactory.DCTFINDER_FILE_SEPARATOR + " not recognized");
				}
			}
			else if (recordLine.startsWith("#") || recordLine.length() == 0) {
				continue;
			}
			fields = recordLine.split("\t");
			predictedClassValue = fields[fields.length-1];			
			classFields = predictedClassValue.split("/");
			predictedClassValue = classFields[0];
			if (classFields.length > 1) {
				score = Double.parseDouble(classFields[1]);
			} else {
				score = 1.0;
			}

			if (predictedClassValue.equals(CRFRecordFactory.CLASS_BEGIN) || predictedClassValue.equals(CRFRecordFactory.CLASS_INSIDE)) {
				text = fields[this.factory.getFeatureId(DCTExtractorRecordFactory.TEXT)];
				if (!text.equals(previousText)) {
					currentDateString += text + " ";
				}
				firstChance = true;
				previousText = text;
				candidateScore = score;
			}
			else if (scoreThreshold > 0 && score < scoreThreshold && !firstChance) {
				text = fields[this.factory.getFeatureId(DCTExtractorRecordFactory.TEXT)];
				if (!text.equals(previousText)) {
					currentDateString += text + " ";
				}
				previousText = text;
			}
			else {
				if (currentDateString.length() > 0) {
					if (firstChance) {
						candidateDates.add(currentDateString.trim());
						candidateScores.add(candidateScore);
					} else {
						secondChanceCandidateDates.add(currentDateString.trim());
					}
					currentDateString = "";
					firstChance = false;
				}
				previousText = null;
			}
		}
		br.close();

		// Last page info
		PageInfo info = infos.get(currentFileName);
		if (info == null) {
			info = new PageInfo();
		} 
		PageInfo newInfo;
		if (getDCTByScores) {
			newInfo = this.getDCTFromCandidatesByScores(candidateDates, secondChanceCandidateDates, candidateScores, downloadDate);
		} else {
			newInfo = this.getDCTFromCandidatesByAgeHeuristic(candidateDates, secondChanceCandidateDates, downloadDate);
		}
		info.setDateString(newInfo.getDateString());
		info.setDCT(newInfo.getDCT());
		infos.put(currentFileName, info);
		return infos;
	}

	private PageInfo getDCTFromCandidatesByAgeHeuristic(ArrayList<String> candidateDates, ArrayList<String> secondChanceCandidateDates, Calendar downloadDate) throws DCTExtractorException {
		PageInfo result = new PageInfo();

		if (candidateDates.isEmpty()) {
			//			candidateDates = secondChanceCandidateDates;
			candidateDates.addAll(secondChanceCandidateDates);
			secondChanceCandidateDates.clear();
		}

		//		System.out.println("CANDIDATES : " + candidateDates);
		//		System.out.println(this.dateParser.getLocale());		

		while(!candidateDates.isEmpty()) {
			Calendar oldestDate = null;
			String bestDateString = null;
			Calendar date;
			for (String dateString : candidateDates) {
				date = this.dateParser.parse(dateString, downloadDate);
				// 7:48 p.m EST Thu November 29 2007
				//				System.out.println(dateString + " -> " + date.getTime());
				if (date != null && (oldestDate == null || oldestDate.after(date)) && (downloadDate == null || !downloadDate.before(date))) {
					oldestDate = date;
					bestDateString = dateString;
				}
			}
			result.setDateString(bestDateString);
			//			System.out.println(bestDateString + " -> " + oldestDate.getTime());            
			result.setDCT(oldestDate);
			//			if (oldestDate != null) {
			//				System.out.println(oldestDate.getTime());
			//			}
			candidateDates.clear();
			if (oldestDate == null) {
				candidateDates.addAll(secondChanceCandidateDates);
				secondChanceCandidateDates.clear();
			}
		}  
		Calendar date = result.getDCT();
		if (date != null) {
			date.set(Calendar.SECOND, 0);
			date.set(Calendar.MINUTE, 0);
			date.set(Calendar.HOUR_OF_DAY, 0);
		}
		return result;
	}

	private PageInfo getDCTFromCandidatesByScores(ArrayList<String> candidateDates, ArrayList<String> secondChanceCandidateDates
			, ArrayList<Double> candidateScores, Calendar downloadDate) throws DCTExtractorException {
		PageInfo result = new PageInfo();

		if (candidateScores.isEmpty() || candidateDates.size() != candidateScores.size()) {
			return getDCTFromCandidatesByAgeHeuristic(candidateDates, secondChanceCandidateDates, downloadDate);
		}

		double highestScore = 0;
		String dateString = "";
		Calendar foundDate = null;
		for (int i = 0; i < candidateDates.size(); i++){
			double score = candidateScores.get(i);
			if (score > highestScore) {
				String candidateDateString = candidateDates.get(i);
				Calendar date = this.dateParser.parse(candidateDateString, downloadDate);
				if (date != null) {
					foundDate = date;
					dateString = candidateDateString;
					highestScore = score;
				}
			}
		}

		if (foundDate != null) {
			result.setDateString(dateString);
			result.setDCT(foundDate);
		}

		return result;
	}

	private PageInfo getDCTFromURL(URL url) throws DCTExtractorException {
		if (url == null) {
			return null;
		}
		String path = url.getPath();

		//		this.factory.getDateParser();

		String tokens[] = {path};
		Calendar dct = DateParser.getDateFromText(tokens, this.urlPatterns, this.locale);
		if (dct == null) {
			return null;
		} else {
			//			System.out.println(path + " -> " + dct.getTime());
			PageInfo pageInfo = new PageInfo();
			pageInfo.setDCT(dct);
			return pageInfo;
		}
	}

	public static HashMap<String, PageInfo> getPageInfosFromDirectory(File dir, CustomOptions options, Locale locale, String wapitiModelFilePath, File wapitiBinaryFile, 
			HashMap<String, URL> urlMapping, Calendar downloadDate, boolean getDCTByScores, boolean verbose) throws IOException, DCTExtractorException, FeatureException, InterruptedException {
		LocalDCTExtractor extractor;

		HashMap<String, PageInfo> result = new HashMap<String, PageInfo>();
		if (dir.isDirectory()) {
			if (verbose) {
				System.out.println("Parse directory " + dir.getAbsolutePath());
			}


			File[] files = dir.listFiles();
			HashSet<File> filesInDir = new HashSet<File>();
			for (File file : files) {
				if (file.isDirectory()) {
					result.putAll(getPageInfosFromDirectory(file, options, locale, wapitiModelFilePath, wapitiBinaryFile, urlMapping, downloadDate, getDCTByScores, verbose));
				}
				else if (HTML_FILTER.accept(file)) {
					filesInDir.add(file);
				}
				else {
					if (verbose) {
						System.out.println("WARN: Unable to parse non-HTML file " + file.getAbsolutePath());
					}
				}
			}

			if (!filesInDir.isEmpty()) {
				HashMap<LocalDCTExtractor, RecordList> allRecords = new HashMap<LocalDCTExtractor, RecordList>();
				int i = 0;

				for (File file : filesInDir) {
					if (verbose) {
						System.out.println("   " + (++i) + " / " + filesInDir.size() + " : " + file.getName());
					}
					URL url = urlMapping.get(file.getName());					
					extractor = getExtractor(locale, options.getProperties(), url, verbose);

					PageInfo pageInfo = extractor.factory.getPageInfos(new FileInputStream(file), file.getAbsolutePath(), false);

					// Try to get DCT from URL
					PageInfo urlPageInfo = extractor.getDCTFromURL(url);
					// If found, just get this
					if (urlPageInfo != null) {
						pageInfo.setDCT(urlPageInfo.getDCT());
					}
					// Else, add records to the set
					else {
						RecordList records = allRecords.get(extractor);
						if (records == null) {
							records = pageInfo.getRecords();
						} else {
							records.addAll(pageInfo.getRecords());
						}
						allRecords.put(extractor, records);
					}
					result.put(file.getAbsolutePath(), pageInfo);
				}

				for (Entry<LocalDCTExtractor, RecordList> recordEntries : allRecords.entrySet()) {
					extractor = recordEntries.getKey();
					extractor.factory.saveToCRF(recordEntries.getValue(), extractor.testOutFileDataset);
					// Launch test
					if (wapitiTest(extractor.testOutFileDataset, extractor.resultFile, wapitiModelFilePath, wapitiBinaryFile, verbose) != 0) {
						throw new DCTExtractorException("Wapiti labeling has failed");
					} 

					// Get dates from Wapiti results
					result = extractor.getDCTFromWapitiResult(extractor.resultFile, downloadDate, result, 0.90, getDCTByScores);
				}
			}
		} else {
			throw new DCTExtractorException(dir.getAbsolutePath() + " is not a directory");
		}
		return result;

	}

	public PageInfo getPageInfos(URL url, Calendar downloadDate, String wapitiModelFilePath, File wapitiBinaryFile) throws IOException, FeatureException, DCTExtractorException, InterruptedException {
		return this.getPageInfos(url, downloadDate, wapitiModelFilePath, wapitiBinaryFile, false);
	}

	public PageInfo getPageInfos(URL url, Calendar downloadDate, String wapitiModelFilePath, File wapitiBinaryFile, boolean getDCTFromScores) throws IOException, FeatureException, DCTExtractorException, InterruptedException {
		return this.getPageInfos(url.openStream(), url.getPath(), url, downloadDate, wapitiModelFilePath, wapitiBinaryFile, getDCTFromScores);
	}

	public PageInfo getPageInfos(File htmlFile, Calendar downloadDate, String wapitiModelFilePath, File wapitiBinaryFile) throws FileNotFoundException, IOException, FeatureException, DCTExtractorException, InterruptedException {
		return this.getPageInfos(htmlFile, downloadDate, wapitiModelFilePath, wapitiBinaryFile, false);
	}

	public PageInfo getPageInfos(File htmlFile, Calendar downloadDate, String wapitiModelFilePath, File wapitiBinaryFile, boolean getDCTFromScores) throws FileNotFoundException, IOException, FeatureException, DCTExtractorException, InterruptedException {
		return this.getPageInfos(htmlFile, null, downloadDate, wapitiModelFilePath, wapitiBinaryFile, getDCTFromScores);
	}

	public PageInfo getPageInfos(File htmlFile, URL url, Calendar downloadDate, String wapitiModelFilePath, File wapitiBinaryFile) throws FileNotFoundException, IOException, FeatureException, DCTExtractorException, InterruptedException {		
		return this.getPageInfos(htmlFile, url, downloadDate, wapitiModelFilePath, wapitiBinaryFile, false);
	}
	
	public PageInfo getPageInfos(File htmlFile, URL url, Calendar downloadDate, String wapitiModelFilePath, File wapitiBinaryFile, boolean getDCTByScores) throws FileNotFoundException, IOException, FeatureException, DCTExtractorException, InterruptedException {		
		return this.getPageInfos(new FileInputStream(htmlFile), htmlFile.getName(), url, downloadDate, wapitiModelFilePath, wapitiBinaryFile, getDCTByScores);
	}
	
	protected PageInfo getPageInfos(InputStream inputStream, String fileName, URL url, Calendar downloadDate, String wapitiModelFilePath, File wapitiBinaryFile) throws IOException, FeatureException, DCTExtractorException, InterruptedException {
		return this.getPageInfos(inputStream, fileName, url, downloadDate, wapitiModelFilePath, wapitiBinaryFile, false);
	}

	protected PageInfo getPageInfos(InputStream inputStream, String fileName, URL url, Calendar downloadDate, String wapitiModelFilePath, File wapitiBinaryFile, boolean getDCTByScores) throws IOException, FeatureException, DCTExtractorException, InterruptedException {		
		PageInfo pageInfo = factory.getPageInfos(inputStream, fileName, false);	

		// Try to get DCT from URL
		PageInfo urlPageInfo = getDCTFromURL(url);
		// If found, just return this
		if (urlPageInfo != null) {
			pageInfo.setDCT(urlPageInfo.getDCT());
		}
		else {
			this.factory.saveToCRF(pageInfo.getRecords(), testOutFileDataset);
			// Launch test
			if (wapitiTest(testOutFileDataset, resultFile, wapitiModelFilePath, wapitiBinaryFile, this.verbose) != 0) {
				throw new DCTExtractorException("Wapiti labeling has failed");
			} 

			//			System.out.println("ICI");
			// Get dates from Wapiti results
			PageInfo dateInfos = getDCTFromWapitiResult(resultFile, downloadDate, null, 0.90, getDCTByScores).values().iterator().next();
			pageInfo.setDateString(dateInfos.getDateString());
			pageInfo.setDCT(dateInfos.getDCT());
		}
		return pageInfo;
	}

	private static LocalDCTExtractor getExtractor(Locale locale, Properties properties, URL parsedURL, boolean verbose) throws IOException, DCTExtractorException, FeatureException {
		/******************
		 * English Locale patch
		 ******************/
		// If locale == Locale.ENGLISH
		// then we keep two extractors (US + UK)
		// and try to choose from the URL
		if (locale == Locale.ENGLISH) {
			Locale specificLocale;
			if (parsedURL == null) {
				specificLocale = Locale.US;
			} else {
				// Change locale if US and if the URL is different from .us, .com, .org, .net
				// what about .ca, .nz ? Don't know their format
				String host = parsedURL.getHost();
				if (! (host.endsWith(".us") || host.endsWith(".com") || host.endsWith(".org") || host.endsWith(".tv") || host.endsWith(".net"))) {
					specificLocale = Locale.UK;
				}
				else {
					specificLocale = Locale.US;
				}
			}
			return getExtractor(specificLocale, properties, parsedURL, verbose);
		}
		else {
			LocalDCTExtractor extractor = extractors.get(locale);
			if (extractor == null) {
				extractor = new LocalDCTExtractor(locale, properties, true, verbose);
				extractors.put(locale, extractor);
			}
			return extractor;
		}
	}

	protected static String testFromDir(File dir, CustomOptions options, Locale locale, String wapitiModelFilePath, File wapitiBinaryFile, HashMap<String, URL> urlMapping, Calendar downloadDate, boolean getDCTFromScores, boolean verbose) throws IOException, DCTExtractorException, FeatureException, InterruptedException {
		HashMap<String, PageInfo> pageInfos = getPageInfosFromDirectory(dir, options, locale, wapitiModelFilePath, wapitiBinaryFile, urlMapping, downloadDate, getDCTFromScores, verbose);
		String result = "";
		for (Entry<String, PageInfo> entry : pageInfos.entrySet()) {
			result += entry.getKey() + "\t" + entry.getValue().toString() + "\n";
		}
		return result;
	}

	protected static String test(File file, CustomOptions options, Locale locale, String wapitiModelFilePath, File wapitiBinaryFile, Calendar downloadDate, boolean getDCTByScores, boolean verbose) throws IOException, DCTExtractorException, FeatureException, InterruptedException {
		return getExtractor(locale, options.getProperties(), null, verbose).getPageInfos(file, downloadDate, wapitiModelFilePath, wapitiBinaryFile, getDCTByScores).toString();
	}

	protected static String test(URL url, CustomOptions options, Locale locale, String wapitiModelFilePath, File wapitiBinaryFile, Calendar downloadDate, boolean getDCTByScores, boolean verbose) throws IOException, DCTExtractorException, FeatureException, InterruptedException {
		return getExtractor(locale, options.getProperties(), url, verbose).getPageInfos(url, downloadDate, wapitiModelFilePath, wapitiBinaryFile, getDCTByScores).toString();
	}

	protected static String test(File dataDir, CustomOptions options, Locale locale, String wapitiModelFilePath, File wapitiBinaryFile, HashMap<String, URL> urlMapping, Calendar downloadDate, boolean getDCTFromScores, boolean verbose) throws FileNotFoundException, FeatureException, DCTExtractorException, IOException, ClassificationException, InterruptedException {

		HashMap<String, PageInfo> pageInfos = getPageInfosFromDirectory(dataDir, options, locale, wapitiModelFilePath, wapitiBinaryFile, urlMapping, downloadDate, getDCTFromScores, verbose);

		/***************
		 * Evaluation
		 ***************/
		return evaluate(pageInfos, verbose);

	}

	protected static void train(File dataDir, CustomOptions options, Locale locale, String wapitiModelFilePath, File wapitiBinaryFile, HashMap<String, URL> urlMapping, boolean verbose) throws FileNotFoundException, FeatureException, DCTExtractorException, IOException, ClassificationException, InterruptedException {
		// Training and development output CRF files
		File trainOutFileDataset = File.createTempFile("train", ".crf"); 
		trainOutFileDataset.deleteOnExit();
		File devOutFileDataset = File.createTempFile("dev", ".crf");
		devOutFileDataset.deleteOnExit();
		File wapitiTemplateFile = File.createTempFile("templates", ".txt");
		wapitiTemplateFile.deleteOnExit();
		// Get data files
		File[] files = dataDir.listFiles(HTML_FILTER);
		ArrayList<File> fileList = new ArrayList<File>();
		for (File file : files) {
			fileList.add(file);
		}
		// Spliting between train, dev and test sets
		double[] weights = {0.9, 0.1};
		List<File>[] fileLists = ListTools.split(fileList, weights);
		// Number of parsed files
		int fileNumber = 0;

		// Parse training set
		DCTExtractorRecordFactory factory = null;
		RecordList trainRecords = null;
		PageInfo pageInfo;
		for (File file : fileLists[0]) {
			URL url = urlMapping.get(file.getName());
			if (verbose) {
				System.out.println("Parse file " + (++fileNumber) + "/" + files.length + ": " + file.getName());
			}

			pageInfo = getExtractor(locale, options.getProperties(), url, verbose).factory.getPageInfos(new FileInputStream(file), file.getAbsolutePath(), true);
			if (trainRecords == null) {
				trainRecords = pageInfo.getRecords();
				factory = (DCTExtractorRecordFactory)trainRecords.getFactory();
			} else {
				trainRecords.addAll(pageInfo.getRecords());
			}
		}

		// Parse dev set
		RecordList devRecords = null;
		for (File file : fileLists[1]) {
			URL url = urlMapping.get(file.getName());
			if (verbose) {
				System.out.println("Parse file " + (++fileNumber) + "/" + files.length + ": " + file.getName());
			}
			pageInfo = getExtractor(locale, options.getProperties(), url, verbose).factory.getPageInfos(new FileInputStream(file), file.getAbsolutePath(), true);
			if (devRecords == null) {
				devRecords = pageInfo.getRecords();
				factory = (DCTExtractorRecordFactory)devRecords.getFactory();
			} else {
				devRecords.addAll(pageInfo.getRecords());
			}
		}


		/***************
		 * Train
		 ***************/
		// Train set
		factory.saveToCRF(trainRecords, trainOutFileDataset);
		if (verbose) {
			System.out.println("  Train CRF file written in " + trainOutFileDataset.getAbsolutePath());
		}
		// Dev set
		factory.saveToCRF(devRecords, devOutFileDataset);
		if (verbose) {
			System.out.println("  Dev CRF file written in " + devOutFileDataset.getAbsolutePath());
		}
		// Templates
		factory.saveTemplates(wapitiTemplateFile);
		if (verbose) {
			System.out.println("  Templates written in " + wapitiTemplateFile.getAbsolutePath());
		}
		// launch train
		if (wapitiTrain(trainOutFileDataset, devOutFileDataset, wapitiTemplateFile, wapitiModelFilePath, wapitiBinaryFile, verbose) != 0) {
			throw new DCTExtractorException("Wapiti training has failed");
		}

	}		

	protected static String crossValidation(File dataDir, CustomOptions options, Locale locale, String wapitiModelFilePath, File wapitiBinaryFile, HashMap<String, URL> urlMapping, Calendar downloadDate, int foldNumber, boolean verbose) throws IOException, FeatureException, DCTExtractorException, InterruptedException, ClassificationException  {
		return crossValidation(dataDir, options, locale, wapitiModelFilePath, wapitiBinaryFile, urlMapping, downloadDate, foldNumber, false, verbose);
	}

	protected static String crossValidation(File dataDir, CustomOptions options, Locale locale, String wapitiModelFilePath, File wapitiBinaryFile, HashMap<String, URL> urlMapping, Calendar downloadDate, int foldNumber,
			boolean stopAtFirstFold, boolean verbose) throws IOException, FeatureException, DCTExtractorException, InterruptedException, ClassificationException {

		// Training and development output CRF files
		File trainOutFileDataset = File.createTempFile("train", ".crf");
		trainOutFileDataset.deleteOnExit();
		File devOutFileDataset = File.createTempFile("dev", ".crf");
		devOutFileDataset.deleteOnExit();
		File wapitiTemplateFile = File.createTempFile("templates", ".txt");
		wapitiTemplateFile.deleteOnExit();

		// Get data files
		File[] files = dataDir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				return file.getName().endsWith(".html");
			}
		});
		ArrayList<File> fileList = new ArrayList<File>();
		for (File file : files) {
			fileList.add(file);
		}

		// Spliting between train, dev and test sets
		List<File>[] fileLists = ListTools.randomSplit(fileList, foldNumber, 1);

		// Number of parsed files
		int fileNumber = 0;
		HashMap<String, PageInfo> pageInfos = new HashMap<String, PageInfo>();
		RecordList[] recordLists = new RecordList[foldNumber];
		PageInfo pageInfo; 	
		DCTExtractorRecordFactory factory = null;
		LocalDCTExtractor extractor;

		for (int foldIndex = 0 ; foldIndex < foldNumber ; foldIndex++) {
			RecordList records = null;

			for (File file : fileLists[foldIndex]) {
				URL url = urlMapping.get(file.getName());
				if (verbose) {
					System.out.println("Parse file " + (++fileNumber) + "/" + files.length + ": " + file.getName());
				}

				//				if (!file.getName().equals("9ee700eb-840c-4f49-852e-77e32eb5082f.html")) {
				//					continue;
				//				}

				pageInfo = getExtractor(locale, options.getProperties(), url, verbose).factory.getPageInfos(new FileInputStream(file), file.getAbsolutePath(), true);
				if (records == null) {
					records = pageInfo.getRecords();
					if (foldIndex == 0) {
						factory = (DCTExtractorRecordFactory)records.getFactory();
					}
				} else {
					records.addAll(pageInfo.getRecords());
				}
			}
			recordLists[foldIndex] = records;
		}
		fileNumber = 0;

		//        File testOutFileDataset = new File(System.getenv("HOME") + "/tmp/test.crf");
		//        File resultFile = new File(System.getenv("HOME") + "/tmp/result.crf");
		//        resultFile.delete();


		for (int foldIndex = 0 ; foldIndex < foldNumber ; foldIndex++) {
			if (verbose) {
				System.out.println("Cross-validation, fold #" + (foldIndex+1) + " / " + foldNumber);
			}	
			RecordList devRecords = null;
			File[] testFiles = null;
			RecordList trainRecords = null;
			for (int i = 0 ; i < recordLists.length ; i++) {
				if (i == foldIndex) {
					testFiles = fileLists[i].toArray(new File[0]);
				}
				else if (i == foldIndex - 1 || foldIndex == 0 && i == foldNumber - 1) {
					devRecords = recordLists[i];
				}
				else {
					if (trainRecords == null) {
						trainRecords = new RecordList(recordLists[i]);
					} else {						
						trainRecords.addAll(recordLists[i]);
					}
				}
			}


			/***************
			 * Train
			 ***************/
			 // Train set
			 factory.saveToCRF(trainRecords, trainOutFileDataset);
			if (verbose) {
				System.out.println("  Train CRF file written in " + trainOutFileDataset.getAbsolutePath());
			}
			// Dev set
			factory.saveToCRF(devRecords, devOutFileDataset);
			if (verbose) {
				System.out.println("  Dev CRF file written in " + devOutFileDataset.getAbsolutePath());
			}
			// Templates
			factory.saveTemplates(wapitiTemplateFile);
			if (verbose) {
				System.out.println("  Templates written in " + wapitiTemplateFile.getAbsolutePath());
			}
			// launch train
			if (wapitiTrain(trainOutFileDataset, devOutFileDataset, wapitiTemplateFile, wapitiModelFilePath, wapitiBinaryFile, verbose) != 0) {
				throw new DCTExtractorException("Wapiti training has failed");
			}

			/**************
			 * Test
			 **************/
			// File by file
			for (File testedFile : testFiles) {
				//				if (!testedFile.getName().equals("c7592d94-b244-4995-bc53-553614804b87.html")) {
				//					continue;
				//				}
				URL url = urlMapping.get(testedFile.getName());
				extractor = getExtractor(locale, options.getProperties(), url, verbose);
				if (verbose) {
					System.out.println("  Parse file " + (++fileNumber) + " / " + files.length + ": " + testedFile.getAbsolutePath());
				}
				pageInfo = extractor.getPageInfos(testedFile, url, downloadDate, wapitiModelFilePath, wapitiBinaryFile);

				/*********************
				 * English Locale patch
				 *********************
				 * If no DCT is found with US parser,
				 * try with UK parser
				 *********************/
				//				if (pageInfo.getDCT() == null && defaultExtractor.getLocale() == Locale.US) {
				//					if (logger != null) {
				//						logger.info("   No DCT found, try with a UK parser");
				//					}
				//					pageInfo = extractors.get(Locale.UK).getPageInfos(testedFile, url, downloadDate);
				//				}
				// Add page info to the pool
				pageInfos.put(testedFile.getAbsolutePath(), pageInfo);
			}
			if (stopAtFirstFold) {
				break;
			}
		}

		/***************
		 * Evaluation
		 ***************/
		return evaluate(pageInfos, verbose);
	}


	protected static String splitValidation(File dataDir, CustomOptions options, Locale locale, String wapitiModelFilePath, File wapitiBinaryFile, HashMap<String, URL> urlMapping, Calendar downloadDate, boolean verbose) throws IOException, FeatureException, DCTExtractorException, InterruptedException, ClassificationException {
		return crossValidation(dataDir, options, locale, wapitiModelFilePath, wapitiBinaryFile, urlMapping, downloadDate, 10, true, verbose);
	}



	public static String evaluate(HashMap<String, PageInfo> pageInfos, boolean verbose) {
		int titleOK = 0, titleNoise = 0, titleSilence = 0;
		int dateOK = 0, dateNoise = 0, dateSilence = 0;
		int dateStringOK = 0;

		PageInfo pageInfo;
		String refTitle, hypTitle;
		Calendar refDate, hypDate;
		String refDateString, hypDateString;

		for (Entry<String, PageInfo> entry : pageInfos.entrySet()) {
			pageInfo = entry.getValue();
			// Title
			refTitle = pageInfo.getRefTitle();
			hypTitle = pageInfo.getTitle();
			if (refTitle == null || refTitle.length() == 0) {
				if (hypTitle == null || hypTitle.length() == 0) {
					titleOK++;
				} else {
					titleNoise++;
				}
			} 
			else if (hypTitle == null || hypTitle.length() == 0) {
				titleSilence++;
			}
			else if (refTitle.equalsIgnoreCase(hypTitle)) {
				titleOK++;
			} 
			else {
				if (verbose) {
					System.out.println("Bad title for file " + entry.getKey() + " \n\"" + hypTitle + "\"" + "\n\"" + refTitle + "\"");
				}
				titleNoise++;
			}
			// Date
			refDate = pageInfo.getRefDCT();
			hypDate = pageInfo.getDCT();
			// Date string
			refDateString = pageInfo.getRefDateString();
			hypDateString = pageInfo.getDateString();

			//			System.out.println(hypDate);
			//			if (hypDate != null) {
			//				System.out.println(hypDate.getTime());
			//			}

			if (refDate == null) {
				if (hypDate == null) {
					dateOK++;
				}
				else {
					dateNoise++;
				}
			}
			else {
				if (hypDate != null) {
					if (refDate.get(Calendar.YEAR) == hypDate.get(Calendar.YEAR) &&
							refDate.get(Calendar.MONTH) == hypDate.get(Calendar.MONTH) &&
							refDate.get(Calendar.DAY_OF_MONTH) == hypDate.get(Calendar.DAY_OF_MONTH)) {
						dateOK++;
					} 
					else {
						if (verbose) {
							System.out.println("Bad date for file " + entry.getKey() + " \n\"" + refDate.getTime() + "\"" + "\n\"" + hypDate.getTime() + "\" (" + hypDateString + ")");
						}
						dateNoise++;
					}
				} else {
					dateSilence++;
					if (verbose) {
						System.out.println("No date found for file " + entry.getKey() + " \n\"" + refDate.getTime() + "\"");
					}

				}
			}
			if (refDateString == null) {
				if (hypDateString == null) {
					dateStringOK++;
				}
			}
			else if (refDateString.equals(hypDateString)) {
				dateStringOK++;
			} 
		}

		String result = "Results: \n -----------------------\n";
		int total = pageInfos.size();
		result += "Title \n";
		result += "   OK: " + titleOK + " / " + total + " = " + ((double)titleOK/(double)total) + "\n";
		result += "   Noise: " + titleNoise + " / " + total + " = " + ((double)titleNoise/(double)total) + "\n";
		result += "   Silence: " + titleSilence + " / " + total + " = " + ((double)titleSilence/(double)total) + "\n";
		result += "Date: \n";
		result += "   OK: " + dateOK + " / " + total + " = " + ((double)dateOK/(double)total) + "\n";
		result += "   Noise: " + dateNoise + " / " + total + " = " + ((double)dateNoise/(double)total) + "\n";
		result += "   Silence: " + dateSilence + " / " + total + " = " + ((double)dateSilence/(double)total) + "\n";
		result += "DateString: " + dateStringOK + " / " + total + " = " + ((double)dateStringOK/(double)total) + "\n";
		return result;
	}




}
