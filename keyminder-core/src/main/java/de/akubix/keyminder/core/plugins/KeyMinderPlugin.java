package de.akubix.keyminder.core.plugins;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Every KeyMinder plugin (or its 'factory') has to be annotated with this annotation.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface KeyMinderPlugin {
	/**
	 * The name of the KeyMinder plugin
	 * @return The plugin name
	 */
	String name();

	/**
	 * A path to the properties file that contains a description for this plugin and, at your option, some configuration values
	 * <br><strong>Note: The properties file has to be UTF-8 encoded.</strong>
	 * <br>
	 * <br>A plugin properties file should look like this (feel free to append your configurations values)
	 * <pre>
	 * version = ${project.version}
	 * author = Your name
	 *
	 * description = An description of your plugin in English
	 * description_de = A description of your plugin in German (you can remove this property if you don't want to specify a German translation)
	 * </pre>
	 * @return The path to the plugin properties file (for example '/de/akubix/keyminder/plugins/my-plugin.properties')
	 */
	String properties();
}
