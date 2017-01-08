/*	KeyMinder
 * Copyright (C) 2015-2017 Bastian Kraemer
 *
 * TerminalController.java
 *
 * KeyMinder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * KeyMinder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with KeyMinder.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.akubix.keyminder.ui.fx;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.KeyMinder;
import de.akubix.keyminder.core.exceptions.UserCanceledOperationException;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.CommandException;
import de.akubix.keyminder.shell.io.ShellOutputWriter;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

public class TerminalController implements Initializable, ShellOutputWriter {

	@FXML
	private TextArea output;

	@FXML
	private TextField input;

	private ApplicationInstance app;
	private Stage terminalWindow;
	private List<String> history = new ArrayList<>();
	private int currentHistoryIndex = 0;

	public TerminalController() {
		this.history = new ArrayList<>();
		this.history.add("");
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		output.setWrapText(true);

		println(" #    #                    #       #");
		println(" #   #    ######  #     #  # #   # #   #   #    #  #####   ######  #####");
		println(" #  #     #        #   #   #  # #  #   #   ##   #  #    #  #       #    #");
		println(" ###      #####      #     #   #   #   #   # #  #  #    #  #####   #    #");
		println(" #  #     #          #     #       #   #   #  # #  #    #  #       #####");
		println(" #   #    #          #     #       #   #   #   ##  #    #  #       #   #");
		println(" #    #   ######     #     #       #   #   #    #  #####   ######  #    #");
		println("\nVersion: " + KeyMinder.getApplicationVersion() + "\n\n");
	}

	public void setupAndShow(ApplicationInstance applicationInstance, Stage stage) {
		this.app = applicationInstance;
		this.terminalWindow = stage;
		app.startOutputRedirect(this);

		terminalWindow.setOnCloseRequest((event) -> app.terminateOutputRedirect(this));
		terminalWindow.show();
		input.requestFocus();
	}

	@FXML
	private void inputKeyPressed(KeyEvent event){

		if(event.getCode() == KeyCode.ENTER){
			runCommand(input.getText());
			input.setText("");
		}
		else if(event.getCode() == KeyCode.UP && currentHistoryIndex != -1){
			currentHistoryIndex--;
			if(currentHistoryIndex < 0){currentHistoryIndex = 0;}
			input.setText(history.get(currentHistoryIndex));
			input.selectAll();
			event.consume();
		}
		else if(event.getCode() == KeyCode.DOWN && currentHistoryIndex != -1){
			currentHistoryIndex++;
			if(currentHistoryIndex >= history.size() - 1){currentHistoryIndex = history.size() - 1;}
			input.setText(history.get(currentHistoryIndex));
			input.selectAll();
			event.consume();
		}
	}

	@FXML
	private void outputKeyReleased(KeyEvent event){

		if(!event.isControlDown()){
			input.requestFocus();
			input.appendText(event.getText());
		}
	}

	private void runCommand(String line){
		if(!line.equals("")){

			print("\n$ " + line + "\n");
			history.add(line);
			currentHistoryIndex = history.size();

			try {
				app.getShell().runShellCommand(this, line);
			} catch (CommandException e) {
				println(e.getMessage());
			} catch (UserCanceledOperationException e) {
				terminalWindow.close();
				return;
			}
		}
		else{
			println("$");
		}
	}

	@Override
	public void print(String text) {
		if(Platform.isFxApplicationThread()){
			output.appendText(text);
		}
		else{
			printAsFxThread(text);
		}
	}

	@Override
	public void println(String text) {
		if(Platform.isFxApplicationThread()){
			output.appendText(text + "\n");
		}
		else{
			printAsFxThread(text + "\n");
		}
	}

	@Override
	public void printf(String text, Object...  args){
		this.print(String.format(text, args));
	}

	@Override
	public void setColor(AnsiColor color) {
	}

	private void printAsFxThread(String str){
		Platform.runLater(() -> output.appendText(str));
	}
}
