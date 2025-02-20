package com.axway.apim.users.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.axway.apim.adapter.user.UserFilter;
import com.axway.apim.api.model.User;
import com.axway.apim.lib.ExportResult;
import com.axway.apim.lib.errorHandling.AppException;
import com.axway.apim.users.lib.params.UserExportParams;
import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

public class ConsoleUserExporter extends UserResultHandler {
	
	Character[] borderStyle = AsciiTable.BASIC_ASCII_NO_DATA_SEPARATORS;

	public ConsoleUserExporter(UserExportParams params, ExportResult result) {
		super(params, result);
	}

	@Override
	public void export(List<User> users) throws AppException {
		switch(params.getWide()) {
		case standard:
			printStandard(users);
			break;
		case wide:
			printWide(users);
			break;
		case ultra:
			printUltra(users);
			break;
		}
		return;
	}
	
	private void printStandard(List<User> users) {
		System.out.println(AsciiTable.getTable(borderStyle, users, Arrays.asList(
				new Column().header("User-Id").headerAlign(HorizontalAlign.LEFT).dataAlign(HorizontalAlign.LEFT).with(user -> user.getId()),
				new Column().header("Login-Name").headerAlign(HorizontalAlign.LEFT).dataAlign(HorizontalAlign.LEFT).with(user -> user.getLoginName()),
				new Column().header("Name").headerAlign(HorizontalAlign.LEFT).dataAlign(HorizontalAlign.LEFT).with(user -> user.getName()),
				new Column().header("Email").with(user -> user.getEmail()),
				new Column().header("Enabled").with(user -> Boolean.toString(user.isEnabled()))
				)));
	}
	
	private void printWide(List<User> users) {
		System.out.println(AsciiTable.getTable(borderStyle, users, Arrays.asList(
				new Column().header("User-Id").headerAlign(HorizontalAlign.LEFT).dataAlign(HorizontalAlign.LEFT).with(user -> user.getId()),
				new Column().header("Login-Name").headerAlign(HorizontalAlign.LEFT).dataAlign(HorizontalAlign.LEFT).with(user -> user.getLoginName()),
				new Column().header("Name").headerAlign(HorizontalAlign.LEFT).dataAlign(HorizontalAlign.LEFT).with(user -> user.getName()),
				new Column().header("Email").with(user -> user.getEmail()),
				new Column().header("Enabled").with(user -> Boolean.toString(user.isEnabled())),
				new Column().header("Organization").with(user -> user.getOrganization().getName()),
				new Column().header("Role").with(user -> user.getRole())
				)));
	}
	
	private void printUltra(List<User> users) {
		System.out.println(AsciiTable.getTable(borderStyle, users, Arrays.asList(
				new Column().header("User-Id").headerAlign(HorizontalAlign.LEFT).dataAlign(HorizontalAlign.LEFT).with(user -> user.getId()),
				new Column().header("Login-Name").headerAlign(HorizontalAlign.LEFT).dataAlign(HorizontalAlign.LEFT).with(user -> user.getLoginName()),
				new Column().header("Name").headerAlign(HorizontalAlign.LEFT).dataAlign(HorizontalAlign.LEFT).with(user -> user.getName()),
				new Column().header("Email").with(user -> user.getEmail()),
				new Column().header("Enabled").with(user -> Boolean.toString(user.isEnabled())),
				new Column().header("Organization").with(user -> user.getOrganization().getName()),
				new Column().header("Role").with(user -> user.getRole()),
				new Column().header("Created on").with(user -> new Date(user.getCreatedOn()).toString()),
				//new Column().header("Last seen").with(user -> getLastSeen(user)), // Not supported by the REST-API - Only returned for the currentUser :-(
				new Column().header("Type").with(user -> user.getType()),
				new Column().header("State").with(user -> user.getState())
				)));
	}

	@Override
	public UserFilter getFilter() throws AppException {
		return getBaseFilterBuilder().build();
	}
	
	private String getLastSeen(User user) {
		if(user.getAuthNUserAttributes()==null || user.getAuthNUserAttributes().getLastSeen()==null) return "N/A";
		return new Date(user.getAuthNUserAttributes().getLastSeen()).toString();
	}
}
