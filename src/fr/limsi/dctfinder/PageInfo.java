package fr.limsi.dctfinder;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import fr.limsi.tools.classification.RecordList;

/**
 * Information concerning a web page. The information
 * concern parsed title and document creation time (in Calendar format
 * or a String as represented in the text) for regular use and test, 
 * as well as reference title and date for evaluation and training.
 * @author xtannier
 *
 */
public class PageInfo {
	private String title;
	private String dateString;
	private Calendar dct;
	private String refTitle;
	private String refDateString;
	private Calendar refDCT;
	private RecordList records;
	private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	
	public PageInfo() {
		this(null, null);
	}
	
	public PageInfo(String title) {
		this(title, null);
	}
	
	public PageInfo(String title, Calendar dct) {
		this.title = title;
		this.dct = dct;
	}
	
	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	/**
	 * @return the dct
	 */
	public Calendar getDCT() {
		return dct;
	}
	/**
	 * @param dct the dct to set
	 */
	public void setDCT(Calendar dct) {
		this.dct = dct;
	}

	/**
	 * @return the dateString
	 */
	public String getDateString() {
		return dateString;
	}

	/**
	 * @param dateString the dateString to set
	 */
	public void setDateString(String dateString) {
		this.dateString = dateString;
	}

	/**
	 * @return the records
	 */
	public RecordList getRecords() {
		return records;
	}

	/**
	 * @param records the records to set
	 */
	public void setRecords(RecordList records) {
		this.records = records;
	}

	/**
	 * @return the refTitle
	 */
	public String getRefTitle() {
		return refTitle;
	}

	/**
	 * @param refTitle the refTitle to set
	 */
	public void setRefTitle(String refTitle) {
		this.refTitle = refTitle;
	}

	/**
	 * @return the refDateString
	 */
	public String getRefDateString() {
		return refDateString;
	}

	/**
	 * @param refDateString the refDateString to set
	 */
	public void setRefDateString(String refDateString) {
		this.refDateString = refDateString;
	}

	/**
	 * @return the refDCT
	 */
	public Calendar getRefDCT() {
		return refDCT;
	}

	/**
	 * @param refDCT the refDCT to set
	 */
	public void setRefDCT(Calendar refDCT) {
		this.refDCT = refDCT;
	}
	
	@Override
	public String toString() {
		String dctStr = null;
		if (this.dct != null) {
			dctStr = SIMPLE_DATE_FORMAT.format(this.dct.getTime());
		}
		return dctStr + "\t" + title; // + "\t" + this.dateString;
	}
}
