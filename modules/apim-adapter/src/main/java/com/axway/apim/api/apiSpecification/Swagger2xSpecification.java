package com.axway.apim.api.apiSpecification;

import java.net.MalformedURLException;
import java.net.URL;

import com.axway.apim.api.API;
import com.axway.apim.api.apiSpecification.filter.JsonNodeOpenAPI3SpecFilter;
import com.axway.apim.api.model.APISpecificationFilter;
import com.axway.apim.lib.CoreParameters;
import com.axway.apim.lib.errorHandling.AppException;
import com.axway.apim.lib.errorHandling.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class Swagger2xSpecification extends APISpecification {
	
	JsonNode swagger = null;
	

	public Swagger2xSpecification() {
		super();
	}

	@Override
	public APISpecType getAPIDefinitionType() throws AppException {
		if(this.mapper.getFactory() instanceof YAMLFactory) {
			return APISpecType.SWAGGGER_API_20_YAML;
		}
		return APISpecType.SWAGGGER_API_20;
	}
	
	@Override
	public byte[] getApiSpecificationContent() {
		// Return the original given API-Spec if no filters are applied
		if(this.filterConfig == null) return this.apiSpecificationContent;
		try {
			return mapper.writeValueAsBytes(swagger);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Error parsing API-Specification", e);
		}
	}
	
	@Override
	public void filterAPISpecification() {
		if(this.filterConfig == null) return;
		JsonNodeOpenAPI3SpecFilter.filter(swagger, filterConfig);
	}

	@Override
	public void configureBasepath(String backendBasepath, API api) throws AppException {
		if(!CoreParameters.getInstance().isReplaceHostInSwagger()) return;
		if(backendBasepath==null && swagger.get("host")==null) {
			throw new AppException("The API specification doesn't contain a host and no backend basepath is given.", ErrorCode.CANT_READ_API_DEFINITION_FILE);
		}
		try {
			if(backendBasepath!=null) { 
				boolean backendBasepathAdjusted = false;
				URL url = new URL(backendBasepath);
				String port = url.getPort()==-1 ? ":"+String.valueOf(url.getDefaultPort()) : ":"+String.valueOf(url.getPort());
				if(port.equals(":443") || port.equals(":80")) port = "";
				
				
				if(swagger.get("host")==null) {
					LOG.debug("Adding new host '"+url.getHost()+port+"' to Swagger-File based on backendBasepath: '"+backendBasepath+"'");
					backendBasepathAdjusted = true;
					((ObjectNode)swagger).put("host", url.getHost()+port);
				} else {
					if(swagger.get("host").asText().equals(url.getHost()+port)) {
						LOG.debug("Swagger Host: '"+swagger.get("host").asText()+"' already matches backendBasepath: '"+backendBasepath+"'. Nothing to do.");
					} else {
						LOG.debug("Replacing existing host: '"+swagger.get("host").asText()+"' in Swagger-File to '"+url.getHost()+port+"' based on configured backendBasepath: '"+backendBasepath+"'");
						backendBasepathAdjusted = true;
						((ObjectNode)swagger).put("host", url.getHost()+port);
					}
				}
				if(url.getPath()!=null && !url.getPath().equals("")) {
					String basePath = url.getPath().endsWith("/") ? url.getPath() : url.getPath() + "/";
					if(swagger.get("basePath")!=null && swagger.get("basePath").asText().equals(basePath)) {
						LOG.debug("Swagger basePath: '"+swagger.get("basePath").asText()+"' already matches backendBasepath: '"+basePath+"'. Nothing to do.");
					} else {
						if(swagger.get("basePath")!=null) {
							LOG.debug("Replacing existing basePath: '"+swagger.get("basePath").asText()+"' in Swagger-File to '"+basePath+"' based on configured backendBasepath: '"+backendBasepath+"'");
						} else {
							LOG.debug("Setup basePath in Swagger-File to '"+basePath+"' based on backendBasepath: '"+backendBasepath+"'");
						}
						backendBasepathAdjusted = true;
						((ObjectNode)swagger).put("basePath", basePath);
					}
				}
				 ArrayNode newSchemes = this.mapper.createArrayNode();
				 newSchemes.add(url.getProtocol());
				if(swagger.get("schemes")==null) {
					LOG.debug("Adding protocol: '"+url.getProtocol()+"' to Swagger-Definition");
					backendBasepathAdjusted = true;
					((ObjectNode)swagger).set("schemes", newSchemes);
				} else {
					if(swagger.get("schemes").size()!=1 || !swagger.get("schemes").get(0).asText().equals(url.getProtocol())) {
						LOG.debug("Replacing existing protocol(s): '"+swagger.get("schemes")+"' with '"+url.getProtocol()+"' according to backendBasePath: '"+backendBasepath+"'.");
						backendBasepathAdjusted = true;
						((ObjectNode)swagger).replace("schemes", newSchemes);
					}
				}
				if(backendBasepathAdjusted) {
					LOG.info("Used the backendBasepath: '"+backendBasepath+"' to adjust the API-Specification.");
				}
				this.apiSpecificationContent = this.mapper.writeValueAsBytes(swagger);
			}
		} catch (MalformedURLException e) {
			throw new AppException("The configured backendBasepath: '"+backendBasepath+"' is invalid.", ErrorCode.BACKEND_BASEPATH_IS_INVALID, e);
		} catch (Exception e) {
			LOG.error("Cannot replace host in provided Swagger-File. Continue with given host.", e);
		}
	}

	@Override
	public boolean parse(byte[] apiSpecificationContent) throws AppException {
		try {
			super.parse(apiSpecificationContent);
			setMapperForDataFormat();
			if(this.mapper==null) return false;
			swagger = this.mapper.readTree(apiSpecificationContent);
			if(!(swagger.has("swagger") && swagger.get("swagger").asText().startsWith("2."))) {
				return false;
			}
			return true;
		} catch (AppException e) {
			if(e.getError()==ErrorCode.UNSUPPORTED_FEATURE) {
				throw e;
			}
			return false;
		} catch (Exception e) {
			LOG.trace("Could load specification as Swagger 2.0", e);
			return false;
		}
	}
}
