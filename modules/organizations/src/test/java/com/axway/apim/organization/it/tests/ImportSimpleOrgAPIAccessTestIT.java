package com.axway.apim.organization.it.tests;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.axway.apim.lib.errorHandling.AppException;
import com.axway.apim.organization.it.ExportOrganizationTestAction;
import com.axway.apim.organization.it.ImportOrganizationTestAction;
import com.axway.apim.test.ImportTestAction;
import com.axway.lib.testActions.TestParams;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.dsl.testng.TestNGCitrusTestRunner;
import com.consol.citrus.functions.core.RandomNumberFunction;
import com.consol.citrus.message.MessageType;

@Test
public class ImportSimpleOrgAPIAccessTestIT extends TestNGCitrusTestRunner implements TestParams {
	
	private ImportTestAction apiImport = new ImportTestAction();
	
	private static String PACKAGE = "/com/axway/apim/organization/orgImport/";
	
	@CitrusTest
	@Test @Parameters("context")
	public void run(@Optional @CitrusResource TestContext context) throws IOException, AppException {
		description("Import organization into API-Manager including API Access");
		ExportOrganizationTestAction exportApp = new ExportOrganizationTestAction(context);
		ImportOrganizationTestAction importApp = new ImportOrganizationTestAction(context);

		variable("orgName", "My-Org-"+importApp.getRandomNum());
		variable("orgDescription", "Org with API-Access");
		
		variable("apiNumber", RandomNumberFunction.getRandomNumber(4, true));
		
		variable("apiPath", "/test-app-api1-${apiNumber}");
		variable("apiName", "Test-App-API1-${apiNumber}");
		variable("apiName1", "${apiName}");

		echo("####### Importing Test API 1: '${apiName}' on path: '${apiPath}' #######");
		createVariable(ImportTestAction.API_DEFINITION,  PACKAGE + "petstore.json");
		createVariable(ImportTestAction.API_CONFIG,  PACKAGE + "test-api-config.json");
		createVariable("expectedReturnCode", "0");
		apiImport.doExecute(context);
		
		echo("####### Extract ID of imported API 1: '${apiName}' on path: '${apiPath}' #######");
		http(builder -> builder.client("apiManager").send().get("/proxies").header("Content-Type", "application/json"));
		http(builder -> builder.client("apiManager").receive().response(HttpStatus.OK).messageType(MessageType.JSON)
			.validate("$.[?(@.path=='${apiPath}')].name", "${apiName}")
			.extractFromPayload("$.[?(@.path=='${apiPath}')].id", "apiId1"));
		
		variable("apiPath", "/test-app-api2-${apiNumber}");
		variable("apiName", "Test-App-API2-${apiNumber}");
		variable("apiName2", "${apiName}");
		
		echo("####### Importing Test API 2: '${apiName}' on path: '${apiPath}' #######");
		createVariable(ImportTestAction.API_DEFINITION,  PACKAGE + "petstore.json");
		createVariable(ImportTestAction.API_CONFIG,  PACKAGE + "test-api-config.json");
		createVariable("expectedReturnCode", "0");
		apiImport.doExecute(context);
		
		echo("####### Extract ID of imported API 2: '${apiName}' on path: '${apiPath}' #######");
		http(builder -> builder.client("apiManager").send().get("/proxies").header("Content-Type", "application/json"));
		http(builder -> builder.client("apiManager").receive().response(HttpStatus.OK).messageType(MessageType.JSON)
				.validate("$.[?(@.path=='${apiPath}')].name", "${apiName}")
				.extractFromPayload("$.[?(@.path=='${apiPath}')].id", "apiId2"));

		echo("####### Import organization to test: '${orgName}' #######");		
		createVariable(PARAM_CONFIGFILE,  PACKAGE + "SingleOrgGrantAPIAccess.json");
		createVariable(PARAM_EXPECTED_RC, "0");
		importApp.doExecute(context);
		
		echo("####### Validate organization: '${orgName}' has been imported #######");
		http(builder -> builder.client("apiManager").send().get("/organizations?field=name&op=eq&value=${orgName}").header("Content-Type", "application/json"));
		http(builder -> builder.client("apiManager").receive().response(HttpStatus.OK).messageType(MessageType.JSON)
			.validate("$.[?(@.name=='${orgName}')].name", "@assertThat(hasSize(1))@")
			.extractFromPayload("$.[?(@.name=='${orgName}')].id", "orgId"));
		
		echo("####### Validate organization: '${orgName}' (${orgId}) has access to the imported API 1 #######");
		http(builder -> builder.client("apiManager").send().get("/organizations/${orgId}/apis").header("Content-Type", "application/json"));
		http(builder -> builder.client("apiManager").receive().response(HttpStatus.OK).messageType(MessageType.JSON)
			.validate("$.[?(@.apiId=='${apiId1}')].enabled", "true")
			.validate("$.[?(@.apiId=='${apiId2}')].enabled", "true"));
		
		echo("####### Re-Import same organization - Should be a No-Change #######");
		createVariable(PARAM_EXPECTED_RC, "10");
		importApp.doExecute(context);
		
		echo("####### Export the organization #######");
		createVariable(PARAM_TARGET, exportApp.getTestDirectory().getPath());
		createVariable(PARAM_EXPECTED_RC, "0");
		createVariable(PARAM_OUTPUT_FORMAT, "json");
		createVariable(PARAM_NAME, "${orgName}");
		exportApp.doExecute(context);
		
		Assert.assertEquals(exportApp.getLastResult().getExportedFiles().size(), 1, "Expected to have one organization exported");
		String exportedConfig = exportApp.getLastResult().getExportedFiles().get(0);
		
		echo("####### Re-Import EXPORTED organization - Should be a No-Change #######");
		createVariable(PARAM_CONFIGFILE,  exportedConfig);
		createVariable("expectedReturnCode", "10");
		importApp.doExecute(context);
	}
}
