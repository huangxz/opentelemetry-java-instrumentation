package io.opentelemetry.instrumentation.api.tracer;

import javax.servlet.http.HttpServletRequest;

import io.opentelemetry.context.propagation.TextMapPropagator;

public class HttpSetter implements TextMapPropagator.Setter<HttpServletRequest> {

	 public static final HttpSetter SETTER = new HttpSetter();
	@Override
	public void set(HttpServletRequest carrier, String key, String value) {
		carrier.setAttribute(key, value);
	}

}
