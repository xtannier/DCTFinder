package fr.limsi.tools.common;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateTools {
	
	public static final SimpleDateFormat SHORT_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
	public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	public static final SimpleDateFormat DIRECTORY_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd");
	public static final SimpleDateFormat LONG_DATE_FORMAT = new SimpleDateFormat("MMM dd yyyy hh:mm:ss zzz", Locale.ENGLISH);
	public static final SimpleDateFormat ISO_DATE_FORMAT1 = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
	public static final SimpleDateFormat ISO_DATE_FORMAT2 = new SimpleDateFormat("yyyyMMdd'T'HHmmssZ");
	public static final SimpleDateFormat ISO_DATE_FORMAT3 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
	public static final SimpleDateFormat ISO_DATE_FORMAT4 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
	public static final SimpleDateFormat ISO_DATE_FORMAT5 = new SimpleDateFormat("yyyyMMdd", Locale.US);
	public static final SimpleDateFormat FRENCH_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
	public static final SimpleDateFormat ENGLISH_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
	/** RFC 822 compliant DateFormat.  */
	public static final SimpleDateFormat RFC_822_DATE_FORMAT = new SimpleDateFormat("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'z", Locale.US);

	
	
	private DateFormat[] formats = new DateFormat[] {
			new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US),
			new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
			new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
			new SimpleDateFormat("yyyy-MM-dd", Locale.US) };

	public synchronized Date parse(String source, ParsePosition pos) {
		Date date = formats[0].parse(source, pos);
		if (date != null) {
			return date;
		}
		date = formats[1].parse(source, pos);
		if (date != null) {
			return date;
		}
		date = formats[2].parse(source, pos);
		if (date != null) {
			return date;
		}
		date = formats[3].parse(source, pos);
		if (date != null) {
			return date;
		}
		return null;
	}

	
	public synchronized static String customDateFormat(Date date, Locale locale) {
		if (locale == Locale.FRENCH) {
			return FRENCH_DATE_FORMAT.format(date);
		} else if (locale == Locale.ENGLISH) {
			return ENGLISH_DATE_FORMAT.format(date);
		} else {
			throw new RuntimeException("Unknown language " + locale.getDisplayLanguage());
		}
	}
	
	public synchronized static Date shortDateParse(String shortDate) throws ParseException {
		return SHORT_DATE_FORMAT.parse(shortDate);
	}

	public synchronized static Date isoDateParse(String isoDate) throws ParseException {	
		try {
			return ISO_DATE_FORMAT1.parse(isoDate);
		} catch (ParseException e1) {
			try {
				return ISO_DATE_FORMAT2.parse(isoDate);
			} catch (ParseException e2) {
				try {
					return ISO_DATE_FORMAT3.parse(isoDate);
				} catch (ParseException e3) {
				    try {
				        return ISO_DATE_FORMAT4.parse(isoDate);
	                } catch (ParseException e4) {
	                    return ISO_DATE_FORMAT5.parse(isoDate);
				    }
				}
			}
		}
	}
	
	public synchronized static String simpleDateFormat(Date date) {
		return SIMPLE_DATE_FORMAT.format(date);
	}
		
	public synchronized static String shortDateFormat(Date date) {
		return SHORT_DATE_FORMAT.format(date);
	}
	
	public synchronized static String longDateFormat(Date date) {
		return LONG_DATE_FORMAT.format(date);
	}
	
	public synchronized static String isoDateFormat(Date date) {
		return ISO_DATE_FORMAT1.format(date);
	}
	
	public static int intValue(Date date) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		return c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DATE);
	}
	
	/**
	 * Difference between date2 and date1 (date2 - date1).
	 * @param date1
	 * @param date2
	 * @param unit
	 * @return
	 */
	public static double diffDate(Date date1, Date date2, int unit) {
		 long delta = date2.getTime() - date1.getTime();
		 int millisecondPerUnit;
		 switch (unit) {
		 case Calendar.DAY_OF_MONTH:
			 millisecondPerUnit = 1000 * 60 * 60 * 24; 
			 break;
		 case Calendar.HOUR:
			 millisecondPerUnit = 1000 * 60 * 60;
			 break;
		 case Calendar.HOUR_OF_DAY:
			 millisecondPerUnit = 1000 * 60 * 60;
			 break;
		 case Calendar.MINUTE:
			 millisecondPerUnit = 1000 * 60;
		 	break;
		 case Calendar.SECOND:
			 millisecondPerUnit = 1000;
			 break;
		 case Calendar.MILLISECOND:
			 millisecondPerUnit = 1;
			 break;
		 default:
			 millisecondPerUnit = 0;
		 }
		 return (double)delta/(double)millisecondPerUnit;
	}

	
	
	public static String weekDayIntToString(Date date, Locale locale) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		int weekDay = calendar.get(Calendar.DAY_OF_WEEK);
		if (locale == Locale.ENGLISH) {
			switch (weekDay) {
			case Calendar.SUNDAY:
				return "Sunday";
			case Calendar.MONDAY:
				return "Monday";
			case Calendar.TUESDAY:
				return "Tuesday";
			case Calendar.WEDNESDAY:
				return "Wednesday";
			case Calendar.THURSDAY:
				return "Thursday";
			case Calendar.FRIDAY:
				return "Friday";
			case Calendar.SATURDAY:
				return "Saturday";				
			default:
				throw new RuntimeException("Cas impossible");
			}
		}
		else {
			throw new RuntimeException("Unknown language " + locale + " for this method");
		}
	}
	
	public static boolean sameDay(Calendar date1, Calendar date2) {
	    return (date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR) &&
	            date1.get(Calendar.MONTH) == date2.get(Calendar.MONTH) &&
	            date1.get(Calendar.DAY_OF_MONTH) == date2.get(Calendar.DAY_OF_MONTH)); 	        
	}
	
	public static void main(String[] args) throws ParseException {
		System.out.println(isoDateParse("20120514T063000Z"));
	}
}
