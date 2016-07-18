package gr.iti.mklab.focused.crawler.bolts.metrics;

import gr.iti.mklab.framework.common.domain.MediaItem;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;

public class MediaCounterBolt extends BaseRichBolt {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Map<String, Integer> domains = new HashMap<String, Integer>();
	private Map<String, Integer> tags = new HashMap<String, Integer>();
	private Map<String, Integer> contributors = new HashMap<String, Integer>();

	private String mongoHostName;

	private String mongodbName;

	private Logger logger;
	

	public MediaCounterBolt(String mongoHostName, String mongodbName) {
		this.mongoHostName = mongoHostName;
		this.mongodbName = mongodbName;
	}
	
	public void prepare(@SuppressWarnings("rawtypes") Map stormConf, TopologyContext context,
			OutputCollector collector) {
		
		logger = Logger.getLogger(MediaCounterBolt.class);
		
		MongoClient client = new MongoClient(mongoHostName);
		MongoDatabase db  = client.getDatabase(mongodbName);
		
		Runnable updater = new MediaCounterUpdater(db);
		Thread thread = new Thread(updater);
		
		thread.start();
	}

	public void execute(Tuple input) {
		MediaItem mediaItem = (MediaItem) input.getValueByField("MediaItem");
		if(mediaItem == null)
			return;
		
		try {
			URL mediaItemUrl = new URL(mediaItem.getUrl());
			String domain = mediaItemUrl.getHost();
			synchronized(domains) {
				Integer count = domains.get(domain);
				if(count == null)
					count = 0;
				domains.put(domain, ++count);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		

		String[] miTags = mediaItem.getTags();
		if(miTags != null && miTags.length>0) {
			synchronized(tags) {
				for(String tag : miTags) {
					Integer count = tags.get(tag);
					if(count == null)
						count = 0;
					tags.put(tag, ++count);
				}
			}
		}

		String contributor = mediaItem.getUserId();
		synchronized(contributors) {
			Integer count = contributors.get(contributor);
			if(count == null)
				count = 0;
			contributors.put(contributor, ++count);
		}
		
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		
	}

	public class MediaCounterUpdater implements Runnable {

		MongoCollection<Document> _tagsCollection;
		MongoCollection<Document> _domainsCollection;
		private MongoCollection<Document> _contributorsCollection;

		public MediaCounterUpdater(MongoDatabase db) {
			_tagsCollection = db.getCollection("tags");
			_domainsCollection = db.getCollection("domains");
			_contributorsCollection = db.getCollection("contributor");
		}
		
		public void run() {
			while(true) {
				try {
					Thread.sleep(10 * 60 * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					continue;
				}
				
				logger.info("============ Update Counters ============= ");
				synchronized(tags) {
					logger.info(tags.size() + " tags to be updated");
					for(Entry<String, Integer> tagEntry : tags.entrySet()) {
						DBObject q = new BasicDBObject("tag", tagEntry.getKey());
						DBObject o = new BasicDBObject("$inc", new BasicDBObject("count", tagEntry.getValue()));
						//_tagsCollection.update(q, o, true, false);
					}
					tags.clear();
				}
				
				synchronized(domains) {
					logger.info(domains.size() + " domains to be updated");
					for(Entry<String, Integer> domainEntry : domains.entrySet()) {
						DBObject q = new BasicDBObject("domain", domainEntry.getKey());
						DBObject o = new BasicDBObject("$inc", new BasicDBObject("count", domainEntry.getValue()));
						//_domainsCollection.update(q, o, true, false);
					}
					domains.clear();
				}
				
				synchronized(contributors) {
					logger.info(contributors.size() + " contributors to be updated");
					for(Entry<String, Integer> contributorEntry : contributors.entrySet()) {
						DBObject q = new BasicDBObject("contributor", contributorEntry.getKey());
						DBObject o = new BasicDBObject("$inc", new BasicDBObject("count", contributorEntry.getValue()));
						//_contributorsCollection.update(q, o, true, false);
					}
					contributors.clear();
				}
			}
		}
		
	}
}
