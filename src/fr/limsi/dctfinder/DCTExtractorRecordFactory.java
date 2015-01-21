package fr.limsi.dctfinder;

import fr.limsi.tools.classification.FeatureException;
import fr.limsi.tools.classification.Record;
import fr.limsi.tools.classification.RecordList;
import fr.limsi.tools.classification.crf.CRFRecordFactory;
import fr.limsi.tools.classification.crf.FeatureTemplate;
import fr.limsi.tools.classification.crf.SeparationRecord;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DCTExtractorRecordFactory extends CRFRecordFactory {
	private static final long serialVersionUID = 1L;

	/********************
	 * Feature types (for evaluation testing)
	 ********************/
	public static final int TEXT_FEATURE_TYPE = 1<<0;
	public static final int STRUCTURE_FEATURE_TYPE = 1<<1;
	public static final int VOCABULARY_FEATURES = 1<<2;
	public static final int STRUCTURE_CONTEXT_FEATURES = 1<<3;
	public static final int POSITION_FEATURES = 1<<4;

	/*********************
	 * Integer char constants
	 *********************/
	private final static int CHAR_DASH = 45;
	private final static int CHAR_LT = 60;
	private final static int CHAR_GT = 62;
	private final static int CHAR_OPENING_BRACKET = 40;
	private final static int CHAR_CLOSING_BRACKET = 41;
	private final static int CHAR_OPENING_SQUARE_BRACKET = 91;
	private final static int CHAR_CLOSING_SQUARE_BRACKET = 93;
	private final static int CHAR_COLON = 58;
	private final static int CHAR_COMMA = 44;
	private final static int CHAR_DOT = 46;
	private final static int CHAR_PIPE = 124;
	private final static int CHAR_SEMI_COMMA = 59;
	private final static int CHAR_DOUBLE_QUOTE = 34;
	private final static int CHAR_SLASH = 47;
	private final static int CHAR_EQUALS = 61;
	private final static int CHAR_AMP = 38;
	private final static int CHAR_NBSP = 160;
	private final static int CHAR_EXCLAMATION_MARK = 33;

	// Max entry length
	private final static int MAX_ENTRY_LENGTH = 1000;
	// Max "kept-in-case" length = 10
	private final static int MAX_KEPT_IN_CASE_LENGH = 10;

	/*********************
	 * Constants
	 *********************/
	private final static String relation = "DCT";

	// Feature names
	protected static final String TEXT = "TEXT";
	protected static final String TYPE = "TYPE";
	protected static final String TIME_RELATED_TAG = "TIME_RELATED_TAG";
	protected static final String VOCABULARY_FEATURE = "VOCABULARY_FEATURE";
	protected static final String DETAILED_VOCABULARY_FEATURE = "DETAILED_VOCABULARY_FEATURE";
	protected static final String DATE_VOCABULARY_FEATURE = "DATE_VOCABULARY_FEATURE";
	protected static final String FULL_DATE_FEATURE = "FULL_DATE_FEATURE";
	protected static final String DISTANCE_FROM_TITLE = "DISTANCE_FROM_TITLE";
	protected static final String DISTANCE_FROM_TITLE_DISC = "DISTANCE_FROM_TITLE_DISC";
	protected static final String POSITION = "POSITION";
	protected static final String POSITION_RATE = "POSITION_RATE";
	protected static final String POSITION_DISC = "POSITION_DISC";
	protected static final String DATE_CONTENT_ONLY_IN_TAG = "DATE_CONTENT_ONLY_SO_FAR";
	protected static final String DATES_SO_FAR = "DATES_SO_FAR";
	protected static final String FULL_DATES_SO_FAR = "FULL_DATES_SO_FAR";
	protected static final String DATES_IN_ALL = "DATES_IN_ALL";
	protected static final String DATES_IN_ALL_DISC = "DATES_IN_ALL_DISC";
	protected static final String WORD_NUMBER_IN_TAG = "WORD_NUMBER_IN_TAG";
	protected static final String WORD_NUMBER_IN_TAG_DISC = "WORD_NUMBER_IN_TAG_DISC";
	protected static final String WORD_NUMBER_IN_TAG_SO_FAR = "WORD_NUMBER_IN_TAG_SO_FAR";
	protected static final String WORD_NUMBER_IN_TAG_SO_FAR_DISC = "WORD_NUMBER_IN_TAG_SO_FAR_DISC";
	protected static final String MAX_TAG_LENGTH_BEFORE = "MAX_TAG_LENGTH_BEFORE";
	protected static final String AVG_TAG_LENGTH_BEFORE = "AVG_TAG_LENGTH_BEFORE";
	protected static final String MAX_TAG_LENGTH_BEFORE_DISC = "MAX_TAG_LENGTH_BEFORE_DISC";
	protected static final String AVG_TAG_LENGTH_BEFORE_DISC = "AVG_TAG_LENGTH_BEFORE_DISC";
	protected static final String FIRST_LONG_TAG_POSITION = "FIRST_LONG_TAG_POSITION";
	protected static final String LAST_LONG_TAG_POSITION = "LAST_LONG_TAG_POSITION";
	protected static final String DISTANCE_FROM_FIRST_LONG_TAG = "DISTANCE_FROM_FIRST_LONG_TAG";
	protected static final String DISTANCE_FROM_LAST_LONG_TAG = "DISTANCE_FROM_LAST_LONG_TAG";
	protected static final String DISTANCE_FROM_FIRST_LONG_TAG_DISC = "DISTANCE_FROM_FIRST_LONG_TAG_DISC";
	protected static final String DISTANCE_FROM_LAST_LONG_TAG_DISC = "DISTANCE_FROM_LAST_LONG_TAG_DISC";
	protected static final String NUMBER_OF_DATE_ELEMENTS_AROUND = "NUMBER_OF_DATE_ELEMENTS_AROUND"; 
	protected static final String NUMBER_OF_DATE_ELEMENTS_AROUND_DISC = "NUMBER_OF_DATE_ELEMENTS_AROUND_DISC";
	protected static final String DISTANCE_FROM_TRIGGER = "DISTANCE_FROM_TRIGGER";
	protected static final String DISTANCE_FROM_TRIGGER_DISC = "DISTANCE_FROM_TRIGGER_DISC";
	protected static final String DISTANCE_FROM_ANTI_TRIGGER = "DISTANCE_FROM_ANTI_TRIGGER";
	protected static final String DISTANCE_FROM_ANTI_TRIGGER_DISC = "DISTANCE_FROM_ANTI_TRIGGER_DISC";

	// File separator in CRF file
	protected final static String DCTFINDER_FILE_SEPARATOR = "DCTFINDER_FILE";


	// Lexical features
	protected static final String INSIDE_TRIGGER = "inside-trigger";
	protected static final String TRIGGER = "trigger";
	protected static final String ANTI_TRIGGER = "anti-trigger";
	protected static final String DATE_ELEM = "date";

	// Features types
	protected static final byte BEFORE_HIGH = -3;
	protected static final byte BEFORE_MEDIUM = -2;
	protected static final byte BEFORE_LOW = -1;
	protected static final byte AFTER_HIGH = 3;
	protected static final byte AFTER_MEDIUM = 2;
	protected static final byte AFTER_LOW = 1;
	protected static final byte INSIDE = 0;

	protected static final byte POSITION_Q1 = 1;
	protected static final byte POSITION_Q2 = 2;
	protected static final byte POSITION_Q3 = 3;
	protected static final byte POSITION_Q4 = 4;

	protected static final byte ZERO = 0;
	protected static final byte ONE = 1;
	protected static final byte TWO = 2;
	protected static final byte THREE_OR_MORE = 3;

	protected static final byte NUMBER_VERY_LOW = 0;
	protected static final byte NUMBER_LOW = 1;
	protected static final byte NUMBER_MEDIUM = 2;
	protected static final byte NUMBER_HIGH = 3;
	protected static final byte NUMBER_VERY_HIGH = 4;

	// Types during HTML parsing
	private final static byte TYPE_TEXT = 1;
	private final static byte TYPE_OPENING_TAG = 2;
	private final static byte TYPE_CLOSING_TAG = 3;

	/*********************
	 * Access to user-defined constants
	 * (parameter file)
	 *********************/
	private final static String MIN_TITLE_SIZE = "MIN_TITLE_SIZE";

	/*********************
	 * WebAnnotator annotation tool
	 * specific constants
	 *********************/
	private static final String WA_START_TAG = "WA_Start";
	private static final String WA_END_TAG = "WA_End";
	private static final String TITLE_TAG = "title";
	// For evaluation only :
	private static final String WA_TYPE_ATTR_NAME = "type";
	private static final String WA_SUBTYPES_ATTR_NAME = "subtypes";
	private static Pattern WA_SUBTYPE_PATTERN;
	private static SimpleDateFormat WA_DATE_FORMAT;
	private static final String WA_TITLE_ATTR_VALUE = "title";


	/*********************
	 * HTML elements to skip (tag + content)
	 *********************/
	private static final HashSet<String> ELEMS_TO_SKIP = new HashSet<String>();

	static {
		ELEMS_TO_SKIP.add("script");
		ELEMS_TO_SKIP.add("noscript");
		ELEMS_TO_SKIP.add("style");
		ELEMS_TO_SKIP.add("option");
		//		ELEMS_TO_SKIP.add("form");
	}

	/*********************
	 * HTML tags to skip (but parse content)
	 *********************/
	private static final HashSet<String> TAGS_TO_SKIP = new HashSet<String>();

	static {
		TAGS_TO_SKIP.add("a");
		TAGS_TO_SKIP.add("br");
		TAGS_TO_SKIP.add("img");
	}

	/********************
	 * General infos
	 ********************/	
	//	private CustomOptions options;
	//	private TypedProperties properties;

	// Currently parsing an element to skip
	private boolean inSkipElem;
	// Current text
	private String runningText;
	// Current word number
	private int wordNumber;
	// Word number in current tag
	private int wordNumberInTag;
	// Charset
	private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
	private Charset charset;
	// Pattern for HTML Charset declaration inside meta:content attribute
	private static final Pattern charsetPattern = Pattern.compile("charset=([^\" ]+)");

	private LinkedList<Integer> lastTagsWordNumber;
	private static final int TAG_LENGTH_WINDOW = 3;
	private static final int LONG_TAG_THRESHOLD = 20;
	private int firstLongTagPosition;
	private int lastLongTagPosition;

	//	/********************
	//	 * Date vocabulary infos
	//	 ********************/
	//	DateParser dateParser;

	/********************
	 * Date infos
	 ********************/
	// Currently parsing an WA-annotated tag 
	private boolean inWADateTag;
	// Number of parsed words since entering
	// in a WA-annotated date
	private int elemNumberInWATag;
	// All patterns for vocabulary matching
	private HashMap<String, HashMap<Pattern, String>> regexes;
	// All patterns for finding date-related tags
	private ArrayList<Pattern> timeTagRelatedPatterns;
	// The maximum number of non-date-related element in a tag
	// containing only a date
	private static final byte MAX_NO_DATE_CONTENT_IN_DATE = 2;
	// The current tag contains only date-related content
	private byte dateContentInTag;
	private byte consecutiveDateContentInTag;
	private byte bestConsecutiveDateContentInTag;
	private byte noDateContentInTag;
	// The number of date met so far in the document
	private int dateContentNumber;
	private int fullDateContentNumber;
	// The position of date elements met in the document
	private ArrayList<Integer> dateElementPositions;
	private static final int DATE_AROUND_LIMITS = 100;
	private int lastTriggerPosition;
	private int lastAntiTriggerPosition;
	private static final int MAX_READ_BYTES = 100000;


	/********************
	 * Title infos
	 ********************/
	// Priority of the heuristic "text is included into the doc <title>"
	private static final byte INCLUDED_IN_DOC_TITLE_PRIORITY_LEVEL = 1;
	// Maximum number for (lowest) priority on title extraction heuristics 
	private static final byte MAX_TITLE_HEURISTIC_PRIORITY_LEVEL = 10;
	// Pattern for extracting HTML title tags (h1, h2, h3...)
	private static Pattern HTML_TITLE_PATTERN = Pattern.compile("h(\\d+)");

	// Content of the HTML <title> tag
	private String docTitle;
	// User-defined minimum size for a title
	private int minTitleSize;
	// Current candidate tag for containing the title (tag name and attribute value)
	private String titleTagCandidate;
	private String titleAttCandidate;
	// Content of the portion of text that is a part of doc title
	private String contentIncludedByDocTitle;
	// All patterns for finding title-related tags
	private ArrayList<Pattern> titleTagRelatedPatterns;
	// All patterns for discarding title-related tags
	private ArrayList<Pattern> titleTagRelatedAntiPatterns;

	// Priority of the currently applied heuristic for title extraction
	private byte inTitlePriority;
	// All candidates for being the title of the document
	private LinkedHashMap<String, TextPosition>[] candidateTitles;

	/********************
	 * Page infos
	 ********************/
	private HashMap<Object, PageInfo> hypPageInfos;

	/********************
	 * Evaluation infos
	 ********************/
	// eval mode or not
	private boolean evalMode;
	// actual title manually annotated on WA 
	private String evalTitle;
	// actual DCT manually annotated on WA
	private Calendar evalDCT;
	// actual text representing the DCT in the web page, as manually annotated on WA
	private String evalDCTString;
	// Parsing the manually annotated title 
	private boolean inWATitleTag;	

	private boolean verbose;

	public DCTExtractorRecordFactory(Locale locale, HashMap<String, HashMap<Pattern, String>> rules,
			ArrayList<Pattern> titleTagRelatedPatterns, ArrayList<Pattern> titleTagRelatedAntiPatterns, ArrayList<Pattern> timeTagRelatedPatterns,
			Properties properties) throws DCTExtractorException, IOException, FeatureException {
		this(locale, rules, titleTagRelatedPatterns, titleTagRelatedAntiPatterns, timeTagRelatedPatterns, properties, false, false);
	}

	public DCTExtractorRecordFactory(Locale locale, HashMap<String, HashMap<Pattern, String>> rules,
			ArrayList<Pattern> titleTagRelatedPatterns, ArrayList<Pattern> titleTagRelatedAntiPatterns, ArrayList<Pattern> timeTagRelatedPatterns,
			Properties properties, boolean verbose) throws DCTExtractorException, IOException, FeatureException {
		this(locale, rules, titleTagRelatedPatterns, titleTagRelatedAntiPatterns, timeTagRelatedPatterns, properties, false, verbose);
	}

	public DCTExtractorRecordFactory(Locale locale, HashMap<String, HashMap<Pattern, String>> rules,
			ArrayList<Pattern> titleTagRelatedPatterns, ArrayList<Pattern> titleTagRelatedAntiPatterns, ArrayList<Pattern> timeTagRelatedPatterns,
			Properties properties, boolean evalMode, boolean verbose) throws DCTExtractorException, IOException, FeatureException {
		super(relation, true);

		this.evalMode = evalMode;
		this.verbose = verbose;
		//		this.options = options;

		if (this.verbose) {
			System.out.println("Feature definition");
		}

		/******************************
		 * Resources
		 ******************************/
		// Min title size
		this.minTitleSize = Integer.parseInt(properties.getProperty(MIN_TITLE_SIZE));
		// Regexes
		//		this.regexes = new HashMap<String, ArrayList<Pattern>>();
		//		this.regexes = new HashMap<String, HashMap<Pattern, String>>();
		this.regexes = rules;
		this.lastTagsWordNumber = new LinkedList<Integer>();
		this.dateElementPositions = new ArrayList<Integer>();

		this.timeTagRelatedPatterns = timeTagRelatedPatterns;
		this.titleTagRelatedPatterns = titleTagRelatedPatterns;
		this.titleTagRelatedAntiPatterns = titleTagRelatedAntiPatterns;

		/******************************
		 * Feature definition
		 ******************************/
		this.startTag(TEXT_FEATURE_TYPE);
		// Inclusion between a text and the other (continuous)
		this.addFeature(TEXT, new String(), "TEXT");
		this.endTag(TEXT_FEATURE_TYPE);

		this.startTag(STRUCTURE_FEATURE_TYPE);
		// Opening or closing tag
		this.addFeature(TYPE, new Byte((byte)0), "TYPE", TYPE_TEXT);
		this.addFeature(TIME_RELATED_TAG, new Boolean(false), "TIME_RELATED_TAG", false);
		this.addFeature(WORD_NUMBER_IN_TAG, new Integer(0), "WORD_NUMBER_IN_TAG", 0);
		this.addFeature(WORD_NUMBER_IN_TAG_DISC, new Byte((byte)0), "WORD_NUMBER_IN_TAG_DISC", NUMBER_VERY_LOW);
		this.addFeature(WORD_NUMBER_IN_TAG_SO_FAR, new Integer(0), "WORD_NUMBER_IN_TAG_SO_FAR", 0);
		this.addFeature(WORD_NUMBER_IN_TAG_SO_FAR_DISC, new Byte((byte)0), "WORD_NUMBER_IN_TAG_SO_FAR_DISC", NUMBER_VERY_LOW);
		this.addFeature(MAX_TAG_LENGTH_BEFORE, new Integer(0), "MAX_TAG_LENGTH_BEFORE", 0);
		this.addFeature(MAX_TAG_LENGTH_BEFORE_DISC, new Byte((byte)0), "MAX_TAG_LENGTH_BEFORE_DISC", NUMBER_VERY_LOW);
		this.addFeature(AVG_TAG_LENGTH_BEFORE, new Double(0), "AVG_TAG_LENGTH_BEFORE", 0.0);
		this.addFeature(AVG_TAG_LENGTH_BEFORE_DISC, new Byte((byte)0), "AVG_TAG_LENGTH_BEFORE_DISC", NUMBER_VERY_LOW);
		this.addFeature(DISTANCE_FROM_FIRST_LONG_TAG, new Integer(0), "DISTANCE_FROM_FIRST_LONG_TAG", -3);
		this.addFeature(DISTANCE_FROM_FIRST_LONG_TAG_DISC, new Byte((byte)0), "DISTANCE_FROM_FIRST_LONG_TAG_DISC", BEFORE_HIGH);
		this.addFeature(DISTANCE_FROM_LAST_LONG_TAG, new Integer(0), "DISTANCE_FROM_LAST_LONG_TAG", -3);
		this.addFeature(DISTANCE_FROM_LAST_LONG_TAG_DISC, new Byte((byte)0), "DISTANCE_FROM_LAST_LONG_TAG_DISC", BEFORE_HIGH);
		this.addFeature(FIRST_LONG_TAG_POSITION, new Integer(0), "FIRST_LONG_TAG_POSITION", 0);
		this.addFeature(LAST_LONG_TAG_POSITION, new Integer(0), "LAST_LONG_TAG_POSITION", 0);
		this.endTag(STRUCTURE_FEATURE_TYPE);
		// Vocabulary (dynamic)
		this.startTag(VOCABULARY_FEATURES);
		this.addFeature(VOCABULARY_FEATURE, new String(), "VOCABULARY_FEATURE", "null");
		this.addFeature(DETAILED_VOCABULARY_FEATURE, new String(), "DETAILED_VOCABULARY_FEATURE", "null");
		this.addFeature(DATE_VOCABULARY_FEATURE, new Boolean(false), "DATE_VOCABULARY_FEATURE", false);
		this.addFeature(FULL_DATE_FEATURE, new Boolean(false), "FULL_DATE_FEATURE", false);
		this.addFeature(DATE_CONTENT_ONLY_IN_TAG, new Boolean(false), "DATE_CONTENT_ONLY_IN_TAG", false);
		this.addFeature(DATES_SO_FAR, new Integer(0), "DATES_SO_FAR", 0);
		this.addFeature(FULL_DATES_SO_FAR, new Integer(0), "FULL_DATES_SO_FAR", 0);
		this.addFeature(DATES_IN_ALL, new Integer(0), "DATES_IN_ALL", 0);
		this.addFeature(DATES_IN_ALL_DISC, new Byte((byte)0), "DATES_IN_ALL_DISC", ZERO);
		this.addFeature(NUMBER_OF_DATE_ELEMENTS_AROUND, new Integer(0), "NUMBER_OF_DATE_ELEMENTS_AROUND", 0);
		this.addFeature(NUMBER_OF_DATE_ELEMENTS_AROUND_DISC, new Byte((byte)0), "NUMBER_OF_DATE_ELEMENTS_AROUND_DISC", NUMBER_VERY_LOW);
		this.addFeature(DISTANCE_FROM_TRIGGER, new Integer(0), "DISTANCE_FROM_TRIGGER", -1);
		this.addFeature(DISTANCE_FROM_TRIGGER_DISC, new Byte((byte)0), "DISTANCE_FROM_TRIGGER_DISC", NUMBER_HIGH);
		this.addFeature(DISTANCE_FROM_ANTI_TRIGGER, new Integer(0), "DISTANCE_FROM_ANTI_TRIGGER", -1);
		this.addFeature(DISTANCE_FROM_ANTI_TRIGGER_DISC, new Byte((byte)0), "DISTANCE_FROM_ANTI_TRIGGER_DISC", NUMBER_HIGH);
		this.endTag(VOCABULARY_FEATURES);

		this.startTag(STRUCTURE_CONTEXT_FEATURES);
		this.addFeature(DISTANCE_FROM_TITLE, new Integer(0), "DISTANCE_FROM_TITLE", Integer.MIN_VALUE);
		this.addFeature(DISTANCE_FROM_TITLE_DISC, new Byte((byte)0), "DISTANCE_FROM_TITLE_DISC", BEFORE_HIGH);
		this.endTag(STRUCTURE_CONTEXT_FEATURES);

		this.startTag(POSITION_FEATURES);
		this.addFeature(POSITION, new Integer(0), "POSITION", 0);
		this.addFeature(POSITION_RATE, new Double(0), "POSITION_RATE", 0);
		this.addFeature(POSITION_DISC, new Byte((byte)0), "POSITION_DISC", POSITION_Q1);
		this.endTag(POSITION_FEATURES);

		/******************************
		 * Eval mode initialisation
		 ******************************/
		if (this.evalMode) {
			// Prepare CRF templates
			this.setTemplates();
			WA_SUBTYPE_PATTERN = Pattern.compile("value:(\\d\\d\\d\\d-\\d\\d-\\d\\d);");
			WA_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
		}
	}


	private void setTemplates() {
		int index = 0;

		FeatureTemplate vocWindowtemplate = new FeatureTemplate(this, "" + (++index), FeatureTemplate.UNIGRAM, "3-voc window");
		vocWindowtemplate.addFeature(VOCABULARY_FEATURE, -2);
		vocWindowtemplate.addFeature(VOCABULARY_FEATURE, -1);
		vocWindowtemplate.addFeature(VOCABULARY_FEATURE, 0);
		//		vocWindowtemplate.addFeature(DATE_VOCABULARY_FEATURE, 0);
		vocWindowtemplate.addFeature(VOCABULARY_FEATURE, 1);
		this.addFeatureTemplate(vocWindowtemplate);

		FeatureTemplate vocDateWindowtemplate2 = new FeatureTemplate(this, "" + (++index), FeatureTemplate.UNIGRAM_BIGRAM, "3-voc window");
		vocDateWindowtemplate2.addFeature(VOCABULARY_FEATURE, -1);
		vocDateWindowtemplate2.addFeature(VOCABULARY_FEATURE, -2);
		vocDateWindowtemplate2.addFeature(FULL_DATE_FEATURE, 0);
		this.addFeatureTemplate(vocDateWindowtemplate2);

		FeatureTemplate  wordNumberTemplate = new FeatureTemplate(this, "" + (++index), FeatureTemplate.UNIGRAM_BIGRAM, "3-voc window");
		wordNumberTemplate.addFeature(WORD_NUMBER_IN_TAG_DISC, 0);
		wordNumberTemplate.addFeature(VOCABULARY_FEATURE, 0);
		wordNumberTemplate.addFeature(VOCABULARY_FEATURE, -1);
		this.addFeatureTemplate(wordNumberTemplate);

		FeatureTemplate  positionTemplate = new FeatureTemplate(this, "" + (++index), FeatureTemplate.UNIGRAM_BIGRAM, "3-voc window");
		positionTemplate.addFeature(POSITION_DISC, 0);
		positionTemplate.addFeature(VOCABULARY_FEATURE, 0);
		positionTemplate.addFeature(VOCABULARY_FEATURE, -1);
		//		positionTemplate.addFeature(VOCABULARY_FEATURE, -2);
		this.addFeatureTemplate(positionTemplate);

		FeatureTemplate  datesAroundTemplate = new FeatureTemplate(this, "" + (++index), FeatureTemplate.UNIGRAM, "3-voc window");
		datesAroundTemplate.addFeature(NUMBER_OF_DATE_ELEMENTS_AROUND_DISC, 0);
		datesAroundTemplate.addFeature(VOCABULARY_FEATURE, 0);
		datesAroundTemplate.addFeature(VOCABULARY_FEATURE, -1);
		datesAroundTemplate.addFeature(VOCABULARY_FEATURE, -2);
		this.addFeatureTemplate(datesAroundTemplate);

		FeatureTemplate  distanceFromFirstLongTagTemplate = new FeatureTemplate(this, "" + (++index), FeatureTemplate.UNIGRAM_BIGRAM, "Distance from first long tag");
		distanceFromFirstLongTagTemplate.addFeature(DISTANCE_FROM_FIRST_LONG_TAG_DISC, 0);
		distanceFromFirstLongTagTemplate.addFeature(VOCABULARY_FEATURE, 0);
		distanceFromFirstLongTagTemplate.addFeature(VOCABULARY_FEATURE, -1);
		this.addFeatureTemplate(distanceFromFirstLongTagTemplate);


		FeatureTemplate dateSoFarContent = new FeatureTemplate(this, "" + (++index), FeatureTemplate.UNIGRAM_BIGRAM, "position");
		dateSoFarContent.addFeature(DATES_SO_FAR, 0);
		dateSoFarContent.addFeature(VOCABULARY_FEATURE, 0);
		dateSoFarContent.addFeature(VOCABULARY_FEATURE, -1);
		this.addFeatureTemplate(dateSoFarContent);


		FeatureTemplate fullDateSoFarContent = new FeatureTemplate(this, "" + (++index), FeatureTemplate.UNIGRAM_BIGRAM, "position");
		fullDateSoFarContent.addFeature(FULL_DATES_SO_FAR, 0);
		fullDateSoFarContent.addFeature(VOCABULARY_FEATURE, 0);
		fullDateSoFarContent.addFeature(VOCABULARY_FEATURE, -1);
		this.addFeatureTemplate(fullDateSoFarContent);


		FeatureTemplate dateContent = new FeatureTemplate(this, "" + (++index), FeatureTemplate.UNIGRAM_BIGRAM, "position");
		dateContent.addFeature(DATES_SO_FAR, 0);
		dateContent.addFeature(POSITION_DISC, 0);
		this.addFeatureTemplate(dateContent);

		FeatureTemplate dateInAllContent = new FeatureTemplate(this, "" + (++index), FeatureTemplate.UNIGRAM_BIGRAM, "position");
		dateInAllContent.addFeature(DATES_IN_ALL_DISC, 0);
		dateInAllContent.addFeature(FULL_DATE_FEATURE, 0);
		this.addFeatureTemplate(dateInAllContent);

		FeatureTemplate dateInAllContent2 = new FeatureTemplate(this, "" + (++index), FeatureTemplate.UNIGRAM_BIGRAM, "position");
		dateInAllContent2.addFeature(DATES_IN_ALL_DISC, 0);
		dateInAllContent2.addFeature(VOCABULARY_FEATURE, -1);
		dateInAllContent2.addFeature(VOCABULARY_FEATURE, 0);
		this.addFeatureTemplate(dateInAllContent2);


		FeatureTemplate distanceFromTrigger = new FeatureTemplate(this, "" + (++index), FeatureTemplate.UNIGRAM, "distance from title");
		distanceFromTrigger.addFeature(DISTANCE_FROM_TRIGGER_DISC, 0);
		distanceFromTrigger.addFeature(VOCABULARY_FEATURE, 0);
		distanceFromTrigger.addFeature(VOCABULARY_FEATURE, -1);
		distanceFromTrigger.addFeature(DATES_SO_FAR, 0);
		this.addFeatureTemplate(distanceFromTrigger);		

		FeatureTemplate distanceFromAntiTrigger = new FeatureTemplate(this, "" + (++index), FeatureTemplate.UNIGRAM, "distance from title");
		distanceFromAntiTrigger.addFeature(DISTANCE_FROM_ANTI_TRIGGER_DISC, 0);
		distanceFromAntiTrigger.addFeature(VOCABULARY_FEATURE, 0);
		distanceFromAntiTrigger.addFeature(VOCABULARY_FEATURE, -1);
		distanceFromAntiTrigger.addFeature(DATES_SO_FAR, 0);
		this.addFeatureTemplate(distanceFromAntiTrigger);		


		//
		FeatureTemplate distanceFromTitle = new FeatureTemplate(this, "" + (++index), FeatureTemplate.UNIGRAM, "distance from title");
		distanceFromTitle.addFeature(DISTANCE_FROM_TITLE_DISC, 0);
		distanceFromTitle.addFeature(VOCABULARY_FEATURE, 0);
		distanceFromTitle.addFeature(VOCABULARY_FEATURE, -1);
		this.addFeatureTemplate(distanceFromTitle);		

		FeatureTemplate distanceFromTitle2 = new FeatureTemplate(this, "" + (++index), FeatureTemplate.UNIGRAM, "distance from title");
		distanceFromTitle2.addFeature(DISTANCE_FROM_TITLE_DISC, 0);
		distanceFromTitle2.addFeature(FULL_DATE_FEATURE, 0);
		this.addFeatureTemplate(distanceFromTitle2);		


		FeatureTemplate biGramTemplate = new FeatureTemplate(this, "" + (++index), FeatureTemplate.UNIGRAM_BIGRAM, "output token bigram");
		this.addFeatureTemplate(biGramTemplate);
	}


	private boolean isTagTimeRelated(String tagName, HashMap<String, String> attributes) {
		// Regexes
		for (Pattern regex : this.timeTagRelatedPatterns) {
			if (regex.matcher(tagName).matches()) {
				return true;
			}
			if (attributes != null) {
				for (String attName : attributes.values()) {
					if (regex.matcher(attName).matches()) {
						return true;
					}				
				}
			}
		}
		return false;
	}

	private String isTagTitle(String tagName, HashMap<String, String> attributes) {
		// Regexes
		String value;
		for (Pattern regex : this.titleTagRelatedAntiPatterns) {
			if (attributes != null) {
				// id
				value = attributes.get("id");
				if (value != null && regex.matcher(value).matches()) {
					return null;
				}				
				// class
				value = attributes.get("class");
				if (value != null && regex.matcher(value).matches()) {
					return null;
				}				
			}
		}
		for (Pattern regex : this.titleTagRelatedPatterns) {
			if (attributes != null) {
				// id
				value = attributes.get("id");
				if (value != null && regex.matcher(value).matches()) {
					return value;
				}				
				// class
				value = attributes.get("class");
				if (value != null && regex.matcher(value).matches()) {
					return value;
				}				
			}
		}
		return null;
	}


	private void setCharset(HashMap<String, String> attributes){
		String contentAttributeValue = attributes.get("content");
		if (contentAttributeValue != null) {
			Matcher matcher = charsetPattern.matcher(contentAttributeValue);
			if (matcher.find()) {
				try {
					this.charset = Charset.forName(matcher.group(1));
					if (this.verbose) {
						System.out.println("Charset detected: " + this.charset.displayName());
					}
				} catch (Exception e) {
					if (this.verbose) {
						System.out.println("WARN: Charset detected: " + matcher.group(1) + " unknown (keep default charset " + DEFAULT_CHARSET.displayName());
					}
					this.charset = DEFAULT_CHARSET;
				}
			}
		}
	}


	private Record getRecordFromEntry(String entry, String text, byte type, HashMap<String, String> attributes) throws FeatureException, DCTExtractorException {
		String classValue = CRFRecordFactory.CLASS_OUT;
		//		boolean openingTag = false;
		//		boolean closingTag = false;
		boolean timeRelated = false;

		//		System.out.println(entry);
		//		System.out.println();

		String vocType = null;
		byte priority;
		String tagCandidate;
		int distance;
		switch (type) {
		case TYPE_TEXT:
			//			entry = entry.trim().replaceAll("\n", " ");
			if (this.inSkipElem || entry.length() == 0) {
				return null;
			}
			if (this.inWADateTag) {
				// For evaluation only
				if (this.evalMode) {
					this.evalDCTString += text;
				}
			}
			// For evaluation only (inWATitleTag is never true otherwise)
			else if (this.inWATitleTag) {
				this.evalTitle += text;
			}
			// Regexes
			for (Entry<String, HashMap<Pattern, String>> vocEntry : this.regexes.entrySet()) {
				for (Pattern regex : vocEntry.getValue().keySet()) {
					if (regex.matcher(entry).matches()) {
						//						System.out.println("entry : " + entry + " - pattern : "  + regex.toString());
						vocType = vocEntry.getKey();
						break;
					}
				}
			}
			if (vocType == null) {
				vocType = "TEXT";
			}
			else if (vocType.equals(ANTI_TRIGGER)) {
				this.lastAntiTriggerPosition = this.wordNumber;
			}
			else if (vocType.endsWith(TRIGGER)) {
				this.lastTriggerPosition = this.wordNumber;
			}
			//
			//			else if (vocType.equals(TRIGGER)) {
			//				this.lastTriggerPosition = this.wordNumber;
			//			}
			//			else if (vocType.equals(INSIDE_TRIGGER)) {
			//				return null;
			//			}
			this.runningText += text;
			this.wordNumber++;
			this.wordNumberInTag++;
			break;
		case TYPE_OPENING_TAG:
			//			if (entry.equals("h5")) {
			//				System.out.println();
			//			}
			if (entry.equals(WA_START_TAG)) {
				// For evaluation only
				if (this.evalMode) {
					if (attributes.get(WA_TYPE_ATTR_NAME).startsWith(DATE_ELEM)) {
						this.inWADateTag = true;
						if (attributes.get(WA_TYPE_ATTR_NAME).equals(DATE_ELEM)) {
							String dateValue = attributes.get(WA_SUBTYPES_ATTR_NAME);
							try {
								Matcher matcher = WA_SUBTYPE_PATTERN.matcher(dateValue);
								if (matcher.matches()) {
									this.evalDCT = new GregorianCalendar();
									this.evalDCT.setTime(WA_DATE_FORMAT.parse(matcher.group(1)));	
								} else {
									throw new DCTExtractorException(dateValue + " is not recognized");
								}							
							} catch (ParseException e) {
								throw new DCTExtractorException(e.getMessage());
							}
						}
					}
					else if (attributes.get(WA_TYPE_ATTR_NAME).equals(WA_TITLE_ATTR_VALUE)) {						
						this.inWATitleTag = true;
						this.evalTitle = "";;
					}					
				}
				// For training/testing
				else {
					if (attributes.get(WA_TYPE_ATTR_NAME).startsWith(DATE_ELEM)) {
						this.inWADateTag = true;
					}
				}
				return null;
			} else if (entry.equals(WA_END_TAG)) {
				this.inWADateTag = false;
				elemNumberInWATag = 0;
				// For evaluation only
				this.inWATitleTag = false;						
				return null;
				//			} else if (entry.equalsIgnoreCase(TITLE_TAG)) {
				//				this.inTitle = true;
			} else if (TAGS_TO_SKIP.contains(entry.toLowerCase())) {
				return null;
			} else if (ELEMS_TO_SKIP.contains(entry.toLowerCase())) {
				this.inSkipElem = true;
				return null;
			} else if (this.inSkipElem) {
				return null;
				//			} else if (entry.toLowerCase().equals(TABLE_TAG)) {
				//				this.inTable++;
			} else if (this.runningText.length() >= this.minTitleSize && this.docTitle.contains(this.runningText)) {
				if (this.contentIncludedByDocTitle == null || this.contentIncludedByDocTitle.length() < this.runningText.length()) {
					this.contentIncludedByDocTitle = this.runningText;
					if (this.candidateTitles[INCLUDED_IN_DOC_TITLE_PRIORITY_LEVEL] == null) {
						this.candidateTitles[INCLUDED_IN_DOC_TITLE_PRIORITY_LEVEL] = new LinkedHashMap<String, TextPosition>();
					} else {
						this.candidateTitles[INCLUDED_IN_DOC_TITLE_PRIORITY_LEVEL].remove("-");
					}
					this.candidateTitles[INCLUDED_IN_DOC_TITLE_PRIORITY_LEVEL].put("-", new TextPosition(this.runningText, this.wordNumber));
				} 			
			}

			if (timeRelated = this.isTagTimeRelated(entry, attributes)) {

			} else {
				Matcher matcher = HTML_TITLE_PATTERN.matcher(entry);
				if (matcher.matches()) {
					priority = Byte.parseByte(matcher.group(1));
					if (priority == 1) {
						priority = 0;
					}
					if (priority > 4) {
						priority = MAX_TITLE_HEURISTIC_PRIORITY_LEVEL;
						tagCandidate = null;
					} else {
						tagCandidate = entry;
					}
					if (priority < this.inTitlePriority) {
						this.inTitlePriority = priority;
						this.titleTagCandidate = tagCandidate;
					}
				}
				else if (((this.titleAttCandidate = this.isTagTitle(entry, attributes)) != null)) {
					if (this.inTitlePriority == MAX_TITLE_HEURISTIC_PRIORITY_LEVEL) {
						this.inTitlePriority = MAX_TITLE_HEURISTIC_PRIORITY_LEVEL - 1;
						//					System.out.println("TITLE " + entry + " " + this.titleAttCandidate);
						this.titleTagCandidate = entry;					
					}
				}
				// Encoding declaration
				else if (entry.equalsIgnoreCase("meta")) {
					setCharset(attributes);
				}
			}
			entry = "<" + entry + ">";
			this.runningText = "";
			this.noDateContentInTag = 0;
			this.dateContentInTag = 0;
			this.consecutiveDateContentInTag = 0;
			if (this.wordNumberInTag > 0) {
				this.lastTagsWordNumber.push(this.wordNumberInTag);
			}
			break;
		case TYPE_CLOSING_TAG:
			boolean elemContainsTitle = false;
			if (entry.equals(WA_START_TAG)) {
				return null;
			} else if (entry.equals(WA_END_TAG)) {
				return null;		 
			} else if (entry.equalsIgnoreCase(TITLE_TAG)) {
				this.docTitle += this.runningText;
				this.runningText = "";
			} else if (TAGS_TO_SKIP.contains(entry.toLowerCase())) {
				return null;
			} else if (ELEMS_TO_SKIP.contains(entry.toLowerCase())) {
				this.inSkipElem = false;
				return null;
			} else if (this.inSkipElem) {
				return null;
			} else if (this.runningText.length() >= this.minTitleSize && this.docTitle.contains(this.runningText)) {
				elemContainsTitle = true;
				if (this.contentIncludedByDocTitle == null || this.contentIncludedByDocTitle.length() < this.runningText.length()) {
					this.contentIncludedByDocTitle = this.runningText;
					if (this.candidateTitles[INCLUDED_IN_DOC_TITLE_PRIORITY_LEVEL] == null) {
						this.candidateTitles[INCLUDED_IN_DOC_TITLE_PRIORITY_LEVEL] = new LinkedHashMap<String, TextPosition>();
					} else {
						this.candidateTitles[INCLUDED_IN_DOC_TITLE_PRIORITY_LEVEL].remove("-");
					}
					this.candidateTitles[INCLUDED_IN_DOC_TITLE_PRIORITY_LEVEL].put("-", new TextPosition(this.runningText, this.wordNumber));
				} 
			} // Encoding declaration
			else if (entry.equalsIgnoreCase("meta")) {
				setCharset(attributes);
			}

			if (this.titleTagCandidate != null && entry.equals(this.titleTagCandidate) && this.noDateContentInTag > MAX_NO_DATE_CONTENT_IN_DATE) {
				if (!elemContainsTitle && this.inTitlePriority < MAX_TITLE_HEURISTIC_PRIORITY_LEVEL - 1) {
					this.inTitlePriority++;
				}				
				if (this.candidateTitles[this.inTitlePriority] != null && this.candidateTitles[this.inTitlePriority].containsKey(titleAttCandidate)) {
					this.candidateTitles[this.inTitlePriority].put(titleAttCandidate, new TextPosition("", -1));
				} else {
					if (this.candidateTitles[this.inTitlePriority] == null) {
						this.candidateTitles[this.inTitlePriority] = new LinkedHashMap<String, TextPosition>();
					}
					this.candidateTitles[this.inTitlePriority].put(titleAttCandidate, new TextPosition(this.runningText, this.wordNumber));
				}
				this.inTitlePriority = MAX_TITLE_HEURISTIC_PRIORITY_LEVEL; 
				this.titleTagCandidate = null;
			}
			entry = "</" + entry + ">";
			if (this.wordNumberInTag > 0) {
				this.lastTagsWordNumber.push(this.wordNumberInTag);
				if (this.wordNumberInTag > LONG_TAG_THRESHOLD) {
					if (this.firstLongTagPosition == 0) {
						this.firstLongTagPosition = this.wordNumber - this.wordNumberInTag + 1;
					} else {
						this.lastLongTagPosition = this.wordNumber + 1;
					}
				}
			}
			break;
		default:
			throw new RuntimeException("Unreachable code (theoretically...)");
		}


		if (this.inWADateTag) {
			if (this.elemNumberInWATag == 0) {
				classValue = CRFRecordFactory.CLASS_BEGIN;
			} else {
				classValue = CRFRecordFactory.CLASS_INSIDE;
			}
			this.elemNumberInWATag++;
		}

		Record record = this.getRecord(entry);
		record.add(TEXT, entry);
		record.add(TYPE, type);
		if (type == TYPE_TEXT) {
			if (vocType != null) {
				if (vocType.startsWith(DATE_ELEM)) {
					record.add(VOCABULARY_FEATURE, "date");
					record.add(DETAILED_VOCABULARY_FEATURE, vocType);
					record.add(DATE_VOCABULARY_FEATURE, true);
					if (vocType.length() == DATE_ELEM.length()) {
						record.add(FULL_DATE_FEATURE, true);
						this.consecutiveDateContentInTag++;
						this.dateContentInTag++;
						this.fullDateContentNumber++;
					}
					this.dateContentInTag++;
					this.consecutiveDateContentInTag++;
					if (this.consecutiveDateContentInTag > this.bestConsecutiveDateContentInTag) {
						this.bestConsecutiveDateContentInTag = this.consecutiveDateContentInTag;
					}
					this.dateElementPositions.add(this.wordNumber);
				} else {
					record.add(VOCABULARY_FEATURE, vocType);
					record.add(DETAILED_VOCABULARY_FEATURE, vocType);
					this.noDateContentInTag++;
					this.consecutiveDateContentInTag = 0;
				}
			} else {
				this.noDateContentInTag++;
			}
			record.add(WORD_NUMBER_IN_TAG_SO_FAR, this.wordNumberInTag);
			record.add(WORD_NUMBER_IN_TAG_SO_FAR_DISC, getDiscreteWordNumberInTag(this.wordNumberInTag));

		} else if (type == TYPE_CLOSING_TAG) {
			if (this.bestConsecutiveDateContentInTag >= 2) {
				this.dateContentNumber++;
			}
			this.bestConsecutiveDateContentInTag = 0;
			if (this.dateContentInTag > this.noDateContentInTag + 1) {
				record.add(DATE_CONTENT_ONLY_IN_TAG, true);
				//				System.out.println("\n\n\n"+ record.get(TEXT) + "\n");
				//				this.dateContentNumber++;
				this.noDateContentInTag = 0;
				this.dateContentInTag = 0;
			}
		}
		if (timeRelated) {
			record.add(TIME_RELATED_TAG, true);
		}
		if (this.lastTriggerPosition == -1) {
			distance = -1;
		} else {
			distance = this.wordNumber - this.lastTriggerPosition;
		}
		record.add(DISTANCE_FROM_TRIGGER, distance);
		record.add(DISTANCE_FROM_TRIGGER_DISC, getDiscreteDistance(distance));
		if (this.lastAntiTriggerPosition == -1) {
			distance = -1;
		} else {
			distance = this.wordNumber - this.lastAntiTriggerPosition;
		}
		record.add(DISTANCE_FROM_ANTI_TRIGGER, distance);
		record.add(DISTANCE_FROM_ANTI_TRIGGER_DISC, getDiscreteDistance(distance));

		record.add(DATES_SO_FAR, Math.min(THREE_OR_MORE, this.dateContentNumber));
		record.add(FULL_DATES_SO_FAR, Math.min(THREE_OR_MORE, this.fullDateContentNumber));
		record.add(POSITION, this.wordNumber);
		record.setClassValue(classValue);
		return record;
	}


	/**
	 * Extract CRF features from all HTML files in a specified directory.
	 * @param dir the directory to parse
	 * @return a list of CRF records
	 * @throws Exception
	 */
	public RecordList getFeaturesFromDirectory(File dir, boolean train) throws Exception {
		File[] files = dir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				return file.getName().endsWith(".html");
			}
		});
		return this.getFeatures(files, train);
	}

	public RecordList getFeatures(File[] files, boolean train) throws FileNotFoundException, FeatureException, DCTExtractorException, IOException {
		// Result
		RecordList records = new RecordList(this);
		// Page info can be partially filled at parsing step
		// (used for final labeling only)
		this.hypPageInfos = new HashMap<Object, PageInfo>();
		// Number of parsed files
		int fileNumber = 1;
		for (File file : files) {

			if (this.verbose) {
				System.out.println("Parse file " + (fileNumber++) + " / " + files.length + ": " + file.getAbsolutePath());
			}
			PageInfo pageInfo = this.getPageInfos(new FileInputStream(file), file.getAbsolutePath(), train);
			if (this.evalMode) {
				this.hypPageInfos.put(file, pageInfo);
			}
			records.addAll(pageInfo.getRecords());
		}

		return records;
	}


	@SuppressWarnings("unchecked")
	public PageInfo getPageInfos(InputStream inputStream, String fileName, boolean train) throws FeatureException, DCTExtractorException, IOException {
		// Result
		PageInfo pageInfo;
		RecordList records = new RecordList(this);

		/********************
		 * Init infos
		 ********************/
		// parsed character (integer and char values)
		int readCharInt;
		//        char readChar;
		// Current entry
		//        String entry = "";
		byte[] entry = new byte[MAX_ENTRY_LENGTH];
		int entryLength = 0;
		String entryStr;
		// Current HTML tag name
		String tagName = null;
		// Current HTML attribute name
		String attName = null; 
		HashMap<String, String> tagAttributes = new HashMap<String, String>();
		// in a HTML tag
		boolean inTag = false;
		// in a closing HTML tag
		boolean inClosingTag = false;
		// in an attribute (= 1 after meeting the '=', = 2 after meeting the '"')
		byte inAttValue = 0;
		// in a HTML comment
		boolean inComment = false;
		// in a HTML entity
		boolean inEntity = false;
		// the current character comes from
		// an HTML entity resolution
		boolean fromEntity = false;
		// progress in "-->" characters from 
		// ending a comment
		int outOfCommentCharacters = 0;
		// Characters that may be kept or not in the output
		byte[] keptInCase = new byte[MAX_KEPT_IN_CASE_LENGH];
		int keptInCaseLength = 0;
		// Current HTML entity content
		String entityContent = "";
		boolean dateContentOnly;
		boolean tagClosed;

		// Add a record for the new file (just to materialize the 
		// separation in the CRF file)
		if (!train) {
			Record fileRecord = this.getRecord(fileName);
			fileRecord.add(TEXT, DCTFINDER_FILE_SEPARATOR + " " + fileName);
			fileRecord.setClassValue(CLASS_OUT);
			records.add(fileRecord);
		}

		this.docTitle = "";
		this.contentIncludedByDocTitle = null;
		this.titleTagCandidate = null;
		this.inWADateTag = false;
		this.inSkipElem = false;
		this.elemNumberInWATag = 0;
		this.wordNumber = 0;
		this.runningText = "";
		this.dateContentNumber = 0;
		this.fullDateContentNumber = 0;
		this.noDateContentInTag = 0;
		this.dateContentInTag = 0;
		this.consecutiveDateContentInTag = 0;
		this.bestConsecutiveDateContentInTag = 0;
		this.inTitlePriority = MAX_TITLE_HEURISTIC_PRIORITY_LEVEL;
		this.candidateTitles = new LinkedHashMap[MAX_TITLE_HEURISTIC_PRIORITY_LEVEL];
		this.lastTagsWordNumber.clear();
		this.firstLongTagPosition = 0;
		this.lastLongTagPosition = 0;
		this.dateElementPositions.clear();
		this.lastTriggerPosition = -1;
		this.lastAntiTriggerPosition = -1;
		this.charset = DEFAULT_CHARSET;

		// Evaluation infos
		if (this.evalMode) {
			this.evalTitle = "";
			this.evalDCT = null;
			this.evalDCTString = "";
		}
		try {
			int bytesRead = 0;
			while ((readCharInt = inputStream.read()) != -1) {
				// Protection from huge files
				bytesRead++;
				if (bytesRead > MAX_READ_BYTES) break;

				try {
					Record record = null;
					//        			System.out.print(readCharInt);
					// Last character of opening comment
					if (readCharInt == CHAR_DASH && inTag && entryLength == 1 && entry[0] == CHAR_EXCLAMATION_MARK && keptInCaseLength == 1 && keptInCase[0] == CHAR_DASH) {
						inComment = true;
						inTag = false;
						entryLength = 0;
						keptInCaseLength = 0;
						continue;
					}
					// Looking for closing comment
					if (inComment) {
						// First character '-'
						if (readCharInt == CHAR_DASH) {
							if (outOfCommentCharacters <= 1) {
								outOfCommentCharacters++;
							} 
						} 
						else if (readCharInt == CHAR_GT && outOfCommentCharacters == 2) {
							outOfCommentCharacters = 0;
							inComment = false;
						}
						else {
							outOfCommentCharacters = 0;
						}
						continue;
					}
					if (inEntity) {
						// End of entity
						//	            		System.out.println(" -> " + keptInCase);
						if (readCharInt == CHAR_SEMI_COMMA) {
							readCharInt = (int) StringEscapeUtils.unescapeHtml4(entityContent + ";").charAt(0);
							inEntity = false;
							fromEntity = true;
						}
						// "=" -> this is not an entity but a URL
						else if (readCharInt == CHAR_EQUALS) {
							for (byte b : entityContent.getBytes(this.charset)) {
								entry[entryLength++] = b;
							}
							entry[entryLength++] = (byte)readCharInt;
							inEntity = false;
						}
						// "&" -> this is not an entity but some javascript
						else if (readCharInt == CHAR_AMP) {
							for (byte b : entityContent.getBytes(this.charset)) {
								entry[entryLength++] = b;
							}
							entry[entryLength++] = (byte)readCharInt;
							inEntity = false;
						}
						// space or '<' -> this is not an entity but a single "&" (illformed HTML)
						else if (Character.isWhitespace(readCharInt) || readCharInt == CHAR_NBSP || readCharInt == CHAR_LT) {
							for (byte b : entityContent.getBytes(this.charset)) {
								entry[entryLength++] = b;
							}
							//        					entry[entryLength++] = (byte)'&';
							//        					entry[entryLength++] = (byte)readCharInt;
							inEntity = false;
						}
						// Inside entity
						else {
							//	            			if (readChar == 'n') {
							//	            				System.out.println("ICI");
							//	            			}
							entityContent += (char)readCharInt;
							continue;
						}
					}
					// carriage return or tabulation
					// space
					if ((Character.isWhitespace(readCharInt) || readCharInt == CHAR_NBSP) && (entryLength == 0 || entry[entryLength-1] != (byte)-61)) {
						//            			if (entry.equals(WA_START_TAG)) {
						//            				System.out.println("\n" + inAttValue);
						//            				System.out.println();
						//            			}

						if (inTag) {
							if (tagName == null) {
								tagName = new String(entry, 0, entryLength, this.charset);
								//        					tagName = entry;
							}
						} else {
							// Here, entry is a new record
							if (entryLength > 0) {
								entryStr = new String(entry, 0, entryLength, this.charset);
								record = getRecordFromEntry(entryStr, entryStr + " ", TYPE_TEXT, null);
							}
						}
						entryLength = 0;
					}
					else {
						// entity
						if (readCharInt == CHAR_AMP) {
							// if '&' is the entity starting character
							// (and not the result of analysing entity "&amp;")
							if (!fromEntity) {
								inEntity = true;
								entityContent = "&";
								continue;
							}
						}

						entityContent = "";
						if (keptInCaseLength > 0) {
							for (int i = 0 ; i < keptInCaseLength ; i++) {
								entry[entryLength++] = keptInCase[i];
							}
							//        				entry += keptInCase;
						}

						// opening tag '<'
						if (readCharInt == CHAR_LT && !fromEntity) {
							// Here, entry is a new record
							if (entryLength > 0) {
								entryStr = new String(entry, 0, entryLength, this.charset);
								record = getRecordFromEntry(entryStr, entryStr, TYPE_TEXT, null);
								entryLength = 0;
							}
							// if already in a tag : illformed HTML, skip the tag
							else if (inTag && !this.inSkipElem) {
								inTag = false;
								attName = null;
								continue;
							}
							inTag = true;
							tagAttributes = new HashMap<String, String>();
							attName = null;
						}
						// closing '>' of an opening tag
						else if (readCharInt == CHAR_GT && !fromEntity && !inClosingTag) {
							if (tagAttributes.isEmpty() && tagName == null) {
								tagName = new String(entry, 0, entryLength, this.charset);
							}
							//	            			entry += readChar;
							// Here, entry is a new record
							if (tagName != null) {
								record = getRecordFromEntry(tagName, null, TYPE_OPENING_TAG, tagAttributes);
							}
							inAttValue = 0;
							entryLength = 0;
							inTag = false;
							tagName = null;
							tagAttributes = new HashMap<String, String>();
						}
						// closing '>' of a closing tag
						else if (readCharInt == CHAR_GT && !fromEntity && inClosingTag) {
							// Here, entry is a new record
							// entry == "" is this is an empty tag 
							if (entryLength == 0) {
								entryStr = tagName;
							} else {
								entryStr = new String(entry, 0, entryLength, this.charset);
							}
							record = getRecordFromEntry(entryStr, null, TYPE_CLOSING_TAG, tagAttributes);
							entryLength = 0;
							inTag = false;
							inClosingTag = false;
							tagName = null;
							tagAttributes = new HashMap<String, String>();
						}
						// '=' between attribute name and value
						else if (readCharInt == CHAR_EQUALS && inTag && inAttValue == 0 && !this.inSkipElem) {
							inAttValue = 1;
							attName = new String(entry, 0, entryLength, this.charset);

							entryLength = 0;
						}
						// '/' indicating a closing tag
						else if (readCharInt == CHAR_SLASH && inTag && attName == null && entryLength == 0) {
							inClosingTag = true;
						}
						// closing '"' for attribute value
						else if (readCharInt == CHAR_DOUBLE_QUOTE && inTag && inAttValue == 2 && attName != null && !this.inSkipElem) {
							tagAttributes.put(attName, new String(entry, 0, entryLength, this.charset));
							entryLength = 0;
							inAttValue = 0;
							attName = null;
						}
						// opening '"' for attribute value
						else if (readCharInt == CHAR_DOUBLE_QUOTE && inTag && inAttValue == 1 && attName != null && !this.inSkipElem) {
							inAttValue = 2;
						}
						// characters to skip if followed by a blank
						else if ((readCharInt == CHAR_COLON || readCharInt == CHAR_COMMA || readCharInt == CHAR_DOT || readCharInt == CHAR_PIPE || readCharInt == CHAR_DASH || readCharInt == CHAR_SEMI_COMMA) && !this.inSkipElem) {
							keptInCase[keptInCaseLength++] = (byte)readCharInt;
							continue;
						}
						// characters to skip in any case
						else if (readCharInt == CHAR_OPENING_BRACKET || readCharInt == CHAR_CLOSING_BRACKET || readCharInt == CHAR_OPENING_SQUARE_BRACKET || readCharInt == CHAR_CLOSING_SQUARE_BRACKET) {
							continue;
						}
						// other character
						else {        				
							entry[entryLength++] = (byte)readCharInt;
						}
					}    	
					// Add record if new one
					if (record != null) {
						// 
						//        			System.out.println(record.get(TEXT));
						if ((Boolean)record.get(DATE_CONTENT_ONLY_IN_TAG)) {
							dateContentOnly = true;
						} else {
							dateContentOnly = false;
						}
						if ((Byte)record.get(TYPE) == TYPE_CLOSING_TAG || (Byte)record.get(TYPE) == TYPE_OPENING_TAG) {
							tagClosed = true;
						} else {
							tagClosed = false;
						}
						Record previousRecord;
						int maxLength = 0; int sumLengths = 0; int length;
						int tagNumberIndex; double avg = 0.0;

						int nothingHappened = 0;        				

						for (int i = records.size() - 1 ; i >= 0 && nothingHappened <= 5; i--) {
							previousRecord = records.get(i);
							if (previousRecord instanceof SeparationRecord) {
								break;
							}
							if ((Byte)previousRecord.get(TYPE) == TYPE_TEXT) {
								previousRecord.replace(DATE_CONTENT_ONLY_IN_TAG, dateContentOnly);
								nothingHappened++;
								if (dateContentOnly) {
									nothingHappened = 0;
								}
								if (tagClosed) {
									nothingHappened = 0;
									previousRecord.add(WORD_NUMBER_IN_TAG, this.wordNumberInTag);
									previousRecord.add(WORD_NUMBER_IN_TAG_DISC, getDiscreteWordNumberInTag(this.wordNumberInTag));

									if (avg == 0.0) {
										for (tagNumberIndex = 1 ; tagNumberIndex <= TAG_LENGTH_WINDOW ; tagNumberIndex++) {
											if (tagNumberIndex < this.lastTagsWordNumber.size()) {
												length = this.lastTagsWordNumber.get(tagNumberIndex);
												if (length > maxLength) {
													maxLength = length;
												}
												sumLengths += length;
											} else {
												break;
											}
										} 
										avg = (double)sumLengths/(double)tagNumberIndex;
									}
									if (avg > 0.0) {
										previousRecord.add(MAX_TAG_LENGTH_BEFORE, maxLength);
										previousRecord.add(MAX_TAG_LENGTH_BEFORE_DISC, getDiscreteMaxTagLength(maxLength));
										previousRecord.add(AVG_TAG_LENGTH_BEFORE, avg);
										previousRecord.add(AVG_TAG_LENGTH_BEFORE_DISC, getDiscreteAvgTagLength(avg));
									}
								}
							} else {
								break;
							}
						}
						if (tagClosed) {
							this.wordNumberInTag = 0;
						}
						records.add(record);

						if ((Boolean)record.get(FULL_DATE_FEATURE)) {
							Record cloneRecord = new Record(record);
							if (record.getClassValue().equals(CRFRecordFactory.CLASS_BEGIN)) {
								cloneRecord.setClassValue(CRFRecordFactory.CLASS_INSIDE);
							}
							records.add(cloneRecord);
							records.add(new Record(cloneRecord));
						}
					}
					keptInCaseLength = 0;
					fromEntity = false;
				} catch (ArrayIndexOutOfBoundsException e) {
					if (entryLength == MAX_ENTRY_LENGTH + 1) {
						entryLength = MAX_ENTRY_LENGTH / 10;
					} 
					else if (keptInCaseLength == MAX_KEPT_IN_CASE_LENGH + 1) {
						keptInCaseLength = MAX_KEPT_IN_CASE_LENGH / 10;
					}
				}
			}
			inputStream.close();

			/*****************
			 * Extract page title
			 *****************/
			// Extracted title
			TextPosition candidateTitle = null;
			int indexTitleLevel;

			// For each priority level (from 0 = highest to MAX_TITLE_HEURISTIC_NUMBER = lowest)
			// find an acceptable title and stop as soon as one is found
			for (indexTitleLevel = 0 ; indexTitleLevel < MAX_TITLE_HEURISTIC_PRIORITY_LEVEL ; indexTitleLevel++) {
				if (this.candidateTitles[indexTitleLevel] != null && !this.candidateTitles[indexTitleLevel].isEmpty()) {
					for (Entry<String, TextPosition> candidateEntry : this.candidateTitles[indexTitleLevel].entrySet()) {
						if (candidateEntry.getValue().getText().length() > this.minTitleSize) {
							candidateTitle = candidateEntry.getValue();
							break;
						}
					}
					if (candidateTitle != null) {
						break;
					}
				}
			}
			// If no good title candidate has been found
			// then we take the <title> tag content (if any)
			if (candidateTitle == null) {
				candidateTitle = new TextPosition(this.docTitle, 0);
			}

			// Add separation record
			records.add(new SeparationRecord());

			// Update and filterrecords (= filter out all records that are not of interest,
			// in order to avoid too sparse non-nil data)
			records = this.updateAndFilterRecords(records, 2, candidateTitle, train);

			//	            logger.info("Title: " + candidateTitle + " (" + indexTitleLevel + ")");
			pageInfo = new PageInfo(candidateTitle.getText().trim());
			pageInfo.setRecords(records);
		} catch (IOException e) {
			throw e;
		} 

		// For evaluation only
		// keep trace of the reference values for title and date
		if (this.evalMode) {
			pageInfo.setRefTitle(this.evalTitle.trim());
			pageInfo.setRefDCT(this.evalDCT);
			//        	System.err.println(this.evalDCTString);
			pageInfo.setRefDateString(this.evalDCTString);
		}

		return pageInfo;
	}

	private static Byte getDiscreteAvgTagLength(double length) {
		if (length < 8) {
			return NUMBER_VERY_LOW;
		} else if (length < 20) {
			return NUMBER_LOW;
		} else if (length < 30) {
			return NUMBER_MEDIUM;
		} else {
			return NUMBER_HIGH;
		}		
	}

	private static Byte getDiscreteMaxTagLength(int length) {
		if (length < 10) {
			return NUMBER_VERY_LOW;
		} else if (length < 40) {
			return NUMBER_LOW;
		} else if (length < 80) {
			return NUMBER_MEDIUM;
		} else {
			return NUMBER_HIGH;
		}		
	}


	private static Byte getDiscreteWordNumberInTag(int wordNumber) {
		if (wordNumber < 13) {
			return NUMBER_VERY_LOW;
		} else if (wordNumber < 30) {
			return NUMBER_LOW;
		} else if (wordNumber < 100) {
			return NUMBER_MEDIUM;
		} else {
			return NUMBER_HIGH;
		}
	}

	/**
	 * Get a discrete value from a word position number
	 * @return a discrete value representing this position (TOP, MEDIUM, BOTTOM of the document) 
	 */
	private static Byte getDiscretePosition(double ratio) {
		if (ratio < 0.25) {
			return POSITION_Q1;
		} else if (ratio < 0.5) {
			return POSITION_Q2;
		} else if (ratio < 0.75) {
			return POSITION_Q3;
		} else {
			return POSITION_Q4;
		}
	}

	private static Byte getDiscreteSmallNumber(int number) {
		switch (number) {
		case 0:
			return ZERO;
		case 1:
			return ONE;
		case 2:
			return TWO;
		default:
			return THREE_OR_MORE;
		}
	}

	private static Byte getDiscretePositiveDistance(int distance) {
		if (distance > 60) {
			return AFTER_HIGH;
		}
		else if (distance > 15) {
			return AFTER_MEDIUM;
		}
		else if (distance > 0) {
			return AFTER_LOW;
		}
		//		else if (distance > -15) {
		//			return BEFORE_LOW;
		//		}
		//		else if (distance > -60) {
		//			return BEFORE_MEDIUM;
		//		}
		else {
			return BEFORE_HIGH;
		}
	}

	private static Byte getDiscreteDateElementsAround(int number) {
		if (number > 40) {
			return NUMBER_HIGH;
		} else if (number > 26) {
			return NUMBER_MEDIUM;
		} else if (number > 16) {
			return NUMBER_LOW;
		} else {
			return NUMBER_VERY_LOW;
		}
	}

	private static Byte getDiscreteDistance(int distance) {
		if (distance < 0) {
			return NUMBER_HIGH;
		} else if (distance < 15) {
			return NUMBER_VERY_LOW;
		} else if (distance < 40) {
			return NUMBER_LOW;
		} else if (distance < 80) {
			return NUMBER_MEDIUM;
		} else {
			return NUMBER_HIGH;
		}
	}



	/**
	 * Update and filter records.
	 * Post-parsing to get features that depend on the title
	 * (the title is extracted only at the end of the document parsing)
	 * or some date.
	 * Also, filters out parts of the input that has no chance to 
	 * be a date. 
	 * @param records the records to update and filter
	 * @param windowSize the size of window that we want to keep around date-related elements
	 * @param title the TextPosition title
	 * @param train whether we are at training stage or not
	 * @return the new list of updated and filtered records
	 * @throws FeatureException
	 */
	private RecordList updateAndFilterRecords(RecordList records, int windowSize, TextPosition title, boolean train) throws FeatureException {
		String titleText = title.getText();
		int titleWordNumber = titleText.split(" ").length;
		int titlePosition = title.getPosition();
		int wordPosition;
		int distance;
		// distance wrt the title
		Byte relativeDistance;

		RecordList result = new RecordList(records.getFactory());
		LinkedList<Record> queue = new LinkedList<Record>();
		int windowRemaining = 0;
		int consecutiveDateTriggerNumber = 0;

		int MIN_CONSECUTIVE_DATE_TRIGGER_NUMBER = 3;
		boolean inDCT = false;
		double ratio;
		boolean lastIsSep = false;
		int dateElementsAround;

		for (Record record : records) {
			if (record instanceof SeparationRecord) {
				result.add(record);
				continue;
			}
			if (((String)record.get(TEXT)).startsWith(DCTFINDER_FILE_SEPARATOR)) {
				result.add(record);
				continue;
			}
			if (!train && (Integer)record.get(WORD_NUMBER_IN_TAG) > 20) {
				continue;
			}
			/************************
			 *  Update record 
			 ************************/
			//			if (record.get(TEXT).toString().startsWith("January")) {
			//				System.out.println();
			//			}
			// Update record wrt title information
			wordPosition = (Integer)record.get(POSITION);
			distance = wordPosition - titlePosition;
			if (distance > 60) {
				relativeDistance = AFTER_HIGH;
			}
			else if (distance > 15) {
				relativeDistance = AFTER_MEDIUM;
			}
			else if (distance > 0) {
				relativeDistance = AFTER_LOW;
			}
			else if (distance > -1 * titleWordNumber) {
				relativeDistance = INSIDE;
				//				relativeDistance = BEFORE_LOW;
			} 
			else if (distance > -1 * titleWordNumber - 15) {
				relativeDistance = BEFORE_LOW;
			}
			else if (distance > -1 * titleWordNumber - 40) {
				relativeDistance = BEFORE_MEDIUM;
			}
			else {
				relativeDistance = BEFORE_HIGH;
			}

			record.add(DISTANCE_FROM_TITLE, distance);
			record.add(DISTANCE_FROM_TITLE_DISC, relativeDistance);

			record.add(DATES_IN_ALL, this.dateContentNumber);
			record.add(DATES_IN_ALL_DISC, getDiscreteSmallNumber(this.dateContentNumber));

			// Relative position in article
			ratio = (double)wordPosition/(double)this.wordNumber;
			record.add(POSITION_RATE, ratio);
			record.add(POSITION_DISC, getDiscretePosition(ratio));

			record.add(FIRST_LONG_TAG_POSITION, this.firstLongTagPosition);
			record.add(LAST_LONG_TAG_POSITION, this.lastLongTagPosition);
			distance = this.firstLongTagPosition - wordPosition;
			record.add(DISTANCE_FROM_FIRST_LONG_TAG, distance);
			record.add(DISTANCE_FROM_FIRST_LONG_TAG_DISC, getDiscretePositiveDistance(distance));
			distance = wordPosition - this.lastLongTagPosition;
			record.add(DISTANCE_FROM_LAST_LONG_TAG, distance);
			record.add(DISTANCE_FROM_LAST_LONG_TAG_DISC, getDiscretePositiveDistance(distance));

			//			inDCT = (record.getClassValue().equals(CRFRecordFactory.CLASS_BEGIN) || record.getClassValue().equals(CRFRecordFactory.CLASS_INSIDE));
			inDCT = false;
			boolean isText = (Byte)record.get(TYPE) == TYPE_TEXT;
			dateElementsAround = 0;

			//			if ((Byte)record.get(TYPE) != TYPE_TEXT) {
			//				continue;
			//			}
			if ((Boolean)record.get(DATE_VOCABULARY_FEATURE) || (inDCT && train)) {
				for (Integer dateElementPositionInDoc : this.dateElementPositions) {
					if (Math.abs(wordPosition - dateElementPositionInDoc) < DATE_AROUND_LIMITS) {
						dateElementsAround++;
					}
				}

				if ((Boolean)record.get(VOCABULARY_FEATURE).equals("date") || inDCT || consecutiveDateTriggerNumber >= MIN_CONSECUTIVE_DATE_TRIGGER_NUMBER - 1) {
					while (!queue.isEmpty()) {
						result.add(queue.pollLast());
					}
					result.add(record);
					windowRemaining = windowSize;
					lastIsSep = false;
				}
				else {
					queue.push(record);
					if (!lastIsSep) {
						result.add(new SeparationRecord());
						lastIsSep = true;
					}
				}
				consecutiveDateTriggerNumber++;
			}
			else if (windowRemaining > 0) {
				result.add(record);
				lastIsSep = false;
				if (isText) {
					windowRemaining--;
					consecutiveDateTriggerNumber = 0;
				}
			}
			else {		
				if (!lastIsSep) {
					result.add(new SeparationRecord());
					lastIsSep = true;
				}
				queue.push(record);
				while (queue.size() > windowSize) {
					queue.pollLast();
				}
				consecutiveDateTriggerNumber = 0;
			}
			record.add(NUMBER_OF_DATE_ELEMENTS_AROUND, dateElementsAround);
			record.add(NUMBER_OF_DATE_ELEMENTS_AROUND_DISC, getDiscreteDateElementsAround(dateElementsAround));
		}
		//		return records;
		return result;
	}

	public HashMap<Object, PageInfo> getLabeledPageInfos() {
		return this.hypPageInfos;
	}


	private class TextPosition {
		private String text;
		private int position;

		TextPosition(String text, int position) {
			this.text = text;
			this.position = position;
		}

		/**
		 * @return the text
		 */
		public String getText() {
			return text;
		}
		/**
		 * @return the position
		 */
		public int getPosition() {
			return position;
		}

		@Override
		public String toString() {
			return this.text;
		}
	}	
}
