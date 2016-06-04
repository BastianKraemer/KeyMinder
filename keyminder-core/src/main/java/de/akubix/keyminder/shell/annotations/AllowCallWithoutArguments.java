package de.akubix.keyminder.shell.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.akubix.keyminder.shell.AbstractShellCommand;

/**
 * A shell {@link AbstractShellCommand} with this annotation can be called without arguments.
 * Any defined {@link Operands} are missing in the parameter map in this case.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowCallWithoutArguments{
}
