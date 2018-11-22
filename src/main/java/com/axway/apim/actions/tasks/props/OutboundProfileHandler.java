package com.axway.apim.actions.tasks.props;

import com.axway.apim.lib.AppException;
import com.axway.apim.swagger.api.IAPIDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class OutboundProfileHandler implements PropertyHandler {

	@Override
	public JsonNode handleProperty(IAPIDefinition desired, JsonNode response) throws AppException {
		ObjectMapper objectMapper = new ObjectMapper();
		if(desired.getOutboundProfiles().size()!=0) {
			((ObjectNode)response).replace("outboundProfiles", objectMapper.valueToTree(desired.getOutboundProfiles()));
		}
		return response;
	}

}
