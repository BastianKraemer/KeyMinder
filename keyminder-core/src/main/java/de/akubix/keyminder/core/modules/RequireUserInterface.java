package de.akubix.keyminder.core.modules;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.akubix.keyminder.ui.KeyMinderUserInterface;

/**
 * This annotation can be used in combination with {@link KeyMinderModule}.
 * The {@link Preload} annotation tells KeyMinder that this module depends on a specific user interface,
 * so it will be loaded only if {@link RequireUserInterface#value()} matches the user interface identifier ({@link KeyMinderUserInterface#id()}).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireUserInterface {
	String value();
}
