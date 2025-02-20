package com.axway.apim.adapter.apis;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.ehcache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axway.apim.adapter.APIManagerAdapter;
import com.axway.apim.adapter.APIManagerAdapter.CacheType;
import com.axway.apim.adapter.jackson.QuotaRestrictionDeserializer;
import com.axway.apim.adapter.jackson.QuotaRestrictionDeserializer.DeserializeMode;
import com.axway.apim.api.API;
import com.axway.apim.api.model.APIQuota;
import com.axway.apim.api.model.QuotaRestriction;
import com.axway.apim.lib.CoreParameters;
import com.axway.apim.lib.errorHandling.AppException;
import com.axway.apim.lib.errorHandling.ErrorCode;
import com.axway.apim.lib.utils.rest.GETRequest;
import com.axway.apim.lib.utils.rest.PUTRequest;
import com.axway.apim.lib.utils.rest.RestAPICall;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

public class APIManagerQuotaAdapter {
	
	private static Logger LOG = LoggerFactory.getLogger(APIManagerQuotaAdapter.class);
	
	public static enum Quota {
		SYSTEM_DEFAULT ("00000000-0000-0000-0000-000000000000", "System default"), 
		APPLICATION_DEFAULT ("00000000-0000-0000-0000-000000000001", "Application default");
		
		private final String quotaId;
		private final String fiendlyName;
		
		private Quota(String quotaId, String fiendlyName) {
			this.quotaId = quotaId;
			this.fiendlyName = fiendlyName;
		}

		public String getQuotaId() {
			return quotaId;
		}
		public String getFiendlyName() {
			return fiendlyName;
		}
	}
	
	Cache<String, String> applicationsQuotaCache;
	
	ObjectMapper mapper = new ObjectMapper();
	
	CoreParameters cmd = CoreParameters.getInstance();

	public APIManagerQuotaAdapter() {
		applicationsQuotaCache = APIManagerAdapter.getCache(CacheType.applicationsQuotaCache, String.class, String.class);
	}
	
	Map<String, String> apiManagerResponse = new HashMap<String, String>();
	
	private void readQuotaFromAPIManager(String quotaId) throws AppException {
		if(!APIManagerAdapter.hasAdminAccount()) return;
		if(this.apiManagerResponse.get(quotaId)!=null) return;
		URI uri;
		HttpResponse httpResponse = null;
		try {
			if(Quota.APPLICATION_DEFAULT.getQuotaId().equals(quotaId) || Quota.SYSTEM_DEFAULT.getQuotaId().equals(quotaId)) {
				uri = new URIBuilder(cmd.getAPIManagerURL()).setPath(cmd.getApiBasepath() + "/quotas/"+quotaId).build();
			} else {
				if(applicationsQuotaCache.containsKey(quotaId)) {
					LOG.debug("Found quota with ID: " + quotaId + " in cache: " + applicationsQuotaCache.get(quotaId));
					this.apiManagerResponse.put(quotaId, applicationsQuotaCache.get(quotaId));
					return;
				}
				uri = new URIBuilder(cmd.getAPIManagerURL()).setPath(cmd.getApiBasepath() + "/applications/"+quotaId+"/quota/").build();
			}
			RestAPICall getRequest = new GETRequest(uri, true);
			LOG.debug("Load quotas with ID: "+quotaId+" from API-Manager.");
			httpResponse = getRequest.execute();
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			String response = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
			if( statusCode != 200){
				throw new AppException("Can't get API-Manager Quota-Configuration. Got status code: " + statusCode, ErrorCode.API_MANAGER_COMMUNICATION);
			}
			this.apiManagerResponse.put(quotaId,response);
			if(!Quota.APPLICATION_DEFAULT.getQuotaId().equals(quotaId) && !Quota.SYSTEM_DEFAULT.getQuotaId().equals(quotaId)) {
				applicationsQuotaCache.put(quotaId, response);
			}
		} catch (URISyntaxException | UnsupportedOperationException | IOException e) {
			throw new AppException("Can't get API-Manager Quota-Configuration.", ErrorCode.UNXPECTED_ERROR, e);
		} finally {
			try {
				if(httpResponse!=null) 
					((CloseableHttpResponse)httpResponse).close();
			} catch (Exception ignore) {}
		}
	}
	
	/**
	 * Returns the configured quotas for the quota ID from the API manager.
	 * 
	 * @param quotaId is either system or application default or a concrete quota ID
	 * @param api is the quote for an API, then it can be passed here directly. This is then bound to the quote as restrictedAPI
	 * @param addRestrictedAPI If false, then no effort is made to load the restricted api and bind it to the quote during deserialization. In this case the api should be passed. Defaults to true
	 * @param ignoreSystemQuotas If true, then no quotas are returned with the switch: system: true. This is useful if quotas for applications are requested and they should not contain application default quotas.
	 * @return the configured quotas
	 * @throws AppException is something goes wrong.
	 */
	public APIQuota getQuota(String quotaId, API api, boolean addRestrictedAPI, boolean ignoreSystemQuotas) throws AppException {
		if(!APIManagerAdapter.hasAdminAccount()) return null;
		readQuotaFromAPIManager(quotaId); // Quota-ID might be the System- or Application-Default Quota
		APIQuota quotaConfig;
		try {
			mapper.registerModule(new SimpleModule().addDeserializer(QuotaRestriction.class, new QuotaRestrictionDeserializer(DeserializeMode.apiManagerData, addRestrictedAPI)));
			quotaConfig = mapper.readValue(apiManagerResponse.get(quotaId), APIQuota.class);
			if(ignoreSystemQuotas && quotaConfig.getSystem()) return null;
			if(api!=null) {
				quotaConfig = filterQuotaForAPI(quotaConfig, api);
			}
		} catch (IOException e) {
			throw new AppException("Error cant load API-Methods for API: '"+api.getId()+"' from API-Manager.", ErrorCode.API_MANAGER_COMMUNICATION, e);
		}
		return quotaConfig;
	}
	
	public APIQuota saveQuota(APIQuota quotaConfig, String quotaId) throws AppException {
		if(!APIManagerAdapter.hasAdminAccount()) return null;
		URI uri;
		HttpEntity entity;
		HttpResponse httpResponse = null;
		try {
			uri = new URIBuilder(cmd.getAPIManagerURL()).setPath(cmd.getApiBasepath()+"/quotas/"+quotaId).build();
			FilterProvider filter = new SimpleFilterProvider().setDefaultFilter(SimpleBeanPropertyFilter.serializeAllExcept(new String[] {"apiId", "apiName", "apiVersion", "apiPath", "vhost", "queryVersion"}));
			mapper.setFilterProvider(filter);
			entity = new StringEntity(mapper.writeValueAsString(quotaConfig), ContentType.APPLICATION_JSON);
			
			RestAPICall request = new PUTRequest(entity, uri, true);
			httpResponse = request.execute();
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			String response = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
			if(statusCode < 200 || statusCode > 299){
				throw new AppException("Can't update API-Manager Quota-Configuration. Response: '"+response+"'", ErrorCode.API_MANAGER_COMMUNICATION);
			}
			// Force reload of this quota next time
			applicationsQuotaCache.remove(quotaId);
			return mapper.readValue(response, APIQuota.class);
		} catch (URISyntaxException | UnsupportedOperationException | IOException e) {
			throw new AppException("Can't update Quota-Configuration in API-Manager.", ErrorCode.UNXPECTED_ERROR, e);
		} finally {
			try {
				if(httpResponse!=null) 
					((CloseableHttpResponse)httpResponse).close();
			} catch (Exception ignore) {}
		}
	}
	
	public APIQuota getDefaultQuota(Quota quotaType) throws AppException {
		if(!APIManagerAdapter.hasAdminAccount()) return null;
		readQuotaFromAPIManager(quotaType.getQuotaId());
		APIQuota quotaConfig;
		try {
			quotaConfig = mapper.readValue(apiManagerResponse.get(quotaType.getQuotaId()), APIQuota.class);
		} catch (IOException e) {
			throw new AppException("Error cant load default quotas from API-Manager.", ErrorCode.UNXPECTED_ERROR, e);
		}
		return quotaConfig;
	}	
	
	private static APIQuota filterQuotaForAPI(APIQuota quotaConfig, API api) throws AppException {
		List<QuotaRestriction> apiRestrictions = new ArrayList<QuotaRestriction>();
		try {
			for(QuotaRestriction restriction : quotaConfig.getRestrictions()) {
				if(restriction.getApiId().equals(api.getId())) {
					restriction.setRestrictedAPI(api);
					restriction.setApiId(api.getId());
					apiRestrictions.add(restriction);
				}
			}
			if(apiRestrictions.size()==0) return null;
			APIQuota apiQuota = new APIQuota();
			apiQuota.setDescription(quotaConfig.getDescription());
			apiQuota.setName(quotaConfig.getName());
			apiQuota.setRestrictions(apiRestrictions);
			return apiQuota;
		} catch (Exception e) {
			throw new AppException("Can't parse quota from API-Manager", ErrorCode.UNXPECTED_ERROR, e);
		}
	}
	
	void setAPIManagerTestResponse(String apiId, String response) {
		this.apiManagerResponse.put(apiId, response);
	}
}
