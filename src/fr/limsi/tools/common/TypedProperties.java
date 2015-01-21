/*
 * TypedProperties.java
 *
 * Created on February 15, 2007, 1:33 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package fr.limsi.tools.common;

import java.util.Properties;

/**
 * This class extends java.util.Properties with typed methods
 * @author xtannier
 */
public class TypedProperties extends Properties {
        
    private static final long serialVersionUID = 1L;    
    
    /** Creates a new instance of TypedProperties */
    public TypedProperties() {
        super();
    }
    
    /** 
     * Creates a new instance of TypedProperties 
     * @param defaults the defaults properties
     */
    public TypedProperties(Properties defaults) {
        super(defaults);
    }
    
	public void setBooleanProperty(String key, boolean value) {
		if (value) {
			this.setProperty(key, "TRUE");
		} else {
			this.setProperty(key, "FALSE");
		}
	}
    
    /**
     * Returns a boolean value corresponding to the key Property.
     * Value <code>true</code> is returned if the value of the property is "1" or "true"
     * <code>false</code> if the value is "0" or "false" (case-insensitive).
     *
     * @param key the key of the property
     * @throws TypedPropertyException if the property has no boolean value representation
     * @return a boolean value corresponding to the key Property, <code>false</code> if the 
     * property is not found.
     */
    public boolean getBooleanProperty(String key) throws TypedPropertyException{        
        return getBooleanProperty(key, false);
    }
    
    
    /**
     * Returns a boolean value corresponding to the key Property.
     * Value <code>true</code> is returned if the value of the property is "1" or "true"
     * <code>false</code> if the value is "0" or "false" (case-insensitive).
     *
     * @param key the key of the property
     * @param defaultValue the boolean value to return if the property key is not found
     * @throws TypedPropertyException if the property has no boolean value representation
     * @return a boolean value corresponding to the key Property.
     */    
    public boolean getBooleanProperty(String key, boolean defaultValue) throws TypedPropertyException{        
        String value = super.getProperty(key);
        if (value == null)
            return defaultValue;
        else if (value.toUpperCase().equals("TRUE") || value.equals("1"))
            return true;
        else if (value.toUpperCase().equals("FALSE") || value.equals("0"))
            return false;
        else 
            throw new TypedPropertyException("key " + key + " has not a boolean value (" + value + ").");
    }

    
    /**
     * Returns an integer value corresponding to the key Property.
     *
     * @param key the key of the property
     * @throws TypedPropertyException if the property has no integer value representation
     * @return an integer value corresponding to the key Property, 0 if the property is not found.
     */     
    public int getIntProperty(String key) throws TypedPropertyException{        
        return getIntProperty(key, 0);
    }
    
    
    /**
     * Returns an integer value corresponding to the key Property.
     *
     * @param key the key of the property
     * @param defaultValue the int value to return if the property key is not found
     * @throws TypedPropertyException if the property has no integer value representation
     * @return an integer value corresponding to the key Property.
     */    
    public int getIntProperty(String key, int defaultValue) throws TypedPropertyException{        
        String value = super.getProperty(key);
        int result;
        if (value == null)
            return defaultValue;
        else {
            try{
                result = Integer.parseInt(value);
                return result;
            } catch (NumberFormatException ex) {
                throw new TypedPropertyException("key " + key + " has not an integer value (" + value + ").");
            }
        }
    }
    
    /**
     * Returns an ArrayList value corresponding to the key Property, with delimiter delim
     *
     * @param key the key of the property
     * @param delim the delimiter 
     * @throws TypedPropertyException if the property has no boolean value representation
     * @return an array value corresponding to the key Property, with delimiter delim.
     */
    public String[] getArrayProperty(String key, String delim) {        
        return getArrayProperty(key, delim, null);
    }
    
    
    /**
     * Returns an array value corresponding to the key Property, with delimiter delim     *
     * @param key the key of the property
     * @param delim the delimiter
     * @param defaultValue the boolean value to return if the property key is not found
     * @throws TypedPropertyException if the property has no boolean value representation
     * @return an array value corresponding to the key Property, with delimiter delim.
     */    
    public String[] getArrayProperty(String key, String delim, String[] defaultValue) {        
        String value = super.getProperty(key);
        if (value == null)
            return defaultValue;
        else 
            return value.split(delim);
    }
    
    
}
