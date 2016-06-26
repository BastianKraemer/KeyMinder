package de.akubix.keyminder.core.modules;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used in combination with {@link KeyMinderModule}.
 * The {@link Preload} annotation tells KeyMinder to load other modules at first (if they are enabled).
 * <br><strong>Note: There is no warranty that all the modules listet in the annotation are started, if they are disabled by the user, they won't be loaded.</strong>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Preload {
	String[] value();
}
