package com.deleidos.dp.beans;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import com.deleidos.dp.deserializors.ConversionUtility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * @author leegc
 * @author yoonj1
 *
 */
public class DataSample {
	private int dataSampleId;
	private String dsGuid;
	private String dsName;
	private String dsFileName;
	private String dsFileType;
	private String dsVersion;
	private int dsFileSize;
	private String dsExtractedContentDir;
	private Timestamp dsLastUpdate;
	private String dsDescription;
	private int recordsParsedCount;
	private Map<String, Profile> DsProfile;
	private List<StructuredNode> dsStructuredProfile;
	private boolean dsContainsStructuredData;

	@JsonProperty("data-sample-id")
	public int getDataSampleId() {
		return dataSampleId;
	}

	@JsonProperty("dsId")
	public String getDsGuid() {
		return dsGuid;
	}

	@JsonProperty("dsName")
	public String getDsName() {
		return dsName;
	}

	@JsonProperty("dsFileName")
	public String getDsFileName() {
		return dsFileName;
	}

	@JsonProperty("dsFileType")
	public String getDsFileType() {
		return dsFileType;
	}
	
	@JsonProperty("dsVersion")
	public String getDsVersion() {
		return dsVersion;
	}

	@JsonProperty("dsLastUpdate")
	public Timestamp getDsLastUpdate() {
		return dsLastUpdate;
	}

	@JsonProperty("dsDescription")
	public String getDsDescription() {
		return dsDescription;
	}

	@JsonProperty("dsProfile")
	public Map<String, Profile> getDsProfile() {
		return DsProfile;
	}

	@JsonProperty("data-sample-id")
	public void setDataSampleId(int dataSampleId) {
		this.dataSampleId = dataSampleId;
	}

	@JsonProperty("dsId")
	public void setDsGuid(String dsGuid) {
		this.dsGuid = dsGuid;
	}

	@JsonProperty("dsName")
	public void setDsName(String dsName) {
		this.dsName = dsName;
	}

	@JsonProperty("dsFileName")
	public void setDsFileName(String dsFileName) {
		this.dsFileName = dsFileName;
	}
	
	@JsonProperty("dsFileType")
	public void setDsFileType(String dsFileType) {
		this.dsFileType = dsFileType;
	}

	@JsonProperty("dsVersion")
	public void setDsVersion(String dsVersion) {
		this.dsVersion = dsVersion;
	}

	@JsonProperty("dsLastUpdate")
	public void setDsLastUpdate(Timestamp dsLastUpdate) {
		this.dsLastUpdate = dsLastUpdate;
	}

	@JsonProperty("dsDescription")
	public void setDsDescription(String dsDescription) {
		this.dsDescription = dsDescription;
	}

	@JsonProperty("dsProfile")
	public void setDsProfile(Map<String, Profile> DsProfile) {
		this.DsProfile = DsProfile;
		this.dsStructuredProfile = ConversionUtility.convertToHeirarchicalList(DsProfile);
		for(StructuredNode node : dsStructuredProfile) {
			if(node.getChildren().size() > 0) {
				dsContainsStructuredData = true;
				return;
			}
		}
		dsContainsStructuredData = false;
	}

	@JsonProperty("dsNumbRecords")
	public int getRecordsParsedCount() {
		return recordsParsedCount;
	}

	@JsonProperty("dsNumbRecords")
	public void setRecordsParsedCount(int recordsParsedCount) {
		this.recordsParsedCount = recordsParsedCount;
	}

	@JsonProperty("dsExtractedContentDir")
	public String getDsExtractedContentDir() {
		return dsExtractedContentDir;
	}

	@JsonProperty("dsExtractedContentDir")
	public void setDsExtractedContentDir(String dsExtractedContentDir) {
		this.dsExtractedContentDir = dsExtractedContentDir;
	}

	public int getDsFileSize() {
		return dsFileSize;
	}

	public void setDsFileSize(int dsFileSize) {
		this.dsFileSize = dsFileSize;
	}

	public List<StructuredNode> getDsStructuredProfile() {
		return dsStructuredProfile;
	}

	public boolean isDsContainsStructuredData() {
		return dsContainsStructuredData;
	}

	public void setDsContainsStructuredData(boolean dsContainsStructuredData) {
		this.dsContainsStructuredData = dsContainsStructuredData;
	}

}
