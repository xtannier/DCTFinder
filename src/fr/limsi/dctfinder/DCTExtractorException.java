package fr.limsi.dctfinder;

public class DCTExtractorException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DCTExtractorException(String message) {
		super(message);
	}

	public DCTExtractorException(Exception cause) {
        super(cause);
    }

}
