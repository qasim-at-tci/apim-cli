package com.axway.apim.test.odata;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.axway.apim.adapter.APIManagerAdapter;
import com.axway.apim.lib.errorHandling.AppException;
import com.axway.apim.test.ImportTestAction;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.dsl.testng.TestNGCitrusTestRunner;
import com.consol.citrus.functions.core.RandomNumberFunction;
import com.consol.citrus.message.MessageType;

@Test
public class ODataV2ImportTestIT extends TestNGCitrusTestRunner {

	private ImportTestAction importAction;
	
	@CitrusTest
	@Test @Parameters("context")
	public void run(@Optional @CitrusResource TestContext context) throws IOException, AppException {
		if(APIManagerAdapter.hasAPIManagerVersion("7.7.20200130")) { // Test with this version, as it requires OpenAPI 3 support
			importAction = new ImportTestAction();
			description("Import an OData V2 specification that must be converted into an OpenAPI V3 specification.");
			
			variable("apiNumber", RandomNumberFunction.getRandomNumber(3, true));
			variable("apiPath", "/odata-v2-api-${apiNumber}");
			variable("apiName", "OData-V2-API-${apiNumber}");
			variable("backendBasepath", "https://services.odata.org/V2/Northwind/Northwind.svc/");
			
			variable("state", "unpublished");
	
			echo("####### Importing OData V2 API: '${apiName}' on path: '${apiPath}' #######");
			createVariable(ImportTestAction.API_DEFINITION,  "/com/axway/apim/api/definition/odata/ODataV2NorthWindMetadata.xml");
			createVariable(ImportTestAction.API_CONFIG,  "/com/axway/apim/test/files/basic/minimal-config-with-backendBasepath.json");
			createVariable("expectedReturnCode", "0");
			importAction.doExecute(context);
	
			echo("####### Validate OData V2 API: '${apiName}' on path: '${apiPath}' has been imported #######");
			http(builder -> builder.client("apiManager").send().get("/proxies").header("Content-Type", "application/json"));
	
			http(builder -> builder.client("apiManager").receive().response(HttpStatus.OK).messageType(MessageType.JSON)
				.validate("$.[?(@.path=='${apiPath}')].name", "${apiName}")
				.extractFromPayload("$.[?(@.path=='${apiPath}')].id", "apiId"));
	
			echo("####### RE-Importing same API: '${apiName}' on path: '${apiPath}' without changes. Expecting No-Change. #######");
			createVariable("expectedReturnCode", "10");
			importAction.doExecute(context);
		}
	}

}
