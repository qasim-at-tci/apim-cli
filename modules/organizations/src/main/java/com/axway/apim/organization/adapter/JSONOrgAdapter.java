package com.axway.apim.organization.adapter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.axway.apim.adapter.APIManagerAdapter;
import com.axway.apim.adapter.apis.APIFilter;
import com.axway.apim.adapter.apis.APIManagerAPIAdapter;
import com.axway.apim.api.API;
import com.axway.apim.api.model.APIAccess;
import com.axway.apim.api.model.CustomProperties.Type;
import com.axway.apim.api.model.Image;
import com.axway.apim.api.model.Organization;
import com.axway.apim.lib.Result;
import com.axway.apim.lib.errorHandling.AppException;
import com.axway.apim.lib.errorHandling.ErrorCode;
import com.axway.apim.lib.utils.Utils;
import com.axway.apim.organization.lib.OrgImportParams;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

public class JSONOrgAdapter extends OrgAdapter {
	
	private ObjectMapper mapper = new ObjectMapper();
	
	OrgImportParams importParams;

	public JSONOrgAdapter(OrgImportParams params) {
		this.importParams = params;
		this.result = new Result(); // Not used, to be refactored
	}

	public void readConfig() throws AppException {
		String config = importParams.getConfig();
		String stage = importParams.getStage();

		File configFile = Utils.locateConfigFile(config);
		if(!configFile.exists()) return;
		File stageConfig = Utils.getStageConfig(stage, importParams.getStageConfig(), configFile);
		List<Organization> baseOrgs;
		// Try to read a list of organizations
		try {
			baseOrgs = mapper.readValue(Utils.substitueVariables(configFile), new TypeReference<List<Organization>>(){});
			if(stageConfig!=null) {
				throw new AppException("Stage overrides are not supported for organization lists.", ErrorCode.CANT_READ_CONFIG_FILE);
			} else {
				this.orgs = baseOrgs;
			}
		// Try to read single organization
		} catch (MismatchedInputException me) {
			try {
				Organization org = mapper.readValue(Utils.substitueVariables(configFile), Organization.class);
				if(stageConfig!=null) {
					try {
						ObjectReader updater = mapper.readerForUpdating(org);
						org = updater.readValue(Utils.substitueVariables(stageConfig));
					} catch (FileNotFoundException e) {
						LOG.warn("No config file found for stage: '"+stage+"'");
					}
				}
				this.orgs = new ArrayList<Organization>();
				this.orgs.add(org);
			} catch (Exception pe) {
				throw new AppException("Cannot read organization(s) from config file: " + config, ErrorCode.ACCESS_ORGANIZATION_ERR, pe);
			}
		} catch (Exception e) {
			throw new AppException("Cannot read organization(s) from config file: " + config, ErrorCode.ACCESS_ORGANIZATION_ERR, e);
		}
		try{
			addImage(orgs, configFile.getCanonicalFile().getParentFile());
		}catch (Exception e){
			throw new AppException("Cannot read image for organization(s) from config file: " + config, ErrorCode.ACCESS_ORGANIZATION_ERR, e);
		}
		addAPIAccess(orgs, result);
		validateCustomProperties(orgs);
		return;
	}
	
	private void addImage(List<Organization> orgs, File parentFolder) throws AppException {
		for(Organization org : orgs) {
			if(org.getImageUrl()==null || org.getImageUrl().equals("")) continue;
			org.setImage(Image.createImageFromFile(new File(parentFolder + File.separator + org.getImageUrl())));
		}
	}
	
	private void addAPIAccess(List<Organization> orgs, Result result) throws AppException {
		APIManagerAPIAdapter apiAdapter = APIManagerAdapter.getInstance().apiAdapter;
		for(Organization org : orgs) {
			if(org.getApiAccess()==null) continue;
			Iterator<APIAccess> it = org.getApiAccess().iterator();
			while(it.hasNext()) {
				APIAccess apiAccess = it.next();
				List<API> apis = apiAdapter.getAPIs(new APIFilter.Builder()
						.hasName(apiAccess.getApiName())
						.build()
				, false);
				if(apis==null || apis.size()==0) {
					LOG.error("API with name: '" + apiAccess.getApiName() + "' not found. Ignoring this APIs.");
					result.setError(ErrorCode.UNKNOWN_API);
					it.remove();
					continue;
				}
				if(apis.size()>1 && apiAccess.getApiVersion()==null) {
					LOG.error("Found: "+apis.size()+" APIs with name: " + apiAccess.getApiName() + " not providing a version. Ignoring this APIs.");
					result.setError(ErrorCode.UNKNOWN_API);
					it.remove();
					continue;
				}
				API api = apis.get(0);
				apiAccess.setApiId(api.getId());
			}
		}
	}
	
	private void validateCustomProperties(List<Organization> orgs) throws AppException {
		for(Organization org : orgs) {
			Utils.validateCustomProperties(org.getCustomProperties(), Type.organization);
		}
	}
}
