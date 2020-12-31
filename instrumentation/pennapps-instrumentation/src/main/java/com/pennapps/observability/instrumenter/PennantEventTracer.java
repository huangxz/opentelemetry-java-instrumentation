package com.pennapps.observability.instrumenter;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.tracer.BaseTracer;

public class PennantEventTracer extends BaseTracer {
	public static final PennantEventTracer TRACER = new PennantEventTracer();

	@Override
	protected String getInstrumentationName() {
		return "PennantEvent";
	}

	public void setAttributes(String name, String time) {
		Span span = Context.current().get(CONTEXT_CLIENT_SPAN_KEY);
		span.setAttribute(name, time);
	}

}
