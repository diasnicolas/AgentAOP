package framework.aop.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import framework.aop.IAspect;

@Target(ElementType.METHOD)
public @interface After {
	
	Class<? extends IAspect>[] value();
}
