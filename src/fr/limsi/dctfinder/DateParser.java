package fr.limsi.dctfinder;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for dates expressed in natural language.
 * Makes use of language-specific, user-defined patterns.
 * Pattern file are formated in two columns; the first column 
 * is the regex, the second column express the semantics of each 
 * group inside the regex.
 * e.g.: 
 * <ul>
 *   <li>([12]\d\d\d)[-./]([012]?\d)[-./]([0123]?\d)	YMD <br>
 * where the regex contains 3 groups of parenthesis and the second
 * field informs that these three groups are respectively
 * the year (Y), the month (M) and the day (D).
 *   <li>February	M2 <br>
 * means that February is a month (M) and should be replaced by the
 * value 2 (February).
 * </ul>
 * All files should start with "date-" and end with ".txt"
 * @author xtannier
 *
 */
public class DateParser {
    // Locale
    private Locale locale;
    // Locale-dependent patterns
    private HashMap<Pattern, String> regexes;


    // Maximum length for a String representing a date
    private static final int MAX_DATE_STRING_LENGTH = 20;


    /**
     * Build a date parser dependent on a locale.
     * If documents are in English and if you know if they are from North America
     * or another country, specify locales en_US or en_UK (date formats are different)
     * @param locale 
     * @param rules : <rule group name, <regex, action>>
     * @throws IOException
     */
    public DateParser(Locale locale, HashMap<Pattern, String> regexes) {
        this.locale = locale;

        // Get date-specific patterns from user-defined files
        this.regexes = regexes;
    }

    /**
     * Returns the Parser's Locale
     * @return the Parser's Locale
     */
    public Locale getLocale() {
        return this.locale;
    }

    /**
     * Add a pattern and its description to the set of patterns.
     * The description corresponds to the second column of the 
     * above-described pattern files.
     * @param pattern
     * @param description
     */
    public void addPattern(Pattern pattern, String description) {
        this.regexes.put(pattern, description);
    }

    /**
     * Analyze a token according to patterns.
     * @param token the String token to analyze
     * @param patterns the list of patterns and descriptions, as described
     * above.
     * @return a HashMap mapping {@link java.util.Calendar} fields (Calendar.MONTH,
     * Calendar.DAY_OF_MONTH, etc.) to their extracted Integer value.
     * @throws DCTExtractorException 
     */
    private static HashMap<Integer, Integer> analyzeToken(String token, HashMap<Pattern, String> patterns) throws DCTExtractorException {
        String datePatternDescription = null;
        Matcher matcher = null;
        String group;
        char[] datePatternDescriptionArray;
        HashMap<Integer, Integer> dateChunks = new HashMap<Integer, Integer>();

        if (token == null) {
            return dateChunks;
        }

        // Apply regexes
        for (Entry<Pattern, String> regexEntry : patterns.entrySet()) {
            matcher = regexEntry.getKey().matcher(token);
            if (matcher.matches()) {
                datePatternDescription = regexEntry.getValue();
                break;
            }
        }

        boolean monthFound = false;
        boolean dayFound = false;

        // If a pattern has been found, analyzes the description 
        // and extract the fields
        if (datePatternDescription != null) {
            datePatternDescriptionArray = datePatternDescription.toCharArray();
            // Parse description
            for (int i = 0 ; i < datePatternDescription.length() ; i++) {
                group = matcher.group(i+1);
                if (group == null) {
                    throw new DCTExtractorException("Pattern description " + datePatternDescription + " does not match the pattern.");
                }					
                switch (datePatternDescriptionArray[i]) {
                // Year
                case 'Y':
                    // A year is a numeric value
                    dateChunks.put(Calendar.YEAR, Integer.parseInt(group));
                    break;
                    // Month
                case 'M':
                    monthFound = true;
                    try {
                        // A month can be a numeric value
                        dateChunks.put(Calendar.MONTH, Integer.parseInt(group)-1);
                    } catch (NumberFormatException e) {
                        int month = 0;
                        char character;
                        // A month can be a String value described by Mi where
                        // i is the value of the month field (e.g. M4 for April)
                        while (i+1 < datePatternDescription.length()) {
                            character = datePatternDescriptionArray[++i];
                            if (Character.isDigit(character)) {
                                month = month * 10 + Integer.parseInt("" + character);
                            } else {
                                break;
                            }
                        };
                        if (month != 0) {
                            dateChunks.put(Calendar.MONTH, month-1);
                        } 
                        // Otherwise, a month can finally be a String to found in other
                        // patterns
                        // e.g. 2006-Jan-16 described by YMD
                        else {
                            i--;
                            dateChunks.putAll(analyzeToken(group, patterns));
                        }
                    }
                    break;
                    // Day 
                case 'D':
                    dayFound = true;
                    // A day of the month is a numeric value
                    dateChunks.put(Calendar.DAY_OF_MONTH, Integer.parseInt(group));
                    break;
                    // AM/PM
                case 'H':
                    try {
                        dateChunks.put(Calendar.AM_PM, Integer.parseInt(group));
                    } catch (NumberFormatException e) {
                        int ampm = 0;
                        char character;
                        while (i+1 < datePatternDescription.length()) {
                            character = datePatternDescriptionArray[++i];
                            if (Character.isDigit(character)) {
                                ampm = ampm * 10 + Integer.parseInt("" + character);
                            } else {
                                break;
                            }
                        };
                        dateChunks.put(Calendar.AM_PM, ampm);
                    }
                default:
                    break;
                }
            }
        }

        // If found both month and day,
        // Check that they are valid
        if (monthFound && dayFound) {
            int month = dateChunks.get(Calendar.MONTH);
            int day = dateChunks.get(Calendar.DAY_OF_MONTH);
            if (month > 11) {
                if (day <= 12) {
                    dateChunks.put(Calendar.MONTH, day - 1);
                    dateChunks.put(Calendar.DAY_OF_MONTH, month + 1);
                } else {
                    dateChunks.clear();
                }
            }
            if (day > 31) {
                dateChunks.clear();
            }
        }
        return dateChunks;
    }


    /**
     * Get a {@link java.util.Calendar} date from natural language text separated
     * into an array of tokens, according to patterns and the locale. 
     * @param tokens the array of tokens to parse
     * @param patterns the list of patterns and descriptions, as described
     * above.
     * @param locale
     * @return
     * @throws DCTExtractorException 
     */
    protected static Calendar getDateFromText(String[] tokens, HashMap<Pattern, String> patterns, Locale locale) throws DCTExtractorException {
        return getDateFromText(tokens, patterns, locale, null);
    }


    /**
     * Get a {@link java.util.Calendar} date from natural language text separated
     * into an array of tokens, according to patterns and the locale. 
     * @param tokens the array of tokens to parse
     * @param patterns the list of patterns and descriptions, as described
     * above.
     * @param locale
     * @param the current date, if inference of under-specified dates needed
     * @return
     * @throws DCTExtractorException 
     */
    protected static Calendar getDateFromText(String[] tokens, HashMap<Pattern, String> patterns, Locale locale, Calendar today) throws DCTExtractorException {
        // If the text is too long, skip
        if (tokens.length > MAX_DATE_STRING_LENGTH) {
            return null;
        }
        // a HashMap mapping {@link java.util.Calendar} fields (Calendar.MONTH,
        // Calendar.DAY_OF_MONTH, etc.) to their extracted Integer value.
        HashMap<Integer, Integer> dateChunks = new HashMap<Integer, Integer>();
        // the same mapping for a single token
        HashMap<Integer, Integer> tokenChunks;

        // Parse all tokens and find the Calendar fields
        for (String token : tokens) {
            tokenChunks = analyzeToken(token, patterns);
            for (Entry<Integer, Integer> entry : tokenChunks.entrySet()) {
                // If the field has not been filled yet
                if (!dateChunks.containsKey(entry.getKey())) {
                    dateChunks.put(entry.getKey(), entry.getValue());
                }
                // Conflict: if the field has already been field
                // priority is to the token the produced the most fields at once.
                // If same number, keep the first one
                else if (tokenChunks.size() > 1) {
                    dateChunks.put(entry.getKey(), entry.getValue());
                }
            }
        }

        // Get date informations
        Calendar result = null;
        Integer year = dateChunks.get(Calendar.YEAR);
        Integer month = dateChunks.get(Calendar.MONTH);
        Integer day = dateChunks.get(Calendar.DAY_OF_MONTH);

        // If year is not specified but we know today date
        // we will be able to infer the year 
        // such as the date is just before today 
        boolean yearFoundWithToday = false;
        if (year == null && today != null) {
            year = today.get(Calendar.YEAR);
            yearFoundWithToday = true;
        }

        // Year, month and day are mandatory
        if (year != null && month != null && day != null) {
            result = new GregorianCalendar(locale);

            // If month is higher than 11, maybe
            // it's because month and day have be mixed
            // up (cf differences between US and UK date formats)
            // try to switch them.
            if (month > 11) {
                int tmp = month;
                month = day - 1;
                day = tmp + 1;
            }
            // if day and month are not well-formed,
            // forget ir
            if (day > 31) {
                return null;
            }
            else if (month > 11) {
                return null;
            }
            // if year was a two-digit number
            // add 2000 to it.
            else if (year < 100) {
                year += 2000;
            }
            // Because we do not use time info, set HH:MM:SS as 00:00:00, otherwise all today's articles fail to pass today.before(result) condition
            result.set(year, month, day, 0, 0, 0);

            // if we know today date, the inferred
            // date must not be AFTER today
            if (today != null) {
                if (today.before(result)) {
                    // If the year was provided by today date,
                    // remove 1 from the year
                    if (yearFoundWithToday) {
                        result.set(Calendar.YEAR, year - 1);
                    }
                    // else, try to switch month and day if possible
                    else {
                        if (month < 12 && day < 13) {
                            int tmp = month;
                            month = day - 1;
                            day = tmp + 1;
                            // Set the date
                            result.set(year, month, day);
                            // if today still before the date, skip it
                            if (today.before(result)) {
                                result = null;
                            }
                        }
                        // else, the date is not good, skip it
                        else {
                            result = null;
                        }
                    }
                }
            }
        }
        return result;

    }

    /**
     * Parse a natural language text into a date (if possible),
     * in the locale specified in constructor.
     * @param text the natural language text that is expected
     * to represent a date
     * @return a {@link java.util.Calendar} representing the
     * date expressed in natural language by the specified text.
     * @throws DCTExtractorException 
     */
    public Calendar parse(String text) throws DCTExtractorException {
        return this.parse(text, null);
    }

    /**
     * Parse a natural language text into a date (if possible),
     * in the locale specified in constructor.
     * @param text the natural language text that is expected
     * to represent a date
     * @param the current date, if inference of under-specified dates needed
     * @return a {@link java.util.Calendar} representing the
     * date expressed in natural language by the specified text.
     * @throws DCTExtractorException 
     */
    public Calendar parse(String text, Calendar today) throws DCTExtractorException {
        // Split the text into tokens
        String[] tokens = text.split("[ ,()]");

        // Analyze all tokens
        return getDateFromText(tokens, this.regexes, this.locale, today);
    }


//    //	/********************
//    //	 * Program options
//    //	 ********************/
//    //	private static final String OPTION_LANG = "l";
//
//    public static void main(String[] args) throws IOException, DCTExtractorException {
////        Enumeration<URL> vocabularyResources = Thread.currentThread().getContextClassLoader().getResources("data/fr/vocabulary/");
////        if (vocabularyResources == null || !vocabularyResources.hasMoreElements()) {
////            throw new DCTExtractorException("No vocabulary resource found in data directory ");
////        }
//        URL url = Thread.currentThread().getContextClassLoader().getResource("data/fr/vocabulary/");
////        while (vocabularyResources.hasMoreElements()) {
////            url = vocabularyResources.nextElement();
//        System.out.println(url.getFile());
//        File file = new File(url.getFile());
//        
//        System.out.println("2. " + file.getAbsolutePath() + " " + file.exists());
//        
//        File[] files = file.listFiles();
//        
//        for (File file2 : files) {
//            System.out.println(file2.getAbsolutePath());
//        }
////            System.out.println(url);
////        }
//        //		// Default values
//        //		if (args.length == 0) {
//        //			String options = " -" + OPTION_LANG + " en_GB "
//        ////					+ "12:59am GMT </span>  13/01/2008)";
//        //					+ "March 21 2013 </span> <div> Mar 21 </div> <div> 2:01";
//        //			String[] newArgs = StringUtils.split(options, " ");
//        //			System.out.println("OPTIONS : \n" + options);
//        //			args = newArgs;
//        //		}
//        //		/**************************/
//        //		/* Program parameters *****/
//        //		/**************************/
//        //		CustomOptions options = new CustomOptions(DateParser.class.getSimpleName());
//        //		
//        //		Option langOption = new Option(OPTION_LANG, true, "Locale: [en|en_US|en_GB]");
//        //		langOption.setRequired(true);
//        //		options.addOption(langOption);
//        //		args = options.parseOptions(args);
//        //
//        //		Locale locale = LanguageTools.getLocaleFromString(options.getOptionValue(OPTION_LANG));
//        //		DateParser parser = new DateParser(locale, new File("/home/xtannier/Recherche/DCTFinder/data/en_GB/vocabulary/"));
//        //		
//        //		String text = "";
//        //		for (String token : args) {
//        //			text += token + " ";
//        //		}
//        //		
//        //		Calendar downloadDate = new GregorianCalendar();
//        //		
//        ////		System.out.println(DateTools.simpleDateFormat(parser.parse(text).getTime()));
//        //		System.out.println(DateTools.simpleDateFormat(parser.parse(text, downloadDate).getTime()));
//    }
}
