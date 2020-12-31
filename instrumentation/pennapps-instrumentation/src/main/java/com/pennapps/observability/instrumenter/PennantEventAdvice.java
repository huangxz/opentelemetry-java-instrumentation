package com.pennapps.observability.instrumenter;

import java.lang.reflect.Method;
import static com.pennapps.observability.instrumenter.PennantEventTracer.TRACER;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.Scope;
import net.bytebuddy.asm.Advice;

public class PennantEventAdvice {
	public static final ContextKey<Span> CONTEXT_CLIENT_SPAN_KEY = ContextKey
			.named("opentelemetry-trace-auto-client-span-key");

	public static final ContextKey<Span> CONTEXT_SERVER_SPAN_KEY = ContextKey
			.named("opentelemetry-trace-server-span-key");

	@Advice.OnMethodEnter(suppress = Throwable.class)
	public static void onEnter(@Advice.Argument(value = 0, readOnly = false) String name,
			@Advice.Argument(value = 1, readOnly = false) String time, @Advice.Origin Method method,
			@Advice.Local("otelSpan") Span span, @Advice.Local("otelScope") Scope scope) {

		TRACER.setAttributes(name, time);
	}

	@Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
	public static void stopSpan(@Advice.Local("otelSpan") Span span, @Advice.Local("otelScope") Scope scope,
			@Advice.Thrown Throwable throwable) {

	}

}
