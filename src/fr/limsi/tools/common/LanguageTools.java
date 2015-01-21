package fr.limsi.tools.common;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class LanguageTools {
	public static Locale getLocaleFromString(String language) {
		if (language == null) {
			return null;
		} else if (language.equalsIgnoreCase("en")) {
			return Locale.ENGLISH;
		} else if (language.equalsIgnoreCase("en_US")) {
			return Locale.US;
		} else if (language.equalsIgnoreCase("en_UK")) {
			return Locale.UK;
		} else if (language.equalsIgnoreCase("en_GB")) {
			return Locale.UK;
		} else if (language.equalsIgnoreCase("english")) {
			return Locale.ENGLISH;
		} else if (language.equalsIgnoreCase("fr")) {
			return Locale.FRENCH;
		} else if (language.equalsIgnoreCase("fr_FR")) {
			return Locale.FRANCE;
		} else if (language.equalsIgnoreCase("french")) {
			return Locale.FRENCH;
		} else if (language.equalsIgnoreCase("français")) {
			return Locale.FRENCH;
		} else if (language.equalsIgnoreCase("francais")) {
			return Locale.FRENCH;
		} else {
			return null;
		}
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
			throw new RuntimeException("Unknown locale " + locale + " for this method");
		}
	}
	
	@Deprecated
	public static String getTwoLetterLanguage(Locale locale) {
		if (locale == Locale.ENGLISH) {
			return "en";
		} else if (locale == Locale.FRENCH) {
			return "fr";
		} else {
			return "??";
		}
	}
}
