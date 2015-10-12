package de.akubix.keyminder.core.interfaces;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ModuleProperties {
    public String name();			// Full module name
    public String description();	// Description of the module
    public String version();		// Module version, use "." to use the current version of this application
    public String dependencies();	// List of all modules that should be started before this module (separated by ";")
    public String author();			// The modules author
}
