package fr.limsi.dctfinder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.Option;
import org.apache.commons.lang3.StringUtils;

import fr.limsi.tools.classification.ClassificationException;
import fr.limsi.tools.classification.FeatureException;
import fr.limsi.tools.common.CommonTools;
import fr.limsi.tools.common.CustomOptions;
import fr.limsi.tools.common.DateTools;
import fr.limsi.tools.common.LanguageTools;

public class DCTExtractorTrainingAndEvaluation {

    /********************
     * Program options
     ********************/
    private static final String OPTION_LANGUAGE = "lang";
    private static final String OPTION_MODE = "m";
    private static final String OPTION_VERBOSE = "v";
    private static final String OPTION_DOWNLOAD_DATE = "download";
    private static final String OPTION_FILE_NAME = "file";
    private static final String OPTION_DIR_NAME = "dir";
    private static final String OPTION_URL = "url";
    private static final String OPTION_CONF_FILE = "c";
    private static final String OPTION_WAPITI_BINARY_FILE = "w";
    private static final String OPTION_GET_DCT_BY_SCORES = "s";
    
    // For evaluation purpose only
    private static final String OPTION_URL_MAPPING_FILE = "url_mapping";
    private static final String OPTION_MODE_USE = "use";
    private static final String OPTION_MODE_TRAIN = "train";
    private static final String OPTION_MODE_TEST = "test";
    private static final String OPTION_MODE_SPLIT_VALIDATION = "split";
    private static final String OPTION_MODE_CROSS_VALIDATION = "cross";
    
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



    /**
     * Get the mapping between local file and URL.
     * The file must have the following format (from L3S-GN1) :
     * <urn:uuid:e4af1d07-4976-498b-ac5d-e3593a6fc195>  http://www.kansascity.com/entertainment/story/448705.html
     * @param urlMappingFile
     * @return
     * @throws IOException 
     * @throws DCTExtractorException 
     */
    private static HashMap<String, URL> getURLMapping(File urlMappingFile) throws IOException, DCTExtractorException {
        InputStream ips = new FileInputStream(urlMappingFile); 
        InputStreamReader ipsr = new InputStreamReader(ips);
        BufferedReader br = new BufferedReader(ipsr);
        Pattern linePattern = Pattern.compile("<urn:uuid:([^>]+)>   (.+)");
        String line;
        Matcher matcher;
        HashMap<String, URL> mapping = new HashMap<String, URL>();
        while ((line = br.readLine())!=null){
            matcher = linePattern.matcher(line);
            if (matcher.matches()) {
                mapping.put(matcher.group(1) + ".html", new URL(matcher.group(2)));
            } else {
                br.close();
                throw new DCTExtractorException("Bad format in URL mapping file: \n" + line);
            }
        }
        br.close();
        return mapping;
    }
    
//    public static void main(String[] args) throws Exception {
//        URL url = new URL("http://www.telegraph.co.uk/comment/10547281/It-is-high-time-we-raised-interest-rates-and-returned-to-normality.html");
//        DCTExtractor extractor = new DCTExtractor(new File("/home/xtannier/tools/wapiti-1.4.0/wapiti"));
//
//        PageInfo pi = extractor.getPageInfos(url.openStream(), url, Locale.ENGLISH, null);
//        System.out.println(pi);
//    }

    public static void main(String[] args) {
        int chronoId = CommonTools.startChrono();

        boolean verbose = false;

        File confFile = new File("./conf/constants.txt");

        // Default values
        if (args.length == 0) {
            //            String home = System.getenv("HOME");
            String options = "" 
                    + " -" + OPTION_CONF_FILE + " " + System.getenv("HOME") + "/Recherche/DCTFinder/conf/constants.txt "
                    //                    + " -" + OPTION_URL + " http://english.yonhapnews.co.kr/national/2013/03/20/46/0301000000AEN20130320004251315F.HTML"
                    //                    + " -" + OPTION_DIR_NAME + " " + System.getenv("HOME") + "/data/Web-FR-2013/date-exported"
                    //                    + " -" + OPTION_LANGUAGE + " fr"
                    //                                        + " -" + OPTION_VERBOSE
                    //                                        + " -" + OPTION_MODE + " " + OPTION_MODE_CROSS_VALIDATION
                    //                                        + " -" + OPTION_DIR_NAME + " " + System.getenv("HOME") + "/data/L3S-GN1/date-exported"
                    //                                        + " -" + OPTION_DOWNLOAD_DATE + " 20080401"
                    //                                        + " -" + OPTION_LANGUAGE + " en"
                    //                                        + " -" + OPTION_URL_MAPPING_FILE + " " + System.getenv("HOME") + "/data/L3S-GN1/url-mapping.txt"
                    //                                        + " -" + OPTION_MODE + " " + OPTION_MODE_TRAIN
                    //                                        + " -" + OPTION_DIR_NAME + " " + System.getenv("HOME") + "/data/L3S-GN1/date-exported"
                    //                                        + " -" + OPTION_DOWNLOAD_DATE + " 20080401"
                    //                                        + " -" + OPTION_LANGUAGE + " en"
                    //                                        + " -" + OPTION_URL_MAPPING_FILE + " " + System.getenv("HOME") + "/data/L3S-GN1/url-mapping.txt"
                    //                                        + " -" + OPTION_MODE + " " + OPTION_MODE_TEST
                    //                                        + " -" + OPTION_DIR_NAME + " " + System.getenv("HOME") + "/data/Web-EN-2013/date-exported"
                    //                                        + " -" + OPTION_LANGUAGE + " en"
                    //                                        + " -" + OPTION_DOWNLOAD_DATE + " 20130326"
                    //                                        + " -" + OPTION_URL_MAPPING_FILE + " " + System.getenv("HOME") + "/data/Web-EN-2013/url-mapping.txt"
                    + " -" + OPTION_MODE + " " + OPTION_MODE_TEST
                    + " -" + OPTION_DIR_NAME + " " + System.getenv("HOME") + "/data/Web-FR-2013/date-exported"
                    + " -" + OPTION_DOWNLOAD_DATE + " 20130401"
                    + " -" + OPTION_LANGUAGE + " fr"
                    + " -" + OPTION_URL_MAPPING_FILE + " " + System.getenv("HOME") + "/data/Web-FR-2013/url-mapping.txt"
                    //              + " -" + CustomOptions.OPTION_VERBOSITY + " " + Level.OFF
                    ;

            String[] newArgs = StringUtils.split(options, " ");
            System.out.println("OPTIONS : \n" + options);
            args = newArgs;
        }
        CustomOptions options = new CustomOptions(DCTExtractorRecordFactory.class.getSimpleName());

        try {

            /**************************/
            /* Program parameters *****/
            /**************************/
            Option modeOption = new Option(OPTION_MODE, true, "Mode: [" + OPTION_MODE_TRAIN + "|" + OPTION_MODE_TEST + "|" + OPTION_MODE_SPLIT_VALIDATION + "|" + OPTION_MODE_CROSS_VALIDATION + "] (default is test)");
            options.addOption(modeOption);
            Option langOption = new Option(OPTION_LANGUAGE, true, "Language: [en|en_US|en_GB|fr]");
            langOption.setRequired(true);
            options.addOption(langOption);
            Option urlMappingOption = new Option(OPTION_URL_MAPPING_FILE, true, "file/URL mapping (format sample: <urn:uuid:934ed874-7230-4e6f-9096-5716ff420a94> http://www.scrippsnews.com/node/29843");
            options.addOption(urlMappingOption);
            Option verboseOption = new Option(OPTION_VERBOSE, false, "Verbose mode (sets verbose level to INFO)");
            options.addOption(verboseOption);
            Option downloadOption = new Option(OPTION_DOWNLOAD_DATE, true, "Download date (format: YYYYMMDD, default is no download date)");
            options.addOption(downloadOption);
            Option urlOption = new Option(OPTION_URL, true, "URL to parse");
            options.addOption(urlOption);
            Option fileOption = new Option(OPTION_FILE_NAME, true, "File to parse");
            options.addOption(fileOption);
            Option dirOption = new Option(OPTION_DIR_NAME, true, "Directory to parse (containing HTML files)");
            options.addOption(dirOption);
            Option confFileOption = new Option(OPTION_CONF_FILE, true, "Configuration file");
            confFileOption.setRequired(true);
            options.addOption(OPTION_WAPITI_BINARY_FILE, true, "Wapiti binary file (default: as specified in configuration file)");
            options.addOption(confFileOption);
            Option getDCTByScoresOption = new Option(OPTION_GET_DCT_BY_SCORES, false, "If set, choose DCT from candidates by picking the best CRF score. Otherwise, use the heuristic described in the LREC paper.");
            options.addOption(getDCTByScoresOption);

            args = options.parseOptions(args);

            /************************/
            /* Load parameters  *****/
            /************************/
            // Logger
            if (options.hasOptionValue(OPTION_VERBOSE)) {
                verbose = true;
                //          logger = Logger.getLogger(LocalDCTExtractor.class);
                //          logger.setLevel(Level.DEBUG);
                //          PatternLayout layout = new PatternLayout("%5p: %m%n");
                //          logger.addAppender(new ConsoleAppender(layout));
            }
            //      if (options.hasOptionValue(CustomOptions.OPTION_VERBOSITY)) {
            //          if (logger == null) {
            //              logger = Logger.getLogger(LocalDCTExtractor.class);
            //              PatternLayout layout = new PatternLayout("%5p: %m%n");
            //              logger.addAppender(new ConsoleAppender(layout));
            //          }
            //          logger.setLevel(options.getLogLevel());
            //      }

            // Locale
            Locale locale = null;
            if (options.hasOptionValue(OPTION_LANGUAGE)) {
                locale = LanguageTools.getLocaleFromString(options.getOptionValue(OPTION_LANGUAGE));
            } else {
                locale = Locale.ENGLISH;
            }

            // Download date
            Calendar downloadDate = null;
            if (options.hasOptionValue(OPTION_DOWNLOAD_DATE)) {
                downloadDate = new GregorianCalendar(locale);
                downloadDate.setTime(DateTools.shortDateParse(options.getOptionValue(OPTION_DOWNLOAD_DATE)));
                downloadDate.set(Calendar.HOUR, 23);
                downloadDate.set(Calendar.MINUTE, 59);
                downloadDate.set(Calendar.SECOND, 59);
                downloadDate.set(Calendar.MILLISECOND, 999);
            }

            // Load properties from conf file
            if (options.hasOptionValue(OPTION_CONF_FILE)) {
                confFile = new File(options.getOptionValue(OPTION_CONF_FILE));
            }
            if (!confFile.exists()) {
                throw new DCTExtractorException("Configuration file name " + confFile.getAbsolutePath() + " does not exist.\nSpecify a configuration file name with option -" + OPTION_CONF_FILE + ".");
            }
            options.addProperties(confFile);

            // Wapiti model file name
            String wapitiModelFileName = options.getProperty(WAPITI_MODEL_FILE);
            if (wapitiModelFileName == null) {
                throw new DCTExtractorException("Parameter " + WAPITI_MODEL_FILE + " must be set in file " + options.getOptionValue(OPTION_CONF_FILE));
            }

            //            File wapitiModelFile = new File(wapitiModelFileName);

            // Wapiti binary file
            File wapitiBinaryFile = null;
            if (options.hasOptionValue(OPTION_WAPITI_BINARY_FILE)) {
                wapitiBinaryFile = new File(options.getOptionValue(OPTION_WAPITI_BINARY_FILE));
            }
            else {
            	throw new DCTExtractorException("Parameter " + OPTION_WAPITI_BINARY_FILE + " must be set");
            }
            if (wapitiBinaryFile.exists()) {
                options.setProperty(WAPITI_BINARY_PATH, wapitiBinaryFile.toString());
            } else {
                throw new DCTExtractorException("Binary file " + wapitiBinaryFile.getAbsolutePath() + " does not exist");
            }

            // Encoding for rule files
            if (options.getProperty(ENCODING) == null) {
                System.out.println("No " + ENCODING + " specified in file " + options.getOptionValue(OPTION_CONF_FILE) + ", set to default UTF-8");
                options.setProperty(ENCODING, "UTF-8");
            }

            // File-to-URL mapping (for massive testing only)
            HashMap<String, URL> urlMapping = new HashMap<String, URL>();
            if (options.hasOptionValue(OPTION_URL_MAPPING_FILE)) {
                urlMapping = getURLMapping(new File(options.getOptionValue(OPTION_URL_MAPPING_FILE)));
            }
            
            // Get DCT by choosing the best CRF score
            // (otherwise, see the heuristic described in the paper)
            boolean getDCTByScores = options.hasOptionValue(OPTION_GET_DCT_BY_SCORES);

            String result = null;

            // Mode (use, training, testing, cross-validation, split-validation)
            String mode = OPTION_MODE_USE;
            if (options.hasOptionValue(OPTION_MODE)) {
                mode = options.getOptionValue(OPTION_MODE);
            }          

            // Regular use mode
            if (mode.equals(OPTION_MODE_USE)) {
                URL wapitiModelURL = Thread.currentThread().getClass().getClassLoader().getResource(wapitiModelFileName);
                if (wapitiModelURL == null) {
                    throw new DCTExtractorException("Could not find Wapiti model file " + wapitiModelFileName);
                }
                String wapitiModelFilePath = wapitiModelURL.getFile();
                // URL parsing
                if (options.hasOptionValue(OPTION_URL)) {
                    result = LocalDCTExtractor.test(new URL(options.getOptionValue(OPTION_URL)), options, locale, wapitiModelFilePath, wapitiBinaryFile, downloadDate, getDCTByScores, verbose);
                }
                // File parsing
                else if (options.hasOptionValue(OPTION_FILE_NAME)) {
                    result = LocalDCTExtractor.test(new File(options.getOptionValue(OPTION_FILE_NAME)), options, locale, wapitiModelFilePath, wapitiBinaryFile, downloadDate, getDCTByScores, verbose);
                } 
                // Directory parsing
                else if (options.hasOptionValue(OPTION_DIR_NAME)) {
                    result = LocalDCTExtractor.testFromDir(new File(options.getOptionValue(OPTION_DIR_NAME)), options, locale, wapitiModelFilePath, wapitiBinaryFile, urlMapping, downloadDate, getDCTByScores, verbose);
                }
                else {
                    throw new DCTExtractorException("In regular use mode, must specify a file (-" + OPTION_FILE_NAME + "), url (-" + OPTION_URL + ") or a directory (-" + OPTION_DIR_NAME + ")");
                }
            }
            // Evaluation modes
            else {
                // Split-validation mode
                if (mode.equals(OPTION_MODE_SPLIT_VALIDATION)) {
                    File dir = new File(options.getOptionValue(OPTION_DIR_NAME));
                    result = LocalDCTExtractor.splitValidation(dir, options, locale, wapitiModelFileName, wapitiBinaryFile, urlMapping, downloadDate, verbose); 
                }
                // Cross validation mode
                else if (mode.equals(OPTION_MODE_CROSS_VALIDATION)) {
                    File dir = new File(options.getOptionValue(OPTION_DIR_NAME));
                    result = LocalDCTExtractor.crossValidation(dir, options, locale, wapitiModelFileName, wapitiBinaryFile, urlMapping, downloadDate, 10, verbose);
                }
                // Training mode
                else if (mode.equals(OPTION_MODE_TRAIN)) {
                    File dir = new File(options.getOptionValue(OPTION_DIR_NAME));
                    LocalDCTExtractor.train(dir, options, locale, wapitiModelFileName, wapitiBinaryFile, urlMapping, verbose);
                }
                // Test mode
                else if (mode.equals(OPTION_MODE_TEST)) {
                    URL wapitiModelURL = Thread.currentThread().getClass().getClassLoader().getResource(wapitiModelFileName);
                    if (wapitiModelURL == null) {
                        throw new DCTExtractorException("Could not find Wapiti model file " + wapitiModelFileName);
                    }
                    String wapitiModelFilePath = wapitiModelURL.getFile();
                    File dir = new File(options.getOptionValue(OPTION_DIR_NAME));
                    result = LocalDCTExtractor.test(dir, options, locale, wapitiModelFilePath, wapitiBinaryFile, urlMapping, downloadDate, getDCTByScores, verbose);
                }
            }

            // Print result
            if (result != null) {
                if (verbose) {
                    System.out.println(result);
                } else {
                    System.out.println(result);
                }
            }
            if (verbose) {
                System.out.println("Time elapsed: " + CommonTools.formatEndChrono(chronoId));
                System.out.println("Time elapsed: " + CommonTools.formatEndChrono(chronoId));
            }
        } catch (DCTExtractorException e) {
            //            e.printStackTrace();
            if (verbose) {
                System.out.println(e.getMessage());
            } else {
                System.err.println(e.getMessage());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (FeatureException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassificationException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (org.apache.commons.cli.ParseException e) {
            System.err.println(e.getMessage() + "\n");
            options.printHelp();
        } 
    }
}
