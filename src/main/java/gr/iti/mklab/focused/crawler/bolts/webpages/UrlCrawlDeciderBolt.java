package gr.iti.mklab.focused.crawler.bolts.webpages;

import static org.apache.storm.utils.Utils.tuple;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;

import gr.iti.mklab.framework.common.domain.WebPage;

public class UrlCrawlDeciderBolt extends BaseRichBolt {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static String MEDIA_STREAM = "mediaitems";
	public static String WEBPAGE_STREAM = "webpages";
	
	private Logger _logger;
	
	private OutputCollector _collector;

	private String inputField;
	private Set<String> socialMediaTargets = new HashSet<String>();
	
	public UrlCrawlDeciderBolt(String inputField) {
		this.inputField = inputField;
		
		socialMediaTargets.add("vimeo.com");
		socialMediaTargets.add("instagram.com");
		socialMediaTargets.add("www.youtube.com");
		socialMediaTargets.add("twitpic.com");
		socialMediaTargets.add("dailymotion.com");
		socialMediaTargets.add("www.facebook.com");
		socialMediaTargets.add("twitter.com");
	}
	
	public void prepare(@SuppressWarnings("rawtypes") Map stormConf, TopologyContext context,
			OutputCollector collector) {
		
		this._collector = collector;
		this._logger = Logger.getLogger(UrlCrawlDeciderBolt.class);
	}

	public void execute(Tuple input) {
		try {
			WebPage webPage = (WebPage) input.getValueByField(inputField);
			if(webPage != null) {
				String domain = webPage.getDomain();
				if(socialMediaTargets.contains(domain)) {
					_collector.emit("mediaitems", tuple(webPage));
					_collector.ack(input);
				}
				else {
					_collector.emit("webpages", input, tuple(webPage, webPage.getDomain()));
					_collector.ack(input);
				}
			}
			else {
				_collector.fail(input);
			}
		} catch(Exception e) {
			_collector.fail(input);
			_logger.error("Exception: " + e.getMessage());
		}
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declareStream(WEBPAGE_STREAM, new Fields("webpages", "domain"));
		declarer.declareStream(MEDIA_STREAM, new Fields("webpages"));
	}

}
