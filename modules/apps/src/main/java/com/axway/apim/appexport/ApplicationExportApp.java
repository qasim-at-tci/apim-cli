package com.axway.apim.appexport;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axway.apim.adapter.APIManagerAdapter;
import com.axway.apim.adapter.clientApps.APIMgrAppsAdapter;
import com.axway.apim.api.model.apps.ClientApplication;
import com.axway.apim.appexport.impl.ApplicationExporter;
import com.axway.apim.appexport.impl.ApplicationExporter.ResultHandler;
import com.axway.apim.appexport.lib.AppExportCLIOptions;
import com.axway.apim.appexport.lib.AppExportParams;
import com.axway.apim.cli.APIMCLIServiceProvider;
import com.axway.apim.cli.CLIServiceMethod;
import com.axway.apim.lib.ExportResult;
import com.axway.apim.lib.errorHandling.AppException;
import com.axway.apim.lib.errorHandling.ErrorCode;
import com.axway.apim.lib.errorHandling.ErrorCodeMapper;
import com.axway.apim.lib.utils.rest.APIMHttpClient;

public class ApplicationExportApp implements APIMCLIServiceProvider {

	private static Logger LOG = LoggerFactory.getLogger(ApplicationExportApp.class);

	static ErrorCodeMapper errorCodeMapper = new ErrorCodeMapper();

	@Override
	public String getName() {
		return "Application - E X P O R T / U T I L S";
	}

	@Override
	public String getVersion() {
		return ApplicationExportApp.class.getPackage().getImplementationVersion();
	}

	@Override
	public String getGroupId() {
		return "app";
	}

	@Override
	public String getGroupDescription() {
		return "Manage your applications";
	}
	
	@CLIServiceMethod(name = "get", description = "Get Applications from the API-Manager in different formats")
	public static int export(String args[]) {
		AppExportParams params;
		try {
			params = (AppExportParams) AppExportCLIOptions.create(args).getParams();
		} catch (AppException e) {
			LOG.error("Error " + e.getMessage());
			return e.getError().getCode();
		}
		ApplicationExportApp app = new ApplicationExportApp();
		return app.export(params).getRc();
	}
	
	@CLIServiceMethod(name = "delete", description = "Delete selected application(s) from the API-Manager")
	public static int delete(String args[]) {
		AppExportParams params;
		try {
			params = (AppExportParams) AppExportCLIOptions.create(args).getParams();
		} catch (AppException e) {
			LOG.error("Error " + e.getMessage());
			return e.getError().getCode();
		}
		ApplicationExportApp app = new ApplicationExportApp();
		return app.delete(params).getRc();
	}
	
	public ExportResult delete(AppExportParams params) {
		ExportResult result = new ExportResult();
		try {
			return runExport(params, ResultHandler.DELETE_APP_HANDLER, result);
		} catch (AppException e) {
			e.logException(LOG);
			result.setError(new ErrorCodeMapper().getMapedErrorCode(e.getError()));
			return result;
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			result.setError(ErrorCode.UNXPECTED_ERROR);
			return result;
		}
	}
	
	public ExportResult export(AppExportParams params) {
		ExportResult result = new ExportResult();
		try {
			params.validateRequiredParameters();
			switch(params.getOutputFormat()) {
			case console:
				return runExport(params, ResultHandler.CONSOLE_EXPORTER, result);
			case json:
				return runExport(params, ResultHandler.JSON_EXPORTER, result);
			case csv:
				return runExport(params, ResultHandler.CSV_EXPORTER, result);
			default:
				return runExport(params, ResultHandler.CONSOLE_EXPORTER, result);
			}
		} catch (AppException e) {
			e.logException(LOG);
			return result;
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			result.setError(ErrorCode.UNXPECTED_ERROR);
			return result;
		} finally {
			try {
				// make sure the cache is updated, even an exception is thrown
				APIManagerAdapter.deleteInstance();
			} catch (Exception ignore) { }
		}
	}

	private ExportResult runExport(AppExportParams params, ResultHandler exportImpl, ExportResult result) throws AppException {
		// We need to clean some Singleton-Instances, as tests are running in the same JVM
		APIManagerAdapter.deleteInstance();
		APIMHttpClient.deleteInstances();

		APIMgrAppsAdapter appAdapter = new APIMgrAppsAdapter();
		ApplicationExporter exporter = ApplicationExporter.create(exportImpl, params, result);
		List<ClientApplication> apps = appAdapter.getApplications(exporter.getFilter(), true);
		if(apps.size()==0) {
			if(LOG.isDebugEnabled()) {
				LOG.info("No applications found using filter: " + exporter.getFilter());
			} else {
				LOG.info("No applications found based on the given filters.");
			}
		} else {
			LOG.info("Found " + apps.size() + " application(s).");
			
			exporter.export(apps);
			if(exporter.hasError()) {
				LOG.info("");
				LOG.error("Please check the log. At least one error was recorded.");
			} else {
				LOG.debug("Successfully exported " + apps.size() + " application(s).");
			}
			APIManagerAdapter.deleteInstance();
		}

		return result;
	}

	public static void main(String args[]) { 
		int rc = export(args);
		System.exit(rc);
	}


}
