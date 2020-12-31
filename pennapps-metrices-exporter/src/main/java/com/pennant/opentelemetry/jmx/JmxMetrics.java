package com.pennant.opentelemetry.jmx;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class JmxMetrics {
	private final Cache<MBeanServerConnection, JmxMBeanPropertyCache> jmxMBeanPropertyCacheMap = CacheBuilder
			.newBuilder().weakKeys().build();
	private final static  String METRIC_NAME_PREFIX = "jvm_jmx_";

	private final static char METRIC_SEPARATOR = '_';
	private boolean lowerCaseMetricName = true;
	private List<JmxProperties> jmx = new ArrayList<JmxProperties>();
	private AttributeList attributes;

	public List<JmxProperties> createMetrics() {
		ManagementFactory.getPlatformMBeanServer();
		jmx.clear();
		ArrayList<MBeanServer> mBeanServers = MBeanServerFactory.findMBeanServer(null);
		for (MBeanServer server : mBeanServers) {			
			try {
				JmxMBeanPropertyCache jmxMBeanPropertyCache = resolveJmxMBeanPropertyCache(server);
				doScrape(server, jmxMBeanPropertyCache);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return jmx;

	}

	private JmxMBeanPropertyCache resolveJmxMBeanPropertyCache(MBeanServer server) throws ExecutionException {
		return jmxMBeanPropertyCacheMap.get(server, JmxMBeanPropertyCache::new);
	}

	public void doScrape(MBeanServerConnection mBeanServerConnection, JmxMBeanPropertyCache jmxMBeanPropertyCache)
			throws Exception {
		Set<ObjectName> mBeanNames = new HashSet<ObjectName>();

		for (ObjectInstance instance : mBeanServerConnection.queryMBeans(null, null)) {
			mBeanNames.add(instance.getObjectName());
		}
		for (GarbageCollectorMXBean mbean : ManagementFactory.getGarbageCollectorMXBeans()) {
			mBeanNames.add(mbean.getObjectName());
		}
		jmxMBeanPropertyCache.onlyKeepMBeans(mBeanNames);

		for (ObjectName objectName : mBeanNames) {
			if (objectName.toString().contains("org.apache.logging.log4j2")) {
				continue;
			}
			scrapeBean(mBeanServerConnection, objectName, jmxMBeanPropertyCache);
		}
	}

	private void scrapeBean(MBeanServerConnection beanConn, ObjectName mbeanName,
			JmxMBeanPropertyCache jmxMBeanPropertyCache) {
		MBeanInfo info;
		try {
			info = beanConn.getMBeanInfo(mbeanName);
		} catch (IOException | JMException e) {

			return;
		}
		MBeanAttributeInfo[] attrInfos = info.getAttributes();

		Map<String, MBeanAttributeInfo> name2AttrInfo = new LinkedHashMap<String, MBeanAttributeInfo>();
		for (int idx = 0; idx < attrInfos.length; ++idx) {
			MBeanAttributeInfo attr = attrInfos[idx];
			if (!attr.isReadable()) {

				continue;
			}
			name2AttrInfo.put(attr.getName(), attr);
 
		}

		try {
			attributes = beanConn.getAttributes(mbeanName, name2AttrInfo.keySet().toArray(new String[0]));

		} catch (Exception e) {

			return;
		}
		for (Attribute attribute : attributes.asList()) {
			MBeanAttributeInfo attr = name2AttrInfo.get(attribute.getName());
			processBeanValue(mbeanName.getDomain(), jmxMBeanPropertyCache.getKeyPropertyList(mbeanName),
					new LinkedList<String>(), attr.getName(), attr.getType(), attr.getDescription(),
					attribute.getValue());
		}
	}

	public void processBeanValue(String domain, LinkedHashMap<String, String> beanProperties,
			LinkedList<String> attrKeys, String attrName, String attrType, String attrDescription, Object value) {
		if (value == null) {
		} else if (value instanceof Number || value instanceof String || value instanceof Boolean) {
			 recordBean(domain, beanProperties, attrKeys, attrName, attrType, attrDescription, value);
		} else if (value instanceof CompositeData) {
			CompositeData composite = (CompositeData) value;
			CompositeType type = composite.getCompositeType();
			attrKeys = new LinkedList<String>(attrKeys);
			attrKeys.add(attrName);
			for (String key : type.keySet()) {
				String typ = type.getType(key).getTypeName();
				Object valu = composite.get(key);
				processBeanValue(domain, beanProperties, attrKeys, key, typ, type.getDescription(), valu);
			}
		}
		else {
		}
		
	}

	private JmxProperties recordBean(String domain, LinkedHashMap<String, String> beanProperties,
			LinkedList<String> attrKeys, String attrName, String attrType, String attrDescription, Object value) {
		
		List<String> LABEL_NAMES = new ArrayList<String>();
		List<String> LABEL_VALUES = new ArrayList<String>();

		String metricName = metricName(domain, beanProperties, attrKeys, attrName);
		JmxProperties j = new JmxProperties();

		j.setMetricname(metricName);
		j.setMetricsdesc(attrDescription);
		j.setValue(metricValue(value));
		Map<String, String> labels = new HashMap<String, String>();
		beanProperties.entrySet().stream().skip(1).forEach(entry -> labels.put(entry.getKey(), entry.getValue()));
		for (Map.Entry<String, String> label : labels.entrySet()) {
			LABEL_NAMES.add(label.getKey());
			LABEL_VALUES.add(label.getValue());
		}
		j.setLabelnames(LABEL_NAMES);
		j.setLabelvalues(LABEL_VALUES);
		jmx.add(j);

		return j;
	}

	private Optional<Double> metricValue(Object value) {
		if (value instanceof Number) {
			return Optional.of(((Number) value).doubleValue()).filter(d -> d >= 0d);
		} else if (value instanceof Boolean) {
			return Optional.of(((Boolean) value) ? 1d : 0d);
		} else {
			return Optional.of(0d);
		}
	}

	private String metricName(String domain, LinkedHashMap<String, String> beanProperties, LinkedList<String> attrKeys,
			String attrName) {
		StringBuilder stringBuilder = new StringBuilder(METRIC_NAME_PREFIX);
		stringBuilder.append(domain.replace('.', METRIC_SEPARATOR));

		if (beanProperties != null && beanProperties.size() > 0) {
			stringBuilder.append(METRIC_SEPARATOR);
			stringBuilder.append(beanProperties.values().iterator().next());
		}

		attrKeys.forEach(key -> {
			stringBuilder.append(METRIC_SEPARATOR);
			stringBuilder.append(key);
		});

		stringBuilder.append(METRIC_SEPARATOR);
		stringBuilder.append(attrName);

		String result = stringBuilder.toString();

		if (lowerCaseMetricName) {
			return result.toLowerCase();
		} else {
			return result;
		}
	}

}
