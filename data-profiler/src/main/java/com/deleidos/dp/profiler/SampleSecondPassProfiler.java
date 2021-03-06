package com.deleidos.dp.profiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.log4j.Logger;

import com.deleidos.dp.accumulator.AbstractProfileAccumulator;
import com.deleidos.dp.accumulator.BundleProfileAccumulator;
import com.deleidos.dp.beans.ColsEntry;
import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.Detail;
import com.deleidos.dp.beans.Interpretation;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.RegionData;
import com.deleidos.dp.beans.RowEntry;
import com.deleidos.dp.calculations.MatchingAlgorithm;
import com.deleidos.dp.calculations.MetricsCalculationsFacade;
import com.deleidos.dp.deserializors.ConversionUtility;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.dp.exceptions.MainTypeException;
import com.deleidos.dp.exceptions.MainTypeRuntimeException;
import com.deleidos.dp.profiler.api.ProfilerRecord;
import com.deleidos.dp.profiler.api.ProfilingProgressUpdateHandler;
import com.deleidos.dp.reversegeocoding.CoordinateProfile;

/**
 * Profiler for handling a second pass on the sample.  At this time, base metrics and interpretations are known.  This
 * information can be used to generate more valuable metrics, such as histograms.
 * @author leegc
 *
 */
public class SampleSecondPassProfiler extends AbstractReverseGeocodingProfiler<DataSample> {
	private static final Logger logger = Logger.getLogger(SampleSecondPassProfiler.class);
	private ProfilingProgressUpdateHandler progressUpdateListener;
	private DataSample dataSample;
	
	public SampleSecondPassProfiler(DataSample existingSample) {
		initializeWithSampleBean(existingSample);
	}

	protected static List<String> emptyCoordinatePair() {
		return new ArrayList<String>(Arrays.asList(null, null));
	}

	private void initializeWithSampleBean(DataSample sample)  {
		try {
			dataSample = sample;
			accumulatorMapping = new HashMap<String, AbstractProfileAccumulator<?>>(sample.getDsProfile().size());
			for(String key : sample.getDsProfile().keySet()) {
				AbstractProfileAccumulator<?> apa = AbstractProfileAccumulator
						.generateProfileAccumulator(key, sample.getDsProfile().get(key));
				accumulatorMapping.put(key, apa);
			}

			Map<String, Profile> latProfiles = new HashMap<String, Profile>();
			Map<String, Profile> lngProfiles = new HashMap<String, Profile>();

			for(String key : sample.getDsProfile().keySet()) { 
				Profile profile = sample.getDsProfile().get(key);
				if(Interpretation.isLatitude(profile.getInterpretation())) {
					latProfiles.put(key, profile);
				} else if(Interpretation.isLongitude(profile.getInterpretation())) {
					lngProfiles.put(key, profile);
				}
			}

			attemptToMatchLatLongPairs(latProfiles, lngProfiles);
		} catch (MainTypeException e) {
			logger.error("Unexpected main type error during second pass", e);
			throw new MainTypeRuntimeException("Unexpected main type error during second pass.");
		}
	}

	private void attemptToMatchLatLongPairs(Map<String, Profile> latProfiles, Map<String, Profile> lngProfiles) {

		Iterator<String> latProfilesKeyIter = latProfiles.keySet().iterator();
		while(latProfilesKeyIter.hasNext()) {
			String latProfileKey = latProfilesKeyIter.next();
			String highestConfidenceLongMatch = null;
			double highestConfidence = 0;
			for(String lngProfileKey : lngProfiles.keySet()) {
				double stringMatchConfidence = 
						MatchingAlgorithm.jaroWinklerComparison(latProfileKey, lngProfileKey);
				// do string comparison
				// track the highestConfidenceMatch
				// later, maybe implement some sort of position indicator in the profile
				if(stringMatchConfidence > .5) {
					if(stringMatchConfidence > highestConfidence) {
						highestConfidenceLongMatch = lngProfileKey;
						highestConfidence = stringMatchConfidence;
					}
				}
			}
			if(highestConfidenceLongMatch != null) {
				// match found, remove from profile mappings
				CoordinateProfile coordinateProfile = new CoordinateProfile(latProfileKey, highestConfidenceLongMatch);
				int addedIndex = coordinateProfiles.size();
				coordinateProfile.setIndex(addedIndex);
				coordinateProfiles.add(coordinateProfile);

				latProfilesKeyIter.remove();
				lngProfiles.remove(highestConfidenceLongMatch);

			}
		}

		for(String unmatchedLat : latProfiles.keySet()) {
			logger.warn(unmatchedLat + " was an unmatched latitude.");
		}
		for(String unmatchedLng : lngProfiles.keySet()) {
			logger.warn(unmatchedLng + " was an unmatched longitude.");
		}
	}

	public ProfilingProgressUpdateHandler getProgressUpdateListener() {
		return progressUpdateListener;
	}

	public void setProgressUpdateListener(ProfilingProgressUpdateHandler progressUpdateListener) {
		this.progressUpdateListener = progressUpdateListener;
	}
	
	@Override
	protected DataSample subclazzFinish() {
		
		for(String key : accumulatorMapping.keySet()) {
			AbstractProfileAccumulator<?> apa = accumulatorMapping.get(key);
			apa.finish();
			dataSample.getDsProfile().put(key, apa.getState());
		}

		for(CoordinateProfile coordinateProfile : coordinateProfiles) {
			for(String key : Arrays.asList(coordinateProfile.getLatitude(), coordinateProfile.getLongitude())) {
				Profile profile = dataSample.getDsProfile().get(key);
				Detail detail = profile.getDetail();

				RegionData regionData = new RegionData();
				regionData.setCols(Arrays.asList(new ColsEntry("Country"), new ColsEntry("Frequency")));
				List<RowEntry> rows = RegionData.rowsFromMapping(coordinateProfile.getCountryFrequencyMapping());
				regionData.setRows(rows);
				regionData.setLatitudeKey(coordinateProfile.getLatitude());
				regionData.setLongitudeKey(coordinateProfile.getLongitude());

				detail.setRegionDataIfApplicable(regionData);
				profile.setDetail(detail);
			}
		}
		
		dataSample.setDsProfile(
				ConversionUtility.addObjectProfiles(dataSample.getDsProfile()));

		return dataSample; 
	}
	
	@Override
	public void accumulateBinaryRecord(BinaryProfilerRecord binaryRecord) {
		String key = binaryRecord.getBinaryName();
		if (accumulatorMapping.containsKey(key)) {
			List<Object> values = binaryRecord.normalizeRecord().get(key);
			accumulateNormalizedValues(accumulatorMapping.get(key), key, values);
		}
	}

	@Override
	public void accumulateRecord(ProfilerRecord record) {
		Map<String, List<Object>> normalizedRecord = record.normalizeRecord(groupingBehavior);
		normalizedRecord.keySet().stream()
			// must do a null check because empty (all nulls in samples) fields will not have an accumulator
			.filter(accumulatorMapping::containsKey)
			.forEach(key -> accumulateNormalizedValues(accumulatorMapping.get(key), key, normalizedRecord.get(key)));

		Map<Integer, Double[]> matchedPairs = new HashMap<Integer, Double[]>();
		coordinateProfiles.forEach((x) -> matchedPairs.put(x.getIndex(), new Double[]{null, null}));
		for(CoordinateProfile coordinateProfile : coordinateProfiles) {
			List<Object> latValues = normalizedRecord.get(coordinateProfile.getLatitude());
			if(latValues == null) {
				continue;
			}
			List<Object> lngValues = normalizedRecord.get(coordinateProfile.getLongitude());
			if(lngValues == null) {
				continue;
			}
			if(latValues.size() != lngValues.size()) {
				logger.warn("Unequal number of latitudes and longitues.");
				continue;
			} else {
				int size = latValues.size();
				for(int i = 0; i < size; i++) {
					try {
						double lat = Double.valueOf(latValues.get(i).toString());
						double lng = Double.valueOf(lngValues.get(i).toString());
						coordinateProfile.getUndeterminedCoordinateBuffer().add(new Double[]{lat, lng});
						reverseGeocodeQueries++;
						bufferedQueries++;

						if(bufferedQueries >= minimumBatchSize) {
							sendCoordinateProfileBatchesToReverseGeocoder();
							logger.debug(reverseGeocodeQueries + " reverse geocoding queries sent.");
							bufferedQueries = 0;
						}

					} catch (NumberFormatException e) {
						logger.error("Lat/lng not in decimal format.  Currently unsupported.");
					} catch(DataAccessException e) {
						logger.error(e);
						logger.error("Lost "+bufferedQueries+" reverse geocoding queries due to connection issue.");
					}
				}
			}
		}
		recordsLoaded++;
	}

}
