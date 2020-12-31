package com.pennant.opentelemetry.jmx;

import java.io.IOException;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;
import com.pennant.opentelemetry.jmx.system.GCMetricsCollector;
import com.pennant.opentelemetry.jmx.system.ProcessMetricCollector;
import com.pennant.opentelemetry.jmx.system.SystemMetricsCollector;
import com.pennant.opentelemetry.jmx.system.ThreadMetricsCollector;

import io.opentelemetry.instrumentation.api.config.Config;
import io.prometheus.client.exporter.HTTPServer;
public class JmxMetricsExport {

	static List<String> LABEL_NAMES = new ArrayList<String>();
	static List<String> LABEL_VALUES = new ArrayList<String>();
	private static final String METRIC_PORT = "otel.metric.port";
	private static final String METRIC_SERVICENAME= "otel.metric.service.name";
	private static final String DEFAULT_SERVICENAME = "Pennant";
	private static int port = 123;
	public static void init() {
		System.out.println("Entering @7247");
		CommonLabels labels = null;
		
		LABEL_NAMES.add("host");
		LABEL_NAMES.add("host_address");
		LABEL_NAMES.add("service");
		try {
			LABEL_VALUES.add(Inet4Address.getLocalHost().getHostName());
			System.out.println(Inet4Address.getLocalHost().getHostName());
			LABEL_VALUES.add(Inet4Address.getLocalHost().getHostAddress());
			if(Config.get().getProperty(METRIC_SERVICENAME) != null){
				LABEL_VALUES.add(Config.get().getProperty(METRIC_SERVICENAME));
			}else{
				LABEL_VALUES.add(DEFAULT_SERVICENAME);
			}
			labels = new CommonLabels();
		} catch (Exception e) {
			e.printStackTrace();
		}
		labels.setCommonlabelnames(LABEL_NAMES);
		labels.setCommonlabelvalues(LABEL_VALUES);
		new GCMetricsCollector(labels).register();
		new ProcessMetricCollector(labels).register();
		new ThreadMetricsCollector(labels).register();
		new SystemMetricsCollector(labels).register();
		new JmxMetricsCollector(labels).register();
		if(Config.get().getProperty(METRIC_PORT) != null){
			port = Integer.parseInt(Config.get().getProperty(METRIC_PORT));
		}
		try {
			HTTPServer server = new HTTPServer(port);
		} catch (IOException e) {

			e.printStackTrace();
		}
	}

}
