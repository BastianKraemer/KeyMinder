package de.akubix.keyminder.modules.keyclip;

import java.util.Properties;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.exceptions.ModuleStartupException;
import de.akubix.keyminder.core.modules.KeyMinderModule;
import de.akubix.keyminder.core.modules.Module;
import de.akubix.keyminder.core.modules.RequireUserInterface;
import de.akubix.keyminder.ui.fx.JavaFxUserInterface;

@KeyMinderModule(name = "KeyClip", properties = "/de/akubix/keyminder/modules/KeyClip.properties")
@RequireUserInterface(JavaFxUserInterface.USER_INTERFACE_ID)
public class KeyClipFactory implements Module {
	@Override
	public void startupModule(ApplicationInstance instance, Properties moduleProperties) throws ModuleStartupException {
		new KeyClip(instance, JavaFxUserInterface.getInstance(instance));
	}
}
