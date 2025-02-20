package com.axway.apim.lib;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axway.apim.adapter.APIManagerAdapter.CacheType;

public class StandardImportParams extends CoreParameters {
	
	private static Logger LOG = LoggerFactory.getLogger(StandardImportParams.class);
	
	private String config;
	
	private String stageConfig;
	
	private String enabledCaches;
	
	private List<CacheType> enabledCacheTypes = null;

	public String getConfig() {
		return config;
	}

	public void setConfig(String config) {
		this.config = config;
	}
	
	public String getStageConfig() {
		return stageConfig;
	}

	public void setStageConfig(String stageConfig) {
		this.stageConfig = stageConfig;
	}

	public static synchronized StandardImportParams getInstance() {
		return (StandardImportParams)CoreParameters.getInstance();
	}
	
	@Override
	public boolean isIgnoreCache() {
		// Caches are disabled for import actions if not explicitly enabled
		if(getEnabledCaches()==null || super.isIgnoreCache()) {
			return true;
		} else {
			return false;
		}
	}
	
	public String getEnabledCaches() {
		return enabledCaches;
	}

	@Override
	public List<CacheType> getEnabledCacheTypes() {
		if(isIgnoreCache()) return null;
		if(getEnabledCaches()==null) return null;
		if(enabledCacheTypes!=null) return enabledCacheTypes;
		enabledCacheTypes = createCacheList(getEnabledCaches());
		LOG.warn("Using caches for Import-Actions is BETA. Enable only as many caches as necessary to improve performance and monitor behavior closely. Please read: https://bit.ly/3FjXRXE");
		return enabledCacheTypes;
	}

	public void setEnabledCaches(String enabledCaches) {
		this.enabledCaches = enabledCaches;
	}

	@Override
	public String toString() {
		return "[" + super.toString() + ", config=" + config + "]";
	}
}