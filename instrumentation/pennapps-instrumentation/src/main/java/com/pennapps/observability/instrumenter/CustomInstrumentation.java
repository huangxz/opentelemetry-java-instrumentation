package com.pennapps.observability.instrumenter;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.safeHasSuperType;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isProtected;
import static net.bytebuddy.matcher.ElementMatchers.isPrivate;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.not;

import java.io.FileReader;
import java.util.Map;
import java.util.Properties;

import com.google.auto.service.AutoService;

import io.opentelemetry.javaagent.tooling.Instrumenter;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import net.bytebuddy.matcher.ElementMatcher.Junction.Disjunction;
import net.bytebuddy.matcher.NameMatcher;

@SuppressWarnings("deprecation")
@AutoService(Instrumenter.class)
public class CustomInstrumentation extends Instrumenter.Default {
	private static Properties properties;
	private static final String SUPER_CLASS = "observability.instrumentation.super.classes";
	private static final String METHODS = "observability.instrumentation.untrace.methods";
	private static final String LIKE_METHODS = "observability.instrumentation.untrace.islike.methods";
	public CustomInstrumentation() {
		super("custom");
	}

	@Override
	public ElementMatcher<TypeDescription> typeMatcher() {
		loadProperites();
		return safeHasSuperType(getSuperClasses());
	}

	@Override
	public String[] helperClassNames() {
		return new String[] { packageName + ".CustomTracer", packageName + ".CustomAdvice", };
	}

	@Override
	public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
		if(getProperity(METHODS) == null && getProperity(LIKE_METHODS) == null){
			return singletonMap(
					isMethod()
					.and(isPublic().or(isProtected()).or(isPrivate()))
					.and(not(isStatic())),
					packageName + ".CustomAdvice");
		}else if(getProperity(METHODS) != null && getProperity(LIKE_METHODS) != null){
			return singletonMap(
					isMethod()
					.and(not(getUnTraceMethods()))
					.and(not(getUnTraceLikeMethods()))
					.and(isPublic().or(isProtected()).or(isPrivate()))
					.and(not(isStatic())),
					packageName + ".CustomAdvice");
		}else if(getProperity(METHODS) != null){
			return singletonMap(
					isMethod()
					.and(not(getUnTraceMethods()))
					.and(isPublic().or(isProtected()).or(isPrivate()))
					.and(not(isStatic())),
					packageName + ".CustomAdvice");
		}else{
		return singletonMap(
				isMethod()
				.and(not(getUnTraceLikeMethods()))
				.and(isPublic().or(isProtected()).or(isPrivate()))
				.and(not(isStatic())),
				packageName + ".CustomAdvice");
		}
	}

	private static  <T extends NamedElement> ElementMatcher.Junction<T> getUnTraceMethods() {
		NameMatcher<?> nm = null;
		Disjunction<?> d = null;
		String unTraceMethods = getProperity(METHODS);
		unTraceMethods = unTraceMethods == null ? "" : unTraceMethods;
		String[] methods = unTraceMethods.split(",");
		return getTypeMatcher(methods , nm, d);
	}

	@SuppressWarnings("unchecked")
	private static <T extends NamedElement> ElementMatcher.Junction<T> getUnTraceLikeMethods() {
		NameMatcher<?> nm = null;
		Disjunction<?> d = null;
		String likeMethods = getProperity(LIKE_METHODS);
		likeMethods = likeMethods == null ? "" : likeMethods;
		String[] methods = likeMethods.split(",");
		nm = (NameMatcher<?>) nameStartsWith(methods[0]);
		if (methods.length == 1) {
			return (Junction<T>) nm;
		} else {
			for (int i = 1; i <= methods.length - 1; i++) {
				if (i != 1) {
					d = (Disjunction<?>) d.or(nameStartsWith(methods[i]));
				} else {
					d = (Disjunction<?>) nm.or(nameStartsWith(methods[i]));
				}

			}
		}
		return (Junction<T>) d;
	}

	private static <T extends NamedElement> ElementMatcher.Junction<T> getSuperClasses() {
		NameMatcher<?> nm = null;
		Disjunction<?> d = null;
		String superClasses = getProperity(SUPER_CLASS);
		superClasses = superClasses == null ? "" : superClasses;
		String[] superClass = superClasses.split(",");
		return getTypeMatcher(superClass, nm, d);
	}
	
	@SuppressWarnings("unchecked")
	private static <T extends NamedElement> ElementMatcher.Junction<T> getTypeMatcher(String[] strArr, NameMatcher<?> nm, Disjunction<?> d){
		nm = (NameMatcher<?>) named(strArr[0]);
		if (strArr.length == 1) {
			return (Junction<T>) nm;
		} else {
			for (int i = 1; i <= strArr.length - 1; i++) {
				if (i != 1) {
					d = (Disjunction<?>) d.or(named(strArr[i]));
				} else {
					d = (Disjunction<?>) nm.or(named(strArr[i]));
				}
			}
		}
		return (Junction<T>) d;
		
	}
	
	private static void loadProperites(){
		try{
			String path = System.getenv("PFF_HOME");
			path = path+"/config/pff.properties";
			System.out.println("The Properties File loaded from "+path);
			 FileReader reader=new FileReader(path);  
			 properties = new Properties();  
			 properties.load(reader); 
		}catch(Exception e){
			System.out.println(e);
		}
		
	}
	
	private static String getProperity(String key){
		try{
			return properties.getProperty(key);
			
		}catch(Exception e){
			return "";
		}
	}
	
}
