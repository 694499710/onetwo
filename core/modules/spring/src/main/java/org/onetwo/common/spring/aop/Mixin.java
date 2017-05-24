package org.onetwo.common.spring.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * @author wayshall
 * <br/>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Mixin {
	/***
	 * implementor
	 * @author wayshall
	 * @return
	 */
	Class<?> value();
	MixinInitor initor() default MixinInitor.REFLECTION;

	enum MixinInitor {
		SPRING,
		REFLECTION
	}
}
