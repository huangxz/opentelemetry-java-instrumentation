package com.pennant.opentelemetry.jmx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.CollectorRegistry;

public class JmxMetricsCollector extends Collector {

	// private AttributeList jmx;
	private JmxMetrics jmxmetrics = new JmxMetrics();
	private CommonLabels commonLabels;

	/**
	 * Creates an empty collector. If you use this constructor
	 */
	public JmxMetricsCollector(CommonLabels commonLabels) {
		this.commonLabels = commonLabels;
	}

	@Override
	public <T extends Collector> T register(CollectorRegistry registry) {
		return super.register(registry);
	}

	@Override
	public List<MetricFamilySamples> collect() {

		List<MetricFamilySamples> jmxlist = new ArrayList<MetricFamilySamples>();
		List<JmxProperties> jmxProperitiesList = jmxmetrics.createMetrics();
		for (JmxProperties jmxproperties : jmxProperitiesList) {
			try {
				if (jmxproperties.getLabelnames().contains("host")) {
					int index = jmxproperties.getLabelnames().indexOf("host");
					jmxproperties.getLabelnames().remove(index);
					jmxproperties.getLabelvalues().remove(index);
				}
				jmxproperties.getLabelnames().addAll(commonLabels.getCommonlabelnames());
				jmxproperties.getLabelvalues().addAll(commonLabels.getCommonlabelvalues());

				jmxlist.add(buildGauge(jmxproperties.getMetricname(), "last value of" + jmxproperties.getMetricsdesc(),
						jmxproperties.getValue().get(), jmxproperties.getLabelnames(), jmxproperties.getLabelvalues()));
			} catch (Exception e) {
				continue;
			}
		}

		return jmxlist;

	}

	public static MetricFamilySamples buildGauge(String name, String help, double value, List<String> lnames,
			List<String> lvalues) {
		return new MetricFamilySamples(name, Type.GAUGE, help,
				Collections.singletonList(new MetricFamilySamples.Sample(name, lnames, lvalues, value)));
	}

}