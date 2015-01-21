package fr.limsi.tools.classification;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
//import org.apache.log4j.Logger;

import fr.limsi.tools.classification.crf.SeparationRecord;

public class RecordFactory implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String PAIR_DELIMITER = "-";
	public static final String MISSING_VALUE = "MISSING_VALUE";
	public static final String NULL_VALUE = "NIL";
	public static final String YES = "1";
	public static final String NO = "0";
	public static final String[] YES_NO_VALUES = {YES, NO};

	public static final short DO_NOT_NORMALIZE = 0;
	public static final short NORMALIZE = 1;

	private HashMap<String, Integer> featureIds; 
	private HashMap<Integer, String> featureNames;
	private List<Feature> features;
	private String relation;
	private ArrayList<String> classes;
	private boolean noMissingValue;
	private HashSet<Integer> currentTags;
	private HashMap<Integer, HashSet<Integer>> inverseCombinedFeatures;
//	protected transient boolean verbose;
	
	public RecordFactory(String relation, String[] classes) {
		this(relation, classes, false);
	}

	public RecordFactory(String relation, List<String> classes) {
		this(relation, classes, false);
	}
	
	public RecordFactory(String relation, String[] classes,
			boolean noMissingValue) {		
		this.featureIds = new HashMap<String, Integer>();
		this.featureNames = new HashMap<Integer, String>();
		this.noMissingValue = false;
		if (classes != null) {
			this.classes = new ArrayList<String>();
			for (String className : classes) {
				this.classes.add(className);
			}
		} else {
			this.classes = null;
		}
		this.relation = relation;
		this.currentTags = new HashSet<Integer>();
		this.features = new ArrayList<Feature>();
		this.inverseCombinedFeatures = new HashMap<>();
		this.noMissingValue = noMissingValue;
	}

	public RecordFactory(String relation, List<String> classes, boolean noMissingValue) {
		this(relation, new String[0]);
		if (classes == null || classes.isEmpty()) {
			this.classes = null;
		} else {
			this.classes = new ArrayList<String>(classes);
		}

		this.noMissingValue = noMissingValue;
	}

		
	public RecordFactory(String relation, String[] classes,
			boolean noMissingValue, List<Feature> features) throws FeatureException {
		this(relation, classes, noMissingValue);
		if (features != null) {
			this.addFeatures(features);
		}
	}


	public RecordFactory(String relation, List<String> classes,
			boolean noMissingValue, List<Feature> features) throws FeatureException {
		this(relation, classes, noMissingValue);
		if (features != null) {
			this.addFeatures(features);
		}
	}

	public void startTag(int tag) throws FeatureException {
		if (!this.currentTags.add(tag)) {
			throw new FeatureException("Tag " + tag + " is already started");
		}
	}

	public void endTag(int tag) throws FeatureException {
		if (!this.currentTags.remove(tag)) {
			throw new FeatureException("Tag " + tag + " is never started");			
		}
	}

	/**
	 * @param features
	 * @param noMissingValue
	 * @param featureNumber
	 * @throws FeatureException 
	 */
	public void addFeatures(List<Feature> features) throws FeatureException {
		for (Feature feature : features) {
			this.addFeature(feature);
		}
	}
	
	public int addFeature(String name, Object type) throws FeatureException {
		return this.addFeature(new Feature(name, type));
	}

	public int addFeature(String name, Object type, String comment) throws FeatureException {
		return this.addFeature(new Feature(name, type, comment));
	}


	public int addFeature(String name, Object type, String comment, Object defaultValue) throws FeatureException {
		return this.addFeature(new Feature(name, type, comment, defaultValue));
	}
	


	public int addCrossedFeature(String... crossedFeatures) throws FeatureException {
		return this.addCrossedFeature(null, crossedFeatures, null, null);
	}
	
	public int addCrossedFeature(String name, String[] crossedFeatures) throws FeatureException {
		return this.addCrossedFeature(name, crossedFeatures, null, null);
	}
	
	public int addCrossedFeature(String name, String[] crossedFeatures, String comment) throws FeatureException {
		return this.addCrossedFeature(name, crossedFeatures, comment, null);
	}
	
	public int addCrossedFeature(String name, String[] crossedFeatures, String comment, Object[] defaultValue) throws FeatureException {
		Object[] crossedObjects = new Object[crossedFeatures.length];
		int[] combinedFeatureIds = new int[crossedFeatures.length];
		int index = 0;
		int id;
		String newComment = "COMBINATION BETWEEN ";
		String newName = "COMB_";
		for (String featureName : crossedFeatures) {
			id = this.getFeatureId(featureName);
			combinedFeatureIds[index] = id;
			Feature feature = this.features.get(id);
			if (feature.getType() instanceof HashSet) {
				crossedObjects[index] = feature.getType();
			} else if (feature.getType() instanceof Boolean) {
				HashSet<String> values = new HashSet<>();
				values.add(YES); values.add(NO);
				crossedObjects[index] = values;
			} else {
				throw new FeatureException("Can't create crossed feature from type " + feature.getType().getClass().getCanonicalName() + " (feature " + featureName + ")");
			}
			// Update comment
			if (comment == null) {
				newComment += "\"" + feature.getComment() + "\", ";
			}
			// Update name
			if (name == null) {
				newName += (id + 1) + "_" + featureName + "_"; 
			}
			// Update inverse combined features index
			HashSet<Integer> featureIds = this.inverseCombinedFeatures.get(id);
			if (featureIds == null) {
				featureIds = new HashSet<>();
			}
			featureIds.add(this.features.size());
			this.inverseCombinedFeatures.put(id, featureIds);
			index++;
		}
		if (name == null) {
			name = newName.substring(0, newName.length() - 1);
		}
		if (comment == null) {
			comment = newComment + " (" + name + ")";
		}
		Feature newFeature = new Feature(name, getCombinations(crossedObjects), comment, getCombinedValue(defaultValue));
		newFeature.setCombinedFeatures(combinedFeatureIds);
		return this.addFeature(newFeature);
	}
	
	/**
	 * @param feature
	 * @param i
	 * @throws FeatureException 
	 */
	@SuppressWarnings("unchecked")
	public int addFeature(Feature feature) throws FeatureException {
		int index = this.featureIds.size();
		HashSet<String> nominalValues;
		if (this.featureIds.containsKey(feature.getName())) {
			throw new FeatureException("Feature name " + feature.getName() + " is used twice");
		}
		this.featureIds.put(feature.getName(), index);
		this.featureNames.put(index, feature.getName());
		if (!this.noMissingValue && feature.getDefaultValue() == null) {
			if (feature.getType() instanceof HashSet) {
				nominalValues = (HashSet<String>)feature.getType();
				nominalValues.add(MISSING_VALUE);
				feature.setDefaultValue(MISSING_VALUE);
//			} else if (feature.getType() instanceof BooleanCombination) {
//				feature.setDefaultValue(((BooleanCombination)feature.getType()).getDefaultValue());
			} else if (feature.getType() instanceof Boolean) {
				feature.setDefaultValue(false);
			} else if  (feature.getType() instanceof Integer) {
				feature.setDefaultValue(0);
			} else if  (feature.getType() instanceof Double) {
				feature.setDefaultValue(0.0);
			} 
		}
		feature.setTags(this.currentTags);
		this.features.add(feature);
		return index;
	}


	/**
	 * @param records
	 * @param splitNumber
	 * @return
	 */
	public RecordList[] split(RecordList records, int splitNumber) {
		double[] weights = new double[splitNumber];
		for (int i = 0 ; i < splitNumber ; i++) {
			weights[i] = 1.0;
		}
		return this.split(records, weights, 0, false);
	}
	
	/**
	 * @param records
	 * @param splitNumber
	 * @return
	 */
	public RecordList[] split(RecordList records, int splitNumber, boolean preserveOrder) {
		double[] weights = new double[splitNumber];
		for (int i = 0 ; i < splitNumber ; i++) {
			weights[i] = 1.0;
		}
		return this.split(records, weights, preserveOrder);
	}

	/**
	 * @param records
	 * @param splitNumber
	 * @param seed
	 * @return
	 */
	public RecordList[] split(RecordList records, int splitNumber, long seed) {
		double[] weights = new double[splitNumber];
		for (int i = 0 ; i < splitNumber ; i++) {
			weights[i] = 1.0;
		}
		return this.split(records, weights, seed, false);
	}

	/**
	 * @param records
	 * @param weights
	 * @return
	 */
	public RecordList[] split(RecordList records, double trainingPercentageSplit) {
		double[] weights = {trainingPercentageSplit, 1.0 - trainingPercentageSplit};
		return this.split(records, weights);
	}
	
	/**
	 * @param records
	 * @param weights
	 * @return
	 */
	public RecordList[] split(RecordList records, double trainingPercentageSplit, boolean preserveOrder) {
		if (trainingPercentageSplit <= 0 || trainingPercentageSplit >= 1) {
			throw new RuntimeException("Percentage split must be in ]0..1[, not " + trainingPercentageSplit);
		}
		double[] weights = {trainingPercentageSplit, 1.0 - trainingPercentageSplit};
		return this.split(records, weights, preserveOrder);
	}

	
	/**
	 * @param records
	 * @param weights
	 * @return
	 */
	public RecordList[] split(RecordList records, double[] weights) {
		return this.split(records, weights, 0, false);
	}
	
	/**
	 * @param records
	 * @param weights
	 * @return
	 */
	public RecordList[] split(RecordList records, double[] weights, boolean preserveOrder) {
		return this.split(records, weights, 0, preserveOrder);
	}


	/**
	 * @param records
	 * @param weights
	 * @param useSeed
	 * @param seed
	 * @return
	 */
	private RecordList[] split(RecordList records, double[] weights, long seed, boolean preserveOrder) {
		int splitNumber = weights.length;
		RecordList[] result = new RecordList[splitNumber];
		double totalWeight = 0;
		double biggestWeight = 0.0;
		int biggestWeightIndex = 0;
		for (int splitIndex = 0 ; splitIndex < splitNumber ; splitIndex++) {
			totalWeight += weights[splitIndex];
			if (weights[splitIndex] > biggestWeight) {
				biggestWeight = weights[splitIndex];
				biggestWeightIndex = splitIndex;
			}
		}
		
		int recordNumber;
		int randomIndex = 0;
		
		Random random = null;
		if (!preserveOrder)
			random = new Random(seed);
			
		HashSet<Integer> selectedRecords = new HashSet<Integer>();
		
		// n-1 first split parts
		for (int splitIndex = 0 ; splitIndex < splitNumber ; splitIndex++) {
			if (splitIndex != biggestWeightIndex) {
				recordNumber = (int)Math.round(weights[splitIndex] / totalWeight * records.size());
				RecordList splitFeatures = new RecordList(records.getFactory());
				for (int recordIndex = 0 ; recordIndex < recordNumber ; recordIndex++) {
					if (preserveOrder) {
						randomIndex++;
					} else {
						do {
							randomIndex = random.nextInt(records.size());
						} while (selectedRecords.contains(randomIndex));
					}
					splitFeatures.add(records.get(randomIndex));
					selectedRecords.add(randomIndex);
				}
				result[splitIndex] = splitFeatures;
			}
		}
		
		// Last split part
		RecordList splitFeatures = new RecordList(records.getFactory());
		for (int recordIndex = 0 ; recordIndex < records.size() ; recordIndex++) {
			if (!selectedRecords.contains(recordIndex)) {
				splitFeatures.add(records.get(recordIndex));
			}
		}
		result[biggestWeightIndex] = splitFeatures;		
		return result;
	}
	
	
	public short getNormalizationInstruction(int index) {
		return this.features.get(index).getNormalizationInstruction();
//		return this.featureNormalizationInstructions.get(index);
	}

	public short getNormalizationInstruction(String featureName) {
		return this.features.get(this.featureIds.get(featureName)).getNormalizationInstruction();
//		return this.featureNormalizationInstructions.get(this.featureIds.get(featureName));
	}

	
	public Record getRecord(Object classifiedObject) {
		return new Record(this, classifiedObject);
	}
	
	protected boolean isClassNameValid(String className) {
		if (this.classes == null) {
			return true;
		} else {
			return this.classes.contains(className);
		}
	}
	
	public ArrayList<String> getClasses() {
		return this.classes;
	}
	
	public void setClasses(String[] classes) {
		this.classes.clear();
		for (String className : classes) {
			this.classes.add(className);
		}
	}
	
	public String getClass(int index) {
		return this.classes.get(index);
	}
	
	public int getFeatureId(String featureName) throws FeatureException {
		try {
			return this.featureIds.get(featureName);
		} catch (NullPointerException e) {
			throw new FeatureException("Feature " + featureName + " has not be declared");
		}
	}
	
	public Feature getFeatureFromId(int id) {
		return this.features.get(id);
	}
	
	public Feature getFeatureFromName(String featureName) throws FeatureException {
		return this.features.get(this.getFeatureId(featureName));
	}
	
	public Class<?> getFeatureType(int index) {		
		return this.features.get(index).getType().getClass();
//		return this.featureTypes.get(index).getClass();
	}
	
	public int getFeatureNumber() {
		return this.getFeatureNumber(false);
	}

	public int getFeatureNumber(boolean enabledOnly) {
		if (enabledOnly) {
			int i = 0;
			for (Feature feature : this.features) {
				if (!feature.isDisabled()) {
					i++;
				}
			}
			return i;
		} else {
			return this.featureIds.size();
		}
	}
	
	public String getFeatureName(int index) {
		return this.featureNames.get(index);
	}
	
	public String getRelation() {
		return this.relation;
	}
	
//
//	/**
//	 * @return 
//	 */
//	public boolean isVerbose() {
//		return this.verbose;
//	}
//
//	/**
//	 * @param 
//	 */
//	public void setVerbose(boolean verbose) {
//		this.verbose = verbose;
//	}

	private String secureFeatureName(String name) {
		return name.replaceAll(",", "_");
	}
	
	
	public boolean saveToSVMRank(ArrayList<RecordList> featureSets, File outFile) throws IOException {
		FileWriter fw = new FileWriter(outFile, false);
		BufferedWriter bw = new BufferedWriter (fw);
		PrintWriter writer = new PrintWriter (bw);
		
		int qid = 1;
		int fid = 0;
		int recordId;
		
		HashMap<String, Integer> classMap = new HashMap<String, Integer>();
		for (String className : this.classes) {
			if (className.equals(MISSING_VALUE)) {
				classMap.put(className, -1);
			} else {
				classMap.put(className, qid);
				qid++;
			}
		}
		
		qid = 1;
		
		for (int i  = 0 ; i < this.featureNames.size() ; i++) {
			if (!this.features.get(i).isDisabled()) {
				writer.print("# " + (fid+1) + " - " + this.featureNames.get(i));
				if (this.features.get(i).getComment() != null) {
					writer.print(" : " + this.features.get(i).getComment().replaceAll("\n", "\n# "));
				} 
				writer.println();
				fid++;
			}
		}
		
		for (RecordList featureSetList : featureSets) {
			writer.println("#########################################################");
			if (featureSetList.getComment() != null) {
				writer.println("# ");
				writer.println("# " + featureSetList.getComment().replaceAll("\n", "\n# "));
				writer.println("# ");
			}
			recordId = 1;
			for (Record featureSet : featureSetList) {
				if (featureSet.getComment() != null) {					
					writer.println("# " + recordId + " - " + featureSet.getComment().replaceAll("\n", "\n# "));
				}
				writer.print(classMap.get(featureSet.getClassValueForClassifier()) + " qid:" + qid);
				fid = 0;
				for (int i = 0 ; i < featureSet.size() ; i++) {
//					if (filledFeatures.contains(i)) {
						writer.print(" " + (fid+1) + ":" + featureSet.get(i));
						fid++;
//					}
				}
				writer.println();
				recordId++;
			}
			qid++;
		}		
		writer.close();
		return true;
	}
	
	public boolean saveToCRF(RecordList records, File outFileData) throws IOException {
		return this.saveToCRF(records, outFileData, false, false);
	}

	public boolean saveToCRF(RecordList records, File outFileData, boolean addComments) throws IOException {
		return this.saveToCRF(records, outFileData, addComments, false);
	}

	public boolean saveToCRF(RecordList records, File outFileData, boolean addComments, boolean append) throws IOException {
		
		if (!outFileData.getParentFile().isDirectory()) {
			outFileData.getParentFile().mkdirs();
		}

		
		/****************
		 * Write dataset
		 ****************/
		FileWriter fw = new FileWriter(outFileData, append);
		BufferedWriter bw = new BufferedWriter (fw);
		PrintWriter writer = new PrintWriter (bw); 
		Object feature;
		String comment;

		if (addComments) {
			for (int i  = 0 ; i < this.featureNames.size() ; i++) {
				if (!this.features.get(i).isDisabled()) {
					writer.print("# " + (i+1) + " - " + this.featureNames.get(i));
					if (this.features.get(i).getComment() != null) {
						writer.print(" : " + this.features.get(i).getComment().replaceAll("\n", " "));
					} 
					writer.println();
				}
			}
			writer.print("# ");
			for (int i  = 0 ; i < this.featureNames.size() ; i++) {
				if (!this.features.get(i).isDisabled()) {
					writer.print(this.featureNames.get(i) + "\t");
				}
			}
			writer.println();
		}

		
		for (Record record : records) {
			if (record instanceof SeparationRecord) {
				writer.println();
				if (addComments) {
					comment = record.getComment();
					if (comment != null) {
						writer.println("# " + comment);
					}
				}
			} else {
				for (int i = 0 ; i < record.size() ; i++) {
					//				if (filledFeatures.contains(i)) {
					if (!this.features.get(i).isDisabled()) {
						feature = record.get(i);

						if (feature == null) {
							//						if (this.noMissingValue) {
							writer.close();
							throw new RuntimeException("No missing value allowed for parameter " + this.getFeatureName(i) + " in record #" + i + ": \n   " + record.toString());
							//						} else {
							//							writer.print("?\t");
							//						}
						} else {
							writer.print(getValue(feature, i, null) + "\t");
						}
					}
				}
				feature = record.getClassValueForClassifier();
				if (feature == null) {
					writer.close();
					throw new RuntimeException("No missing class value allowed...");
				} else {
					writer.println(feature.toString());
				}
			}
		}
		writer.close();

		return true;
	}
	
	public boolean saveToArff(RecordList records, File outFile) throws IOException {
		FileWriter fw = new FileWriter(outFile, false);
		BufferedWriter bw = new BufferedWriter (fw);
		PrintWriter writer = new PrintWriter (bw); 
		HashSet<Integer> stringFeatureIndexes = new HashSet<Integer>();
		Feature feature;
		// Headers
		writer.println("% ");
		writer.println("% ");
		writer.println("% ");
		writer.println("% ");
		writer.println("@RELATION " + this.relation);
		writer.println();
		int attNumber = 1;
		for (int i  = 0 ; i < this.featureNames.size() ; i++) {
//			if (filledFeatures.contains(i)) {
			feature = this.features.get(i); 
			if (!feature.isDisabled()) {
				if (records.getFactory().getFeatureType(i).equals(String.class)) {
					stringFeatureIndexes.add(i);
				}

				writer.print("%% " + (attNumber));
				if (feature.getComment() != null) {
					writer.print(" - " + feature.getComment().replaceAll("\n", "\n%% "));
				}
				writer.println();
				writer.println("@ATTRIBUTE " + this.secureFeatureName(feature.getName()) + " " + this.getArffType(i));
				attNumber++;
			}
		}
		if (this.classes != null) {
			writer.println("@ATTRIBUTE class {" + StringUtils.join(this.classes, ",") + "}");
		}
		writer.println();
		// Data
		writer.println("@DATA");
		writer.println();
		String comment;
		Object value;
		if (records.getComment() != null) {
			writer.println("% ");
			writer.println("% " + records.getComment().replaceAll("\n", "\n% "));
			writer.println("% ");
		}
		writer.println();
		int recordIndex = 1;
		for (Record record : records) {
			comment = record.getComment();
			if (comment != null) {
				if (record.getClassValueForClassifier() != null) {
					writer.println("% " + recordIndex + " - " + comment + " -> " + record.getClassValueForClassifier());
				} else {
					writer.println("% " + recordIndex + " - " + comment);
				}
			}
			for (int i = 0 ; i < record.size() ; i++) {
//				if (filledFeatures.contains(i)) {
				if (!this.features.get(i).isDisabled()) {
					value = record.get(i);
					
					if (value == null) {
						if (this.noMissingValue) {
							writer.close();
							System.out.println(this.getFeatureName(i));
							throw new RuntimeException("No missing value allowed for parameter " + this.getFeatureName(i) + " in : \n   " + record.toString());
						} else {
							writer.print("?");
						}
					} else {
						writer.print(getValue(value, i, stringFeatureIndexes));
					}
					if (this.classes != null || i < record.size() - 1) {
						writer.print(",");
					}
				}
			}
			// Class
			if (this.classes != null) {
				value = record.getClassValueForClassifier();
				if (value == null) {
//					if (this.noMissingValue) {
//						writer.close();
//						throw new RuntimeException("No missing value allowed...");
//					} else {
					writer.print("?");
//					}
				} else {
					writer.print(value.toString());
				}
			}
			writer.println();
			recordIndex++;
		}
		writer.close();
		return true;
	}	
	
	private static String getValue(Object o, int index, HashSet<Integer> stringFeatureIndexes) {
		if (o instanceof Boolean) {
			if ((Boolean)o) {
				return YES;
			} else {
				return NO;
			}
		} else {
			if (stringFeatureIndexes != null && stringFeatureIndexes.contains(index)) {
				return "\"" + o.toString() + "\"";
			} else {
				return o.toString();
			}
		}
	}
	
	public List<Feature> getFeatures() {
		return this.features;
	}
	
	public List<Feature> getFeatures(String regex) {
		ArrayList<Feature> result = new ArrayList<>();
		Pattern pattern = Pattern.compile(regex);
		for (Feature feature : this.features) {
			if (pattern.matcher(feature.getName()).matches()) {
				result.add(feature);
			}
		}
		return result;
	}
	
	public List<Feature> getFeatures(int tag) {
		ArrayList<Feature> result = new ArrayList<>();
		for (Feature feature : this.features) {
			if (feature.hasTag(tag)) {
				result.add(feature); 
			}
		}
		return result;		
	}
	
	private String getArffType(int index) {
		Class<?> classType = this.getFeatureType(index);

		if (classType.equals(Integer.class)) {
			return "NUMERIC";
		} 
		else if (classType.equals(Double.class)) {
			return "NUMERIC";
		}  
		else if (classType.equals(String.class)) {
			return "STRING";
		}  
		else if (classType.equals(Boolean.class)) {
			return "{" + StringUtils.join(YES_NO_VALUES, ",") + "}";
		}  
//		else if (classType.equals(BooleanCombination.class)) {
//			return "{" + StringUtils.join(((BooleanCombination)this.features.get(index).getType()).getValues(), ",") + "}";
//		}  
		else if (classType.equals(HashSet.class)) {
			@SuppressWarnings("unchecked")
			TreeSet<String> list = new TreeSet<String>((HashSet<String>)this.features.get(index).getType());
			return "{" + StringUtils.join(list, ",") + "}";
		} 
		else {
			throw new RuntimeException("Not Implemented for type " + classType);
		}
	}
	
	
	public String addFeatureValue(int index, Object value, Record record, boolean replace) throws FeatureException {
		if (value == null) {
			return null;
		}
		Class<?> classType = this.getFeatureType(index);
		boolean ok = false;
		if (classType.equals(Integer.class)) {
			if (value instanceof Integer) {
				ok = true;
			}
		} 
		else if (classType.equals(Double.class)) {
			if (value instanceof Double) {
				if (((Double) value).isNaN() || ((Double) value).isInfinite()) {
					throw new FeatureException("Cannot add value " + value + " for feature " + this.getFeatureName(index));
				}
				ok = true;
			}
		}  
		else if (classType.equals(Short.class)) {
			if (value instanceof Short) {
				ok = true;
			}
		}  
		else if (classType.equals(Byte.class)) {
			if (value instanceof Byte) {
				ok = true;
			}
		}  
		else if (classType.equals(String.class)) {
			if (value instanceof String) {
				ok = true;
			}
		}  
		else if (classType.equals(Boolean.class)) {
			if (value instanceof Boolean) {
				ok = true;
			}
		}
//		else if (classType.equals(BooleanCombination.class)) {
//			if (value instanceof String) {
//				for (Object o : ((BooleanCombination)this.features.get(index).getType()).getValues()) {
//					if (value.equals(o)) {
//						ok = true;
//					}
//				}
//			}
//		}
		else if (classType.equals(HashSet.class)) {
			Feature feature = this.features.get(index);
			@SuppressWarnings("unchecked")
			HashSet<String> list = (HashSet<String>)feature.getType();
			if (value instanceof String) {
				if (list.contains(value)) {
					ok = true;
				} else {
					if (((String) value).contains(MISSING_VALUE)) {
						value = MISSING_VALUE;
						ok = true;
					}
				}
			}
			if (value instanceof Number) {
				String strValue = getDiscretizedValue(feature, (Number)value);
				if (strValue != null && list.contains(strValue)) {
					value = strValue;
					ok = true;
				}
			}
			if (!ok) {
				throw new FeatureException("Bad value " + value + ", expecting one of " + list.toString() + " for feature " + this.getFeatureName(index));
			}
		} 
		else {
			throw new FeatureException("Expecting " + classType + ", got " + value.getClass() + " for feature " + this.getFeatureName(index));
		}

		if (ok) {
			// Add current record
			record.add(index, value, replace);
			// If the corresponding feature is involved in
			// some combined features, update them
			HashSet<Integer> featuresToUpdate = this.inverseCombinedFeatures.get(index);
			if (featuresToUpdate != null) {
				for (Integer featureToUpdate : featuresToUpdate) {
					record.updateCombinedFeatureValue(featureToUpdate);
				}
			}
			return this.getFeatureName(index);
		} else {
			throw new FeatureException("Bad class " + value.getClass() + ", expecting " + this.getFeatureType(index) + " for feature " + this.getFeatureName(index));
		}
	}
	
	private String getDiscretizedValue(Feature feature, Number numValue) {
		Number[] discretizationInstruction = feature.getDiscretizationInstruction();
		if (discretizationInstruction == null) {
			return null;
		} else {
			String value = null;
			for (Number n : discretizationInstruction) {
				if (numValue.floatValue() <= n.floatValue()) {
					value = Feature.DISCRETIZED_INTERVAL_PREFIX + n;
					break;
				}
			}
			if (value == null) {
				value = Feature.DISCRETIZED_HIGHEST_VALUE;
			}
			return value;
		}
	}
	
	protected Object getFeatureFromString(int index, String featureValue) throws FeatureException {
		Class<?> classType = this.getFeatureType(index);

		if (classType.equals(Integer.class)) {
			return Integer.parseInt(featureValue);	
		} 
		else if (classType.equals(Double.class)) {
			return Double.parseDouble(featureValue);	
		}  
		else if (classType.equals(String.class)) {
			return featureValue;	
		}  
		else if (classType.equals(HashSet.class)) {
			@SuppressWarnings("unchecked")
			HashSet<String> list = (HashSet<String>)this.features.get(index).getType();
			if (list.contains(featureValue)) {
				return featureValue;
			} else {
				throw new FeatureException("Value " + featureValue + " found while expecting one of: " + list);
			}
		} 
		else {
			throw new FeatureException("Not Implemented for type " + classType);
		}
	}
	
	@SuppressWarnings("unchecked")
	public HashSet<String> getNominalFeatureTypes(int index) {
		return (HashSet<String>)this.features.get(index).getType();
	}

	public boolean isNoMissingValue() {
		return noMissingValue;
	}

	public void setNoMissingValue(boolean noMissingValue) {
		this.noMissingValue = noMissingValue;
	}

	public static String[] getCombinations(Object[] objects) {
		switch (objects.length) {
		case 0:
			return new String[0];
		case 1:
			String[] result = {objects[0].toString()};
			return result;
		case 2:
			return getCombinations(objects[0], objects[1]);
		default:
			Object[] tmpObjects = new Object[objects.length - 1];
			for (int i = 0 ; i < objects.length - 1 ; i++) {
				tmpObjects[i] = objects[i];
			}
			String[] subCp = getCombinations(tmpObjects);
			return getCombinations(subCp, objects[objects.length - 1]);
		} 
	}

	@SuppressWarnings("unchecked")
	public static String[] getCombinations(Object o1, Object o2) {
		String[] list1; 
		String[] list2;
		if (o1 instanceof Boolean) {
			String[] booleans = RecordFactory.YES_NO_VALUES;
			list1 = booleans;
		} else if (o1 instanceof String[]) {
			list1 = (String[]) o1;
		} else if (o1 instanceof HashSet) {
			list1 = ((HashSet<String>) o1).toArray(new String[0]);
		} else {
			throw new RuntimeException("Not implemented with class " + o1.getClass());
		}
		if (o2 instanceof Boolean) {
			String[] booleans = RecordFactory.YES_NO_VALUES;
			list2 = booleans;
		} else if (o2 instanceof String[]) {
			list2 = (String[]) o2;
		} else if (o2 instanceof HashSet) {
			list2 = ((HashSet<String>) o2).toArray(new String[0]);
		} else {
			throw new RuntimeException("Not implemented with class " + o2.getClass());
		}
		String[] result = new String[list1.length * list2.length];
		int index = 0;
		for (int i = 0 ; i < list1.length ; i++) {
			for (int j = 0 ; j < list2.length ; j++) {
				result[index++] = list1[i] + RecordFactory.PAIR_DELIMITER + list2[j];
			}
		}
		return result;
	}
	

	
	public static String getCombinedValue(Object[] values) throws FeatureException {
		if (values == null) {
			return null;
		}
		if (values.length == 0) {
			throw new FeatureException("O-size array can not be considered as a feature value");
		}
		boolean nullValue = false;
		String result = null;
		if (values[0] != null) {
			result = getValue(values[0], -1, null);
			for (int i = 1 ; i < values.length ; i++) {
				if (values[i] == null) {
					nullValue = true;
				} else {
					result += RecordFactory.PAIR_DELIMITER + getValue(values[i], -1, null);
				}
			}
			if (nullValue) {
				result = null;
			}
		}
		return result;
	}
	
	public int setFeatureIndex(Feature feature) {
		return this.features.indexOf(feature);
	}
	
	public static RecordFactory load(File file) throws Exception {
		FileInputStream fis = new FileInputStream(file);
		ObjectInputStream ois = new ObjectInputStream(fis);
		RecordFactory factory = (RecordFactory) ois.readObject();
		ois.close();
//		factory.logger = logger;
		return factory;
	}

	
	public void save(File file) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		try {
			oos.writeObject(this); 
			oos.flush();
		} finally {
			try {
				oos.close();
			} finally {
				fos.close();
			}
		}
	}
	
//	public static void main(String[] args) {
//		String[] tab = new String[2];
//		System.out.println(tab.getClass());
//		System.out.println((new String[0]).getClass());
//	}


}
