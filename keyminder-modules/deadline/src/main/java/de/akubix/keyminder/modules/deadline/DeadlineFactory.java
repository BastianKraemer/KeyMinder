package de.akubix.keyminder.modules.deadline;

import java.util.Properties;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.exceptions.ModuleStartupException;
import de.akubix.keyminder.core.modules.KeyMinderModule;
import de.akubix.keyminder.core.modules.Module;

@KeyMinderModule(name = "Deadline", properties = "/de/akubix/keyminder/modules/Deadline.properties")
public class DeadlineFactory implements Module {
	@Override
	public void startupModule(ApplicationInstance instance, Properties moduleProperties) throws ModuleStartupException {
		new Deadline(instance);
	}
}
