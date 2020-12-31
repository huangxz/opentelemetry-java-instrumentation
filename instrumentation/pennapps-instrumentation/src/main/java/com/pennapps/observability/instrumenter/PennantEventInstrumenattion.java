package com.pennapps.observability.instrumenter;

import java.util.Map;

import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.google.auto.service.AutoService;

import io.opentelemetry.javaagent.tooling.Instrumenter;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(Instrumenter.class)
public class PennantEventInstrumenattion extends Instrumenter.Default{
	public PennantEventInstrumenattion(){
		super("PennantEvent");
	}

	@Override
	public ElementMatcher<? super TypeDescription> typeMatcher() {
		return named("com.pennapps.web.observability.PerformanceMeter");
	}

	@Override
	public String[] helperClassNames() {
		return new String[] {packageName + ".PennantEventAdvice", packageName + ".PennantEventTracer"};
	}
	
	@Override
	public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
		return singletonMap(
				isMethod()
				.and(named("call"))
				.and(takesArgument(0, named("java.lang.String")))
		        .and(takesArgument(1, named("java.lang.String"))),
				packageName + ".PennantEventAdvice");
	}

}
