package com.deleidos.dp.h2;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.tools.DeleteDbFiles;
import org.h2.tools.Server;

import com.deleidos.dp.beans.BinaryDetail;
import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.DataSampleMetaData;
import com.deleidos.dp.beans.Detail;
import com.deleidos.dp.beans.NumberDetail;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.beans.SchemaMetaData;
import com.deleidos.dp.beans.StringDetail;
import com.deleidos.dp.exceptions.DataAccessException;
import com.deleidos.hd.h2.H2Config;
import com.deleidos.hd.h2.H2Database;
import com.deleidos.hd.h2.H2TestDatabase;

/**
 * Data access object to persist and retrieve schemas, samples, and metrics from
 * the H2 server.
 * 
 * @author leegc
 * @author yoonj1
 *
 */
public class H2DataAccessObject {
	public static final Logger logger = Logger.getLogger(H2DataAccessObject.class);
	private H2Database h2Database;
	private boolean isLive;
	protected static H2DataAccessObject h2Dao = null;
	private H2MetricsDataAccessObject h2Metrics;
	private H2SampleDataAccessObject h2Samples;
	private H2SchemaDataAccessObject h2Schema;
	public static final boolean debug = false;

	private H2DataAccessObject(H2Database h2Database) {
		this.h2Database = h2Database;
		h2Metrics = new H2MetricsDataAccessObject(this);
		h2Samples = new H2SampleDataAccessObject(this);
		h2Schema = new H2SchemaDataAccessObject(this);
	}

	/**
	 * Get or instantiate the static instance of the H2 Data Access Object.
	 * 
	 * @return The static H2DataAccessObject
	 * @throws DataAccessException 
	 * @throws IOException 
	 */
	public static H2DataAccessObject getInstance() throws DataAccessException {
		if (h2Dao == null) {
			try {
				h2Dao = new H2DataAccessObject(new H2Database());
			} catch (IOException e) {
				logger.error("Could not find configuration file.");
				logger.error(e);
			}
		}
		return h2Dao;
	}

	public static H2DataAccessObject setInstance(H2Database database) throws DataAccessException {
		h2Dao = new H2DataAccessObject(database);
		return h2Dao;
	}

	/**
	 * Remove all files in the database directory with the database name.
	 */
	public void purge() {
		h2Database.purge();
	}

	/**
	 * Return the generated key from a statement (H2 only allows a maximum of
	 * one to be returned per query). Calling this method will not execute the
	 * statement.
	 * 
	 * @param stmt
	 *            The executed statement
	 * @return The key generated by executing this statement
	 * @throws SQLException
	 *             Thrown if there is an error in the query.
	 */
	public int getGeneratedKey(Statement stmt) throws SQLException {
		ResultSet gKeys = stmt.getGeneratedKeys();
		int fieldId = -1;
		if (gKeys.next()) {
			fieldId = gKeys.getInt(1);
			stmt.close();
		} else {
			stmt.close();
			return -1;
		}
		return fieldId;
	}

	/**
	 * Run a query in H2.
	 * 
	 * @param sql
	 *            The string of the SQL query.
	 * @return The result set from executing this query.
	 * @throws SQLException
	 *             Thrown if there is an error in the query.
	 */
	public ResultSet query(Connection dbConnection, String sql) throws SQLException {
		if (debug) {
			return queryWithOutput(dbConnection, sql);
		} else {
			ResultSet rs = dbConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)
					.executeQuery(sql);
			dbConnection.close();
			return rs;
		}
	}

	/**
	 * Run a query in H2 and log the output at the debug level.
	 * 
	 * @param sql
	 *            The string of the SQL query.  It is the callers job to close the conneciton.
	 * @return The result set from executing this query.
	 * @throws SQLException
	 *             Thrown if there is an error in the query.
	 */
	public ResultSet queryWithOutput(Connection dbConnection, String sql) throws SQLException {
		ResultSet rs = query(dbConnection, sql);
		ResultSetMetaData rsmd = rs.getMetaData();
		int c = rsmd.getColumnCount();
		StringBuilder sb = new StringBuilder();
		logger.info(sql);
		for (int i = 1; i <= c; i++) {
			sb.append(rsmd.getColumnName(i) + "\t");
		}
		logger.info(sb);
		while (rs.next()) {
			StringBuilder s = new StringBuilder();
			for (int i = 1; i <= c; i++) {
				s.append(rs.getString(i) + "\t");
			}
			logger.info(s.toString());
		}
		rs.beforeFirst();
		dbConnection.close();
		return rs;
	}

	/**
	 * Add a schema to H2.
	 * 
	 * @param schemaBean
	 *            the schema object as a bean
	 * @return The guid of the schema
	 * @throws DataAccessException 
	 */
	public String addSchema(Schema schemaBean) throws DataAccessException {
		if (schemaBean.getsName() == null) {
			schemaBean.setsName(schemaBean.getsGuid());
		}
		if (schemaBean.getsDescription() == null) {
			schemaBean.setsDescription("This is a schema generated by the Schema Wizard.");
		}
		if (schemaBean.getsVersion() == null) {
			schemaBean.setsVersion("1.0");
		}
		schemaBean.setsLastUpdate(Timestamp.from(Instant.now()));
		try {
			Connection connection = h2Database.getNewConnection();
			h2Schema.addSchema(connection, schemaBean);
			connection.close();
			return schemaBean.getsGuid();
		} catch (SQLException e) {
			logger.error("SQL Exception adding schema.", e);
			throw new DataAccessException("SQL Exception adding schema.", e);
		}
	}

	/**
	 * Update a schema in H2. TODO
	 * 
	 * @param schemaBean
	 *            the schema object as a bean
	 * @return The guid of the schema
	 */
	//	public String updateSchema(Schema schemaBean) {
	//		h2Schema.updateSchema(schemaBean);
	//		
	//		for (Profile : schemaBean.getsProfile())
	//		
	//		return schemaBean.getsGuid();
	//	}

	/**
	 * Get a list of the schema meta data in H2
	 * 
	 * @return a list of SchemaMetaData beans
	 * @throws DataAccessException 
	 */
	public List<SchemaMetaData> getAllSchemaMetaData() throws DataAccessException {
		try {
			Connection connection = h2Database.getNewConnection();
			List<SchemaMetaData> schemaMetaData = h2Schema.getAllSchemaMetaData(connection);
			connection.close();
			return schemaMetaData;
		} catch(SQLException e) {
			logger.error("Error retrieving all schema meta data.", e);
			throw new DataAccessException("Error retrieving all schema meta data.", e);
		}
	}

	/**
	 * Get a list of the sample meta data in H2
	 * 
	 * @return a list of SampleMetaDataBeans
	 * @throws DataAccessException 
	 */
	public List<DataSampleMetaData> getAllSampleMetaData() throws DataAccessException {
		try {
			Connection connection = h2Database.getNewConnection();
			List<DataSampleMetaData> dsMetaData = h2Samples.getAllSampleMetaData(connection);
			connection.close();
			return dsMetaData;
		} catch(SQLException e) {
			logger.error("Error retrieving all sample meta data.", e);
			throw new DataAccessException("Error retrieving all sample meta data.", e);
		}
	}

	/**
	 * Get a specific schema meta data object by its GUID
	 * 
	 * @param guid
	 *            the desired guid
	 * @return the SchemaMetaData bean that coincides with the GUID
	 * @throws DataAccessException 
	 */
	public SchemaMetaData getSchemaMetaDataByGuid(String guid) throws DataAccessException {
		try {
			Connection connection = h2Database.getNewConnection();
			SchemaMetaData schemaMetaData = new SchemaMetaData();
			schemaMetaData = h2Schema.getSchemaMetaDataByGuid(connection, guid);
			connection.close();
			return schemaMetaData;
		} catch(SQLException e) {
			logger.error("SQL error deleting schema metadata with guid "+guid+".", e);
			throw new DataAccessException("SQL error deleting schema metadata with guid "+guid+".", e);
		}

	}

	/**
	 * Get a schema by its guid
	 * 
	 * @param guid
	 *            the schema's guid
	 * @param showHistogram
	 *            true if histogram data should be displayed, false if it should
	 *            be removed
	 * @return the Schema bean
	 * @throws DataAccessException 
	 */
	public Schema getSchemaByGuid(String guid, boolean showHistogram) throws DataAccessException {
		if(guid == null) {
			return null;
		}
		try {
			Connection connection = h2Database.getNewConnection();
			Schema schema = h2Schema.getSchemaByGuid(connection, guid);

			if (!showHistogram) {
				for (String key : schema.getsProfile().keySet()) {
					Profile profile = schema.getsProfile().get(key);
					Detail detail = profile.getDetail();
					if (detail instanceof NumberDetail) {
						NumberDetail nm = ((NumberDetail) Profile.getNumberDetail(profile));
						nm.setFreqHistogram(null);
					} else if (detail instanceof StringDetail) {
						StringDetail sm = ((StringDetail) Profile.getStringDetail(profile));
						sm.setTermFreqHistogram(null);
					} else if (detail instanceof BinaryDetail) {
						logger.error("Detected as binary in " + getClass().getName() + ".");
					}
					profile.setDetail(detail);
				}
			}
			connection.close();
			return schema;
		} catch(SQLException e) {
			logger.error("SQL error getting schema with guid "+guid+".");
			throw new DataAccessException("SQL error getting schema with guid "+guid+".", e);
		}
	}

	/**
	 * Gets the field-descriptor
	 * 
	 * @param guid
	 *            The Schema's Guid
	 * @param showHistogram
	 *            True if histogram data should be displayed; False if it should
	 *            be removed
	 * @return
	 * @throws DataAccessException 
	 */
	public Map<String, Profile> getSchemaFieldByGuid(String guid, boolean showHistogram) throws DataAccessException {

		try {
			Connection connection = h2Database.getNewConnection();
			Map<String, Profile> map = new HashMap<String, Profile>();
			map = h2Schema.getSchemaFieldByGuid(connection, guid);

			if (!showHistogram) {
				for (String key : map.keySet()) {
					Profile profile = map.get(key);
					Detail detail = profile.getDetail();
					if (detail instanceof NumberDetail) {
						NumberDetail nm = ((NumberDetail) Profile.getNumberDetail(profile));
						nm.setFreqHistogram(null);
					} else if (detail instanceof StringDetail) {
						StringDetail sm = ((StringDetail) Profile.getStringDetail(profile));
						sm.setTermFreqHistogram(null);
					} else if (detail instanceof BinaryDetail) {
						logger.error("Detected as binary in " + getClass().getName() + ".");
					}
					profile.setDetail(detail);
				}
			}
			connection.close();
			return map;
		} catch(SQLException e) {
			logger.error("SQL error getting schema fields for guid "+guid+".", e);
			throw new DataAccessException("SQL error getting schema fields for guid "+guid+".", e);
		}
	}

	/**
	 * Delete schema based on its guid
	 * 
	 * @param guid
	 * @throws DataAccessException 
	 */
	public void deleteSchemaFromDeletionQueue(String guid) throws DataAccessException {
		try {
			Connection connection = h2Database.getNewConnection();
			logger.info("Deleting schema " + guid +" from database.");
			h2Schema.deleteSchemaFromDeletionQueue(connection, guid);
			connection.close();
		} catch(SQLException e) {
			logger.error("SQL error deleting schema with guid "+guid+".", e);
			throw new DataAccessException("SQL error deleting schema with guid "+guid+".", e);
		}
	}

	/**
	 * Add a sample
	 * 
	 * @param sample
	 *            the DataSample bean to be added
	 * @throws DataAccessException 
	 */
	public String addSample(DataSample sample) throws DataAccessException {
		try {
			Connection connection = h2Database.getNewConnection();
			String resultingGuid = h2Samples.addSample(connection, sample);
			connection.close();
			return resultingGuid;
		} catch(SQLException e) {
			logger.error("Sql error adding sample.", e);
			throw new DataAccessException("Sql error adding sample.", e);
		}
	}

	/**
	 * Call underlying H2SampleDAO class to retrieve a mapping of all the sample
	 * names to their respective media types in the database.
	 * 
	 * @return
	 * @throws DataAccessException 
	 */
	public Map<String, String> getExistingSampleNames(Connection dbConnection) throws DataAccessException {
		try {
			Map<String, String> existingSampleNames = h2Samples.getExistingSampleNames(dbConnection);
			return existingSampleNames;
		} catch (SQLException e) {
			logger.error("Sql error getting existing sample names.", e);
			throw new DataAccessException("Sql error getting existing sample names.", e);
		}
	}

	/**
	 * Get a list of samples by their guids
	 * 
	 * @param guids
	 *            ordered list of guids
	 * @return an ordered list of DataSample beans
	 * @throws DataAccessException 
	 * @throws SQLException
	 */
	public List<DataSample> getSamplesByGuids(String[] guids) throws DataAccessException {
		try {
			Connection connection = h2Database.getNewConnection();
			List<DataSample> samples = new ArrayList<DataSample>();
			for (String guid : guids) {
				samples.add(h2Samples.getSampleByGuid(connection, guid));
			}
			connection.close();
			return samples;
		} catch(SQLException e) {
			logger.error("SQL error getting multiple samples.", e);
			throw new DataAccessException("SQL error getting multiple samples.", e);
		}
	}

	/**
	 * Gets a given Data Sample bean given its Guid
	 * 
	 * @param guid
	 * @return
	 * @throws DataAccessException 
	 */
	public DataSample getSampleByGuid(String guid) throws DataAccessException {
		try {
			Connection connection = h2Database.getNewConnection();
			DataSample sample = new DataSample();
			sample = h2Samples.getSampleByGuid(connection, guid);
			connection.close();
			return sample;
		} catch(SQLException e) {
			logger.error("SQL error getting sample with guid "+guid+".", e);
			throw new DataAccessException("SQL error getting sample with guid "+guid+".", e);
		}
	}

	/**
	 * Gets a Data Sample Meta Data bean given its Guid
	 * 
	 * @param guid
	 * @return
	 * @throws DataAccessException 
	 */
	public DataSampleMetaData getSampleMetaDataByGuid(String guid) throws DataAccessException {
		try {
			Connection connection = h2Database.getNewConnection();
			DataSampleMetaData sampleMetaData;
			sampleMetaData = h2Samples.getDataSampleMetaDataByGuid(connection, guid);
			connection.close();
			return sampleMetaData;
		} catch(SQLException e) {
			logger.error("SQL error getting sample metadata for guid "+guid+".", e);
			throw new DataAccessException("SQL error getting sample metadata for guid "+guid+".", e);
		}
	}

	/**
	 * Gets the field-descriptor
	 * 
	 * @param guid
	 * @return
	 * @throws DataAccessException 
	 */
	public Map<String, Profile> getSampleFieldByGuid(String guid, boolean showHistogram) throws DataAccessException {

		try {
			Connection connection = h2Database.getNewConnection();
			Map<String, Profile> map = new HashMap<String, Profile>();
			map = h2Samples.getSampleFieldByGuid(connection, guid);

			if (!showHistogram) {
				for (String key : map.keySet()) {
					Profile profile = map.get(key);
					Detail detail = profile.getDetail();
					if (detail instanceof NumberDetail) {
						NumberDetail nm = ((NumberDetail) Profile.getNumberDetail(profile));
						nm.setFreqHistogram(null);
					} else if (detail instanceof StringDetail) {
						StringDetail sm = ((StringDetail) Profile.getStringDetail(profile));
						sm.setTermFreqHistogram(null);
					} else if (detail instanceof BinaryDetail) {
						logger.error("Detected as binary in " + getClass().getName() + ".");
					}
					profile.setDetail(detail);
				}
			}
			connection.close();
			return map;
		} catch(SQLException e) {
			logger.error("SQL error populating profile for sample "+guid+".", e);
			throw new DataAccessException("SQL error populating profile for sample "+guid+".", e);
		}
	}

	public void deleteSchemaByGuid(String guid) throws DataAccessException {
		try {
			Connection connection = h2Database.getNewConnection();
			h2Schema.deleteSchemaByGuid(connection, guid);
			connection.close();
		} catch(SQLException e) {
			logger.error("SQL error deleting schema with guid "+guid+".", e);
			throw new DataAccessException("SQL error deleting schema with guid "+guid+".", e);
		}
	}

	public void deleteSampleByGuid(String guid) throws DataAccessException {
		try {
			Connection connection = h2Database.getNewConnection();
			h2Samples.deleteSampleByGuid(connection, guid);
			connection.close();
		} catch(SQLException e) {
			logger.error("SQL error deleting sample with guid " +guid+ ".", e);
			throw new DataAccessException("SQL error deleting sample with guid " +guid+ ".", e);
		}
	}

	/**
	 * Has logic to determine if a GUID is a Schema or Data Sample.
	 * 
	 * @param guid
	 *            An ambiguous GUID belonging to either a Schema or Data Sample
	 * @throws DataAccessException 
	 */
	public void deleteByGuid(String guid) throws DataAccessException {
		try {
			Connection connection = h2Database.getNewConnection();
			Schema schema = h2Schema.getSchemaByGuid(connection, guid);
			DataSample sample = h2Samples.getSampleByGuid(connection, guid);

			if (schema != null) {
				h2Schema.deleteSchemaByGuid(connection, guid);
			} else if (sample != null) {
				h2Samples.deleteSampleByGuid(connection, guid);
			} else {
				connection.close();
				logger.error("No such guid exists in the database.");
				throw new DataAccessException("Error finding guid in H2 database");
			}
			connection.close();
		} catch(SQLException e) {
			logger.error("Error finding guid in H2 database", e);
			throw new DataAccessException("Error finding guid in H2 database", e);
		}
	}

	public static void setH2DAO(H2DataAccessObject h2dao) {
		h2Dao = h2dao;
	}

	public H2MetricsDataAccessObject getH2Metrics() {
		return h2Metrics;
	}

	public H2SampleDataAccessObject getH2Samples() {
		return h2Samples;
	}

	public H2SchemaDataAccessObject getH2Schema() {
		return h2Schema;
	}

	public boolean testConnection(Connection conn) {
		try{
			isLive = conn.isValid(5);
			return isLive;
		} catch(SQLException e) {
			return false;
		}
	}

	public boolean isLive() {
		return isLive;
	}

	public H2Database getH2Database() {
		return h2Database;
	}

	public void setH2Database(H2Database h2Database) {
		this.h2Database = h2Database;
	}
}
