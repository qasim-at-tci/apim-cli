package com.axway.apim.users;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axway.apim.adapter.APIManagerAdapter;
import com.axway.apim.adapter.user.APIManagerUserAdapter;
import com.axway.apim.api.model.User;
import com.axway.apim.lib.errorHandling.AppException;
import com.axway.apim.lib.errorHandling.ErrorCode;

public class UserImportManager {
	
	private static Logger LOG = LoggerFactory.getLogger(UserImportManager.class);
	
	private APIManagerUserAdapter userAdapter;
	
	public UserImportManager() throws AppException {
		super();
		this.userAdapter = APIManagerAdapter.getInstance().userAdapter;
	}

	public void replicate(User desiredUser, User actualUser) throws AppException {
		if(actualUser==null) {
			userAdapter.createUser(desiredUser);
		} else if(usersAreEqual(desiredUser, actualUser)) {
			LOG.debug("No changes detected between Desired- and Actual-User: " + desiredUser.getName());
			throw new AppException("No changes detected between Desired- and Actual-User.", ErrorCode.NO_CHANGE);			
		} else {
			LOG.debug("Update existing user: " + desiredUser.getName());
			userAdapter.updateUser(desiredUser, actualUser);
			LOG.info("Successfully replicated user: "+desiredUser.getName()+" into API-Manager");
		}
		return;
	}
	
	private static boolean usersAreEqual(User desiredUser, User actualUser) {
		return desiredUser.deepEquals(actualUser);
	}
}
