/* KeyMinder
 * Copyright (C) 2015 Bastian Kraemer
 *
 * Deadline.java
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
package de.akubix.keyminder.plugins.deadline;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.KeyMinder;
import de.akubix.keyminder.core.events.Compliance;
import de.akubix.keyminder.core.events.EventTypes.ComplianceEvent;
import de.akubix.keyminder.core.events.EventTypes.DefaultEvent;
import de.akubix.keyminder.core.tree.TreeNode;
import de.akubix.keyminder.core.tree.TreeStore;
import de.akubix.keyminder.locale.LocaleLoader;
import de.akubix.keyminder.ui.fx.JavaFxUserInterface;
import de.akubix.keyminder.ui.fx.JavaFxUserInterfaceApi;
import de.akubix.keyminder.ui.fx.MenuEntryPosition;
import de.akubix.keyminder.ui.fx.events.FxSettingsEvent;
import de.akubix.keyminder.ui.fx.utils.FxCommons;
import de.akubix.keyminder.ui.fx.utils.ImageMap;
import de.akubix.keyminder.ui.fx.utils.StylesheetMap;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class Deadline {
	protected static final String NODE_EXPIRATION_ATTRIBUTE = "expiration_date";
	private static final String WARNING_DIFFERENCE_SETTINGS_VALUE = "deadline.warndifference";
	private static long warningDifferenceInMilliseconds;
	private static int warningDifferenceInDays;
	private final ApplicationInstance app;
	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	private final boolean javaFxUserInterfaceLoaded;
	private final ResourceBundle locale;

	public Deadline(ApplicationInstance instance){
		this.app = instance;
		this.locale = LocaleLoader.loadLanguagePack("plugins", "deadline", app.getLocale());
		this.javaFxUserInterfaceLoaded = JavaFxUserInterface.isLoaded(instance);


		// Initiate a node check if a new file is opened
		instance.addEventHandler(DefaultEvent.OnFileOpened, () -> {
				if(this.javaFxUserInterfaceLoaded){
					runCheckAsThread(false);
				}
				else{
					checkForExpiredNodes(false);
				}
			});

		instance.addEventHandler(ComplianceEvent.AllowFileClosing, () -> {
				if(backgroundWorker != null){
					if(backgroundWorker.isAlive()){
						backgroundWorker.interrupt();
						try {
							backgroundWorker.join(5000);
						} catch (Exception e) {
							app.alert("ERROR: Thread of plugin \"Deadline\" does no terminate.");
						}
					}
				}
				return Compliance.AGREE;
			});

		if(this.javaFxUserInterfaceLoaded){
			final JavaFxUserInterfaceApi fxUI = JavaFxUserInterface.getInstance(this.app);
			fxUI.addMenuEntry(
					FxCommons.createFxMenuItem(locale.getString("deadline.menu_set_expiration_date"),
							ImageMap.getIcon("icon_waiting"),
							(ActionEvent ae) -> showSetExpireDateWindow(instance.getTree().getSelectedNode())),
							MenuEntryPosition.TOOLS, true);

			fxUI.addMenuEntry(
					FxCommons.createFxMenuItem(locale.getString("deadline.menu_show_expired_nodes"),
							ImageMap.getIcon("icon_deadline"),
							(ActionEvent ae) -> showExpiredNodesList()),
							MenuEntryPosition.TOOLS, true);

			// Append some controls for configuration the warning time to the general settings tab
			fxUI.addEventListener(FxSettingsEvent.OnSettingsDialogOpened, (TabPane tabControl, Map<String, String> settings) -> {
				try {
					Separator s = new Separator(Orientation.HORIZONTAL);
					s.setStyle("-fx-padding: 8 0 8 0");

					HBox hbox = new HBox(4);

					Spinner<Integer> numberField = new Spinner<>(1, 31, 14);
					numberField.setEditable(true);
					//SpinnerValueFactory.IntegerSpinnerValueFactory spinnerValueFactory = (IntegerSpinnerValueFactory) numberField.getValueFactory();

					numberField.setStyle("-fx-min-width: 64px; -fx-max-width: -fx-min-width;");
					// Configure the number days you want to be warned before a node/password becomes invalid
					Label pre = new Label(locale.getString("deadline.settings_warn_time_pre"));
					Label post = new Label(locale.getString("deadline.settings_warn_time_post"));
					pre.setAlignment(Pos.CENTER_LEFT); post.setAlignment(Pos.CENTER_LEFT);
					pre.setStyle("-fx-min-height: 22; -fx-max-height: -fx-min-height;");
					post.setStyle(pre.getStyle());
					hbox.getChildren().addAll(pre, numberField, post);

					if(settings.containsKey(WARNING_DIFFERENCE_SETTINGS_VALUE)){
						try{
							((IntegerSpinnerValueFactory) numberField.getValueFactory()).setValue(Integer.parseInt(settings.get(WARNING_DIFFERENCE_SETTINGS_VALUE)));
						}
						catch(NumberFormatException numEx){} //Keep default value
					}

					numberField.valueProperty().addListener((obs, oldValue, newValue) -> settings.put(WARNING_DIFFERENCE_SETTINGS_VALUE, Integer.toString(newValue)));

					ScrollPane sp = (ScrollPane) tabControl.getTabs().get(0).getContent();
					VBox vbox = (VBox) sp.getContent();
					vbox.getChildren().addAll(s, hbox);
				}
				catch(ClassCastException cce){
					instance.log("Deadline plugin: Cannot create settings view (incompatible).");
				}
			});
		}

		// Load the values for "warningDifferenceInDays" and" warningDifferenceInDays" and update them if the settings are updated.
		loadWarnDiffFromSettings(instance);
		instance.addEventHandler(DefaultEvent.OnSettingsChanged, () -> loadWarnDiffFromSettings(instance));

		/* ==========================================================================================================================================
			Add some commands to interact with this plugin via KeyMinder Shell (ConsoleMode or Terminal)
		   ========================================================================================================================================== */

		app.getShell().addCommand(DeadlineCmd.class.getName());
		app.getShell().addCommand(DateConv.class.getName());
	}

	/* =========================================================== load settings e.g. default values =============================================================== */

	private void loadWarnDiffFromSettings(ApplicationInstance instance){
		String value = instance.getSettingsValue(WARNING_DIFFERENCE_SETTINGS_VALUE);
		if(value.equals("")){
			setWarnDiffDefaultValues();
		}
		else{
			try{
				warningDifferenceInDays = Integer.parseInt(value);
				if(!(warningDifferenceInDays > 0)){
					setWarnDiffDefaultValues();
					instance.log(String.format("Error in plugin 'Deadline': Warn time must be greater than zero. Using default value (%d days)", warningDifferenceInDays));
				}
				else{
					warningDifferenceInMilliseconds = ((long) warningDifferenceInDays) * 24 * 60 * 60 * 1000;
				}
			}
			catch(NumberFormatException nuFormatEx){
				setWarnDiffDefaultValues();
				instance.log(String.format("Error in plugin 'Deadline': Cannot convert %s to long. Using default value (%d days)", value, warningDifferenceInDays));
			}
		}
	}

	private void setWarnDiffDefaultValues(){
		warningDifferenceInMilliseconds = 1209600000; //14 days in milliseconds
		warningDifferenceInDays = 14;
	}

	/* =============================================================== check for expired nodes =================================================================== */

	private class ExpiredNodeData{
		public final boolean isExpired;
		public final String date;
		public final String nodeId;

		public ExpiredNodeData(boolean alreadyExpired, String expiresOn, String nodeId){
			this.isExpired = alreadyExpired;
			this.date = expiresOn;
			this.nodeId = nodeId;
		}
	}

	Thread backgroundWorker = null;
	private void runCheckAsThread(boolean forceConsoleOutput){
		if(backgroundWorker == null || !backgroundWorker.isAlive()){
			backgroundWorker = new Thread(() -> checkForExpiredNodes(forceConsoleOutput));
			backgroundWorker.start();
		}
	}

	public void checkForExpiredNodes(boolean forceConsoleOutput){
		List<ExpiredNodeData> expiredNodes = new ArrayList<>();
		List<ExpiredNodeData> nearlyExpiredNodes = new ArrayList<>();
		if(KeyMinder.verbose_mode || forceConsoleOutput){app.println("Checking for expired nodes...");}
		checkForExpiredNodes(app.getTree(), Instant.now().getEpochSecond() * 1000, expiredNodes, nearlyExpiredNodes, KeyMinder.verbose_mode);

		if(KeyMinder.verbose_mode || forceConsoleOutput){app.println(String.format("Check completed - %d expired node(s) found, %d node(s) will expire during the next %d days.", expiredNodes.size(), nearlyExpiredNodes.size(), warningDifferenceInDays));}

		if(expiredNodes.size() > 0 || nearlyExpiredNodes.size() > 0){
			if(this.javaFxUserInterfaceLoaded && !forceConsoleOutput){
				final JavaFxUserInterfaceApi fxUI = JavaFxUserInterface.getInstance(this.app);

				Runnable run = () -> {
					Button notification = new Button("", ImageMap.getFxImageView(("icon_warning")));
					notification.setMinWidth(24);
					notification.setMaxWidth(24);
					Tooltip tooltip = new Tooltip(	locale.getString("deadline.report.title") + "\n" +
													(expiredNodes.size() > 0 ? String.format(locale.getString("deadline.report.expired_nodes") + "\n", expiredNodes.size()) : "") +
													(nearlyExpiredNodes.size() > 0 ? String.format(locale.getString("deadline.report.nearlyexpired_nodes") + "\n", nearlyExpiredNodes.size(), warningDifferenceInDays) : ""));

					notification.setTooltip(tooltip);
					notification.getStyleClass().add("noBorder");

					Label l = new Label(locale.getString("deadline.report.ui_notification"));
					l.setTooltip(tooltip);
					l.setStyle("-fx-padding: 4px; -fx-cursor: Hand;");
					Pane treeNotificationPanel = new Pane(l);
					treeNotificationPanel.getStyleClass().add("highlighted");

					Runnable r = () -> {
						showExpiredNodesList();
						fxUI.removeNotificationItem(notification);
						fxUI.removeTreePanel(treeNotificationPanel);
					};

					notification.setOnAction((event) -> r.run());
					l.setOnMouseClicked((MouseEvent event) -> {
						if(event.getButton() == MouseButton.PRIMARY){
							r.run();
						}
						else{
							fxUI.removeTreePanel(treeNotificationPanel);
						}
					});

					fxUI.addTreePanel(treeNotificationPanel, true);
					fxUI.addNotificationItem(notification, true);
				};
				fxUI.runAsFXThread(run);
			}
			else{
				if(expiredNodes.size() > 0){printExpireNodeList(expiredNodes, true);}
				if(nearlyExpiredNodes.size() > 0){printExpireNodeList(nearlyExpiredNodes, false);}
			}
		}
	}

	private void checkForExpiredNodes(TreeStore tree, long now, List<ExpiredNodeData> expiredNodes, List<ExpiredNodeData> nearlyExpiredNodes, boolean enableLiveLog){
		tree.allNodes((TreeNode node) -> {
			if(Thread.interrupted()){return;}
			if(node.hasAttribute(NODE_EXPIRATION_ATTRIBUTE)){
				try{
					long value = Long.parseLong(node.getAttribute(NODE_EXPIRATION_ATTRIBUTE));
					long diff = value - now;
					if(diff < 0){
						// has been expired
						if(enableLiveLog){app.println("\t- Node '" + node.getText() + "' ist expired");}
						expiredNodes.add(new ExpiredNodeData(true, Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).toLocalDate().format(dateTimeFormatter), node.getId()));
					}
					else if(diff < warningDifferenceInMilliseconds){
						// Warning
						if(enableLiveLog){app.println(String.format("\t- Node '" + node.getText() + "' will expire during the next %d days.", warningDifferenceInDays));}
						nearlyExpiredNodes.add(new ExpiredNodeData(false, Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).toLocalDate().format(dateTimeFormatter), node.getId()));
					}
				}
				catch(NumberFormatException nuFormatEx){
					if(KeyMinder.verbose_mode){app.println("Plugin \"Expiration\": Unable to parse expiration date of node '" + node.getText() + "'");}
				}
			}
		});
	}

	private void printExpireNodeList(List<ExpiredNodeData> list, boolean printExpiredNodes){
		TreeStore t = app.getTree();
		app.println(
			String.format("\n" + (printExpiredNodes ? "Already expired nodes:" : "Nodes that will expire during the next %d days:") + "\n"
						+ "----------------------------------------------------------------------", warningDifferenceInDays));
		for(ExpiredNodeData x: list){
			if(x.isExpired == printExpiredNodes){
				TreeNode n = t.getNodeById(x.nodeId);
				if(n != null){
					app.println(String.format("%s\t%s (%s)", x.date, n.getText(), n.getNodePath()));
				}
			}
		}
	}

	/* =============================================================== UI - set expiration date =================================================================== */

	private void showSetExpireDateWindow(TreeNode node){
		Stage me = new Stage();
		me.setTitle(de.akubix.keyminder.core.ApplicationInstance.APP_NAME + " - " + locale.getString("deadline.dialog.title"));

		BorderPane root = new BorderPane();

		Label title = new Label(locale.getString("deadline.dialog.select_expiration_date"));
		Pane top = new Pane(title);
		top.getStyleClass().add("header");
		root.setTop(top);

		VBox vbox = new VBox(4);

		CheckBox activate = new CheckBox(locale.getString("deadline.dialog.enable_expiration_date"));
		activate.setSelected(true);

		// Date-Picker
		DatePicker datePicker = new DatePicker();
		datePicker.setMinWidth(290);
		vbox.setPadding(new Insets(4,0,0,0));
		if(node.hasAttribute(NODE_EXPIRATION_ATTRIBUTE)){
			try{
				datePicker.setValue(Instant.ofEpochMilli(Long.parseLong(node.getAttribute(NODE_EXPIRATION_ATTRIBUTE))).atZone(ZoneId.systemDefault()).toLocalDate());
			}
			catch(NumberFormatException nuFormatEx){
				app.alert(String.format(locale.getString("deadline.dialog.number_format_exception_message"), node.getText()));
				return;
			}
		}
		else{
			datePicker.setValue(LocalDate.now());
		}

		activate.setOnAction((ActionEvent ae) -> {datePicker.setDisable(!activate.isSelected());});

		vbox.getChildren().addAll(datePicker, activate);

		BorderPane.setMargin(vbox, new Insets(4,10,0,10));
		root.setCenter(vbox);

		HBox bottom = new HBox(4);

		Button acceptButton = new Button(locale.getString("deadline.dialog.button_accept"));
		acceptButton.setOnAction((ActionEvent ae) -> {
			if(activate.isSelected()){
				node.setAttribute(NODE_EXPIRATION_ATTRIBUTE, Long.toString(datePicker.getValue().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().getEpochSecond() * 1000));
			}
			else{
				if(node.hasAttribute(NODE_EXPIRATION_ATTRIBUTE)){
					node.removeAttribute(NODE_EXPIRATION_ATTRIBUTE);
				}
			}
			me.close();
		});

		Button cancelButton = new Button(LocaleLoader.getBundle(JavaFxUserInterface.LANGUAGE_BUNDLE_KEY).getString("cancel"));
		cancelButton.setOnAction((ActionEvent ae) -> me.close());

		acceptButton.setMinWidth(130);
		cancelButton.setMinWidth(130);

		bottom.setAlignment(Pos.CENTER);
		bottom.getChildren().addAll(acceptButton, cancelButton);

		acceptButton.setDefaultButton(true);

		root.setBottom(bottom);
		BorderPane.setMargin(bottom, new Insets(0,10,10,10));

		Scene myScene = new Scene(root, 300, 120);
		StylesheetMap.assignStylesheets(myScene);

		me.initModality( Modality.APPLICATION_MODAL );
		me.setScene(myScene);
		me.setResizable(false);
		ImageMap.addDefaultIconsToStage(me);

		me.showAndWait();
	}

	/* =============================================================== UI - list expired nodes =================================================================== */

	private static double windowWidth = 300;
	private void showExpiredNodesList(){
		// Generate a new list with all expired nodes
		List<ExpiredNodeData> expiredNodesList = new ArrayList<>();
		List<ExpiredNodeData> nearlyExpiredNodesList = new ArrayList<>();
		checkForExpiredNodes(app.getTree(), Instant.now().getEpochSecond() * 1000, expiredNodesList, nearlyExpiredNodesList, false);

		Stage me = new Stage();
		me.setTitle(de.akubix.keyminder.core.ApplicationInstance.APP_NAME + " - " + locale.getString("deadline.uireport.title"));

		BorderPane root = new BorderPane();

		Label title = new Label(locale.getString("deadline.uireport.headline"));
		Pane top = new Pane(title);
		top.getStyleClass().add("header");
		root.setTop(top);

		VBox vbox = new VBox(4);
		VBox expired = new VBox(4);
		VBox nearlyExpired = new VBox(4);

		Consumer<? super ExpiredNodeData> c = (x) -> {Pane p = createExpiredNodeDataRow(x);	if(p != null){(x.isExpired ? expired : nearlyExpired).getChildren().add(p);}};
		expiredNodesList.forEach(c);
		nearlyExpiredNodesList.forEach(c);

		vbox.getChildren().addAll(	FxCommons.createFxLabelWithStyleClass(locale.getString("deadline.uireport.label_expired"), "h2"),
									createScrollPane(expired), new Separator(Orientation.HORIZONTAL),
									FxCommons.createFxLabelWithStyleClass(locale.getString("deadline.uireport.label_nearly_expired"), "h2"),
									createScrollPane(nearlyExpired));

		BorderPane.setMargin(vbox, new Insets(4,10,0,10));
		root.setCenter(vbox);

		Button okButton = new Button(LocaleLoader.getBundle(JavaFxUserInterface.LANGUAGE_BUNDLE_KEY).getString("okay"));
		okButton.setOnAction((ActionEvent ae) -> me.close());

		okButton.setDefaultButton(true);
		okButton.setMinWidth(200);
		root.setBottom(okButton);

		BorderPane.setAlignment(okButton, Pos.CENTER);
		BorderPane.setMargin(okButton, new Insets(4));

		Scene myScene = new Scene(root, windowWidth, 320);
		StylesheetMap.assignStylesheets(myScene);

		me.setScene(myScene);
		me.setResizable(false);
		ImageMap.addDefaultIconsToStage(me);

		me.show();
	}

	private Pane createExpiredNodeDataRow(ExpiredNodeData d){
		HBox hbox = new HBox(4);
		TreeNode node = app.getTree().getNodeById(d.nodeId);
		if(node == null){return null;}
		Label dateLabel = new Label(d.date);
		dateLabel.setMinWidth(80);
		dateLabel.setMinHeight(24);

		Hyperlink link = new Hyperlink(node.getText());
		link.setMinHeight(24);
		link.setOnAction((event) -> {app.getTree().setSelectedNode(node); link.setVisited(false);});
		hbox.getChildren().addAll(dateLabel, link);
		return hbox;
	}

	private ScrollPane createScrollPane(Node item){
		ScrollPane scrollPane = new ScrollPane(item);
		scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		scrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

		scrollPane.setMaxWidth(windowWidth - 6);
		scrollPane.setMinWidth(windowWidth - 6);
		scrollPane.setMinHeight(100);
		scrollPane.setMaxHeight(100);
		return scrollPane;
	}
}
