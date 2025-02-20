package com.axway.apim.test.changestate;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.axway.apim.adapter.apis.APIManagerMockBase;
import com.axway.apim.api.API;
import com.axway.apim.api.apiSpecification.APISpecification;
import com.axway.apim.api.apiSpecification.APISpecificationFactory;
import com.axway.apim.api.apiSpecification.Swagger2xSpecification;
import com.axway.apim.api.model.CaCert;
import com.axway.apim.api.model.InboundProfile;
import com.axway.apim.api.model.TagMap;
import com.axway.apim.apiimport.APIChangeState;
import com.axway.apim.lib.errorHandling.AppException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class APIChangeStateTest extends APIManagerMockBase {
	
	API testAPI1;
	API testAPI2;
	
	@BeforeClass
	private void initTestIndicator() throws AppException, IOException {
		setupMockData();
	}
	
	@BeforeMethod
	private void setupTestAPIs() throws AppException, IOException {
		testAPI1 = getTestAPI("ChangeStateTestAPI.json");
		testAPI2 = getTestAPI("ChangeStateTestAPI.json");
	}	
	
	private static String TEST_PACKAGE = "com/axway/apim/api/state/";
	
	@Test
	public void testHasNoChange() throws JsonParseException, JsonMappingException, IOException, AppException {
		APIChangeState changeState = new APIChangeState(testAPI1, testAPI2);
		Assert.assertTrue(!changeState.hasAnyChanges(), "APIs are equal");
		Assert.assertEquals(changeState.getAllChanges().size(),0,  "No changes properties");
		Assert.assertFalse(changeState.isRecreateAPI(), "No need to Re-Create API");
	}
	
	@Test
	public void testCaCertsAreChanged() throws JsonParseException, JsonMappingException, IOException, AppException {
		CaCert caCert = new CaCert();
		caCert.setAlias("ABC");
		caCert.setExpired("Expired");
		caCert.setCertBlob("Something");
		caCert.setInbound("true");
		caCert.setOutbound("true");
		testAPI2.getCaCerts().add(caCert);
		APIChangeState changeState = new APIChangeState(testAPI1, testAPI2);
		Assert.assertTrue(changeState.hasAnyChanges(), "APIs should not be eqaul");
		Assert.assertEquals(changeState.getAllChanges().size(),1,  "CaCerts is changed");
		Assert.assertTrue(changeState.getAllChanges().contains("caCerts"), "Expect the caCert as a changed prop");
		Assert.assertFalse(changeState.isRecreateAPI(), "No need to Re-Create API");
		APIChangeState.copyChangedProps(testAPI1, testAPI2, changeState.getAllChanges());
		APIChangeState validatePropsAreCopied = new APIChangeState(testAPI1, testAPI2);
		Assert.assertTrue(!validatePropsAreCopied.hasAnyChanges(), "APIs are NOW equal");
		Assert.assertFalse(changeState.isRecreateAPI(), "No need to Re-Create API");
	}
	
	@Test
	public void testAPINameHasChanged() throws JsonParseException, JsonMappingException, IOException, AppException {
		testAPI2.setName("New Name for API");
		APIChangeState changeState = new APIChangeState(testAPI1, testAPI2);
		Assert.assertTrue(changeState.hasAnyChanges(), "There must be a change");
		Assert.assertEquals(changeState.getAllChanges().size(),1,  "One change");
		Assert.assertEquals(changeState.getBreakingChanges().size(),0,  "Name should not be a breaking change");
		Assert.assertEquals(changeState.getNonBreakingChanges().size(),1,  "Name is a breaking change");
		Assert.assertTrue(changeState.getAllChanges().contains("name"), "Expect the name as a changed prop");
		Assert.assertFalse(changeState.isRecreateAPI(), "No need to Re-Create API");
		APIChangeState.copyChangedProps(testAPI1, testAPI2, changeState.getAllChanges());
		APIChangeState validatePropsAreCopied = new APIChangeState(testAPI1, testAPI2);
		Assert.assertTrue(!validatePropsAreCopied.hasAnyChanges(), "APIs are NOW equal");
		Assert.assertFalse(changeState.isRecreateAPI(), "No need to Re-Create API");
	}
	
	@Test
	public void testTagsAreChanged() throws JsonParseException, JsonMappingException, IOException, AppException {
		TagMap<String, String[]> newTags = new TagMap<String, String[]>();
		newTags.put("Group A", new String[] {"Value 1", "Value 2"});
		testAPI2.setTags(newTags);
		APIChangeState changeState = new APIChangeState(testAPI1, testAPI2);
		Assert.assertTrue(changeState.hasAnyChanges(), "APIs should not be eqaul");
		Assert.assertEquals(changeState.getAllChanges().size(),1,  "TagMaps is changed");
		Assert.assertTrue(changeState.getAllChanges().contains("tags"), "Expect the tags as a changed prop");
		Assert.assertFalse(changeState.isRecreateAPI(), "No need to Re-Create API");
		APIChangeState.copyChangedProps(testAPI1, testAPI2, changeState.getAllChanges());
		APIChangeState validatePropsAreCopied = new APIChangeState(testAPI1, testAPI2);
		Assert.assertTrue(!validatePropsAreCopied.hasAnyChanges(), "APIs are NOW equal");
		Assert.assertFalse(changeState.isRecreateAPI(), "No need to Re-Create API");
	}
	
	@Test
	public void testInboundProfilesAreChanged() throws JsonParseException, JsonMappingException, IOException, AppException {
		Map<String, InboundProfile> inboundProfiles = new HashMap<String, InboundProfile>();
		InboundProfile profile = new InboundProfile();
		profile.setCorsProfile("_default");
		profile.setSecurityProfile("_somethingElse");
		profile.setMonitorAPI(false);
		profile.setMonitorSubject("This is my monitor");
		inboundProfiles.put("_default", profile);
		testAPI2.setInboundProfiles(inboundProfiles);
		APIChangeState changeState = new APIChangeState(testAPI1, testAPI2);
		Assert.assertTrue(changeState.hasAnyChanges(), "APIs should not be eqaul");
		Assert.assertEquals(changeState.getAllChanges().size(),1,  "InboundProfiles are changed");
		Assert.assertEquals(changeState.getBreakingChanges().size(),1,  "InboundProfiles are breaking");
		Assert.assertTrue(changeState.getAllChanges().contains("inboundProfiles"), "Expect the inboundProfile as a changed prop");
		Assert.assertFalse(changeState.isRecreateAPI(), "No need to Re-Create API");
		APIChangeState.copyChangedProps(testAPI1, testAPI2, changeState.getAllChanges());
		APIChangeState validatePropsAreCopied = new APIChangeState(testAPI1, testAPI2);
		Assert.assertTrue(!validatePropsAreCopied.hasAnyChanges(), "APIs are NOW equal");
		Assert.assertFalse(changeState.isRecreateAPI(), "No need to Re-Create API");
	}
	
	@Test
	public void testChangedAPIDefinition() throws JsonParseException, JsonMappingException, IOException, AppException {
		APISpecification spec1 = new Swagger2xSpecification();
		spec1.parse(new String("TEST API 1111111").getBytes());
		testAPI1.setApiDefinition(spec1);
		APISpecification spec2 = new Swagger2xSpecification();
		spec2.parse(new String("TEST API 2222222").getBytes());
		testAPI2.setApiDefinition(spec2);
		APIChangeState changeState = new APIChangeState(testAPI1, testAPI2);
		Assert.assertTrue(changeState.isRecreateAPI(), "API needs to be recreated");
	}
	
	@Test
	public void testUnchangedAPIDefinition() throws JsonParseException, JsonMappingException, IOException, AppException {
		APISpecification spec1 = new Swagger2xSpecification();
		spec1.parse(new String("TEST API 3333333").getBytes());
		testAPI1.setApiDefinition(spec1);
		APISpecification spec2 = new Swagger2xSpecification();
		spec2.parse(new String("TEST API 3333333").getBytes());
		testAPI2.setApiDefinition(spec2);
		APIChangeState changeState = new APIChangeState(testAPI1, testAPI2);
		Assert.assertFalse(changeState.isRecreateAPI(), "API-Definition is unchanged.");
	}
	
	
	
	private API getTestAPI(String configFile) throws JsonParseException, JsonMappingException, IOException {
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE+configFile);
		API testAPI = mapper.readValue(is, API.class);		
		return testAPI;
	}
}
