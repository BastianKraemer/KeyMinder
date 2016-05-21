package de.akubix.keyminder.core.interfaces;

public interface CommandOutputProvider {
	public void print(String text);
	public void println(String text);
	public void printf(String text, Object...  args);
}
