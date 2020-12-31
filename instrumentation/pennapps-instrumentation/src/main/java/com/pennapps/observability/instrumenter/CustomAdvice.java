package com.pennapps.observability.instrumenter;

import static com.pennapps.observability.instrumenter.CustomTracer.TRACER;

import java.lang.reflect.Method;
import io.opentelemetry.context.Scope;
import io.opentelemetry.api.trace.Span;
import net.bytebuddy.asm.Advice;

public class CustomAdvice {
	@Advice.OnMethodEnter(suppress = Throwable.class)
	public static void onEnter(@Advice.Origin Method method, @Advice.Local("otelSpan") Span span,
			@Advice.Local("otelScope") Scope scope) {
		span = TRACER.startSpan(method);
		scope = TRACER.startScope(span);
		
	}

	@Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
	public static void stopSpan(@Advice.Local("otelSpan") Span span, @Advice.Local("otelScope") Scope scope,
			@Advice.Thrown Throwable throwable) {
		scope.close();

		if (throwable != null) {
			TRACER.endExceptionally(span, throwable);
		} else {
			TRACER.end(span);
		}
	}
	
}
