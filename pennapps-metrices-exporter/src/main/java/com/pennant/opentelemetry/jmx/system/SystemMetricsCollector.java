package com.pennant.opentelemetry.jmx.system;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import java.io.File;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.pennant.opentelemetry.jmx.CommonLabels;
import com.pennant.opentelemetry.jmx.JmxProperties;

public class SystemMetricsCollector extends Collector {

	public ClassLoadingMXBean classLoadingBean;
	private CommonLabels commonLabels;

	/**
	 * Creates an empty collector. If you use this constructor
	 */
	public SystemMetricsCollector(CommonLabels commonLabels) {
		classLoadingBean = ManagementFactory.getClassLoadingMXBean();
		this.commonLabels = commonLabels;
	}

	@Override
	public <T extends Collector> T register(CollectorRegistry registry) {
		return super.register(registry);
	}

	@Override
	public List<MetricFamilySamples> collect() {
		List<MetricFamilySamples> jmxlist = new ArrayList<MetricFamilySamples>();
		// memory pool
		for (MemoryPoolMXBean memoryPoolBean : ManagementFactory.getPlatformMXBeans(MemoryPoolMXBean.class)) {
			String area = MemoryType.HEAP.equals(memoryPoolBean.getType()) ? "heap" : "nonheap";
			List<String> LABEL_NAME = new ArrayList<String>();
			List<String> LABEL_VALUE = new ArrayList<String>();
			LABEL_NAME.addAll(commonLabels.getCommonlabelnames());
			LABEL_VALUE.addAll(commonLabels.getCommonlabelvalues());
			LABEL_NAME.add("id");
			LABEL_VALUE.add(memoryPoolBean.getName());
			LABEL_NAME.add("area");
			LABEL_VALUE.add(area);

			jmxlist.add(buildGauge("jvm_memory_used", "last value of used memory", memoryPoolBean.getUsage().getUsed(),
					LABEL_NAME, LABEL_VALUE));
			jmxlist.add(buildGauge("jvm_memory_committed", "last value of comitted memory",
					memoryPoolBean.getUsage().getCommitted(), LABEL_NAME, LABEL_VALUE));
			jmxlist.add(buildGauge("jvm_memory_max", "last value of max memory", memoryPoolBean.getUsage().getMax(),
					LABEL_NAME, LABEL_VALUE));
		}
		// buffered memory
		for (BufferPoolMXBean bufferPoolBean : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {

			List<String> LABEL_NAME = new ArrayList<String>();
			List<String> LABEL_VALUE = new ArrayList<String>();
			LABEL_NAME.addAll(commonLabels.getCommonlabelnames());
			LABEL_VALUE.addAll(commonLabels.getCommonlabelvalues());
			LABEL_NAME.add("id");
			LABEL_VALUE.add(bufferPoolBean.getName());

			jmxlist.add(buildGauge("jvm_buffer_count", "last value of used memory", bufferPoolBean.getCount(),
					LABEL_NAME, LABEL_VALUE));
			jmxlist.add(buildGauge("jvm_buffer_total_capacity", "last value of comitted memory",
					bufferPoolBean.getTotalCapacity(), LABEL_NAME, LABEL_VALUE));
			jmxlist.add(buildGauge("jvm_buffer_memory_used", "last value of max memory", bufferPoolBean.getMemoryUsed(),
					LABEL_NAME, LABEL_VALUE));
		}

		// DISK SPACE METRICS
		jmxlist.add(buildGauge("disk_free", "last value of free disk space", new File("/").getFreeSpace()));
		jmxlist.add(buildGauge("disk_total", "last value of total disk space", new File("/").getTotalSpace()));

		// class loading metrics
		jmxlist.add(
				buildGauge("jvm_classes_loaded", "last value of total number of classes currently loaded in the JVM",
						classLoadingBean.getLoadedClassCount()));
		jmxlist.add(buildGauge("jvm_classes_unloaded",
				"last value of total number of classes currently unloaded in the JVM",
				classLoadingBean.getUnloadedClassCount()));

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