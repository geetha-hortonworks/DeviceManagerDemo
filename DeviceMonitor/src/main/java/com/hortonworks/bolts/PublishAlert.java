package com.hortonworks.bolts;

import java.util.HashMap;
import java.util.Map;

import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.ClientTransport;
import org.cometd.client.transport.LongPollingTransport;
import org.eclipse.jetty.client.HttpClient;

import com.hortonworks.events.DeviceAlert;
import com.hortonworks.util.Constants;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class PublishAlert extends BaseRichBolt {
	private String pubSubUrl = Constants.pubSubUrl;
	private String alertChannel = Constants.alertChannel;
	private BayeuxClient bayuexClient;
	private OutputCollector collector;
	
	public void execute(Tuple tuple) {
		DeviceAlert deviceAlert = (DeviceAlert) tuple.getValueByField("DeviceAlert");
		
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("deviceSerialNumber", deviceAlert.getSerialNumber());
		data.put("alertDescription", deviceAlert.getAlertDescription());
		data.put("deviceModel", deviceAlert.getDeviceModel());
		data.put("latitude", deviceAlert.getLatitude());
		data.put("longitude", deviceAlert.getLongitude());
		bayuexClient.getChannel(alertChannel).publish(data);
		
		collector.emit(tuple, new Values((DeviceAlert)deviceAlert));
		collector.ack(tuple);
	}

	public void prepare(Map arg0, TopologyContext arg1, OutputCollector collector) {
		this.collector = collector;
		
		HttpClient httpClient = new HttpClient();
		try {
			httpClient.start();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Prepare the transport
		Map<String, Object> options = new HashMap<String, Object>();
		ClientTransport transport = new LongPollingTransport(options, httpClient);

		// Create the BayeuxClient
		bayuexClient = new BayeuxClient(pubSubUrl, transport);
		
		bayuexClient.handshake();
		boolean handshaken = bayuexClient.waitFor(3000, BayeuxClient.State.CONNECTED);
		if (handshaken)
		{
			System.out.println("Connected to Cometd Http PubSub Platform");
		}
		else{
			System.out.println("Could not connect to Cometd Http PubSub Platform");
		}
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("DeviceAlert"));
	}
}