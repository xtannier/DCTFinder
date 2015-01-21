package fr.limsi.tools.classification;

public class ClassificationException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ClassificationException(Exception e) {
		super (e);
	}

	public ClassificationException(String message) {
		super(message);
	}


}
