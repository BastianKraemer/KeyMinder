/*	KeyMinder
 * Copyright (C) 2015-2016 Bastian Kraemer
 *
 * Terminal.java
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.akubix.keyminder.ui.fx;

import java.util.ArrayList;
import java.util.List;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.KeyMinder;
import de.akubix.keyminder.core.exceptions.UserCanceledOperationException;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.CommandException;
import de.akubix.keyminder.ui.fx.utils.ImageMap;
import de.akubix.keyminder.ui.fx.utils.StylesheetMap;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

public class Terminal implements de.akubix.keyminder.shell.io.ShellOutputWriter {
	private Stage terminalwindow;
	private TextArea output;
	private TextField input;
	private ApplicationInstance app;
	private List<String> history = new ArrayList<>();
	private int currentHistoryIndex = 0;

	public Terminal(ApplicationInstance instance){
		this.app = instance;
		this.history.add("");
	}

	public void show(){
		BorderPane root = new BorderPane();

		output = new TextArea();
		input = new TextField();

		root.setCenter(output);

		HBox hbox = new HBox(0);
		TextField prompt = new TextField("$");
		prompt.setMaxWidth(16);
		hbox.getChildren().addAll(prompt, input);
		HBox.setHgrow(input, Priority.ALWAYS);

		root.setBottom(hbox);

		Scene myScene = new Scene(root, 640, 320);
		StylesheetMap.assignStylesheet(myScene, StylesheetMap.WindowSelector.Terminal);

		input.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
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
		});

		output.addEventFilter(KeyEvent.KEY_RELEASED, (KeyEvent event) -> {
			if(!event.isControlDown()){input.requestFocus(); input.appendText(event.getText());}
		});

		output.setWrapText(true);
		output.setEditable(false);
		input.requestFocus();

		input.setFocusTraversable(false);
		output.setFocusTraversable(false);
		prompt.setFocusTraversable(false);

		terminalwindow = new Stage();
		terminalwindow.setTitle(ApplicationInstance.APP_NAME + " Terminal");
		terminalwindow.setScene(myScene);

		terminalwindow.setResizable(true);
		ImageMap.addDefaultIconsToStage(terminalwindow);
		terminalwindow.setMinWidth(560);
		terminalwindow.setMinHeight(240);

		terminalwindow.setOnCloseRequest((event) -> app.terminateOutputRedirect(this));
		terminalwindow.show();

		println(" #    #                    #       #");
		println(" #   #    ######  #     #  # #   # #   #   #    #  #####   ######  #####");
		println(" #  #     #        #   #   #  # #  #   #   ##   #  #    #  #       #    #");
		println(" ###      #####      #     #   #   #   #   # #  #  #    #  #####   #    #");
		println(" #  #     #          #     #       #   #   #  # #  #    #  #       #####");
		println(" #   #    #          #     #       #   #   #   ##  #    #  #       #   #");
		println(" #    #   ######     #     #       #   #   #    #  #####   ######  #    #");
		println("\nVersion: " + KeyMinder.getApplicationVersion() + "\n\n");

		app.startOutputRedirect(this);
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
				terminalwindow.close();
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
