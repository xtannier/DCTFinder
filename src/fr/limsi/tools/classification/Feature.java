package fr.limsi.tools.classification;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Feature implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected static final String DISCRETIZED_INTERVAL_PREFIX = "STEP_";
	public static final String DISCRETIZED_HIGHEST_VALUE = "STEP_MORE";
	private String name;
	private String comment;
	private Object type;
	private Short normalizationInstruction;
	private Object defaultValue;
	private boolean disabled;
	private int tags;
	private Number[] discretizationInstruction;
	private int[] combinedFeatures;
	// Index of the feature in the record
//	private int index;
	
	public Feature(String name, Object type) throws FeatureException {
		this(name, type, null, null, null);
	}
	
	public Feature(String name, Object type, String comment) throws FeatureException {
		this(name, type, comment, null, null);
	}


	public Feature(String name, Object type, String comment, Object defaultValue) throws FeatureException {
		this(name, type, comment, defaultValue, null);
	}


	@SuppressWarnings("unchecked")
	public Feature(String name, Object type, String comment, Object defaultValue, Short normalizationInstruction) throws FeatureException {
		if (name.contains(" ")) {
			throw new FeatureException("Please do not insert a space into a feature name (" + name + ")");
		}
		this.name = name;
		this.type = type;
		this.comment = comment;
		this.normalizationInstruction = normalizationInstruction;
		this.defaultValue = defaultValue;
		this.disabled = false;
		this.tags = 0;
		this.discretizationInstruction = null;
		this.combinedFeatures = null;
		if (type instanceof String[]) {
			HashSet<String> newType = new HashSet<String>();
			for (String elem : (String[])type) {
				newType.add(elem);
			}
			this.type = newType;
		} 
		else if (type instanceof Number[]) {
			HashSet<String> newType = new HashSet<String>();
			for (Number elem : (Number[])type) {
				newType.add(DISCRETIZED_INTERVAL_PREFIX + elem);
			}
			newType.add(DISCRETIZED_HIGHEST_VALUE);
			this.discretizationInstruction = (Number[])type;
			this.type = newType;			
		}
		
		if (this.type instanceof HashSet) {
			if (((HashSet<String>)this.type).contains(RecordFactory.NULL_VALUE) ){
				throw new FeatureException("Feature " + name + " contains the null value '" + RecordFactory.NULL_VALUE + "', which is forbidden");
			}
		} 
//		else if (this.type instanceof BooleanCombination) {
//		} 
		else if (this.type instanceof Boolean) {
		} 
		else if (this.type instanceof Integer) {
		} 
		else if (this.type instanceof Byte) {
		} 
		else if (this.type instanceof Short) {
		} 
		else if (this.type instanceof Double) {
		} 
		else if (this.type instanceof String) {
		}
		else {
			throw new RuntimeException("Type " + type.getClass() + " not implemented (feature name: " + name + ")");
		}
	}

	public boolean isNumeric() {
		return this.type instanceof Double || this.type instanceof Integer || this.type instanceof Long;	
	}
	

	
	public int setTag(int tag) throws FeatureException {
		double log = Math.log(tag)/Math.log(2);
		if (log != Math.floor(log)) {
			throw new FeatureException("Tag " + tag + " is not a power of 2 !");
		}
		this.tags |= tag;
		return this.tags;
	}
	
	public int setTags(Set<Integer> tags) throws FeatureException {
		for (Integer tag : tags) {
			this.setTag(tag);
		}
		return this.tags;
	}
	
	public int getTags() {
		return this.tags;
	}
	
	public int removeTag(int tag) {
		double log = Math.log(tag)/Math.log(2);
		if (log != Math.floor(log)) {
			throw new RuntimeException("Tag " + tag + " is not a power of 2 !");
		}
		this.tags ^= tag;
		return this.tags;
	}
	
	public boolean hasTag(int tag) {
		double log = Math.log(tag)/Math.log(2);
		if (log != Math.floor(log)) {
			throw new RuntimeException("Tag " + tag + " is not a power of 2 !");
		}
		return (this.tags & tag) > 0;
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the comment
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @param comment the comment to set
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * @return the type
	 */
	public Object getType() {
		return type;
	}


	/**
	 * @return the normalizationInstruction
	 */
	public Short getNormalizationInstruction() {
		return normalizationInstruction;
	}

	/**
	 * @param normalizationInstruction the normalizationInstruction to set
	 */
	public void setNormalizationInstruction(Short normalizationInstruction) {
		this.normalizationInstruction = normalizationInstruction;
	}

	/**
	 * @return the defaultValue
	 */
	public Object getDefaultValue() {
		return defaultValue;
	}

	/**
	 * @param defaultValue the defaultValue to set
	 */
	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * @return the disabled
	 */
	public boolean isDisabled() {
		return disabled;
	}

	/**
	 * @param disabled the disabled to set
	 */
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	/**
	 * @return the discretizationInstruction
	 */
	public Number[] getDiscretizationInstruction() {
		return discretizationInstruction;
	}

	/**
	 * @param discretizationInstruction the discretizationInstruction to set
	 */
	public void setDiscretizationInstruction(Number[] discretizationInstruction) {
		this.discretizationInstruction = discretizationInstruction;
	}

	/**
	 * @return the combinedFeatures
	 */
	public int[] getCombinedFeatures() {
		return combinedFeatures;
	}

	/**
	 * @param combinedFeatures the combinedFeatures to set
	 */
	public void setCombinedFeatures(int[] combinedFeatures) {
		this.combinedFeatures = combinedFeatures;
	}
	
	@Override
	public String toString() {
		return this.name + " (" + this.comment + ")";
	}
}
