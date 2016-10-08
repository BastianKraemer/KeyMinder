package de.akubix.keyminder.shell;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import de.akubix.keyminder.shell.parse.ParsedCommand;
import de.akubix.keyminder.shell.parse.ShellExecOption;

public class ShellTest {

	@Test
	public void testSingleCmd(){
		List<ParsedCommand> res;

		res = Shell.parseCommandLineString("echo hello world");
		verify(arr("echo"), arr2d(arr("hello", "world")), arr(ShellExecOption.NONE), res);

		res = Shell.parseCommandLineString("echo hello world;");
		verify(arr("echo"), arr2d(arr("hello", "world")), arr(ShellExecOption.NONE), res);

		res = Shell.parseCommandLineString("echo \"hello world\"");
		verify(arr("echo"), arr2d(arr("hello world")), arr(ShellExecOption.NONE), res);

		res = Shell.parseCommandLineString("echo \"hello world\" 123;;;");
		verify(arr("echo"), arr2d(arr("hello world", "123")), arr(ShellExecOption.NONE), res);

		res = Shell.parseCommandLineString("echo abc \"\\${hello} \\\"\\${world}\\\"\"");
		verify(arr("echo"), arr2d(arr("abc", "${hello} \"${world}\"")), arr(ShellExecOption.NONE), res);
	}

	@Test
	public void testMultiCmd(){
		List<ParsedCommand> res;

		res = Shell.parseCommandLineString("echo hello world; ls -al");
		verify(arr("echo", "ls"), arr2d(arr("hello", "world"), arr("-al")), arr(ShellExecOption.NONE, ShellExecOption.NONE), res);

		res = Shell.parseCommandLineString("echo \"hello world\"; ls -a \"-l\";");
		verify(arr("echo", "ls"), arr2d(arr("hello world"), arr("-a", "-l")), arr(ShellExecOption.NONE, ShellExecOption.NONE), res);

		res = Shell.parseCommandLineString("echo \"hello world\";; ls  ;find . -name \"*\"");
		verify(arr("echo", "ls", "find"), arr2d(arr("hello world"), arr(), arr(".", "-name", "*")), arr(ShellExecOption.NONE, ShellExecOption.NONE, ShellExecOption.NONE), res);
	}

	@Test
	public void testPipe(){
		List<ParsedCommand> res;

		res = Shell.parseCommandLineString("echo hello world | tac");
		verify(arr("echo", "tac"), arr2d(arr("hello", "world"), arr()), arr(ShellExecOption.PIPE, ShellExecOption.NONE), res);

		res = Shell.parseCommandLineString("echo hello world | tac;");
		verify(arr("echo", "tac"), arr2d(arr("hello", "world"), arr()), arr(ShellExecOption.PIPE, ShellExecOption.NONE), res);

		res = Shell.parseCommandLineString("echo hello && echo world");
		verify(arr("echo", "echo"), arr2d(arr("hello"), arr("world")), arr(ShellExecOption.REQUIRE_EXIT_0, ShellExecOption.NONE), res);
	}

	@Test
	public void testEscape(){
		List<ParsedCommand> res;

		res = Shell.parseCommandLineString("echo \"\\\"hello world\\\"\"");
		verify(arr("echo"), arr2d(arr("\"hello world\"")), arr(ShellExecOption.NONE), res);

		res = Shell.parseCommandLineString("echo \"\\\"hello\\\\\\\"world\\\"\"");
		verify(arr("echo"), arr2d(arr("\"hello\\\"world\"")), arr(ShellExecOption.NONE), res);

		res = Shell.parseCommandLineString("echo #U+0022");
		verify(arr("echo"), arr2d(arr("#U+0022")), arr(ShellExecOption.NONE), res);

		res = Shell.parseCommandLineString("echo \\'#U+0022\\'");
		verify(arr("echo"), arr2d(arr("'#U+0022'")), arr(ShellExecOption.NONE), res);
	}

	private void verify(String[] expectedCmds, String expectedArgs[][], ShellExecOption[] expectedOption, List<ParsedCommand> result){
		assertEquals("Command array length", expectedCmds.length, result.size());
		for(int i = 0; i < result.size(); i++){
			assertEquals(expectedCmds[i], result.get(i).getCommand());
			assertEquals(expectedOption[i], result.get(i).getExecOption());

			List<String> args = result.get(i).getArguments();
			assertArrayEquals(expectedArgs[i], args.toArray(new String[args.size()]));
		}
	}

	private String[] arr(){
		return new String[0];
	}

	private String[] arr(String... str){
		return str;
	}

	private ShellExecOption[] arr(ShellExecOption... option){
		return option;
	}

	private String[][] arr2d(String[]... strArr){
		return strArr;
	}
}
