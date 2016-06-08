package de.akubix.keyminder.shell.io;

import de.akubix.keyminder.shell.AnsiColor;

public interface ShellOutputWriter {
	public void print(String text);
	public void println(String text);
	public void printf(String text, Object...  args);
	public void setColor(AnsiColor color);
}
