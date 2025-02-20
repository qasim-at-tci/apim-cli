package com.axway.apim.appexport.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.axway.apim.adapter.APIManagerAdapter;
import com.axway.apim.adapter.clientApps.ClientAppFilter;
import com.axway.apim.adapter.jackson.ImageSerializer;
import com.axway.apim.adapter.jackson.QuotaRestrictionSerializer;
import com.axway.apim.api.model.Image;
import com.axway.apim.api.model.QuotaRestriction;
import com.axway.apim.api.model.apps.ClientApplication;
import com.axway.apim.appexport.impl.jackson.AppExportSerializerModifier;
import com.axway.apim.appexport.lib.AppExportParams;
import com.axway.apim.appexport.model.ExportApplication;
import com.axway.apim.lib.ExportResult;
import com.axway.apim.lib.StandardExportParams.Wide;
import com.axway.apim.lib.errorHandling.AppException;
import com.axway.apim.lib.errorHandling.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

public class JsonApplicationExporter extends ApplicationExporter {

	public JsonApplicationExporter(AppExportParams params, ExportResult result) {
		super(params, result);
	}

	@Override
	public void export(List<ClientApplication> apps) throws AppException {
		for(ClientApplication app : apps) {
			saveApplicationLocally(new ExportApplication(app));
		}
		return;
	}
	
	private void saveApplicationLocally(ExportApplication app) throws AppException {
		String folderName = getExportFolder(app);
		String targetFolder = params.getTarget();
		File localFolder = new File(targetFolder +File.separator+ folderName);
		LOG.info("Going to export applications into folder: " + localFolder);
		if(localFolder.exists()) {
			if(AppExportParams.getInstance().isDeleteTarget()) {
				LOG.debug("Existing local export folder: " + localFolder + " already exists and will be deleted.");
				try {
					FileUtils.deleteDirectory(localFolder);
				} catch (IOException e) {
					throw new AppException("Error deleting local folder", ErrorCode.UNXPECTED_ERROR, e);
				}				
			} else {
				LOG.warn("Local export folder: " + localFolder + " already exists. Application will not be exported. (You may set -deleteTarget)");
				this.hasError = true;
				return;
			}
		}
		if (!localFolder.mkdirs()) {
			throw new AppException("Cannot create export folder: " + localFolder, ErrorCode.UNXPECTED_ERROR);
		}
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new SimpleModule().setSerializerModifier(new AppExportSerializerModifier(localFolder)));
		mapper.registerModule(new SimpleModule().addSerializer(Image.class, new ImageSerializer()));
		mapper.registerModule(new SimpleModule().addSerializer(QuotaRestriction.class, new QuotaRestrictionSerializer(null)));
		
		FilterProvider filter = new SimpleFilterProvider()
				.setDefaultFilter(SimpleBeanPropertyFilter.serializeAllExcept(new String[] {"id", "apiId", "createdBy", "createdOn", "enabled"}))
				.addFilter("QuotaRestrictionFilter", SimpleBeanPropertyFilter.serializeAllExcept(new String[] {"api", "apiId"})) // Is handled in ExportApplication
				.addFilter("APIAccessFilter", SimpleBeanPropertyFilter.filterOutAllExcept(new String[] {"apiName", "apiVersion"}))
				.addFilter("ApplicationPermissionFilter", SimpleBeanPropertyFilter.serializeAllExcept(new String[] {"userId", "createdBy", "id" }))
				.addFilter("ClientAppCredentialFilter", SimpleBeanPropertyFilter.serializeAllExcept(new String[] {"applicationId", "id", "createdOn", "createdBy"}))
				.addFilter("ClientAppOauthResourceFilter", SimpleBeanPropertyFilter.serializeAllExcept(new String[] {"applicationId", "id", "uriprefix", "scopes", "enabled"}));
		mapper.setFilterProvider(filter);
		mapper.setSerializationInclusion(Include.NON_NULL);
		try {
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			mapper.writeValue(new File(localFolder.getCanonicalPath() + "/application-config.json"), app);
			this.result.addExportedFile(localFolder.getCanonicalPath() + "/application-config.json");
		} catch (Exception e) {
			throw new AppException("Can't write Application-Configuration file for application: '"+app.getName()+"'", ErrorCode.UNXPECTED_ERROR, e);
		}
		if(app.getImage()!=null) {
			writeBytesToFile(app.getImage().getImageContent(), localFolder+File.separator + app.getImage().getBaseFilename());
		}
		LOG.info("Successfully exported application to folder: " + localFolder);
		if(!APIManagerAdapter.hasAdminAccount()) {
			LOG.warn("Export has been done with an Org-Admin account only. Export is restricted by the following: ");
			LOG.warn("- No Quotas has been exported for the API");
			LOG.warn("- No Client-Organizations");
			LOG.warn("- Only subscribed applications from the Org-Admins organization");
		}
	}
	
	private String getExportFolder(ExportApplication app) {
		String appName = app.getName();
		appName = appName.replace(" ", "-");
		return appName;
	}
	
	public static void writeBytesToFile(byte[] bFile, String fileDest) throws AppException {

		try (FileOutputStream fileOuputStream = new FileOutputStream(fileDest)) {
			fileOuputStream.write(bFile);
		} catch (IOException e) {
			throw new AppException("Can't write file", ErrorCode.UNXPECTED_ERROR, e);
		}
	}
	
	public static void storeCaCert(File localFolder, String certBlob, String filename) throws AppException {
		if(certBlob==null) return;
		try {
			writeBytesToFile(certBlob.getBytes(), localFolder + "/" + filename);
		} catch (AppException e) {
			throw new AppException("Can't write certificate to disc", ErrorCode.UNXPECTED_ERROR, e);
		}
	}
	
	protected void removeApplicationDefaultQuota(ClientApplication app) {
		if(app.getAppQuota()==null) return;
		if(app.getAppQuota().getId().equals(APIManagerAdapter.APPLICATION_DEFAULT_QUOTA)) {
			app.setAppQuota(null);
		}
	}

	@Override
	public ClientAppFilter getFilter() throws AppException {
		return getBaseFilterBuilder()
				.includeQuotas(true)
				.includeCredentials(true)
				.includeAPIAccess(true)
				.includeImage(true)
				.includeOauthResources(true)
				.includeAppPermissions(true)
				.build();
	}
}
