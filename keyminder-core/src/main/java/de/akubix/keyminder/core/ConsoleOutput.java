package de.akubix.keyminder.core;

import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

public class ConsoleOutput implements ShellOutputWriter {
	@Override
	public void print(String text) {
		System.out.print(text);
		System.out.flush();
	}

	@Override
	public void println(String text) {
		System.out.println(text);
		System.out.flush();
	}

	@Override
	public void printf(String text, Object... args) {
		System.out.printf(text, args);
		System.out.flush();
	}

	@Override
	public void setColor(AnsiColor color) {
		if(KeyMinder.enableColoredOutput){
			this.print(color.getAnsiCode());
		}
	}
}
