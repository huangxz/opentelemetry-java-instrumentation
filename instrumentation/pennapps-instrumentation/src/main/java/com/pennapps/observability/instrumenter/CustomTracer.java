package com.pennapps.observability.instrumenter;

import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;

import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.impl.EventProcessingThreadImpl;
import org.zkoss.zul.Button;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Treeitem;

import com.pennanttech.pennapps.web.menu.MenuItem;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Span.Kind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.tracer.BaseTracer;
import io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge;

public class CustomTracer extends BaseTracer {
	public static final CustomTracer TRACER = new CustomTracer();
	public static final String ZKID = "ZK-SID";
	public static final String SPAN_NAME = "Span-Name";
	public static final String SERVICE_NAME = "Service_Name";
	public static final String JAEGER_SERNAME = "otel.exporter.jaeger.service.name";

	@Override
	protected String getInstrumentationName() {
		return "io.opentelemetry.auto.PLF";
	}

	public Span startSpan(Method method) {
		String spanName = spanNameForMethod(method);
		return startSpan(spanName, Kind.INTERNAL);
	}

	public Span startSpan(String spanName, Kind kind) {
		Context parentContext = getParentContext(spanName);
		Span span;
		if (parentContext != null) {
			span = tracer.spanBuilder(spanName).setSpanKind(kind).setParent(parentContext).startSpan();
		} else {
			span = tracer.spanBuilder(spanName).setSpanKind(kind).startSpan();
		}
		return span;
	}

	public Scope startScope(Span span) {
		Context clientSpanContext = Context.current().with(CONTEXT_CLIENT_SPAN_KEY, span);
		Context newContext = clientSpanContext.with(span);
		return newContext.makeCurrent();
	}

	public void end(Span span) {
		endSpan(span, -1);
	}

	public void endSpan(Span span, long endTimeNanos) {
		if (endTimeNanos >= 0) {
			span.end(endTimeNanos);
		} else {
			span.end();
		}
	}

	public void endExceptionally(Span span, Throwable throwable) {
		endExceptionally(span, throwable, -1);
	}

	public void endExceptionally(Span span, Throwable throwable, long timestamp) {
		onError(span, unwrapThrowable(throwable));
		endSpan(span, timestamp);
	}

	public Context getParentContext(String spanName) {

		if (Span.getInvalid() != Java8BytecodeBridge.spanFromContext(Context.current())) {
			return null;
		}

		if (!(Thread.currentThread() instanceof EventProcessingThreadImpl)) {
			return null;
		}

		try {
			HttpServletRequest httpServletRequest = getRequestfromThread();
			String zkSid = httpServletRequest.getHeader(ZKID);
			Context httpContext;
			if (zkSid != null) {
				httpContext = (Context) httpServletRequest.getAttribute(zkSid);
			} else {
				httpContext = (Context) httpServletRequest
						.getAttribute(httpServletRequest.getSession().getId().toString());
			}
			updateSpanDetails(httpContext, httpServletRequest, spanName);
			return httpContext;

		} catch (Exception e) {
			return null;
		}

	}

	protected HttpServletRequest getRequestfromThread() {
		EventProcessingThreadImpl emp = (EventProcessingThreadImpl) Thread.currentThread();
		Desktop desktop = emp.getComponent().getDesktop();
		HttpServletRequest httpServletRequest = (HttpServletRequest) desktop.getExecution().getNativeRequest();
		return httpServletRequest;
	}

	protected void updateSpanDetails(Context context, HttpServletRequest httpServletRequest, String spanName) {

		Span httpSpan = Java8BytecodeBridge.spanFromContext(context);
		String contrlName = spanName.split("-")[0];
		EventProcessingThreadImpl emp = (EventProcessingThreadImpl) Thread.currentThread();
		spanName = (String) httpServletRequest.getAttribute(SPAN_NAME);

		if (spanName == null) {
			String eventName = emp.getEvent().getName();
			Span parentSpan = getParentSpan(httpServletRequest);
			if (parentSpan != null) {
				updateSpanName(eventName, parentSpan, emp, contrlName);
				httpSpan.updateName("Http Request");
			} else {
				updateSpanName(eventName, httpSpan, emp, contrlName);
			}
			httpServletRequest.setAttribute(SPAN_NAME, emp.getEvent().getName());
		}
	}

	protected Span getParentSpan(HttpServletRequest request) {
		Context context = (Context) request.getAttribute("eventContext");
		if (context == null) {
			return null;
		}
		return Java8BytecodeBridge.spanFromContext(context);

	}

	protected void updateSpanName(String eventName, Span span, EventProcessingThreadImpl emp, String contrlName) {
		if (emp.getEvent() instanceof ForwardEvent) {
			ForwardEvent event = (ForwardEvent) emp.getEvent();
			if (eventName.equals("onMenuItemClick")) {
				Treeitem treeitem = (Treeitem) event.getOrigin().getTarget();
				MenuItem menuItem = (MenuItem) treeitem.getAttribute("data");
				updateName(span, eventName, "-", treeitem.getLabel());
				span.setAttribute("PAGE-URL", menuItem.getNavigateUrl());
			} else if (eventName.contains("onClick$")) {
				Button button = (Button) event.getOrigin().getTarget();
				if (!button.getLabel().equals("")) {
					updateName(span, "OnClick", "-", button.getLabel(), " Button :", contrlName);
				} else {
					updateName(span, eventName, " :", contrlName);
				}
			} else if (eventName.contains("DoubleClick")) {
				updateName(span, "OnDoubleClick-ListItem", ":", contrlName);

			} else if (eventName.contains("onSelectTab")) {
				Tab tab = (Tab) event.getOrigin().getTarget();
				updateName(span, "ONSelectTab-", tab.getLabel(), " :", contrlName);
			} else if (eventName.contains("onFilterMenuItem")) {
				Menuitem menuItem = (Menuitem) event.getOrigin().getTarget();
				updateName(span, eventName, "-", menuItem.getLabel());
			}else{
				updateName(span, eventName, ": ", contrlName);
			}

		} else {
			updateName(span, eventName, ": ", contrlName);
		}

	}

	private void updateName(Span span, String... args) {
		String name = "";

		for (String arg : args) {
			name = name + arg;
		}

		span.updateName(name);
	}
}
