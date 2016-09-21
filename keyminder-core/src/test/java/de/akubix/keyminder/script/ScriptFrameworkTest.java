package de.akubix.keyminder.script;

import javax.script.ScriptException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.akubix.keyminder.core.KeyMinderInstanceBuilder;

public class ScriptFrameworkTest {
	private static ScriptFramework scriptFramework;

	@BeforeClass
	public static void prepareTest(){
		scriptFramework = new ScriptFramework(KeyMinderInstanceBuilder.getNewInstance("./keyminder_settings.xml"));
	}

	@Test
	public void testSimpleEval() throws ScriptException{
		String result = (String)  scriptFramework.eval("var helloWorld = function(){return 'Hello world';}; helloWorld();");
		String result2 = (String)  scriptFramework.eval("helloWorld();");
		Assert.assertEquals("Hello world", result);
		Assert.assertEquals("Hello world", result2);
	}

	@Test(expected = ScriptException.class)
	public void testReset() throws ScriptException{
		scriptFramework.eval("function helloWorld(){return 'Hello world';}");
		scriptFramework.reset();
		scriptFramework.eval("helloWorld();");
	}

	@Test(expected = ScriptException.class)
	public void testAccessFilter() throws ScriptException{
		scriptFramework.eval("de.akubix.keyminder.ApplicationInstance.APP_NAME");
	}

	@Test
	public void testAccessFilter2() throws ScriptException{
		Assert.assertEquals(5, scriptFramework.eval("5"));
		Assert.assertEquals(3.141, scriptFramework.eval("3.141"));
	}

	@Test
	public void testEvalWithReturn() throws ScriptException {

		 MyTestResult result = scriptFramework.eval(
			"result.setIntVal(Math.pow(2, 3));" +
			"var arr = new Array(); arr.push('abc'); arr.push('def');" +
			"result.strArr = arr;" +
			"result;",
			"result", new MyTestResult());

		Assert.assertEquals(result.getStaticString(), "hello world");
		Assert.assertEquals(8, result.intVal);
		Assert.assertArrayEquals(new String[]{"abc",  "def"}, result.strArr);
	}

	@Test(expected = ScriptException.class)
	public void testEvalWithWrongReturnType() throws ScriptException {
		scriptFramework.eval("function f(){return 'hello';}\nf();", "result", new MyTestResult());
	}

	public static class MyTestResult {

		private String staticString = "hello world";
		public int intVal;
		public String[] strArr;

		public void setIntVal(int value){
			this.intVal = value;
		}

		public String getStaticString(){
			return staticString;
		}
	}
}
