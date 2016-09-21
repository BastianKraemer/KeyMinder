package de.akubix.keyminder.script;

import java.util.function.Supplier;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import de.akubix.keyminder.core.ApplicationInstance;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

@SuppressWarnings("restriction")
public class ScriptFramework {

	private static ScriptEngine javaScriptEngine = null;
	private Bindings bindings = null;
	private Supplier<AbstractScriptEnvironment> environmentSupplier;

	public ScriptFramework(ApplicationInstance app){
		this( () -> new SimpleScriptEnvironment(app));
	}

	public ScriptFramework(Supplier<AbstractScriptEnvironment> scriptEnvSupplier){
		this.environmentSupplier = scriptEnvSupplier;
		if(javaScriptEngine == null){
			NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
			javaScriptEngine = factory.getScriptEngine(new ScriptClassFilter());
		}
	}

	private Bindings setupEnvironment() throws ScriptException {

		Bindings binding = new SimpleBindings();
		binding.put("KeyMinder", environmentSupplier.get());

		javaScriptEngine.eval("function lookup(varName){return KeyMinder.lookup(varName);} function isDefined(varName){return KeyMinder.isDefined(varName);}", binding);
		return binding;
	}

	private Bindings getBindings() throws ScriptException {
		if(bindings == null){
			bindings = setupEnvironment();
		}
		return bindings;
	}

	public void reset(){
		bindings = null;
	}

	public Object eval(String js) throws ScriptException {
		return javaScriptEngine.eval(js, getBindings());
	}

	@SuppressWarnings("unchecked")
	public <T extends Object> T eval(String js, String resultObjectName, T resultObject) throws ScriptException {
		Bindings binding = getBindings();

		if(resultObjectName != null){
			binding.put(resultObjectName, resultObject);
		}

		Object result = javaScriptEngine.eval(js, binding);

		binding.remove(resultObjectName);

		if(result == null){
			throw new ScriptException("Returned object is 'null'.");
		}

		if(resultObject.getClass().isAssignableFrom(result.getClass())){
			return (T) result;
		}
		else{
			throw new ScriptException(String.format("Returned object is not of type '%s'.", resultObject.getClass().getSimpleName()));
		}
	}
}
