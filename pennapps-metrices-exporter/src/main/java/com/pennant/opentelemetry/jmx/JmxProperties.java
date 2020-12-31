package com.pennant.opentelemetry.jmx;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.management.Attribute;

public class JmxProperties {

	private String metricname;
	private String metricsdesc;
	private Optional<Double> value;
	private List<String> labelnames;
	private List<String> labelvalues;

	public String getMetricname() {
		return metricname;
	}

	public void setMetricname(String metricname) {
		this.metricname = metricname;
	}

	public String getMetricsdesc() {
		return metricsdesc;
	}

	public void setMetricsdesc(String metricsdesc) {
		this.metricsdesc = metricsdesc;
	}

	public void setValue(Optional<Double> metricValue) {
		this.value = metricValue;

	}

	public Optional<Double> getValue() {
		return value;
	}

	public List<String> getLabelnames() {
		return labelnames;
	}

	public void setLabelnames(List<String> labelnames) {
		this.labelnames = labelnames;
	}

	public List<String> getLabelvalues() {
		return labelvalues;
	}

	public void setLabelvalues(List<String> labelvalues) {
		this.labelvalues = labelvalues;
	}
}
