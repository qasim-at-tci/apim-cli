package com.axway.apim.export.test.basic;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.axway.apim.export.test.ExportTestAction;
import com.axway.apim.test.ImportTestAction;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.dsl.testng.TestNGCitrusTestRunner;
import com.consol.citrus.functions.core.RandomNumberFunction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Test
public class WildcardAPIExportTestIT extends TestNGCitrusTestRunner {

	private ExportTestAction swaggerExport;
	private ImportTestAction swaggerImport;
	
	@CitrusTest
	@Test @Parameters("context")
	public void run(@Optional @CitrusResource TestContext context) throws IOException {		
		ObjectMapper mapper = new ObjectMapper();

		swaggerExport = new ExportTestAction();
		swaggerImport = new ImportTestAction();
		description("Import two APIs to export them afterwards");

		variable("apiNumber", RandomNumberFunction.getRandomNumber(3, true));
		variable("apiPath1", "/api/test/"+this.getClass().getSimpleName()+"-${apiNumber}/1");
		variable("apiPath2", "/api/test/"+this.getClass().getSimpleName()+"-${apiNumber}/2");
		variable("apiName1", this.getClass().getSimpleName()+"-${apiNumber}-1");
		variable("apiName2", this.getClass().getSimpleName()+"-${apiNumber}-2");
		variable("state", "unpublished");
		variable("exportLocation", "citrus:systemProperty('java.io.tmpdir')/"+this.getClass().getSimpleName()+"-${apiNumber}");
		
		// These are the folder and filenames generated by the export tool 
		variable("exportFolder1", "api-test-${apiName1}");
		variable("exportFolder2", "api-test-${apiName2}");
		variable("exportAPIName1", "${apiName1}.json");
		variable("exportAPIName2", "${apiName2}.json");

		echo("####### Importing the API 1, which should exported in the second step #######");
		createVariable(ImportTestAction.API_DEFINITION,  "/test/export/files/basic/petstore.json");
		createVariable(ImportTestAction.API_CONFIG,  "/test/export/files/basic/minimal-config.json");
		createVariable("apiPath", "${apiPath1}");
		createVariable("apiName", "${apiName1}");
		createVariable("expectedReturnCode", "0");
		swaggerImport.doExecute(context);
		
		echo("####### Importing the API 2, which should exported in the second step #######");
		createVariable(ImportTestAction.API_DEFINITION,  "/test/export/files/basic/petstore.json");
		createVariable(ImportTestAction.API_CONFIG,  "/test/export/files/basic/minimal-config.json");
		createVariable("apiPath", "${apiPath2}");
		createVariable("apiName", "${apiName2}");
		createVariable("expectedReturnCode", "0");
		swaggerImport.doExecute(context);

		echo("####### Export the API2 from the API-Manager #######");
		createVariable(ExportTestAction.EXPORT_API,  "/api/test/"+this.getClass().getSimpleName()+"-${apiNumber}*");
		createVariable("expectedReturnCode", "0");
		swaggerExport.doExecute(context);
		
		String exportedAPIConfigFile = context.getVariable("exportLocation")+"/"+context.getVariable("exportFolder1")+"/api-config.json";
		
		echo("####### Reading exported API-Config file: '"+exportedAPIConfigFile+"' #######");
		JsonNode exportedAPIConfig = mapper.readTree(new FileInputStream(new File(exportedAPIConfigFile)));
		
		assertEquals(exportedAPIConfig.get("version").asText(), 			"2.0.0");
		assertEquals(exportedAPIConfig.get("organization").asText(),		"API Development "+context.getVariable("orgNumber"));
		//assertEquals(exportedAPIConfig.get("backendBasepath").asText(), 	"https://petstore.swagger.io");
		assertEquals(exportedAPIConfig.get("state").asText(), 				"unpublished");
		assertEquals(exportedAPIConfig.get("path").asText(), 				context.getVariable("apiPath1"));
		assertEquals(exportedAPIConfig.get("name").asText(), 				context.getVariable("apiName1"));
		assertEquals(exportedAPIConfig.get("caCerts").size(), 				4);
		
		assertEquals(exportedAPIConfig.get("caCerts").get(0).get("certFile").asText(), 				"swagger.io.crt");
		assertEquals(exportedAPIConfig.get("caCerts").get(0).get("inbound").asBoolean(), 			false);
		assertEquals(exportedAPIConfig.get("caCerts").get(0).get("outbound").asBoolean(), 			true);
		
		assertTrue(new File(context.getVariable("exportLocation")+"/"+context.getVariable("exportFolder1")+"/swagger.io.crt").exists(), "Certificate swagger.io.crt is missing");
		assertTrue(new File(context.getVariable("exportLocation")+"/"+context.getVariable("exportFolder1")+"/StarfieldServicesRootCertificateAuthority-G2.crt").exists(), "Certificate StarfieldServicesRootCertificateAuthority-G2.crt is missing");
		assertTrue(new File(context.getVariable("exportLocation")+"/"+context.getVariable("exportFolder1")+"/AmazonRootCA1.crt").exists(), "Certificate AmazonRootCA1.crt is missing");
		assertTrue(new File(context.getVariable("exportLocation")+"/"+context.getVariable("exportFolder1")+"/Amazon.crt").exists(), "Certificate Amazon.crt is missing");
		
		assertTrue(new File(context.getVariable("exportLocation")+"/"+context.getVariable("exportFolder1")+"/"+context.getVariable("exportAPIName1")).exists(), "Exported Swagger-File is missing");
		
		exportedAPIConfigFile = context.getVariable("exportLocation")+"/"+context.getVariable("exportFolder2")+"/api-config.json";
		
		echo("####### Reading exported API-Config file: '"+exportedAPIConfigFile+"' #######");
		exportedAPIConfig = mapper.readTree(new FileInputStream(new File(exportedAPIConfigFile)));
		
		assertEquals(exportedAPIConfig.get("version").asText(), 			"2.0.0");
		assertEquals(exportedAPIConfig.get("organization").asText(),		"API Development "+context.getVariable("orgNumber"));
		//assertEquals(exportedAPIConfig.get("backendBasepath").asText(), 	"https://petstore.swagger.io");
		assertEquals(exportedAPIConfig.get("state").asText(), 				"unpublished");
		assertEquals(exportedAPIConfig.get("path").asText(), 				context.getVariable("apiPath2"));
		assertEquals(exportedAPIConfig.get("name").asText(), 				context.getVariable("apiName2"));
		assertEquals(exportedAPIConfig.get("caCerts").size(), 				4);
		
		assertEquals(exportedAPIConfig.get("caCerts").get(0).get("certFile").asText(), 				"swagger.io.crt");
		assertEquals(exportedAPIConfig.get("caCerts").get(0).get("inbound").asBoolean(), 			false);
		assertEquals(exportedAPIConfig.get("caCerts").get(0).get("outbound").asBoolean(), 			true);
		
		assertTrue(new File(context.getVariable("exportLocation")+"/"+context.getVariable("exportFolder2")+"/swagger.io.crt").exists(), "Certificate swagger.io.crt is missing");
		assertTrue(new File(context.getVariable("exportLocation")+"/"+context.getVariable("exportFolder2")+"/StarfieldServicesRootCertificateAuthority-G2.crt").exists(), "Certificate StarfieldServicesRootCertificateAuthority-G2.crt is missing");
		assertTrue(new File(context.getVariable("exportLocation")+"/"+context.getVariable("exportFolder2")+"/AmazonRootCA1.crt").exists(), "Certificate AmazonRootCA1.crt is missing");
		assertTrue(new File(context.getVariable("exportLocation")+"/"+context.getVariable("exportFolder2")+"/Amazon.crt").exists(), "Certificate Amazon.crt is missing");
		
		assertTrue(new File(context.getVariable("exportLocation")+"/"+context.getVariable("exportFolder2")+"/"+context.getVariable("exportAPIName2")).exists(), "Exported Swagger-File is missing");
	}
}
