package de.akubix.keyminder.plugins.keyclip;

import java.util.Properties;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.exceptions.PluginStartupException;
import de.akubix.keyminder.core.plugins.KeyMinderPlugin;
import de.akubix.keyminder.core.plugins.Plugin;
import de.akubix.keyminder.core.plugins.RequireUserInterface;
import de.akubix.keyminder.ui.fx.JavaFxUserInterface;

@KeyMinderPlugin(name = "KeyClip", properties = "/de/akubix/keyminder/plugins/KeyClip.properties")
@RequireUserInterface(JavaFxUserInterface.USER_INTERFACE_ID)
public class KeyClipFactory implements Plugin {
	@Override
	public Object startup(ApplicationInstance instance, Properties pluginProperties) throws PluginStartupException {
		return new KeyClip(instance, JavaFxUserInterface.getInstance(instance));
	}
}
