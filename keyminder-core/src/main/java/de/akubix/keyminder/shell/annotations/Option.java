package de.akubix.keyminder.shell.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Options.class)
public @interface Option {
	/**
	 * The name of a argument should start with "-" or "--"
	 * @return The name of the argument switch
	 */
	String name();

	/**
	 * The number of arguments followed by this switch. For example '2' for a command like 'echo "hello" "world"'
	 * @return The parameter count
	 */
	int paramCnt() default 0;


	/**
	 * The default value if the switch is not present
	 * @return The default value for these options
	 */
	String[] defaultValue() default {};

	/**
	 * The default value if the switch is not present
	 * @return The default value for these options
	 */
	String[] alias() default {};
}
