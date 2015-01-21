package fr.limsi.dctfinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;
//import org.apache.log4j.Logger;


import fr.limsi.tools.classification.FeatureException;
import fr.limsi.tools.common.LanguageTools;

/**
 * Web page title and document creation time extractor. 
 * @author xtannier
 *
 */
public class DCTExtractor {
    
    private final static String CONFIG_FILE_PATH_IN_PROJECT = "conf/constants.txt";
    
    private HashMap<Locale, LocalDCTExtractor> extractors;
    private Properties properties;
    private File wapitiBinaryFile;
    private File wapitiModelFile;

    /**
     * 
     * @param wapitiBinaryPath the Path to Wapiti binary
     * @param logger an org.apache.log4j.Logger for logging (can be null)
     * @throws DCTExtractorException
     */
    public DCTExtractor(Path wapitiBinaryPath) throws DCTExtractorException {
        this(wapitiBinaryPath.toFile());
    }

    
    /**
     * 
     * @param wapitiBinaryFile the File to Wapiti binary
     * @param logger an org.apache.log4j.Logger for logging (can be null)
     * @throws DCTExtractorException
     */
    public DCTExtractor(File wapitiBinaryFile) throws DCTExtractorException {
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_PATH_IN_PROJECT);

        this.properties = new Properties();
        try {
            properties.load(stream);
        } catch (IOException e) {
            throw new RuntimeException("Couln't load configuration file " + CONFIG_FILE_PATH_IN_PROJECT + ": " + e.getMessage());
        }
        if (wapitiBinaryFile.isFile()) {
            this.wapitiBinaryFile = wapitiBinaryFile;
        } else {
            throw new RuntimeException("Wapiti binary path " + wapitiBinaryFile.getAbsolutePath() + " does not exist!");
        }
        // Wapiti model file name (copy in temporary file)
        String wapitiModelFileName = this.properties.getProperty(LocalDCTExtractor.WAPITI_MODEL_FILE);
        if (wapitiModelFileName == null) {
            throw new RuntimeException("Parameter " + LocalDCTExtractor.WAPITI_MODEL_FILE + " must be set in file " + CONFIG_FILE_PATH_IN_PROJECT);
        }
        
        InputStream wapitiModelStream = this.getClass().getClassLoader().getResourceAsStream(wapitiModelFileName);
        if (wapitiModelStream == null) {
            throw new RuntimeException("Could not find Wapiti model file " + wapitiModelFileName);
        }
        
        try {
            this.wapitiModelFile = File.createTempFile("wapiti-model-", ".bin");
        } catch (IOException e) {
            throw new DCTExtractorException(e);
        }
        this.wapitiModelFile.deleteOnExit();
        OutputStream wapitiModelStreamCopy;
        try {
            wapitiModelStreamCopy = new FileOutputStream(wapitiModelFile);
        } catch (FileNotFoundException e) {
            throw new DCTExtractorException(e);
        }
        byte[] buffer = new byte[1024];
        int length;
        try {
            while ((length = wapitiModelStream.read(buffer)) > 0) {
                wapitiModelStreamCopy.write(buffer, 0, length);
            }
            wapitiModelStreamCopy.close();
        } catch (IOException e) {
            throw new DCTExtractorException(e);
        }

        this.extractors = new HashMap<Locale, LocalDCTExtractor>();
    }
    
    
    /**
     * Get PageInfo from an InputStream
     * @param stream the InputStream
     * @param url the corresponding URL
     * @param locale the page origin or language 
     * @param downloadDate the date of download
     * @return a PageInfo object contaning estimated title and DCT
     * @throws DCTExtractorException
     */
    public PageInfo getPageInfos(InputStream stream, URL url, Locale locale, Calendar downloadDate) throws DCTExtractorException {
        /******************
         * English Locale patch
         ******************/
        // If locale == Locale.ENGLISH
        // then we keep two extractors (US + UK)
        // and try to choose from the URL
        if (locale == Locale.ENGLISH) {
            Locale specificLocale;
            if (url == null) {
                specificLocale = Locale.US;
            } else {
                // Change locale if US and if the URL is different from .us, .com, .org, .net
                // what about .ca, .nz ? Don't know their format
                String host = url.getHost();
                if (host.endsWith(".us") || host.endsWith(".com") || host.endsWith(".org") || host.endsWith(".tv") || host.endsWith(".net")) {
                    specificLocale = Locale.US;
            	} else if (host.endsWith(".ru")) {
            		specificLocale = LanguageTools.getLocaleFromString("ru");
            	}
                else {
                    specificLocale = Locale.UK;
                }
            }
            return getPageInfos(stream, url, specificLocale, downloadDate);
        }
        else {
            try {
                LocalDCTExtractor extractor = this.extractors.get(locale);
                if (extractor == null) {
                    extractor = new LocalDCTExtractor(locale, properties, true, false);
                    extractors.put(locale, extractor);
                }	
                return extractor.getPageInfos(stream, "testfile", url, downloadDate, this.wapitiModelFile.getAbsolutePath(), wapitiBinaryFile);
            } catch (InterruptedException e) {
                throw new DCTExtractorException(e);
            } catch (IOException e) {
                throw new DCTExtractorException(e);
            } catch (FeatureException e) {
                throw new DCTExtractorException(e);
            }
        }        
    }    
}
