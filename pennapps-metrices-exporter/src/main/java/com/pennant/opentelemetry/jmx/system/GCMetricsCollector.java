package com.pennant.opentelemetry.jmx.system;

import com.pennant.opentelemetry.jmx.CommonLabels;
import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GCMetricsCollector extends Collector {

	private static final String METRIC_NAME_PREFIX = "jvm_gc_";

	private static final String CONCURRENT_PHASE_TIME_METRIC_FULL_NAME = METRIC_NAME_PREFIX + "concurrent_phase_time";

	private static final String PAUSE_METRIC_FULL_NAME = METRIC_NAME_PREFIX + "pause";

	private static final String MEMORY_PROMOTED_METRIC_FULL_NAME = METRIC_NAME_PREFIX + "memory_promoted";

	private static final String MAX_DATA_SIZE_METRIC_FULL_NAME = METRIC_NAME_PREFIX + "max_data_size";

	private static final String LIVE_DATA_SIZE_METRIC_FULL_NAME = METRIC_NAME_PREFIX + "live_data_size";

	private static final String MEMORY_ALLOCATED_METRIC_FULL_NAME = METRIC_NAME_PREFIX + "memory_allocated";

	private List<MetricFamilySamples> jmxlist = new ArrayList<MetricFamilySamples>();
	private String youngGenPoolName;
	private String oldGenPoolName;
	private long youngGenSizeAfter = 0L;
	private CommonLabels commonlabels;

	public GCMetricsCollector(CommonLabels commonlabels) {
		this.commonlabels = commonlabels;
		initPoolProperties();

	}

	@Override
	public <T extends Collector> T register(CollectorRegistry registry) {

		return super.register(registry);
	}

	@Override
	public List<MetricFamilySamples> collect() {
		// NotificationListener notificationListener = this::handleNotification;
		for (GarbageCollectorMXBean mbean : ManagementFactory.getGarbageCollectorMXBeans()) {
			NotificationListener notificationListener = new NotificationListener() {

				@Override
				public void handleNotification(Notification notification, Object handback) {
					try {
						jmxlist.clear();
						String type = notification.getType();
						if (type.equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
							CompositeData cd = (CompositeData) notification.getUserData();
							GarbageCollectionNotificationInfo notificationInfo = GarbageCollectionNotificationInfo
									.from(cd);

							if (isConcurrentPhase(notificationInfo.getGcCause())) {
								jmxlist.add(buildGauge(CONCURRENT_PHASE_TIME_METRIC_FULL_NAME,
										"gc concurent phase time", notificationInfo.getGcInfo().getDuration()));
							} else {
								List<String> lnames = new ArrayList<>();
								List<String> lvalues = new ArrayList<>();
								lnames.addAll(commonlabels.getCommonlabelnames());
								lvalues.addAll(commonlabels.getCommonlabelvalues());
								lnames.add("cause");
								lnames.add("action");
								lvalues.add(notificationInfo.getGcCause());
								lvalues.add(notificationInfo.getGcAction());

								jmxlist.add(buildcounter(PAUSE_METRIC_FULL_NAME, "gc pause ",
										notificationInfo.getGcInfo().getDuration(), lnames, lvalues));

							}
							GcInfo gcInfo = notificationInfo.getGcInfo();

							Map<String, MemoryUsage> before = gcInfo.getMemoryUsageBeforeGc();
							Map<String, MemoryUsage> after = gcInfo.getMemoryUsageAfterGc();

							if (oldGenPoolName != null) {
								long oldBefore = before.get(oldGenPoolName).getUsed();
								long oldAfter = after.get(oldGenPoolName).getUsed();

								long bytes = oldAfter - oldBefore;
								if (bytes > 0L) {
									jmxlist.add(buildGauge(MEMORY_PROMOTED_METRIC_FULL_NAME, "gc promoted", bytes));

									if (oldAfter < oldBefore || GcGenerationAge
											.fromName(notificationInfo.getGcName()) == GcGenerationAge.OLD) {

										jmxlist.add(buildGauge(LIVE_DATA_SIZE_METRIC_FULL_NAME, "gc live data", bytes));
										jmxlist.add(
												buildGauge(MAX_DATA_SIZE_METRIC_FULL_NAME, "gc max data size", bytes));
									}
								}
							}

							if (youngGenPoolName != null) {
								long youngBefore = before.get(youngGenPoolName).getUsed();
								long youngAfter = after.get(youngGenPoolName).getUsed();
								long delta = youngBefore - youngGenSizeAfter;
								youngGenSizeAfter = youngAfter;
								if (delta > 0L) {

									jmxlist.add(buildGauge(MEMORY_ALLOCATED_METRIC_FULL_NAME, "gc memory allocated",
											delta));

								}
							}
						}
					} catch (Exception e) {
					}

				}
			};
			if (mbean instanceof NotificationEmitter) {
				((NotificationEmitter) mbean).addNotificationListener(notificationListener, null, null);
			}
		}

		return jmxlist;

	}

	private void initPoolProperties() {
		for (MemoryPoolMXBean mbean : ManagementFactory.getMemoryPoolMXBeans()) {
			if (isYoungGenPool(mbean.getName())) {
				youngGenPoolName = mbean.getName();
			}
			if (isOldGenPool(mbean.getName())) {
				oldGenPoolName = mbean.getName();
			}
		}
	}

	private boolean isConcurrentPhase(String cause) {
		return "No GC".equals(cause);
	}

	private boolean isOldGenPool(String name) {
		return name.endsWith("Old Gen") || name.endsWith("Tenured Gen");
	}

	private boolean isYoungGenPool(String name) {
		return name.endsWith("Eden Space");
	}

	private MetricFamilySamples buildGauge(String name, String help, double value) {
		return new MetricFamilySamples(name, Type.GAUGE, help,
				Collections.singletonList(new MetricFamilySamples.Sample(name, commonlabels.getCommonlabelnames(),
						commonlabels.getCommonlabelvalues(), value)));
	}

	private static MetricFamilySamples buildcounter(String name, String help, double value, List<String> lnames,
			List<String> lvalues) {
		return new MetricFamilySamples(name, Type.COUNTER, help,
				Collections.singletonList(new MetricFamilySamples.Sample(name, lnames, lvalues, value)));
	}
}

/**
 * Generalization of which parts of the heap are considered "young" or "old" for
 * multiple GC implementations
 */
enum GcGenerationAge {
	OLD, YOUNG, UNKNOWN;

	private static Map<String, GcGenerationAge> knownCollectors = new HashMap<String, GcGenerationAge>() {
		{
			put("ConcurrentMarkSweep", OLD);
			put("Copy", YOUNG);
			put("G1 Old Generation", OLD);
			put("G1 Young Generation", YOUNG);
			put("MarkSweepCompact", OLD);
			put("PS MarkSweep", OLD);
			put("PS Scavenge", YOUNG);
			put("ParNew", YOUNG);
		}
	};

	static GcGenerationAge fromName(String name) {
		GcGenerationAge t = knownCollectors.get(name);
		return (t == null) ? UNKNOWN : t;
	}
}