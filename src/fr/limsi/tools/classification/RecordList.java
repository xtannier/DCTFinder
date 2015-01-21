package fr.limsi.tools.classification;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;

public class RecordList extends LinkedList<Record> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private transient RecordFactory factory;
	private String comment;
	private Double density;
	
	public RecordList(RecordFactory factory) {
		super();
		this.factory = factory;
		this.density = null;
	}
	
	public RecordList(RecordList copy) {
		this(copy.getFactory());
		this.addAll(copy);
	}

	/**
	 * @return the factory
	 */
	public RecordFactory getFactory() {
		return factory;
	}

	
	public void save(File file) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		
//		System.out.println("FACTURY : " + this.factory);
//		
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
	
	public RecordList filterOutNumeric() {
		List<Feature> features = this.getFactory().getFeatures();
		for (Feature feature : features) {
			if (feature.isNumeric()) {
				feature.setDisabled(true);
			} 
		}
		return this;
	}
	
	public RecordList filterOutNonNumeric() {
		List<Feature> features = this.getFactory().getFeatures();
		for (Feature feature : features) {
			if (!feature.isNumeric()) {
				feature.setDisabled(true);
			} 
		}
		return this;
	}
	
	public RecordList enableTaggedFeatures(int tagsIn) {
		if (tagsIn > 0) {
			List<Feature> features = this.getFactory().getFeatures();
			for (Feature feature : features) {
				if ((feature.getTags() & tagsIn) != 0) {
					feature.setDisabled(false);
				} 
			}
		}
		return this;		
	}

	public RecordList disableTaggedFeatures(int tagsOut) {
		if (tagsOut > 0) {
			List<Feature> features = this.getFactory().getFeatures();
			for (Feature feature : features) {
				if ((feature.getTags() & tagsOut) != 0) {
					feature.setDisabled(true);
				} 
			}
		}
		return this;		
	}

	public RecordList enableOnlyTaggedFeatures(int tagsIn) {
		if (tagsIn > 0) {
			List<Feature> features = this.getFactory().getFeatures();
			for (Feature feature : features) {
				if ((feature.getTags() & tagsIn) == 0) {
					feature.setDisabled(true);
				} 
			}
		}
		return this;
	}
	
	public RecordList enableAllFeatures() {
		List<Feature> features = this.getFactory().getFeatures();
		for (Feature feature : features) {
			feature.setDisabled(false);
		}		
		return this;
	}

	public RecordList disableAllFeatures() {
		List<Feature> features = this.getFactory().getFeatures();
		for (Feature feature : features) {
			feature.setDisabled(true);
		}		
		return this;
	}

	
	public static RecordList load(File file, RecordFactory factory) throws IOException, ClassNotFoundException {
		FileInputStream fis = new FileInputStream(file);
		ObjectInputStream ois = new ObjectInputStream(fis);
		RecordList records = (RecordList) ois.readObject();
		fis.close();
		records.factory = factory;
		
		for (Record record : records) {
			record.factory = factory;
		}
		
		return records;
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
	 * @return the density
	 */
	public Double getDensity() {
		return density;
	}

	/**
	 * @param density the density to set
	 */
	public void setDensity(Double density) {
		this.density = density;
	}

	@Override
	public String toString() {
		String ret = "";
		for (Record record : this) {
			ret += record.toString() + "\n";
		}
		return ret;
	}
}
