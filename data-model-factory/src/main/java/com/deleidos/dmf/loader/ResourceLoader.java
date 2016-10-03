package com.deleidos.dmf.loader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.tika.mime.MediaType;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.deleidos.dmf.analyzer.workflows.DefinedTestResource;
import com.deleidos.dmf.detector.CEFDetector;
import com.deleidos.dmf.detector.CSVDetector;
import com.deleidos.dmf.detector.JSONDetector;
import com.deleidos.dmf.parser.JNetPcapTikaParser;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.h2.H2DataAccessObject;
import com.deleidos.dp.interpretation.IEConfig;
import com.deleidos.dp.interpretation.InterpretationEngineFacade;
import com.deleidos.hd.h2.H2TestDatabase;

/**
 * Class to load resources for tests.  Test classes that want to use 'defined resources' should extend this class.  For information regarding defined resources, see DefinedTestResource.java
 * @author leegc
 *
 */
public class ResourceLoader {
	private Logger logger = Logger.getLogger(ResourceLoader.class);
	public boolean debug = false;
	public boolean localBulkTest = false;
	protected List<DefinedTestResource> streamSources;

	public void loadToFiles(File file, String expectedDataType, String expectedBodyContentType) throws FileNotFoundException {
		for(String s : file.list()) {
			File f = new File(file, s);
			if(f.isFile()) {
				streamSources.add(new DefinedTestResource(f.getPath(), expectedDataType, expectedBodyContentType, new BufferedInputStream(new FileInputStream(f)), true, true));
			} else {
				loadToFiles(f, f.getName(), null);
			}
		}
	}

	@Before
	public void initFiles() throws FileNotFoundException, UnsupportedEncodingException {
		localBulkTest = false;
		streamSources = new ArrayList<DefinedTestResource>();

	}
	
	protected boolean addToSource(DefinedTestResource dtr) {
		return streamSources.add(dtr);
	}
	
	protected void addToSources(String resourceName, String expectedType) throws UnsupportedEncodingException {
		addToSources(resourceName, expectedType, null, true, true);
	}
	
	protected void addToSources(String resourceName, String expectedType, boolean isDetectorReady, boolean isParserReady) throws UnsupportedEncodingException {
		addToSources(resourceName, expectedType, null, isDetectorReady, isParserReady);
	}

	protected void addToSources(String resourceName, String expectedType, String expectedBodyContentType, boolean isDetectorReady, boolean isParserReady) throws UnsupportedEncodingException {
		if(!resourceName.startsWith("/")) {
			resourceName = "/" + resourceName;
		}
		String path = resourceName;
		path = URLDecoder.decode(path, "UTF8");
		InputStream is = getClass().getResourceAsStream(path);
		if(is == null) {
			logger.error("Resource " + path + " not found.  Ignoring.");
		} else {
			streamSources.add(new DefinedTestResource(path, expectedType, expectedBodyContentType, is, isDetectorReady, isParserReady));
		}
		//sources.put(resourceName, expectedType);
	}
}

