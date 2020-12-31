package com.pennant.opentelemetry.jmx;

import java.util.List;

public class CommonLabels {
	private List<String> commonlabelnames;
	private List<String> commonlabelvalues;

	public List<String> getCommonlabelnames() {
		return commonlabelnames;
	}

	public void setCommonlabelnames(List<String> commonlabelnames) {
		this.commonlabelnames = commonlabelnames;
	}

	public List<String> getCommonlabelvalues() {
		return commonlabelvalues;
	}

	public void setCommonlabelvalues(List<String> commonlabelvalues) {
		this.commonlabelvalues = commonlabelvalues;
	}
}
