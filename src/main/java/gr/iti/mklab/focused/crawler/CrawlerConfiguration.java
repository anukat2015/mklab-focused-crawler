package gr.iti.mklab.focused.crawler;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import gr.iti.mklab.framework.client.mongo.MongoHandler;
import gr.iti.mklab.framework.common.domain.Item;
import gr.iti.mklab.framework.common.domain.config.Configuration;
import gr.iti.mklab.framework.common.factories.ObjectFactory;

public class CrawlerConfiguration extends Configuration {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5269944272731808440L;

	@Expose
	@SerializedName(value = "streams")
	private Map<String, Configuration> streamConfigMap = null;
	
	@Expose
	@SerializedName(value = "storages")
	private Map<String, Configuration> storageConfigMap = null;
	
	@Expose
	@SerializedName(value = "filters")
	private Map<String, Configuration> filterConfigMap = null;
	
	@Expose
	@SerializedName(value = "processors")
	private Map<String, Configuration> processorsConfigMap = null;
	
	@Expose
	@SerializedName(value = "subscribers")
	private Map<String, Configuration> subscriberConfigMap = null;
	
	
	public CrawlerConfiguration() {
		streamConfigMap = new HashMap<String, Configuration>();
		storageConfigMap = new HashMap<String, Configuration>();
		filterConfigMap = new HashMap<String, Configuration>();
		processorsConfigMap = new HashMap<String, Configuration>();
		subscriberConfigMap = new HashMap<String, Configuration>();
	}
	
	public void setStreamConfig(String streamId, Configuration config){
		streamConfigMap.put(streamId,config);
	}
	
	
	public Configuration getStreamConfig(String streamId){
		return streamConfigMap.get(streamId);
	}
	
	public void setStorageConfig(String storageId, Configuration config){
		storageConfigMap.put(storageId,config);
	}
	
	public Configuration getStorageConfig(String storageId){
		return storageConfigMap.get(storageId);
	}
	
	public void setSubscriberConfig(String subscriberId, Configuration config){
		this.subscriberConfigMap.put(subscriberId, config);
	}
	
	public Configuration getSubscriberConfig(String subscriberId){
		return subscriberConfigMap.get(subscriberId);
	}
	
	public void setFilterConfig(String filterId, Configuration config){
		filterConfigMap.put(filterId, config);
	}
	
	public Configuration getFilterConfig(String filterId){
		return filterConfigMap.get(filterId);
	}
	
	public void setProcessorConfig(String processorId, Configuration config){
		processorsConfigMap.put(processorId, config);
	}
	
	public Configuration getProcessorConfig(String processorId){
		return processorsConfigMap.get(processorId);
	}
	
	public void setParameter(String name, String value){
		super.setParameter(name,value);
	}
	
	public String getParameter(String name) {
		return super.getParameter(name);
	}
	
	public Set<String> getStreamIds() {
		return streamConfigMap.keySet();
	}
	
	public Set<String> getStorageIds() {
		return storageConfigMap.keySet();
	}
	
	public Set<String> getSubscriberIds(){
		return subscriberConfigMap.keySet();
	}
	
	public Set<String> getFilterIds(){
		return filterConfigMap.keySet();
	}
	
	public Set<String> getProcessorsIds(){
		return processorsConfigMap.keySet();
	}
	
	public static Configuration readFromMongo(String host, String dbName, String collectionName) 
			throws Exception {
		MongoHandler mongo = new MongoHandler(host, dbName, collectionName, null);
		
		String json = mongo.findOne();
		Gson gson = new GsonBuilder()
        	.excludeFieldsWithoutExposeAnnotation()
        	.create();
		Configuration config = gson.fromJson(json, Configuration.class);
        return config;
	}
	
	public void readItemsFromMongo(String host, String dbName, String collection, List<Item> mongoItems) throws Exception{
		MongoHandler mongo = new MongoHandler(host, dbName, collection, null);
		List<String> jsonItems = mongo.findMany(-1);
	
		for(String json : jsonItems){
			
			Item item = ObjectFactory.create(json);
			
			mongoItems.add(item);
			
		}
		
	}
	
	public void saveToMongo(String host, String dbName, String collectionName) 
			throws Exception {
		MongoHandler mongo = new MongoHandler(host, dbName, collectionName, null);
		
		mongo.insert(this);
		
	}
	
	public static Configuration readFromFile(File file) 
			    throws ParserConfigurationException, SAXException, IOException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		ParseHandler handler = new ParseHandler();
		parser.parse(file, handler);
		return handler.getConfig();
	}
	
	
	private static class ParseHandler extends DefaultHandler {

		private enum ParseState{
			IDLE,
			IN_CONFIG_PARAM,
			IN_CONFIG_STREAM,
			IN_CONFIG_STREAM_PARAM,
			IN_CONFIG_SUBSCRIBER,
			IN_CONFIG_SUBSCRIBER_PARAM,
			IN_CONFIG_STORAGE,
			IN_CONFIG_STORAGE_PARAM,
			IN_CONFIG_FILTER,
			IN_CONFIG_FILTER_PARAM,
			IN_CONFIG_PROCESSOR,
			IN_CONFIG_PROCESSOR_PARAM,
		}
		
		private CrawlerConfiguration config = new CrawlerConfiguration();
	    private ParseState state = ParseState.IDLE;
	    private StringBuilder value = null;
	    private String name = null;
	    private Configuration sconfig = null;
	    private Configuration srconfig = null;
	    private Configuration storage_config = null; 
	    private Configuration filter_config = null; 
	    private Configuration processor_config = null; 
	    private String streamId = null, storageId = null, 
	    		subscriberId = null, filterId = null, processorId = null;
		
		public CrawlerConfiguration getConfig() {
			return config;
		}

		@Override
		public void startElement(String uri, String localName, String name,
				Attributes attributes) throws SAXException {
			
			//System.out.println("IN: "+name);
			
			if (name.equalsIgnoreCase("Parameter")) {
				this.name = attributes.getValue("name");
				if (this.name == null) return;
				value = new StringBuilder();
				if (state == ParseState.IDLE) {
					state = ParseState.IN_CONFIG_PARAM;
				}else if(state == ParseState.IN_CONFIG_STREAM) {
					state = ParseState.IN_CONFIG_STREAM_PARAM;
				}
				else if(state == ParseState.IN_CONFIG_STORAGE) {
					state = ParseState.IN_CONFIG_STORAGE_PARAM;
				}
				else if(state == ParseState.IN_CONFIG_SUBSCRIBER) {
					state = ParseState.IN_CONFIG_SUBSCRIBER_PARAM;
				}
				else if(state == ParseState.IN_CONFIG_FILTER) {
					state = ParseState.IN_CONFIG_FILTER_PARAM;
				}
				else if(state == ParseState.IN_CONFIG_PROCESSOR) {
					state = ParseState.IN_CONFIG_PROCESSOR_PARAM;
				}
			}
			
			else if (name.equalsIgnoreCase("Stream")){
				streamId = attributes.getValue("id");
				value = new StringBuilder();
				if (streamId == null) return;
				sconfig = new Configuration();
				state = ParseState.IN_CONFIG_STREAM;
			}
			else if (name.equalsIgnoreCase("Subscriber")){
				subscriberId = attributes.getValue("id");
				value = new StringBuilder();
				if (subscriberId == null) return;
				srconfig = new Configuration();
				state = ParseState.IN_CONFIG_SUBSCRIBER;
			}
			else if (name.equalsIgnoreCase("Storage")){
				storageId = attributes.getValue("id");
				value = new StringBuilder();
				if (storageId == null) return;
				storage_config = new Configuration();
				state = ParseState.IN_CONFIG_STORAGE;
			}
			else if (name.equalsIgnoreCase("Filter")){
				filterId = attributes.getValue("id");
				value = new StringBuilder();
				if (filterId == null) return;
				filter_config = new Configuration();
				state = ParseState.IN_CONFIG_FILTER;
			}
			else if (name.equalsIgnoreCase("Processor")){
				processorId = attributes.getValue("id");
				value = new StringBuilder();
				if (processorId == null) return;
				processor_config = new Configuration();
				state = ParseState.IN_CONFIG_PROCESSOR;
			}
		}
		
		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {
			if (value != null){
				value.append(ch, start, length);
			}
		}

		@Override
		public void endElement(String uri, String localName, String name)
				throws SAXException {

			if (name.equalsIgnoreCase("Parameter") && state == ParseState.IN_CONFIG_PARAM) {
				config.setParameter(this.name, value.toString().trim());
				state = ParseState.IDLE;
			}
			else if (name.equalsIgnoreCase("Parameter") && state == ParseState.IN_CONFIG_STREAM_PARAM){
				sconfig.setParameter(this.name, value.toString().trim());
				state = ParseState.IN_CONFIG_STREAM;
			}
			else if (name.equalsIgnoreCase("Parameter") && state == ParseState.IN_CONFIG_SUBSCRIBER_PARAM){
				srconfig.setParameter(this.name, value.toString().trim());
				state = ParseState.IN_CONFIG_SUBSCRIBER;
			}
			else if (name.equalsIgnoreCase("Parameter") && state == ParseState.IN_CONFIG_STORAGE_PARAM){
				storage_config.setParameter(this.name, value.toString().trim());
				state = ParseState.IN_CONFIG_STORAGE;
			}
			else if (name.equalsIgnoreCase("Parameter") && state == ParseState.IN_CONFIG_FILTER_PARAM){
				filter_config.setParameter(this.name, value.toString().trim());
				state = ParseState.IN_CONFIG_FILTER;
			}
			else if (name.equalsIgnoreCase("Parameter") && state == ParseState.IN_CONFIG_PROCESSOR_PARAM){
				processor_config.setParameter(this.name, value.toString().trim());
				state = ParseState.IN_CONFIG_PROCESSOR;
			}
			else if (name.equalsIgnoreCase("Stream")){
				config.setStreamConfig(streamId, sconfig);
				state = ParseState.IDLE;
			}
			else if (name.equalsIgnoreCase("Subscriber")){
				config.setSubscriberConfig(subscriberId, srconfig);
				state = ParseState.IDLE;
			}
			else if (name.equalsIgnoreCase("Storage")){
				config.setStorageConfig(storageId, storage_config);
				state = ParseState.IDLE;
			}
			else if (name.equalsIgnoreCase("Filter")){
				config.setFilterConfig(filterId, filter_config);
				state = ParseState.IDLE;
			}
			else if (name.equalsIgnoreCase("Processor")){
				config.setProcessorConfig(processorId, processor_config);
				state = ParseState.IDLE;
			}	
		}
	}
}