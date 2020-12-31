package io.opentelemetry.javaagent.instrumentation.servlet.v3_0;

import static io.opentelemetry.javaagent.instrumentation.servlet.v3_0.Servlet3HttpServerTracer.tracer;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.bootstrap.AgentInitializer;
import io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.bytebuddy.asm.Advice;

public class Servlet3Advice {
	public static final String JAEGER_SERNAME = "otel.exporter.jaeger.service.name";
	@Advice.OnMethodEnter(suppress = Throwable.class)
	public static void onEnter(@Advice.Argument(value = 0, readOnly = false) ServletRequest request,
			@Advice.Argument(value = 1, readOnly = false) ServletResponse response, @Advice.Local("otelSpan") Span span,
			@Advice.Local("otelScope") Scope scope) {
		if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
			return;
		}
		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		Context attachedContext = tracer().getServerContext(httpServletRequest);
		if (attachedContext != null) {
			if (tracer().needsRescoping(attachedContext)) {
				scope = attachedContext.makeCurrent();
			}

			// We are inside nested servlet/filter, don't create new span
			return;
		}
		Context ctx = tracer().startSpan(httpServletRequest);
		span = Java8BytecodeBridge.spanFromContext(ctx);
		scope = tracer().startScope(span, httpServletRequest);
		startMetricExporter();
	}

	@Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
	public static void stopSpan(@Advice.Argument(0) ServletRequest request,
			@Advice.Argument(1) ServletResponse response, @Advice.Thrown Throwable throwable,
			@Advice.Local("otelSpan") Span span, @Advice.Local("otelScope") Scope scope) {
		if (scope == null) {
			return;
		}
		scope.close();
		if (span == null) {
			// an existing span was found
			return;
		}
		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		HttpServletResponse httpServletResponse = (HttpServletResponse) response;
		if(httpServletRequest.getRequestURI().endsWith("zkau") && httpServletResponse.getHeader("Content-Encoding")== null)
		{
			Context parentContext = (Context) request.getAttribute("eventContext");
			Span sp = Java8BytecodeBridge.spanFromContext(parentContext);
			String[] s = sp.toString().split("name=");
			s = s[1].split(",");
			if(s[0].equals("/PFSWeb/zkau")){
			return;
			}
		}
		
		tracer().setPrincipal(span, httpServletRequest);
		if (throwable != null) {
			tracer().endExceptionally(span, throwable, (HttpServletResponse) response);
			endParentSpan((HttpServletRequest) request);
			return;
		}

		AtomicBoolean responseHandled = new AtomicBoolean(false);

		// In case of async servlets wait for the actual response to be ready
		if (request.isAsyncStarted()) {
			try {
				request.getAsyncContext().addListener(new TagSettingAsyncListener(responseHandled, span));
			} catch (IllegalStateException e) {
				// org.eclipse.jetty.server.Request may throw an exception here
				// if request became
				// finished after check above. We just ignore that exception and
				// move on.
			}
		}

		// Check again in case the request finished before adding the listener.
		if (!request.isAsyncStarted() && responseHandled.compareAndSet(false, true)) {
			tracer().end(span, (HttpServletResponse) response);
			endParentSpan((HttpServletRequest) request);
		}
	
	}
		
	public static void endParentSpan(HttpServletRequest request){
		Context parentContext = (Context) request.getAttribute("eventContext");
		Scope scope = (Scope) request.getAttribute("eventScope");
		if(parentContext != null){
			
			Span span = Java8BytecodeBridge.spanFromContext(parentContext);
			if(request.getAttribute("EventstartTime") != null){
				span.setAttribute("3.Event_start_Time", request.getAttribute("EventstartTime").toString());
				}
			if(request.getAttribute("EventEndTime") != null){
			span.setAttribute("4.Event_end_Time", request.getAttribute("EventEndTime").toString());
			}
			scope.close();
			span.end();
		}
	}
	
	public static void startMetricExporter(){
		if(!AgentInitializer.metric_server_start){
			 //Try to start the metrices exporter
			AgentInitializer.metric_server_start = true;
		      String isMetrices = Config.get().getProperty("otel.metrics.enable");
		      if(isMetrices != null && isMetrices.equals("true")){
		    	  System.out.println("The Jmx Metric Server Started  @7247");
		    	  Class<?> tracerInstallerClass;
		  		try {
		  			tracerInstallerClass = AgentInitializer.AGENT_CLASSLOADER
		  					.loadClass("com.pennant.opentelemetry.jmx.JmxMetricsExport");

		  			Method tracerInstallerMethod =  tracerInstallerClass.getMethod("init");
		  			tracerInstallerMethod.invoke(null);
		  		} catch (Exception e) {
		  			e.printStackTrace();
		  		}
		      }
		}
	}
	
}
