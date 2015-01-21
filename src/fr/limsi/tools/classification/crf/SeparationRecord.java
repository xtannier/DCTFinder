package fr.limsi.tools.classification.crf;

import fr.limsi.tools.classification.Record;

public class SeparationRecord extends Record {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public SeparationRecord() {
		super(null, null);
	}
	
	public SeparationRecord(String comment) {
		super(null, null);
		this.setComment(comment);
	}

}
