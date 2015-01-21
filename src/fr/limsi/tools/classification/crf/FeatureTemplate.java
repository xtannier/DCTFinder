package fr.limsi.tools.classification.crf;

import java.util.ArrayList;
import java.util.List;

import fr.limsi.tools.classification.ClassificationException;
import fr.limsi.tools.classification.FeatureException;

public class FeatureTemplate {
	private String name;
	private List<Template> templates;
	private short type;
	private CRFRecordFactory factory;
	private String comment;
	
	public static final short UNIGRAM = 1;
	public static final short BIGRAM = 2;
	public static final short UNIGRAM_BIGRAM = 3;

	public FeatureTemplate(CRFRecordFactory factory, String name, short type) {
		this(factory, name, type, null);
	}
	
	public FeatureTemplate(CRFRecordFactory factory, String name, short type, String comment) {
		this.factory = factory;
		this.name = name;
		this.type = type;
		this.comment = comment;
		this.templates = new ArrayList<Template>();
	}
	
	public FeatureTemplate(CRFRecordFactory factory, String name, short type, String featureName, int position) {
		this(factory, name, type, null, featureName, position);
	}

	public FeatureTemplate(CRFRecordFactory factory, String name, short type, String comment, String featureName, int position) {
		this.factory = factory;
		this.name = name;
		this.type = type;
		this.templates = new ArrayList<Template>();
		this.addFeature(featureName, position);
	}
	
	public String getTemplateStringValue() throws ClassificationException {
		String result;
		switch (this.type) {
		case UNIGRAM:
			result = "u";
			break;
		case BIGRAM:
			result = "b";
			break;
		case UNIGRAM_BIGRAM:
			result = "*";
			break;
		default:
			throw new ClassificationException("Type " + this.type + " unknown");
		}
		result += this.name;
		if (this.templates.size() > 0) {
			result += ":";		
		}
		try {
			Template template;
			for (int templateIndex = 0 ; templateIndex < this.templates.size() ; templateIndex++) {
				template = this.templates.get(templateIndex);
				result += "%x[" +  template.getPosition() + "," + this.factory.getFeatureId(template.getFeature()) + "]";
				if (templateIndex != this.templates.size() - 1) {
					result += "/";
				}
			}
		} catch (FeatureException e) {
			throw new ClassificationException(e);
		}
		return result;
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	public void addFeature(String featureName, int position) {
		this.templates.add(new Template(featureName, position));
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

	private class Template {
		private String featureName;
		private int position;
		
		Template(String featureName, int position) {
			this.featureName = featureName;
			this.position = position;
		}

		/**
		 * @return the feature
		 */
		public String getFeature() {
			return featureName;
		}

		/**
		 * @return the position
		 */
		public int getPosition() {
			return position;
		}
	}
}
