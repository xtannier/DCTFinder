package fr.limsi.dctfinder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class Test {
	
	private final static String PATH_TO_WAPITI = "/home/xtannier/tools/wapiti-1.4.0/bin/wapiti";
	private final static String TEST_URL = "http://www.irishtimes.com/news/world/asia-pacific/irish-man-expected-to-apply-for-bail-over-allegedly-assaulting-his-brother-1.2054061";
	
	public static void main(String[] args) throws IOException, DCTExtractorException {
		// File or Path to Wapiti binary file
		File wapitiBinaryFile = new File(PATH_TO_WAPITI);
		// or: Path wapitiBinaryFile = Paths.get("/path/to/wapiti");

		// Create DCT extractor
		DCTExtractor extractor = new DCTExtractor(wapitiBinaryFile);

		// Specify locale (can be Locale.US, Locale.UK, Locale.FRANCE, Locale.FRENCH, ...)
		Locale locale = Locale.ENGLISH; 
		// Create URL
		URL url = new URL(TEST_URL); 
		// Open inputstream from a downloaded file or directly from the URL.
		InputStream is = url.openStream(); 
		// Get download date (Calendar object)
		// Knowing download date will lead to better results,
		//	    but it can be set to null
		Calendar downloadDate = new GregorianCalendar();

		// Get page info
		// the URL (second parameter) is used to detect a specific locale (e.g. UK), in case
		//	     a more general one is specified (e.g. ENGLISH)
		// Specific locales are important, because ways to write dates can be very different
		//	     in different countries spaeking the same language (e.g. US versus UK)
		// If we know in advance from which country the page is, specify the country.
		// DCTFinder provides extraction rules for Locale.UK, Locale.US and Locale.FRENCH,
		//	     but we can specify your own rules
		PageInfo pageInfo = extractor.getPageInfos(is, url, locale, downloadDate);

		// Get download date
		Calendar dctCalendar = pageInfo.getDCT();
		Date calendarDate = dctCalendar.getTime();

		// Get title
		String title = pageInfo.getTitle();
		
		System.out.println("Title: " + title);
		System.out.println("DCT:   " + calendarDate);
	}
}
