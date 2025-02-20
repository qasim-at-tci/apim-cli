package com.axway.apim.adapter.apis;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.axway.apim.adapter.clientApps.ClientAppFilter;
import com.axway.apim.adapter.jackson.AppCredentialsDeserializer;
import com.axway.apim.api.model.apps.APIKey;
import com.axway.apim.api.model.apps.ClientAppCredential;
import com.axway.apim.api.model.apps.ClientApplication;
import com.axway.apim.lib.errorHandling.AppException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class ClientAppFilterTest extends APIManagerMockBase {
	
	private static String TEST_PACKAGE = "com/axway/apim/adapter/apimanager/testApps/";
	
	@BeforeClass
	public void setupTestIndicator() throws AppException, IOException {
		setupMockData();
	}
	
	@Test
	public void hasFullWildCardName() throws AppException {
		ClientAppFilter filter = new ClientAppFilter.Builder()
				.hasName("*")
				.build();
		Assert.assertEquals(filter.getFilters().size(), 0);
	}
	
	@Test
	public void credentialANDRedirectURLFilterTest() throws AppException, JsonParseException, JsonMappingException, IOException {
		ClientApplication testApp = getTestApp("client-app-with-two-redirectUrls.json");
		
		ClientAppFilter filter = new ClientAppFilter.Builder()
				.hasCredential("6cd55c27-675a-444a-9bc7-ae9a7869184d")
				.build();
		assertFalse(filter.filter(testApp), "App must match with API-Key: 6cd55c27-675a-444a-9bc7-ae9a7869184d");

		filter = new ClientAppFilter.Builder()
				.hasCredential("*675a*")
				.build();
		assertFalse(filter.filter(testApp), "App must match with wildcard search for API-Key: 6cd55c27-675a-444a-9bc7-ae9a7869184d");
		
		filter = new ClientAppFilter.Builder()
				.hasCredential("*XXXXX*")
				.build();
		assertTrue(filter.filter(testApp), "App SHOULD NOT match with wildcard search *XXXXX*");
		
		filter = new ClientAppFilter.Builder()
				.hasCredential("*XXXXX*")
				.hasRedirectUrl("*ZZZZZ*")
				.build();
		assertTrue(filter.filter(testApp), "App SHOULD NOT match");
		
		filter = new ClientAppFilter.Builder()
				.hasCredential("*XXXXX*")
				.hasRedirectUrl("*oauthclient:8088*")
				.build();
		assertTrue(filter.filter(testApp), "App SHOULD NOT match as a wrong credential is given");
		
		filter = new ClientAppFilter.Builder()
				.hasCredential("ClientConfidentialApp")
				.hasRedirectUrl("*oauthclient:8088*")
				.build();
		assertFalse(filter.filter(testApp), "App SHOULD match with correct credential and redirect url");
		
		filter = new ClientAppFilter.Builder()
				.hasRedirectUrl("*oauthclient:8088*")
				.build();
		assertFalse(filter.filter(testApp), "App SHOULD match with correct wildcard redirect url");
		
		filter = new ClientAppFilter.Builder()
				.hasRedirectUrl("https://oauthclient:8088/client/apigateway/callback")
				.build();
		assertFalse(filter.filter(testApp), "App SHOULD match with correct redirect url");
	}
	
	@Test
	public void appWithoutCredentialTest() throws AppException, JsonParseException, JsonMappingException, IOException {
		ClientApplication testApp = getTestApp("client-app-with-two-redirectUrls.json");
		testApp.setCredentials(null);
		
		ClientAppFilter filter = new ClientAppFilter.Builder()
				.hasCredential("6cd55c27-675a-444a-9bc7-ae9a7869184d")
				.build();
		assertTrue(filter.filter(testApp), "App SHOULD NOT match as there are no credentials");
		
		filter = new ClientAppFilter.Builder()
				.hasRedirectUrl("*anything*")
				.build();
		assertTrue(filter.filter(testApp), "App SHOULD NOT match as there are no credentials");
	}
	
	@Test
	public void testAppHavingAPIKeyButNoClientID() throws AppException, JsonParseException, JsonMappingException, IOException {
		ClientApplication testApp = getTestApp("client-app-with-two-api-key-only.json");
		((APIKey)testApp.getCredentials().get(0)).setApiKey(null);
		
		ClientAppFilter filter = new ClientAppFilter.Builder()
				.hasCredential("Does-not-exists")
				.build();
		assertTrue(filter.filter(testApp), "App SHOULD NOT match as there are no credentials");
	}
	
	@Test
	public void testAppHavingAccessToAPI() throws AppException, JsonParseException, JsonMappingException, IOException {
		ClientApplication testApp = getTestApp("client-app-with-apis.json");
		
		ClientAppFilter filter = new ClientAppFilter.Builder()
				.hasApiName("This API does not exists")
				.build();
		assertTrue(filter.filter(testApp), "App SHOULD NOT match as the given API doesn't exists.");
		
		filter = new ClientAppFilter.Builder()
				.hasApiName("*HIPAA*")
				.build();
		assertFalse(filter.filter(testApp), "App SHOULD match as the given API exists.");
		
		filter = new ClientAppFilter.Builder()
				.hasApiName("EMR-HealthCatalog")
				.build();
		assertFalse(filter.filter(testApp), "App SHOULD match as the given API exists.");
	}
	
	@Test
	public void testFilterAppCreatedByAndOrganization() throws AppException, JsonParseException, JsonMappingException, IOException {
		
		ClientAppFilter filter = new ClientAppFilter.Builder()
				.hasCreatedByLoginName("fred")
				.hasOrganizationName("FHIR")
				.build();
		Assert.assertEquals(filter.getFilters().size(), 6);
		Assert.assertEquals(filter.getFilters().get(0).getValue(), "orgid");
		Assert.assertEquals(filter.getFilters().get(1).getValue(), "eq");
		Assert.assertEquals(filter.getFilters().get(2).getValue(), "2efca39a-2572-4b62-8d0f-53241d93d362");
		Assert.assertEquals(filter.getFilters().get(3).getValue(), "userid");
		Assert.assertEquals(filter.getFilters().get(4).getValue(), "eq");
		Assert.assertEquals(filter.getFilters().get(5).getValue(), "c888af4e-0728-4e82-880c-7cf490138220");
	}
	
	private ClientApplication getTestApp(String appConfig) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new SimpleModule().addDeserializer(ClientAppCredential.class, new AppCredentialsDeserializer()));
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE+appConfig);
		ClientApplication app = mapper.readValue(is, ClientApplication.class);
		return app;
	}
}
