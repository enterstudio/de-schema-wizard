package com.deleidos.dp.metrics;

import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.deleidos.dp.accumulator.AbstractProfileAccumulator;
import com.deleidos.dp.accumulator.BundleProfileAccumulator;
import com.deleidos.dp.accumulator.NumberProfileAccumulator;
import com.deleidos.dp.beans.NumberDetail;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.StringDetail;
import com.deleidos.dp.calculations.MatchingAlgorithm;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.environ.DPMockUpEnvironmentTest;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.exceptions.MainTypeException;
import com.deleidos.dp.interpretation.InterpretationEngineFacade;
import com.deleidos.hd.enums.DetailType;
import com.deleidos.hd.enums.MainType;

public class MatchingAlgorithmsTest extends DPMockUpEnvironmentTest {
	private Logger logger = Logger.getLogger(MatchingAlgorithmsTest.class);
	private String s1 = "Volume";
	private String s2 = "Vol";
	private String s3 = "lat";
	private String s4 = "lng";
	private String s5 = "waypoints.lat";
	private String s6 = "waypoints.lng";
	
	@Test
	public void jaroWinklerTest() {
		double jwMatch = MatchingAlgorithm.jaroWinklerComparison(s1, s2);
		logger.info(s1 + " and " + s2 + " matched with " + jwMatch + " confidence.");
		assertTrue(jwMatch > .80);
		jwMatch = MatchingAlgorithm.jaroWinklerComparison(s3, s4);
		logger.info(s3 + " and " + s4 + " matched with " + jwMatch + " confidence.");
		assertTrue(jwMatch > .50);
		jwMatch = MatchingAlgorithm.jaroWinklerComparison(s5, s6);
		logger.info(s5 + " and " + s6 + " matched with " + jwMatch + " confidence.");
		assertTrue(jwMatch > .50);
	}
	
	@Test
	public void matchingTest() throws MainTypeException {
		List<Object> values = Arrays.asList("MD","MD","MD","NJ","NJ","NJ","NJ");
		Profile p = BundleProfileAccumulator.generateProfile("Region", values);
		/*StringProfileAccumulator sma = new StringProfileAccumulator("Region", "MD");
		sma.accumulate("MD");
		sma.accumulate("MD");
		sma.accumulate("MD");
		sma.accumulate("NJ");
		sma.accumulate("NJ");
		sma.accumulate("NJ");
		sma.accumulate("NJ");
		sma.finish();*/
		
		List<Object> values2 = Arrays.asList("NJ","NJ","NJ","DE","DE","DE","DE");
		Profile p2 = BundleProfileAccumulator.generateProfile("Rgn", values2);
		/*StringProfileAccumulator sma2 = new StringProfileAccumulator("Rgn", "NJ");
		sma2.accumulate("NJ");
		sma2.accumulate("NJ");
		sma2.accumulate("NJ");

		sma2.accumulate("DE");
		sma2.accumulate("DE");
		sma2.accumulate("DE");
		sma2.accumulate("DE");
		sma2.finish();
		double similarity = MetricsCalculationsFacade.match(sma.getFieldName(), sma.getState(), sma2.getFieldName(), sma2.getState());
		logger.info("Similarity for desired-to-be-matching string fields " + similarity);
		assertTrue(similarity > .80);*/
		
		double newSimilarity = matchProfiles("Region", p, "Rgn", p2);
		logger.info("New similarity is " +newSimilarity);
	}
	
	@Test
	public void noMatchingTest() throws MainTypeException {
		List<Object> values = new ArrayList<Object>();
		//StringProfileAccumulator sma = new StringProfileAccumulator("Name", "John");
		values.add("Greg");
		values.add("Aaron");
		values.add("Jamal");
		values.add("Matt");
		values.add("Jim");
		values.add("Dave");
		values.add("Kevin");
		//sma.finish();
		Profile p = BundleProfileAccumulator.generateProfile("Name", values);
		
		List<Object> values2 = new ArrayList<Object>();
		//StringProfileAccumulator sma2 = new StringProfileAccumulator("Rgn", "NJ");
		values2.add("NJ");
		values2.add("NJ");
		values2.add("NJ");

		values2.add("DE");
		values2.add("DE");
		values2.add("DE");
		values2.add("DE");
		Profile p2 = BundleProfileAccumulator.generateProfile("Rgn", values2);

		//sma2.finish();
		
		double newSimilarity = matchProfiles("Name", p, "Rgn", p2);
		logger.info("New similarity is " +newSimilarity);
		assertTrue(newSimilarity < .80);
	}
	
	@Test
	public void numberMetricsMatchingTest() throws DataAccessException {
		
		Profile numberProfile = new Profile();
		NumberDetail nm = new NumberDetail();
		String profile1Name = "Cls";
		numberProfile.setMainType(MainType.NUMBER.toString());
		nm.setDetailType(DetailType.DECIMAL.toString());
		nm.setMin(BigDecimal.valueOf(2059.19));
		nm.setMax(BigDecimal.valueOf(2096.88));
		nm.setAverage(BigDecimal.valueOf(2080.972));
		nm.setStdDev(10.70483);
		nm.setNumDistinctValues("5");
		numberProfile.setDetail(nm);
		
		String profile2Name = "Close";
		Profile numberProfile2 = new Profile();
		NumberDetail nm2 = new NumberDetail();
		numberProfile2.setMainType(MainType.NUMBER.toString());
		nm2.setDetailType(DetailType.DECIMAL.toString());
		nm2.setMin(BigDecimal.valueOf(2075.37));
		nm2.setMax(BigDecimal.valueOf(2116.4));
		nm2.setAverage(BigDecimal.valueOf(2099.88));
		nm2.setStdDev(7.639143);
		nm2.setNumDistinctValues("5");
		numberProfile2.setDetail(nm2);
		
		double newSimilarity = matchProfiles(profile1Name, numberProfile, profile2Name, numberProfile2);
		logger.info("New similarity is " + newSimilarity);
		assertTrue(newSimilarity > .8);
	}
	
	@Test
	public void latLngTest() throws MainTypeException {
		NumberProfileAccumulator nma = AbstractProfileAccumulator.generateNumberProfileAccumulator("lat");
		nma.accumulate(16.0, true);
		nma.accumulate(20, true);
		nma.finish();
		
		NumberProfileAccumulator nma2 = AbstractProfileAccumulator.generateNumberProfileAccumulator("latitude");
		nma2.accumulate(15.0, true);
		nma2.accumulate(22, true);
		nma2.finish();
		
		double similarity = matchProfiles(nma.getFieldName(), nma.getState(), nma2.getFieldName(), nma2.getState());
		logger.info("Similarity for desired-to-be-matching number fields " + similarity);
		assertTrue(similarity > .80);
	}
	
	@Test
	public void dtgFieldMatchingTest() {
		Profile dtg1 = new Profile();
		dtg1.setMainType(MainType.STRING.toString());
		StringDetail sd = new StringDetail();
		sd.setDetailType(DetailType.PHRASE.toString());
		sd.setAverageLength(14.12);
		sd.setMinLength(13);
		sd.setMaxLength(15);
		sd.setStdDevLength(.74);
		sd.setNumDistinctValues("495");
		dtg1.setDetail(sd);
		
		Profile dtg2 = new Profile();
		dtg2.setMainType(MainType.STRING.toString());
		StringDetail sd2 = new StringDetail();
		sd2.setDetailType(DetailType.PHRASE.toString());
		sd2.setAverageLength(14.32);
		sd2.setMinLength(13);
		sd2.setMaxLength(15);
		sd2.setStdDevLength(.64);
		sd2.setNumDistinctValues("498");
		dtg2.setDetail(sd2);
		
		double d = matchProfiles("DTG", dtg1, "DTG", dtg2);
		logger.info("DTG similarity calculated to be " + d +".");
	}
	
	private Map<String, Profile> profileToMap(String name, Profile profile) {
		Map<String, Profile> profileMap = new HashMap<String, Profile>();
		profileMap.put(name, profile);
		try {
			return InterpretationEngineFacade.getInstance().interpret(null, profileMap, 0);
		} catch (DataAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	private double matchProfiles(String name1, Profile profile1, String name2, Profile profile2) {
		// need to interpretation the profiles because the interpretations need to be set
		// for the matching algorithm
		Map<String, Profile> profileMap1 = profileToMap(name1, profile1);
		Map<String, Profile> profileMap2 = profileToMap(name2, profile2);
		return MatchingAlgorithm.match(name1, profileMap1.get(name1),
				name2, profileMap2.get(name2));
	}
}
