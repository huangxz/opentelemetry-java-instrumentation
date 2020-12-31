package com.pennant.opentelemetry.jmx.system;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import com.pennant.opentelemetry.jmx.CommonLabels;
import com.pennant.opentelemetry.jmx.JmxProperties;

public class ThreadMetricsCollector extends Collector {

	private ThreadMXBean threadBean;

	CommonLabels commonLabels;

	/**
	 * Creates an empty collector. If you use this constructor
	 */
	public ThreadMetricsCollector(CommonLabels commonLabels) {

		threadBean = ManagementFactory.getThreadMXBean();
		this.commonLabels = commonLabels;
	}

	@Override
	public <T extends Collector> T register(CollectorRegistry registry) {
		return super.register(registry);
	}

	@Override
	public List<MetricFamilySamples> collect() {

		List<MetricFamilySamples> jmxlist = new ArrayList<MetricFamilySamples>();

		jmxlist.add(buildGauge("jvm_threads_peak", "last value of peak number of live threads since the JVM start",
				threadBean.getPeakThreadCount()));
		jmxlist.add(buildGauge("jvm_threads_daemon", "last value of demon number of live threads since the JVM start",
				threadBean.getDaemonThreadCount()));
		jmxlist.add(buildGauge("jvm_threads_live", "last value of count number of live threads since the JVM start",
				threadBean.getThreadCount()));
		for (Thread.State state : Thread.State.values()) {
			JmxProperties props = new JmxProperties();
			List<String> LABEL_NAME = new ArrayList<String>();
			List<String> LABEL_VALUE = new ArrayList<String>();
			LABEL_NAME.addAll(commonLabels.getCommonlabelnames());
			LABEL_VALUE.addAll(commonLabels.getCommonlabelvalues());
			if (LABEL_NAME.contains("state")) {
				LABEL_VALUE.remove(2);
				LABEL_VALUE.add(state.name());
			} else {
				LABEL_NAME.add("state");
				LABEL_VALUE.add(state.name());
			}

			long count = Arrays.stream(threadBean.getThreadInfo(threadBean.getAllThreadIds())).filter(Objects::nonNull)
					.map(ThreadInfo::getThreadState).filter(s -> s == state).count();

			jmxlist.add(buildGauge("jvm_threads_states",
					"last value of peak number of live threads since the JVM start", count, LABEL_NAME, LABEL_VALUE));
		}

		return jmxlist;
	}

	private static MetricFamilySamples buildGauge(String name, String help, double value, List<String> lnames,
			List<String> lvalues) {
		return new MetricFamilySamples(name, Type.GAUGE, help,
				Collections.singletonList(new MetricFamilySamples.Sample(name, lnames, lvalues, value)));
	}

	private MetricFamilySamples buildGauge(String name, String help, double value) {
		return new MetricFamilySamples(name, Type.GAUGE, help,
				Collections.singletonList(new MetricFamilySamples.Sample(name, commonLabels.getCommonlabelnames(),
						commonLabels.getCommonlabelvalues(), value)));
	}

}