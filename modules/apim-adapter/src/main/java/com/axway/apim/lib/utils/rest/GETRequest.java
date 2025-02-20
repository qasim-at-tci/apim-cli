package com.axway.apim.lib.utils.rest;

import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import com.axway.apim.lib.errorHandling.AppException;

public class GETRequest extends RestAPICall {

	public GETRequest(URI uri) {
		super(null, uri);
	}
	
	public GETRequest(URI uri, boolean useAdmin) {
		super(null, uri, useAdmin);
	}

	@Override
	public HttpResponse execute() throws AppException {
		HttpGet httpGet = new HttpGet(uri);
		//httpGet.setHeader("Content-type", this.contentType);
		HttpResponse response = sendRequest(httpGet);
		return response;
	}
}
