package de.akubix.keyminder.shell.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Operands {
	int cnt();
	int nodeArgAt() default -1;
	boolean optionalNodeArg() default true;
	String description() default "";
}
