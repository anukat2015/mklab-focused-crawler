package eu.socialsensor.focused.crawler.bolts.webpages;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;

import eu.socialsensor.framework.common.domain.WebPage;

import static backtype.storm.utils.Utils.tuple;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.utils.Utils;

public class URLExpanderBolt extends BaseRichBolt {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5514715036795163046L;

	private OutputCollector _collector;

	private MongoClient _mongo;
	private DB _database;
	private DBCollection _collection;

	private String mongoCollectionName;
	private String mongoDbName;
	private String mongoHost;
	
	private int max_redirects = 3;
	
	private Set<String> targets = new HashSet<String>();
	
	public URLExpanderBolt(String mongoHost, String mongoDbName, String mongoCollectionName) throws Exception {
		this.mongoHost = mongoHost;
		this.mongoDbName = mongoDbName;
		this.mongoCollectionName = mongoCollectionName;
		
		targets.add("vimeo.com");
		targets.add("instagram.com");
		targets.add("www.youtube.com");
		targets.add("twitpic.com");
		targets.add("dailymotion.com");
		targets.add("www.facebook.com");
	}
	
	public void prepare(@SuppressWarnings("rawtypes") Map stormConf, TopologyContext context,
			OutputCollector collector) {
		this._collector = collector;
		try {
			_mongo = new MongoClient(mongoHost);
			_database = _mongo.getDB(mongoDbName);
			_collection = _database.getCollection(mongoCollectionName);
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public void execute(Tuple tuple) {	
		WebPage webPage = (WebPage) tuple.getValueByField("webPage");
		if(webPage != null) {
			try {
				String url = webPage.getUrl();
				String expandedUrl = expand(url);
				
				if(expandedUrl != null) {
					try {
						URL temp = new URL(expandedUrl);
					
						String domain = temp.getHost();
					
						BasicDBObject f = new BasicDBObject("expandedUrl", expandedUrl);
						f.put("domain", domain);
						BasicDBObject o = new BasicDBObject("$set", f);
						BasicDBObject q = new BasicDBObject("url", url);
						_collection.update(q , o);
					
						webPage.setExpandedUrl(expandedUrl);
						synchronized(_collector) {
							if(targets.contains(domain)) 
								_collector.emit("media", tuple(webPage));
							else 
								_collector.emit("article", tuple(webPage));
							
						}
					}
					catch(Exception e) {
						BasicDBObject o = new BasicDBObject("$set", new BasicDBObject("status", "failed"));
						BasicDBObject q = new BasicDBObject("url", url);
						_collection.update(q , o);
					}
				}
				else {
					BasicDBObject o = new BasicDBObject("$set", new BasicDBObject("status", "failed"));
					BasicDBObject q = new BasicDBObject("url", url);
					_collection.update(q , o);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			Utils.sleep(500);
		}
		
	}

//	class ExpanderThread extends Thread {
//		
//		int pages = 0;
//		
//		public void run() {
//			while(true) {
//				WebPage webPage = null;
//				synchronized(queue) {
//					if(++pages%100==0) {
//						System.out.println(pages + " pages. " + queue.size() + " entries in queue!");
//					}
//					webPage = queue.poll();
//				}
//				if(webPage != null) {
//					
//					try {
//						String url = webPage.getUrl();
//						
//						String expandedUrl = expand(url);
//						if(expandedUrl != null) {
//							URL temp = new URL(expandedUrl);
//							String domain = temp.getHost();
//							
//							BasicDBObject f = new BasicDBObject("expandeUrl", expandedUrl);
//							f.put("domain", domain);
//							BasicDBObject o = new BasicDBObject("$set", f);
//							BasicDBObject q = new BasicDBObject("url", url);
//							_collection.update(q , o);
//							
//							if(targets.contains(domain))
//								_collector.emit("media", tuple(url, expandedUrl));
//							else
//								_collector.emit("article", tuple(url, expandedUrl));
//						}
//					} catch (Exception e) {
//
//					}
//				}
//				else {
//					Utils.sleep(500);
//				}
//			}
//		};
//	}
	
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declareStream("media", new Fields("webPage"));
		declarer.declareStream("article", new Fields("webPage"));
	}

	public String expand(String shortUrl) throws IOException {
		int redirects = 0;
		HttpURLConnection connection;
		while(true && redirects < max_redirects) {
			try {
				URL url = new URL(shortUrl);
				connection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY); 
				connection.setInstanceFollowRedirects(false);
				connection.setReadTimeout(1000);
				connection.connect();
				String expandedURL = connection.getHeaderField("Location");
				if(expandedURL == null) {
					return shortUrl;
				}
				else {
					shortUrl = expandedURL;
					redirects++;
				}    
			}
			catch(Exception e) {
				return null;
			}
		}
		return shortUrl;
    }

}