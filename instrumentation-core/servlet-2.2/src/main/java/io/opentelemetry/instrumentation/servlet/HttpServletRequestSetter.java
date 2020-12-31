package io.opentelemetry.instrumentation.servlet;

import javax.servlet.http.HttpServletRequest;

import io.opentelemetry.context.propagation.TextMapPropagator;

public class HttpServletRequestSetter implements TextMapPropagator.Getter<HttpServletRequest>{
	public static final HttpServletRequestSetter SETTER = new HttpServletRequestSetter();
	@Override
	public Iterable<String> keys(HttpServletRequest carrier) {
		// TODO Auto-generated method stub
		return null;
	}
	 @Override
	  public String get(HttpServletRequest carrier, String key) {
	    return carrier.getHeader("ZK-SID");
	  }
	

}
