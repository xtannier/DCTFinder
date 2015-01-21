package fr.limsi.tools.classification.crf;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import fr.limsi.tools.classification.ClassificationException;
import fr.limsi.tools.classification.Feature;
import fr.limsi.tools.classification.FeatureException;
import fr.limsi.tools.classification.RecordFactory;

public class CRFRecordFactory extends RecordFactory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// CRF classes
	public final static String CLASS_BEGIN = "B";
	public final static String CLASS_INSIDE = "I";
	public final static String CLASS_OUT = "O";
	public final static String[] classes = {CLASS_BEGIN, CLASS_INSIDE, CLASS_OUT};

	
	private List<FeatureTemplate> featureTemplates;

	public CRFRecordFactory(String relation,
			boolean noMissingValue) {
		this(relation, classes, noMissingValue);
	}
	
	public CRFRecordFactory(String relation) {
		this(relation, classes);
	}

	public CRFRecordFactory(String relation, String[] classes) {
		super(relation, classes);
		this.featureTemplates = new ArrayList<FeatureTemplate>();
	}
	
	public CRFRecordFactory(String relation, String[] classes,
			boolean noMissingValue) {
		super(relation, classes, noMissingValue);
		this.featureTemplates = new ArrayList<FeatureTemplate>();
	}

	public CRFRecordFactory(String relation, String[] classes,
			boolean noMissingValue, List<Feature> features)
			throws FeatureException {
		super(relation, classes, noMissingValue, features);
		this.featureTemplates = new ArrayList<FeatureTemplate>();
	}
	
	public void addFeatureTemplate(FeatureTemplate featureTemplate) {
		this.featureTemplates.add(featureTemplate);
	}
	
	public boolean saveTemplates(File outFile) throws IOException, ClassificationException {
		if (!outFile.getParentFile().isDirectory()) {
			outFile.getParentFile().mkdirs();
		}
		/****************
		 * Write templates
		 ****************/
		FileWriter fw = new FileWriter(outFile, false);
		BufferedWriter bw = new BufferedWriter (fw);
		PrintWriter writer = new PrintWriter (bw);
		for (FeatureTemplate template : this.featureTemplates) {
			writer.println("# " + template.getComment());
			writer.println(template.getTemplateStringValue());
		}
		writer.close();
		return true;
	}
	
}
