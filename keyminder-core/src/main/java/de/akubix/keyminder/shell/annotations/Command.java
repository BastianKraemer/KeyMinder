package de.akubix.keyminder.shell.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.akubix.keyminder.shell.AbstractShellCommand;

/**
 * Any class that should be used as shell command needs to be annotated with @{@link Command} and must implement  {@link AbstractShellCommand}.
 * It is recommended to extend the {@link AbstractShellCommand} class for most command implementations.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {
	String value();
}
