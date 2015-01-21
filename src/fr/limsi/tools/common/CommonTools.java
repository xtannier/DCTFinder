package fr.limsi.tools.common;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * @author xtannier
 */
public class CommonTools {
	
	private static HashMap<Integer, Long> startTimes = new HashMap<Integer, Long>();
	private static int chronoId = 0;
	private static int SECOND_DURATION_IN_MILLIS = 1000;
	private static int MINUTE_DURATION_IN_MILLIS = SECOND_DURATION_IN_MILLIS * 60;
	private static int HOUR_DURATION_IN_MILLIS = MINUTE_DURATION_IN_MILLIS * 60;
	
	public static final DecimalFormat TWO_DIGIT_FORMAT = new DecimalFormat( "#,###,###,#00" );


	
    /**
     * Waits until a key has been pressed.
     */
    public static void waitKeyPressed() {
    	try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
	
	/**
	 * Starts the chrono
	 */
	public static int startChrono() {
		startTimes.put(++chronoId, System.currentTimeMillis());
		return chronoId;
	}
	
	/**
	 * Ends the chrono and return the time in seconds since last start
	 * @return
	 */
	public static double endChrono() {
		if (startTimes.size() == 1) {
			for (Entry<Integer, Long> entry : startTimes.entrySet()) {
				long endTime = System.currentTimeMillis();
				long elapsed = endTime - entry.getValue();
				return ((double)elapsed/1000.0);
			}
			return -1;
		} else {
			throw new RuntimeException("Several chrono starts are recorded, please specify an id");
		}
	}

	
	/**
	 * Ends the chrono and return the time in seconds since last start
	 * @return
	 */
	public static double endChrono(int chronoId) {
		long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTimes.get(chronoId);
        return ((double)elapsed/1000.0);
	}
	
	public static String formatEndChrono(int chronoId) {
        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTimes.get(chronoId);
        
        long elapsedHours = elapsed / HOUR_DURATION_IN_MILLIS;
        elapsed = elapsed - (elapsedHours * HOUR_DURATION_IN_MILLIS);
        long elapsedMinutes = elapsed / MINUTE_DURATION_IN_MILLIS;
        elapsed = elapsed - (elapsedMinutes * MINUTE_DURATION_IN_MILLIS);
        long elapsedSeconds = elapsed / SECOND_DURATION_IN_MILLIS;
        return TWO_DIGIT_FORMAT.format(elapsedHours) + ":" + TWO_DIGIT_FORMAT.format(elapsedMinutes) + ":" + TWO_DIGIT_FORMAT.format(elapsedSeconds);
	}
	
	/**
	 * Returns a String representation of the current date and time 
	 * @return a String representation of the current date and time
	 */
	public static String logTime() {
		Calendar now = Calendar.getInstance();
		int hh = now.get(Calendar.HOUR_OF_DAY);         
		int mm = now.get(Calendar.MINUTE);         
		int ss = now.get(Calendar.SECOND);         
		int mois = now.get(Calendar.MONTH) +  1;         
		int jour= now.get(Calendar.DAY_OF_MONTH);         
		int annee = now.get(Calendar.YEAR);                    
		return jour+" / "+mois+" / " +annee+ "   "+ hh+":"+mm+":"+ss + "\n";    
	}

    
}
