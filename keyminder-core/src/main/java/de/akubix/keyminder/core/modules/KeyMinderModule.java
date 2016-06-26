package de.akubix.keyminder.core.modules;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Every KeyMinder module (or its 'factory') has to be annotated with this annotation.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface KeyMinderModule {
	/**
	 * The name of the KeyMinder module
	 * @return The module name
	 */
	String name();

	/**
	 * A path to the properties file taht contains a description for this module and, at your option, some configuration values
	 * <br><strong>Note: The properties file has to be UTF-8 encoded.</strong>
	 * <br>
	 * <br>A module properties file should look like this (feel free to append your configurations values)
	 * <pre>
	 * version = ${project.version}
	 * author = Your name
	 *
	 * description = An description of your module in English
	 * description_de = A description of your module in German (you can remove this property if you don't want to specify a German translation)
	 * </pre>
	 * @return The path to the module properties file (for example '/de/akubix/keyminder/modules/my-module.properties')
	 */
	String properties();
}
