package fr.limsi.tools.common;

/**
 * This exception is raised when a TypedProperty method is called for a wring type property
 * @author xtannier
 */
public class TypedPropertyException extends java.lang.Exception {
    
    private static final long serialVersionUID = 1L;    
    
    /**
     * Creates a new instance of <code>TypedPropertyException</code> without detail message.
     */
    public TypedPropertyException() {
    }
    
    
    /**
     * Constructs an instance of <code>TypedPropertyException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public TypedPropertyException(String msg) {
        super("Exception: Property error " + msg);
    }
}
