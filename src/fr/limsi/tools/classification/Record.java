package fr.limsi.tools.classification;

import java.io.Serializable;
import java.util.List;


public class Record implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String comment;
	private Object[] instances;
	private boolean[] featureAssigned;
	private String classValue;
	private String classValueForClassifier;
	private String predictedClass;	
	private double predictedConfidence;
	private Object classifiedObject;
	protected transient RecordFactory factory;

	public Record(Record clone) {
		this.factory = clone.factory;
		this.comment = clone.comment;
		
		this.instances = new Object[this.factory.getFeatureNumber()];
		for (int i = 0 ; i < this.instances.length ; i++) {
			this.instances[i] = clone.instances[i];
		}
		this.featureAssigned = new boolean[this.factory.getFeatureNumber()];
		for (int i = 0 ; i < this.featureAssigned.length ; i++) {
			this.featureAssigned[i] = clone.featureAssigned[i];
		}
		this.classValue = clone.classValue;
		this.classValueForClassifier = clone.classValueForClassifier;
		this.predictedClass = clone.predictedClass;
		this.predictedConfidence = clone.predictedConfidence;
		this.classifiedObject = clone.classifiedObject;
	}
	
	protected Record(RecordFactory featureSetFactory, Object classifiedObject) {		
		this.factory = featureSetFactory;
		this.comment = null;
		this.classValue = null;
		this.classValueForClassifier = null;
		this.predictedClass = null;
		this.predictedConfidence = -1;
		this.classifiedObject = classifiedObject;
		if (this.factory != null) {
			this.instances = new Object[this.factory.getFeatureNumber()];
			this.featureAssigned = new boolean[this.factory.getFeatureNumber()];
			List<Feature> features = this.factory.getFeatures();
			Feature feature;
			for (int i = 0 ; i < features.size() ; i++) {
				feature = features.get(i);
				this.instances[i] = feature.getDefaultValue();
				if (feature.getCombinedFeatures() != null) {
					try {
						this.updateCombinedFeatureValue(i);
					} catch (FeatureException e) {
						throw new RuntimeException("Should never happen!");
					}
				}
			}
		}
	}
	
	protected void add(int index, Object value) throws FeatureException {
		add(index, value, false);
	}
	
	
	public void add(int index, Object value, boolean replace) throws FeatureException {
		if (this.featureAssigned[index]) {
			if (!replace) {
				throw new FeatureException("A value has already been given to feature " + index + ". " + this.factory.getFeatureName(index));
			}
		}
		this.instances[index] = value;
		this.featureAssigned[index] = true;
		
	}
	
	public int replace(String featureName, Object value) throws FeatureException {
		return add(featureName, value, true);
	}

	public int add(String featureName, Object value) throws FeatureException {
		return add(featureName, value, false);
	}
	
	private int add(String featureName, Object value, boolean replace) throws FeatureException {
		// If the value comes from a cross-product, check if none of the elements is null
//		if (value instanceof String) {
//			String[] splitValues = ((String)value).split(RecordFactory.PAIR_DELIMITER);
//			for (String splitValue : splitValues) {
//				if (splitValue.equals(RecordFactory.NULL_VALUE)) {
//					value = null;
//					break;
//				}
//			}
//		}
//		if (value instanceof Double) {
//			if (((Double) value).isNaN() || ((Double) value).isInfinite()) {
//				throw new FeatureException("Cannot add value " + value + " for feature " + featureName);
//			}
//		}
		int index = this.factory.getFeatureId(featureName);
		this.factory.addFeatureValue(index, value, this, replace);
		return index;
	}
	
	public boolean addFromLine(String line, String delim) throws FeatureException {
		String[] fields = line.split(delim);
		for (int i = 0 ; i < fields.length ; i++) {
			this.instances[i] = this.factory.getFeatureFromString(i, fields[i]);
		}
		return true;
	}
	
	public Object get(int index) {
		return this.instances[index];
	}
	
	public Object get(String key) throws FeatureException {
		return this.instances[this.factory.getFeatureId(key)];
	}
	
	public int size() {
		return this.instances.length;
	}
	
	public RecordFactory getFactory() {
		return this.factory;
	}
	
	public String getClassValue() {
		return classValue;
	}
	
	public int getFeatureNumber() {
		return this.factory.getFeatureNumber();
	}
	
	protected Object updateCombinedFeatureValue(int index) throws FeatureException {
		Feature feature = this.factory.getFeatureFromId(index);
		int[] featureIndexes = feature.getCombinedFeatures();
		Object[] combinedValues = new Object[featureIndexes.length];
		for (int i = 0 ; i < combinedValues.length ; i++) {
			combinedValues[i] = this.get(featureIndexes[i]);
		}
		return this.factory.addFeatureValue(index, RecordFactory.getCombinedValue(combinedValues), this, true);
	}

	public void setClassValue(String classValue) throws FeatureException {
		if (classValue == null || classValue.equals(RecordFactory.MISSING_VALUE)) {
			this.classValue = null;
			this.classValueForClassifier = null;
		}
		else if (this.factory.isClassNameValid(classValue)) {
			this.classValue = classValue;
			this.classValueForClassifier = classValue;
		} else {
			throw new FeatureException("Unlisted value class: " + classValue);
		}
	}

	/**
	 * @return the classValueForClassifier
	 */
	public String getClassValueForClassifier() {
		return classValueForClassifier;
	}

	/**
	 * @param classValueForClassifier the classValueForClassifier to set
	 */
	public void setClassValueForClassifier(String classValueForClassifier) {
		this.classValueForClassifier = classValueForClassifier;
	}

	/**
	 * @return the predictedClass
	 */
	public String getPredictedClass() {
		return predictedClass;
	}

	/**
	 * @param predictedClass the predictedClass to set
	 */
	public void setPredictedClass(String predictedClass) {
		this.predictedClass = predictedClass;
	}

	/**
	 * @return the predictedConfidence
	 */
	public double getPredictedConfidence() {
		return predictedConfidence;
	}

	/**
	 * @param predictedConfidence the predictedConfidence to set
	 */
	public void setPredictedConfidence(double predictedConfidence) {
		this.predictedConfidence = predictedConfidence;
	}

	/**
	 * @return the classifiedObject
	 */
	public Object getClassifiedObject() {
		return classifiedObject;
	}

	/**
	 * @param classifiedObject the classifiedObject to set
	 */
	public void setClassifiedObject(Object classifiedObject) {
		this.classifiedObject = classifiedObject;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	@Override
	public String toString() {
		String ret = "";
		Feature feature;
		if (this.comment != null) {
			ret += "%% " + this.comment + "\n";
		}
		for (int i = 0 ; i < this.instances.length ; i++) {			
			feature = this.factory.getFeatureFromId(i); 
			if (!feature.isDisabled()) {
				ret += this.factory.getFeatureName(i) + "=" + this.instances[i] + ",";
			}
		}
		ret += " CLASS=" + this.classValue;
		return ret;
	}
}
