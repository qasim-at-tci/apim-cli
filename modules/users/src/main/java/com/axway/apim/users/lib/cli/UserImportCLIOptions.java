package com.axway.apim.users.lib.cli;

import org.apache.commons.cli.Option;

import com.axway.apim.lib.CLIOptions;
import com.axway.apim.lib.CoreCLIOptions;
import com.axway.apim.lib.Parameters;
import com.axway.apim.lib.StandardImportCLIOptions;
import com.axway.apim.lib.errorHandling.AppException;
import com.axway.apim.users.lib.UserImportParams;

public class UserImportCLIOptions extends CLIOptions {

	private UserImportCLIOptions(String[] args) {
		super(args);
	}
	
	public static CLIOptions create(String[] args) {
		CLIOptions cliOptions = new UserImportCLIOptions(args);
		cliOptions = new StandardImportCLIOptions(cliOptions);
		cliOptions = new CoreCLIOptions(cliOptions);
		cliOptions.addOptions();
		cliOptions.parse();
		return cliOptions;
	}

	@Override
	public void addOptions() {
		// Define command line options required for Application export
		Option option = new Option("c", "config", true, "This is the JSON-Formatted Organization-Config file containing the organization. You may get that config file using apim org get with output set to JSON.");
		option.setRequired(true);
		option.setArgName("user_config.json");
		addOption(option);
	}

	@Override
	public void printUsage(String message, String[] args) {
		super.printUsage(message, args);		
		System.out.println("----------------------------------------------------------------------------------------");
		System.out.println("How to imports organizations using the JSON-Config format");
		System.out.println("Import an organization using enviornment properties: env.api-env.properties:");
		System.out.println(getBinaryName()+" org import -c myOrgs/partner-org.json -s api-env");
		System.out.println(getBinaryName()+" org import -c myOrgs/development-org.json -h localhost -u apiadmin -p changeme");
		System.out.println();
		System.out.println();
		System.out.println("For more information and advanced examples please visit:");
		System.out.println("https://github.com/Axway-API-Management-Plus/apim-cli/wiki");
	}

	@Override
	protected String getAppName() {
		return "Organization-Import";
	}
	
	@Override
	public Parameters getParams() throws AppException {
		UserImportParams params = new UserImportParams();

		params.setConfig(getValue("config"));
		return params;
	}
}
