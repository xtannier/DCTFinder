package fr.limsi.tools.common.files;

import java.io.File;
import java.io.FileFilter;

public class HtmlFileFilter implements FileFilter {

	@Override
	public boolean accept(File file) {
		return file.getName().endsWith(".html") || file.getName().endsWith(".htm");
	}
	
}
