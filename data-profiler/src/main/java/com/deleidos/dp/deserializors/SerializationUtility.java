package com.deleidos.dp.deserializors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.deleidos.dp.beans.DataSample;
import com.deleidos.dp.beans.Profile;
import com.deleidos.dp.beans.Schema;
import com.deleidos.dp.exceptions.SerializationException;
import com.deleidos.dp.profiler.DefaultProfilerRecord;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Convenience class that encapsulates serialization functionality.  Also holds a static instance of an ObjectMapper.
 * @author leegc
 *
 */
public class SerializationUtility {
	private static final Logger logger = Logger.getLogger(SerializationUtility.class);
	private static ObjectMapper objectMapper;
	static {
		objectMapper = new ObjectMapper();
		boolean failOnUnknown = false;
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failOnUnknown);
		logger.info("Object mapper FAIL_ON_UNKNOWN_PROPERTIES is " + failOnUnknown);
		SimpleModule module = new SimpleModule();
		module.addDeserializer(DataSample.class, new DataSampleDeserializer());
		module.addDeserializer(Schema.class, new SchemaDeserializer());
		module.addDeserializer(Profile.class, new ProfileDeserializer());
		objectMapper.registerModule(module);
	}
	public static ObjectMapper getObjectMapper() {
		return objectMapper;
	}
	public static void setObjectMapper(ObjectMapper objectMapper) {
		SerializationUtility.objectMapper = objectMapper;
	}

	public static <T> T deserialize(Object json, TypeReference<T> object) {
		try {
			return objectMapper.readValue(json.toString(), object);
		} catch (IOException e) {
			logger.error(e);
			throw new SerializationException(e);
		}
	}

	public static <T> T deserialize(Object json, Class<T> clazz) {
		try {
			return objectMapper.readValue(json.toString(), clazz);
		} catch (IOException e) {
			logger.error(e);
			throw new SerializationException(e);
		}
	}

	public static String serialize(Object bean) {
		try {
			return objectMapper.writeValueAsString(bean);
		} catch (IOException e) {
			logger.error(e);
			throw new SerializationException(e);
		}
	}
}
