package com.axway.apim.api.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class APISpecIncludeExcludeFilter {
	
	private Map<String, List<String>> pathMap;
	
	private List<String> paths = new ArrayList<String>();
	
	private List<String> tags = new ArrayList<String>();
	
	private List<String> models = new ArrayList<String>();
	
	public List<String> getPaths() {
		return paths;
	}

	public void setPaths(List<String> paths) {
		this.paths = paths;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public List<String> getModels() {
		return models;
	}

	public void setModels(List<String> models) {
		this.models = models;
	}
	
	public List<String> getHttpMethods(String path, boolean includeWildcard) {
		if(pathMap==null) {
			pathMap = new HashMap<String, List<String>>();
			for(String pathAndMethod : paths) {
				String p = pathAndMethod.split(":")[0];
				String v = pathAndMethod.split(":")[1];
				List<String> verbs = pathMap.get(p);
				if(verbs == null) {
					verbs = new ArrayList<String>();
				}
				verbs.add(v.toLowerCase());
				pathMap.put(p, verbs);
			}
		}
		if(pathMap.containsKey(path)) {
			return pathMap.get(path);
		} else if(includeWildcard && pathMap.containsKey("*")) {
			return pathMap.get("*");
		} else {
			return null;
		}
	}
	
	public boolean filter(String path, String httpMethod, List<String> tags, boolean useWildcard, boolean pathAndTags) {
		List<String> httpMethods4Path = getHttpMethods(path, useWildcard);
		// If filter has both configured check them in combination
		if(pathMap!=null && !pathMap.isEmpty() && this.tags!=null && !this.tags.isEmpty()) {
			if(httpMethods4Path==null) return false;
			if((httpMethods4Path.contains(httpMethod.toLowerCase()) || httpMethods4Path.contains("*")) && containsTags(tags)) {
				return true;
			} else {
				return false;
			}
		}
		if(pathAndTags) return false;
		// 
		if(pathMap!=null && !pathMap.isEmpty()) {
			if(httpMethods4Path==null) return false;
			if(httpMethods4Path.contains(httpMethod.toLowerCase()) || httpMethods4Path.contains("*")) {
				return true;
			} else {
				return false;
			}
		}
		// Check is the tag is configured
		if(this.tags!=null && !this.tags.isEmpty()) {
			if(containsTags(tags)) {
				return true;
			} else {
				return false;
			}
		}
		return false;
	}

	/**
	 * This method is used for tests only
	 * @param pathAndVerbs an array of path and verbs to include or exclude
	 */
	public void addPath(String[] pathAndVerbs) {
		this.paths.addAll(Arrays.asList(pathAndVerbs));
	}
	
	/**
	 * This method is used for tests only
	 * @param tags a list of tags to include or exclude
	 */
	public void addTag(String[] tags) {
		this.tags.addAll(Arrays.asList(tags));
	}
	
	/**
	 * This method is used for tests only
	 * @param models a list of models to include or exclude
	 */
	public void addModel(String[] models) {
		this.models.addAll(Arrays.asList(models));
	}
	
	private boolean containsTags(List<String> tags) {
		for(String tag : tags) {
			if(this.tags.contains(tag)) return true;
		}
		return false;
	}
}
