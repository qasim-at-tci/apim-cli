package com.axway.apim.lib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axway.apim.adapter.APIManagerAdapter.CacheType;
import com.axway.apim.lib.errorHandling.AppException;
import com.axway.apim.lib.errorHandling.ErrorCode;
import com.axway.apim.lib.utils.TestIndicator;

public class CoreParameters implements Parameters {
	
	private static Logger LOG = LoggerFactory.getLogger(CoreParameters.class);
	
	public enum Mode {
		replace, 
		ignore, 
		add;
		
		public static Mode valueOfDefault(String key) {
			for (Mode e : values()) {
		        if (e.name().equals(key)) {
		            return e;
		        }
		    }
			return Mode.add;
		}
	}
	
	public static String APIM_CLI_HOME = "AXWAY_APIM_CLI_HOME";
	
	public static String DEFAULT_API_BASEPATH = "/api/portal/v1.3";
	
	private String apiManagerUrl = null;
	
	private static CoreParameters instance;
	
	private List<CacheType> cachesToClear = null;
	
	int defaultPort = 8075;
	
	/**
	 * These properties are used for instance to translate key in given config files
	 */
	private Map<String, String> properties;
	
	private String stage;
	
	private String returnCodeMapping;
	
	private String clearCache;
	
	private String hostname;
	
	private String apiBasepath;
	
	private int port = -1;
	
	private String adminUsername;
	
	private String adminPassword;
	
	private String username;
	
	private String password;
	
	private Boolean ignoreAdminAccount = false;
	
	private Boolean allowOrgAdminsToPublish = true;
	
	private Boolean force;
	
	private Boolean ignoreQuotas;
	
	private Boolean zeroDowntimeUpdate;
	
	private Mode quotaMode;
	private Mode clientAppsMode;
	private Mode clientOrgsMode;
	
	private String detailsExportFile;
	
	private Boolean replaceHostInSwagger = true;
	
	private Boolean rollback = true;
	
	private String confDir;
	
	private Boolean ignoreCache = false;
	
	private String apimCLIHome;
	
	private String proxyHost;
	
	private Integer proxyPort;
	
	private String proxyUsername;
	
	private String proxyPassword;
	
	private int retryDelay;
	
	public CoreParameters() {
		super();
		CoreParameters.instance = this;
	}

	public static synchronized CoreParameters getInstance() {
		if(CoreParameters.instance == null && TestIndicator.getInstance().isTestRunning()) {
			try {
				return new CoreParameters(); // Skip this, just return an empty CommandParams to avoid NPE
			} catch (Exception ignore) {}
		}
		return CoreParameters.instance;
	}

	public String getStage() {
		return stage;
	}

	public void setStage(String stage) {
		this.stage = stage;
	}

	public String getReturnCodeMapping() {
		if(returnCodeMapping!=null) return returnCodeMapping;
		return getFromProperties("returnCodeMapping");
	}

	public void setReturnCodeMapping(String returnCodeMapping) {
		this.returnCodeMapping = returnCodeMapping;
	}

	public String getClearCache() {
		if(clearCache!=null) return clearCache;
		return getFromProperties("clearCache");
	}

	public void setClearCache(String clearCache) {
		this.clearCache = clearCache;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getHostname() {
		if(hostname!=null) return hostname;
		return getFromProperties("host");
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	public int getPort() {
		if(port==-1) {
			if(getFromProperties("port")!=null) {
				return Integer.parseInt(getFromProperties("port"));
			} else {
				return defaultPort;
			}
		}
		return port;
	}

	public void setApiBasepath(String apiBasepath) {
		this.apiBasepath = apiBasepath;
	}
	
	public String getApiBasepath() {
		if(apiBasepath==null) return DEFAULT_API_BASEPATH;
		return apiBasepath;
	}

	public String getUsername() {
		if(username!=null) {
			return username;
		} else {
			if(getFromProperties("username")!=null) {
				return getFromProperties("username");
			};
			// Perhaps the admin_username is given
			return getAdminUsername();
		}
	}
	
	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		if(password!=null) {
			return password;
		} else {
			if(getFromProperties("password")!=null) {
				return getFromProperties("password");
			};
			// Perhaps the admin_password is given (hopefully in combination with the admin_username)
			return getAdminPassword();
		}
	}
	
	public void setPassword(String password) {
		this.password = password;
	}

	public String getAdminUsername() {
		if(adminUsername!=null) return adminUsername;
		if(this.properties!=null) {
			return this.properties.get("admin_username");
		}
		return  null;
	}

	public void setAdminUsername(String adminUsername) {
		this.adminUsername = adminUsername;
	}

	public String getAdminPassword() {
		if(adminPassword!=null) return adminPassword;
		if(this.properties!=null) {
			return this.properties.get("admin_password");
		}
		return  null;
	}

	public void setAdminPassword(String adminPassword) {
		this.adminPassword = adminPassword;
	}
	
	public Boolean isForce() {
		if(force!=null) return force;
		return Boolean.parseBoolean(getFromProperties("force"));
	}

	public void setForce(Boolean force) {
		if(force==null) return;
		this.force = force;
	}

	public void setIgnoreQuotas(Boolean ignoreQuotas) {
		if(ignoreQuotas==null) return;
		this.ignoreQuotas = ignoreQuotas;
	}
	
	public Boolean isIgnoreQuotas() {
		if(ignoreQuotas!=null) return ignoreQuotas;
		return Boolean.parseBoolean(getFromProperties("ignoreQuotas"));
	}

	public Mode getQuotaMode() {
		if(quotaMode!=null) return quotaMode;
		if(getFromProperties("quotaMode")!=null) {
			return Mode.valueOf(getFromProperties("quotaMode"));
		}
		return Mode.add;
	}

	public void setQuotaMode(Mode quotaMode) {
		if(quotaMode==null) return;
		this.quotaMode = quotaMode;
	}

	public Boolean isIgnoreClientApps() {
		if(clientAppsMode==Mode.ignore) return true;
		if(getFromProperties("clientAppsMode")!=null) {
			return Boolean.parseBoolean(getFromProperties("clientAppsMode"));
		}
		return false;
	}
	
	public Mode getClientAppsMode() {
		if(clientAppsMode!=null) return clientAppsMode;
		if(getFromProperties("clientAppsMode")!=null) {
			return Mode.valueOf(getFromProperties("clientAppsMode"));
		}
		return Mode.add;
	}

	public void setClientAppsMode(Mode clientAppsMode) {
		if(clientAppsMode==null) return; // Stick with the default
		this.clientAppsMode = clientAppsMode;
	}

	public Boolean isIgnoreClientOrgs() {
		if(clientOrgsMode==Mode.ignore) return true;
		if(getFromProperties("clientOrgsMode")!=null) {
			return Boolean.parseBoolean(getFromProperties("clientOrgsMode"));
		}
		return false;
	}
	
	public Mode getClientOrgsMode() {
		if(clientOrgsMode!=null) return clientOrgsMode;
		if(getFromProperties("clientOrgsMode")!=null) {
			return Mode.valueOf(getFromProperties("clientOrgsMode"));
		}
		return Mode.add;
	}

	public void setClientOrgsMode(Mode clientOrgsMode) {
		if(clientOrgsMode==null) return; // Stick with the default
		this.clientOrgsMode = clientOrgsMode;
	}

	public String getAPIManagerURL() {
		if(apiManagerUrl==null) {
			apiManagerUrl = "https://"+this.getHostname()+":"+this.getPort();
		}
		return apiManagerUrl;
	}
	
	public Boolean isIgnoreAdminAccount() {
		return ignoreAdminAccount;
	}

	public void setIgnoreAdminAccount(Boolean ignoreAdminAccount) {
		if(ignoreAdminAccount==null) return;
		this.ignoreAdminAccount = ignoreAdminAccount;
	}
	
	public Boolean isAllowOrgAdminsToPublish() {
		return allowOrgAdminsToPublish;
	}

	public void setAllowOrgAdminsToPublish(Boolean allowOrgAdminsToPublish) {
		if(allowOrgAdminsToPublish==null) return;
		this.allowOrgAdminsToPublish = allowOrgAdminsToPublish;
	}
	
	public String getDetailsExportFile() {
		return detailsExportFile;
	}

	public void setDetailsExportFile(String detailsExportFile) {
		this.detailsExportFile = detailsExportFile;
	}
	
	public boolean isReplaceHostInSwagger() {
		return replaceHostInSwagger;
	}

	public void setReplaceHostInSwagger(Boolean replaceHostInSwagger) {
		if(replaceHostInSwagger==null) return;
		this.replaceHostInSwagger = replaceHostInSwagger;
	}
	
	public Boolean isRollback() {
		return rollback;
	}

	public void setRollback(Boolean rollback) {
		if(rollback==null) return;
		this.rollback = rollback;
	}
	
	public String getConfDir() {
		return confDir;
	}

	public void setConfDir(String confDir) {
		this.confDir = confDir;
	}

	public boolean isIgnoreCache() {
		return ignoreCache;
	}

	public void setIgnoreCache(Boolean ignoreCache) {
		if(ignoreCache==null) return;
		this.ignoreCache = ignoreCache;
	}

	public String getApimCLIHome() {
		return apimCLIHome;
	}

	public void setApimCLIHome(String apimCLIHome) {
		this.apimCLIHome = apimCLIHome;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public Integer getProxyPort() {
		if(proxyPort == null) return -1;
		return proxyPort;
	}

	public void setProxyPort(Integer proxyPort) {
		this.proxyPort = proxyPort;
	}

	public String getProxyUsername() {
		return proxyUsername;
	}

	public void setProxyUsername(String proxyUsername) {
		this.proxyUsername = proxyUsername;
	}

	public String getProxyPassword() {
		return proxyPassword;
	}

	public void setProxyPassword(String proxyPassword) {
		this.proxyPassword = proxyPassword;
	}
	
	public int getRetryDelay() {
		if(retryDelay==0) return 1000;
		return retryDelay;
	}
	
	public void setRetryDelay(String retryDelay) {
		this.retryDelay = 1000;
		if(retryDelay==null || retryDelay.equals("null")) {
			return;
		}
		try {
			this.retryDelay = Integer.parseInt(retryDelay);
			LOG.info("Retrying unexpected API-Manager REST-API responses with a delay of " + this.retryDelay + " milliseconds.");
		} catch(Exception e) {
			LOG.error("Error while parsing given retryDelay: '"+retryDelay+"' as a milliseconds. Using default of 1000 milliseconds.");
		}
	}

	public void setRetryDelay(int retryDelay) {
		this.retryDelay = retryDelay;
	}

	public Boolean isZeroDowntimeUpdate() {
		if(zeroDowntimeUpdate==null) return false;
		return zeroDowntimeUpdate;
	}

	public void setZeroDowntimeUpdate(Boolean zeroDowntimeUpdate) {
		this.zeroDowntimeUpdate = zeroDowntimeUpdate;
	}

	public List<CacheType> clearCaches() {
		if(getClearCache()==null) return null;
		if(cachesToClear!=null) return cachesToClear;
		cachesToClear = createCacheList(getClearCache());
		return cachesToClear;
	}
	
	protected List<CacheType> createCacheList(String configString) {
		List<CacheType> cachesList = new ArrayList<CacheType>();
		for(String cacheName : configString.split(",")) {
			if(cacheName.equals("ALL")) cacheName = "*";
			cacheName = cacheName.trim();
			if(cacheName.contains("*")) {
				Pattern pattern = Pattern.compile(cacheName.replace("*", ".*").toLowerCase());
				for(CacheType cacheType : CacheType.values()) {
					Matcher matcher = pattern.matcher(cacheType.name().toLowerCase());
					if(matcher.matches()) {
						cachesList.add(cacheType);
						continue;
					}
				}
			} else {
				try {
					cachesList.add(CacheType.valueOf(cacheName));
				} catch (IllegalArgumentException e) {
					LOG.error("Unknown cache: " +cacheName + " configured.");
					LOG.error("Available caches: " + Arrays.asList(CacheType.values()));
				}
			}
		}
		return cachesList;
	}
	
	
	public void validateRequiredParameters() throws AppException {
		if(TestIndicator.getInstance().isTestRunning()) return;
		boolean parameterMissing = false;
		if(getUsername()==null && getAdminUsername()==null) {
			parameterMissing = true;
			LOG.error("Required parameter: 'username' or 'admin_username' is missing.");
		}
		if(getPassword()==null && getAdminPassword()==null) {
			parameterMissing = true;
			LOG.error("Required parameter: 'password' or 'admin_password' is missing.");
		}
		if(getHostname()==null) {
			parameterMissing = true;
			LOG.error("Required parameter: 'host' is missing.");
		}
		if(parameterMissing) {
			LOG.error("Missing required parameters. Use either Command-Line-Options or Environment.Properties to provided required parameters.");
			LOG.error("Get help with option -help");
			LOG.error("");
			throw new AppException("Missing required parameters.", ErrorCode.MISSING_PARAMETER);
		}
	}
	
	public Map<String, String> getProperties() {
		return this.properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}
	
	private String getFromProperties(String key) {
		if(this.properties==null) return null;
		return this.properties.get(key);
	}
	
	public List<CacheType> getEnabledCacheTypes() {
		return null;
	}

	@Override
	public String toString() {
		return "[hostname=" + hostname + ", username=" + username + ", stage=" + stage + "]";
	}
}
