package com.axway.apim.adapter.user;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.util.EntityUtils;
import org.ehcache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axway.apim.adapter.APIManagerAdapter;
import com.axway.apim.adapter.APIManagerAdapter.CacheType;
import com.axway.apim.api.model.Image;
import com.axway.apim.api.model.User;
import com.axway.apim.lib.CoreParameters;
import com.axway.apim.lib.errorHandling.AppException;
import com.axway.apim.lib.errorHandling.ErrorCode;
import com.axway.apim.lib.utils.Utils;
import com.axway.apim.lib.utils.rest.DELRequest;
import com.axway.apim.lib.utils.rest.GETRequest;
import com.axway.apim.lib.utils.rest.POSTRequest;
import com.axway.apim.lib.utils.rest.PUTRequest;
import com.axway.apim.lib.utils.rest.RestAPICall;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

public class APIManagerUserAdapter {
	
	CoreParameters cmd = CoreParameters.getInstance();
	
	private static Logger LOG = LoggerFactory.getLogger(APIManagerUserAdapter.class);
	
	ObjectMapper mapper = APIManagerAdapter.mapper;
	
	Map<UserFilter, String> apiManagerResponse = new HashMap<UserFilter, String>();
	
	Cache<String, String> userCache;
	
	public APIManagerUserAdapter() {
		userCache = APIManagerAdapter.getCache(CacheType.userCache, String.class, String.class);
	}
	
	private void readUsersFromAPIManager(UserFilter filter) throws AppException {
		if(apiManagerResponse.get(filter) != null) return;
		if(!APIManagerAdapter.hasAdminAccount()) {
			LOG.warn("Using OrgAdmin only to load users.");
		}
		String userId = "";
		// Specific user-id is requested
		if(filter.getId()!=null) {
			if(userCache.containsKey(filter.getId())) {
				apiManagerResponse.put(filter, userCache.get(filter.getId()));
				return;
			}
			userId = "/"+filter.getId();
		}
		URI uri;
		HttpResponse httpResponse = null;
		try {
			uri = new URIBuilder(cmd.getAPIManagerURL()).setPath(cmd.getApiBasepath() + "/users"+userId)
					.addParameters(filter.getFilters())
					.build();
			RestAPICall getRequest = new GETRequest(uri, APIManagerAdapter.hasAdminAccount());
			LOG.debug("Load users from API-Manager using filter: " + filter);
			LOG.trace("Load users with URI: " + uri);
			httpResponse = getRequest.execute();
			if(httpResponse.getStatusLine().getStatusCode()==HttpStatus.SC_NOT_FOUND) {
				throw new AppException("No user found for user id: " + userId, ErrorCode.UNKNOWN_USER);
			} else if(httpResponse.getStatusLine().getStatusCode()!=HttpStatus.SC_OK) {
				LOG.error("Sent request: " + uri);
				LOG.error("Received Status-Code: " +httpResponse.getStatusLine().getStatusCode()+ ", Response: '" + EntityUtils.toString(httpResponse.getEntity()) + "'");
				throw new AppException("", ErrorCode.API_MANAGER_COMMUNICATION);
			}
			String response = EntityUtils.toString(httpResponse.getEntity());
			if(!userId.equals("")) {
				// Store it as an Array
				response = "[" + response+ "]";
				apiManagerResponse.put(filter, response);
				userCache.put(userId, response);
			} else {
				// We got an Array from API-Manager
				apiManagerResponse.put(filter, response);
			}
		} catch (Exception e) {
			throw new AppException("Error cant read users from API-Manager with filter: "+filter, ErrorCode.API_MANAGER_COMMUNICATION, e);
		} finally {
			try {
				if(httpResponse!=null) 
					((CloseableHttpResponse)httpResponse).close();
			} catch (Exception ignore) {}
		}
	}
	
	public List<User> getUsers(UserFilter filter) throws AppException {
		readUsersFromAPIManager(filter);
		try {
			List<User> allUsers = mapper.readValue(this.apiManagerResponse.get(filter), new TypeReference<List<User>>(){});
			List<User> foundUsers = new ArrayList<User>();
			for(User user : allUsers) {
				if(!filter.filter(user)) continue; 
				addImage(user, filter.isIncludeImage());
				foundUsers.add(user);
			}
			Utils.addCustomPropertiesForEntity(foundUsers, this.apiManagerResponse.get(filter), filter);
			return foundUsers;
		} catch (IOException e) {
			LOG.error("Error cant read users from API-Manager with filter: "+filter+". Returned response: " + apiManagerResponse);
			throw new AppException("Error cant read users from API-Manager with filter: "+filter, ErrorCode.API_MANAGER_COMMUNICATION, e);
		}
	}
	
	void addImage(User user, boolean addImage) throws AppException {
		if(!addImage) return;
		URI uri;
		if(user.getImageUrl()==null) return;
		try {
			uri = new URIBuilder(cmd.getAPIManagerURL()).setPath(cmd.getApiBasepath() + "/users/"+user.getId()+"/image")
					.build();
			Image image = APIManagerAdapter.getImageFromAPIM(uri, "user-image");
			user.setImage(image);
		} catch (URISyntaxException e) {
			throw new AppException("Error loading image for user: " + user.getLoginName(), ErrorCode.UNXPECTED_ERROR, e);
		}
	}
	
	public List<User> getAllUsers() throws AppException {
		return getUsers(new UserFilter.Builder().build());
	}
	
	public User getUserForLoginName(String loginName) throws AppException {
		User user = getUser(new UserFilter.Builder().hasLoginName(loginName).build());
		return user;
	}
	
	public User getUserForId(String userId) throws AppException {
		User user = getUser(new UserFilter.Builder().hasId(userId).build());
		return user;
	}
	
	public User getUser(UserFilter filter) throws AppException {
		List<User> users = getUsers(filter);
		if(users.size()>1) {
			throw new AppException("No unique user found", ErrorCode.UNKNOWN_USER);
		}
		if(users.size()==0) {
			LOG.debug("No user found using filter: " + filter);
			return null;
		}
		return users.get(0);
	}
	
	public User updateUser(User desiredUser, User actualUser) throws AppException {
		User updatedUser = createOrUpdateUser(desiredUser, actualUser);
		if(desiredUser.getPassword()!=null) {
			LOG.info("Password of existing user: '" + actualUser.getLoginName() + "' ("+actualUser.getId()+") will not be updated.");
		}
		return updatedUser;
	}
	
	public User createUser(User desiredUser) throws AppException {
		User createdUser = createOrUpdateUser(desiredUser, null);
		changepassword(desiredUser.getPassword(), createdUser);
		return createdUser;
	}
	
	public User createOrUpdateUser(User desiredUser, User actualUser) throws AppException {
		HttpResponse httpResponse = null;
		User createdUser;
		FilterProvider filter;
		try {
			URI uri;
			if(actualUser==null) {
				filter = new SimpleFilterProvider().setDefaultFilter(
						SimpleBeanPropertyFilter.serializeAllExcept(new String[] {"password", "image", "organization", "createdOn"}));
				uri = new URIBuilder(cmd.getAPIManagerURL()).setPath(cmd.getApiBasepath()+"/users").build();
			} else {
				desiredUser.setId(actualUser.getId());
				desiredUser.setType(actualUser.getType());
				filter = new SimpleFilterProvider().setDefaultFilter(
						SimpleBeanPropertyFilter.serializeAllExcept(new String[] {"password", "image", "organization"}));
				uri = new URIBuilder(cmd.getAPIManagerURL()).setPath(cmd.getApiBasepath()+"/users/"+actualUser.getId()).build();
			}
			mapper.setFilterProvider(filter);
			mapper.setSerializationInclusion(Include.NON_NULL);
			try {
				RestAPICall request;
				if(actualUser==null) {
					String json = mapper.writeValueAsString(desiredUser);
					HttpEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
					request = new POSTRequest(entity, uri, false);
				} else {
					String json = mapper.writeValueAsString(desiredUser);
					HttpEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
					request = new PUTRequest(entity, uri, false);
				}
				httpResponse = request.execute();
				int statusCode = httpResponse.getStatusLine().getStatusCode();
				if(statusCode < 200 || statusCode > 299){
					LOG.error("Error creating/updating user. Response-Code: "+statusCode+". Got response: '"+EntityUtils.toString(httpResponse.getEntity())+"'");
					throw new AppException("Error creating/updating user. Response-Code: "+statusCode+"", ErrorCode.UNXPECTED_ERROR);
				}
				createdUser = mapper.readValue(httpResponse.getEntity().getContent(), User.class);
				desiredUser.setId(createdUser.getId());
				saveImage(desiredUser, actualUser);
			} catch (Exception e) {
				throw new AppException("Error creating/updating user.", ErrorCode.UNXPECTED_ERROR, e);
			} finally {
				try {
					((CloseableHttpResponse)httpResponse).close();
				} catch (Exception ignore) { }
			}
			// Force reload of updated user next time
			userCache.remove(createdUser.getId());
			return createdUser;

		} catch (Exception e) {
			throw new AppException("Error creating/updating user", ErrorCode.UNXPECTED_ERROR, e);
		}
	}
	
	public void changepassword(String newPassword, User actualUser) throws AppException {
		if(newPassword==null) return;
		HttpResponse httpResponse = null;
		URI uri;
		try {
			RestAPICall request;
			uri = new URIBuilder(cmd.getAPIManagerURL()).setPath(cmd.getApiBasepath()+"/users/"+actualUser.getId()+"/changepassword").build();
			HttpEntity entity = new StringEntity("newPassword="+newPassword, ContentType.APPLICATION_FORM_URLENCODED);
			if ( "oadmin".equals(actualUser.getRole()) ) { // An orgadmin cannot change the password of another orgadmin, even he has created that orgadmin
				request = new POSTRequest(entity, uri, true);
			} else {
				request = new POSTRequest(entity, uri, false);
			}
			httpResponse = request.execute();
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			if(statusCode != 204){
				LOG.error("Error changing password of user. Response-Code: "+statusCode+". Got response: '"+EntityUtils.toString(httpResponse.getEntity())+"'");
				throw new AppException("Error changing password of user. Response-Code: "+statusCode+"", ErrorCode.ERROR_CHANGEPASSWORD);
			}
		} catch (Exception e) {
			throw new AppException("Error changing password of user.", ErrorCode.ERROR_CHANGEPASSWORD, e);
		} finally {
			try {
				((CloseableHttpResponse)httpResponse).close();
			} catch (Exception ignore) { }
		}
	}
	
	public void deleteUser(User user) throws AppException {
		HttpResponse httpResponse = null;
		URI uri;
		try {
			uri = new URIBuilder(cmd.getAPIManagerURL()).setPath(cmd.getApiBasepath()+"/users/"+user.getId()).build();
			RestAPICall request = new DELRequest(uri, true);
			httpResponse = request.execute();
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			if(statusCode != 204){
				LOG.error("Error deleting user. Response-Code: "+statusCode+". Got response: '"+EntityUtils.toString(httpResponse.getEntity())+"'");
				throw new AppException("Error deleting user. Response-Code: "+statusCode+"", ErrorCode.API_MANAGER_COMMUNICATION);
			}
			// Also remove this user from cache
			userCache.remove(user.getId());
			LOG.info("User: "+user.getName()+" ("+user.getId()+")" + " successfully deleted");
		} catch (Exception e) {
			throw new AppException("Error deleting user", ErrorCode.ACCESS_ORGANIZATION_ERR, e);
		} finally {
			try {
				((CloseableHttpResponse)httpResponse).close();
			} catch (Exception ignore) { }
		}
	}
	
	private void saveImage(User user, User actualUser) throws URISyntaxException, AppException {
		if(user.getImage()==null) return;
		if(actualUser!=null && user.getImage().equals(actualUser.getImage())) return;
		HttpResponse httpResponse = null;
		URI uri = new URIBuilder(cmd.getAPIManagerURL()).setPath(cmd.getApiBasepath()+"/users/"+user.getId()+"/image/").build();
		HttpEntity entity = MultipartEntityBuilder.create()
			.addBinaryBody("file", user.getImage().getInputStream(), ContentType.create("image/jpeg"), user.getImage().getBaseFilename())
			.build();
		try {
			RestAPICall apiCall = new POSTRequest(entity, uri, false);
			httpResponse = apiCall.execute();
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			if(statusCode < 200 || statusCode > 299){
				LOG.error("Error saving/updating user image. Response-Code: "+statusCode+". Got response: '"+EntityUtils.toString(httpResponse.getEntity())+"'");
			}
		} catch (Exception e) {
			throw new AppException("Error uploading user image", ErrorCode.CANT_CREATE_API_PROXY, e);
		} finally {
			try {
				((CloseableHttpResponse)httpResponse).close();
			} catch (Exception ignore) { }
		}
	}
	
	public void setAPIManagerTestResponse(UserFilter key, String response) {
		this.apiManagerResponse.put(key, response);
	}
}
