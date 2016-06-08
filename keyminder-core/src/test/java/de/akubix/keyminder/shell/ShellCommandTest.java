package de.akubix.keyminder.shell;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URL;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.KeyMinderInstanceBuilder;
import de.akubix.keyminder.shell.commands.NoArgsCmd;
import de.akubix.keyminder.shell.commands.SwitchTestCmd;
import de.akubix.keyminder.shell.commands.TestCmd;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;

public class ShellCommandTest {

	private ApplicationInstance app;

	@Before
	public void prepareInstance(){
		app = KeyMinderInstanceBuilder.getNewInstance("./keyminder_settings.xml");

		final String testFileName = "keyminder_testfile.xml";
		URL url = this.getClass().getResource("/de/akubix/keyminder/" + testFileName);
		app.openFile(new File(url.getFile()));
	}

	@Test
	public void noArgumentsTest() throws CommandException {
		assertEquals("OK",
					 run(new NoArgsCmd(), new String[]{}));
	}

	@Test(expected=CommandException.class)
	public void noArgumentsTest2() throws CommandException {
		assertEquals("OK",
					 run(new NoArgsCmd(), new String[]{"hello world"}));
	}

	@Test(expected=CommandException.class)
	public void noRequireOpenedFileTest() throws CommandException {
		app.closeFile();
		run(new TestCmd(), new String[]{"hello", "world"});
	}

	@Test
	public void testAnonymousArgumentsAnnotation() throws CommandException {
		assertEquals("hello,/My Passwords/E-Mail,world",
					 run(new TestCmd(), new String[]{"hello", "/My Passwords/E-Mail", "world"}));

		assertEquals("hello,world",
				 run(new TestCmd(), new String[]{"hello", "world"}));
	}

	@Test(expected=CommandException.class)
	public void testAnonymousArgumentsAnnotation2() throws CommandException {
		assertEquals("hello,world",
				 run(new TestCmd(), new String[]{"hello world"}));
	}

	@Test
	public void testSwitchAnnotation() throws CommandException {
		assertEquals("req;N;;opt;1,2",
			run(new SwitchTestCmd(), new String[]{"-required", "req"}));

		assertEquals("req;Y;;opt;1,2",
			run(new SwitchTestCmd(), new String[]{"-optional1", "-required", "req"}));

		assertEquals("req;Y;abc;opt;1,2",
			run(new SwitchTestCmd(), new String[]{"-optional1", "-required", "req", "-optional2", "abc"}));

		assertEquals("req;Y;abc;opt;p1,p2",
			run(new SwitchTestCmd(), new String[]
					{"-optional1", "-required", "req", "-optional2", "abc", "-twoparams", "p1", "p2"}));

	}

	@Test(expected=CommandException.class)
	public void testSwitchAnnotation2() throws CommandException {
		run(new SwitchTestCmd(), new String[]{"-optional1"});
	}

	@Test(expected=CommandException.class)
	public void testSwitchAnnotation3() throws CommandException {
		run(new SwitchTestCmd(), new String[]{"-required", "req", "-optional2", "abc", "def"});
	}

	// Helper methods
	private String run(ShellCommand cmd, String[] args) throws CommandException {
		CommandInput in = cmd.parseArguments(app, Arrays.asList(args));
		return getOutput(cmd.exec(app, app, in));
	}

	private String getOutput(CommandOutput out){
		return (String) out.getOutputData();
	}
}
