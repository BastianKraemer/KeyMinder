/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	Terminal.java

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.akubix.keyminder.ui.fx;

import java.util.ArrayList;
import java.util.List;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.lib.Tools;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

public class Terminal implements de.akubix.keyminder.core.interfaces.CommandOutputProvider {
	private Stage terminalwindow;
	private TextArea output;
	private TextField input;
	private ApplicationInstance app;
	private List<String> history = new ArrayList<String>();
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
		de.akubix.keyminder.lib.gui.StyleSelector.assignStylesheets(myScene, de.akubix.keyminder.lib.gui.StyleSelector.WindowSelector.Terminal);

		input.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if(event.getCode() == KeyCode.ENTER){
					runCommand(input.getText());
					input.setText("");
				}
				else if(event.getCode() == KeyCode.UP && currentHistoryIndex != -1){
					if(currentHistoryIndex > 0){currentHistoryIndex--;}
					input.setText(history.get(currentHistoryIndex));
					input.selectAll();
					event.consume();
				}
				else if(event.getCode() == KeyCode.DOWN && currentHistoryIndex != -1){
					if(currentHistoryIndex < history.size() - 1){currentHistoryIndex++;}
					input.setText(history.get(currentHistoryIndex));
					input.selectAll();
					event.consume();
				}
			}});

		output.addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if(!event.isControlDown()){input.requestFocus(); input.appendText(event.getText());}
			}});

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
		Tools.addDefaultIconsToStage(terminalwindow);
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
		println("\nVersion: " + de.akubix.keyminder.core.ApplicationInstance.APP_VERSION + "\n\n");

		app.tryToEstablishOutputRedirect(this);
	}

	private void runCommand(String line){
		if(!line.equals("")){
			String cmd;
			String[] param;
			if(line.contains(" ")){
				String[] splitstr = line.split(" ", 2);
				cmd = splitstr[0];
				param = de.akubix.keyminder.core.ApplicationInstance.splitParameters(splitstr[1]);
			}
			else{
				cmd = line;
				param = new String[0];
			}
			if(cmd.toLowerCase().equals("exit")){terminalwindow.close(); return;}

			print("\n$ " + line + "\n");
			history.add(line);
			currentHistoryIndex = history.size();
			if(app.commandAvailable(cmd)){app.execute(this, cmd, param);}else{println("Unknown command: '" + cmd + "'");}
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

	private void printAsFxThread(String str){
		Platform.runLater(() -> output.appendText(str));
	}
}
