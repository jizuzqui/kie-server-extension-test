package com.bbva.ether.bpmaas.marshalling.json;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonCustomMarshallerHelper {

	public static final String BBVA_MARSHALLING_ENVIRONMENT_ENTRY = "BBVACustomUnmarshallingMode";
	
	private static final Map<String, String> primitiveClassesTranslatorMap;

	static {
		// Adding primitive types as defined in the BPMN file and their equivalent types in Java.
		Map<String, String> classRelationMap = new HashMap<String, String>();
		
		classRelationMap.put("String", "java.lang.String");
		classRelationMap.put("Boolean", "java.lang.Boolean");
		classRelationMap.put("Integer", "java.lang.Integer");
		classRelationMap.put("Float", "java.lang.Float");
		classRelationMap.put("Object", "java.lang.Object");
		
		primitiveClassesTranslatorMap = Collections.unmodifiableMap(classRelationMap);
	}

	/**
	 * @param data String payload in JSON format to be unmarshalled
	 * @param processDefinitionVariables Support map with name and fqcn for each complex variable.
	 * @param classLoader Classloader containing the complex types that will be used for unmarshalling.
	 * @return Unmarshalled map of variables.
	 */
	public static Map<String, Object> unmarshall (String data, Map<String, String> processDefinitionVariables, ClassLoader classLoader) {

		Map<String, Object> outputs = new HashMap<String, Object>();

		ObjectMapper mapper = new ObjectMapper();

		JsonNode rootNode;

		try {
			rootNode = mapper.readTree(data);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		Iterator<Entry<String, JsonNode>> iter = rootNode.fields();

		System.out.println("Starting input payload iteration");
		while (iter.hasNext()) {
			Entry<String, JsonNode> fieldSet = iter.next();

			try {
				// Class name will be 
				//    a) The FQCN as defined in the BPMN, 
				//    b) the primitive type with its FQCN retrieved from primitiveClassesTranslatorMap, or 
				//    c) java.lang.Object if variable is not defined in the BPMN.
				String fieldClassName = resolveClassName(processDefinitionVariables.get(fieldSet.getKey()));
				String fieldName = fieldSet.getKey();
				String fieldValue = fieldSet.getValue().toString();
				System.out.println("About serializing " + fieldName + " with value " + fieldValue + " regarding class " + fieldClassName);

				try {
					outputs.put(fieldName, mapper.readValue(fieldValue, Class.forName(fieldClassName, true, classLoader)));
				} catch (JsonProcessingException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}

		System.out.println("End of input payload iteration");

		return outputs;
	}

	
	public static String marshall (Map<String, Object> variables) {
		
		String result = "";
		
		ObjectMapper mapper = new ObjectMapper();
		
		try {
			result = mapper.writeValueAsString(variables);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		return result;
	}
	
	/**
	 * 
	 * @param initialClassName
	 * @return the class name that will be used to create the instance of an object with reflection.
	 */
	private static String resolveClassName(String initialClassName) {
		if(initialClassName == null) {
			return "java.lang.Object";
		}
		else if(primitiveClassesTranslatorMap.get(initialClassName) != null) {
			return primitiveClassesTranslatorMap.get(initialClassName);
		}
		else {
			return initialClassName;
		}
	}
}
