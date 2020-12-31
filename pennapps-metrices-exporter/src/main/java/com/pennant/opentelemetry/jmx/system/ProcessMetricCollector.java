package com.pennant.opentelemetry.jmx.system;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import com.pennant.opentelemetry.jmx.CommonLabels;

public class ProcessMetricCollector extends Collector {

	private boolean perUpdateMetricsEnabled;
	static List<String> LABEL_NAMES = new ArrayList<String>();
	static List<String> LABEL_VALUES = new ArrayList<String>();
	private OperatingSystemMXBean operatingSystemBean;
	private Runtime runtime;

	private Optional<Method> systemCpuUsage;
	private CommonLabels commonlabels;
	private Optional<Method> processCpuUsage;
	private static final List<String> OPERATING_SYSTEM_BEAN_CLASS_NAMES = Arrays.asList(
			"com.sun.management.OperatingSystemMXBean", 
			"com.ibm.lang.management.OperatingSystemMXBean" 
	);
//create a collector with commonLabels
	public ProcessMetricCollector(CommonLabels commonlabels) {
		this.operatingSystemBean = ManagementFactory.getOperatingSystemMXBean();
		this.runtime = Runtime.getRuntime();
		this.systemCpuUsage = findOSBeanMethod("getSystemCpuLoad");
		this.processCpuUsage = findOSBeanMethod("getProcessCpuLoad");
		this.commonlabels = commonlabels;
	}

	public ProcessMetricCollector enableUpdateMetricsEnabled() {
		this.perUpdateMetricsEnabled = true;
		return this;
	}

	@Override
	public <T extends Collector> T register(CollectorRegistry registry) {
		return super.register(registry);
	}

	@Override
	public List<MetricFamilySamples> collect() {
		List<MetricFamilySamples> jmxlist = new ArrayList<MetricFamilySamples>();
		try {
			jmxlist.add(buildGauge("system_cpu_count", "Number of sessions currently active",
					runtime.availableProcessors()));
			if (operatingSystemBean.getSystemLoadAverage() >= 0) {
				jmxlist.add(buildGauge("system_load_average_1m", "Number of sessions currently active",
						operatingSystemBean.getSystemLoadAverage()));
			}
			double value = (double) systemCpuUsage.get().invoke(operatingSystemBean);
			if (value >= 0D && systemCpuUsage.isPresent()) {
				jmxlist.add(buildGauge("system_cpu_usage", "Number of sessions currently active", value));
			}
			double processvalue = (double) processCpuUsage.get().invoke(operatingSystemBean);
			if (processvalue >= 0D) {
				jmxlist.add(buildGauge("process_cpu_usage", "Number of sessions currently active", processvalue));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jmxlist;
	}

	private MetricFamilySamples buildGauge(String name, String help, double value) {
		return new MetricFamilySamples(name, Type.GAUGE, help,
				Collections.singletonList(new MetricFamilySamples.Sample(name, commonlabels.getCommonlabelnames(),
						commonlabels.getCommonlabelvalues(), value)));
	}

	private Optional<Method> findOSBeanMethod(String methodName) {
		return OPERATING_SYSTEM_BEAN_CLASS_NAMES.stream().flatMap((cn) -> {
			try {
				return Stream.of(Class.forName(cn));
			} catch (ClassNotFoundException e) {
				return Stream.<Class<?>>empty();
			}
		}).flatMap(clazz -> {
			try {
				clazz.cast(operatingSystemBean);
				return Stream.of(clazz.getDeclaredMethod(methodName));
			} catch (ClassCastException | NoSuchMethodException | SecurityException e) {
				return Stream.empty();
			}
		}).findFirst();
	}

}