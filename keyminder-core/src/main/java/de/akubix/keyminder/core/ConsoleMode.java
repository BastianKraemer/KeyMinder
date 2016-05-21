/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	ConsoleMode.java

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
package de.akubix.keyminder.core;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;

import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.core.exceptions.UserCanceledOperationException;
import de.akubix.keyminder.core.interfaces.Command;
import de.akubix.keyminder.core.interfaces.CommandOutputProvider;
import de.akubix.keyminder.core.interfaces.events.DefaultEventHandler;
import de.akubix.keyminder.core.interfaces.events.EventTypes.DefaultEvent;
import de.akubix.keyminder.lib.TreeSearch;

/**
 * ConsoleMode for this application. This is an alternative user interface for using this with a console only.
 */
public class ConsoleMode {

	private ApplicationInstance app;

	public ConsoleMode(ApplicationInstance instance){
		this.app = instance;
	}

	private static Scanner in;

	public void start(){
		try{
			in = new Scanner(System.in);

			System.out.println("\nKeyMinder\tCopyright (C) 2015   Bastian Kraemer\n\n"
							 + "This program comes with ABSOLUTELY NO WARRANTY; for details type 'license -w'.\n"
							 + "This is free software, and you are welcome to redistribute it under certain conditions; type 'license' for details.\n\n");

			app.startup(true);

			app.addEventHandler(DefaultEvent.OnExit, new DefaultEventHandler() {
				@Override
				public void eventFired() {
					in.close();
				}
			});

			app.loadDefaultFile();

			while(true){
				try{
					System.out.print("\n$ ");
					String input = in.nextLine();
					String cmd;
					String[] param;

					if(!input.equals("")){
						if(input.contains(" ")){
							String[] splitstr = input.split(" ", 2);
							cmd = splitstr[0];
							param = de.akubix.keyminder.core.ApplicationInstance.splitParameters(splitstr[1]);
						}
						else{
							cmd = input;
							param = new String[0];
						}

						if(app.commandAvailable(cmd)){
							app.execute(app, cmd, param);
						}
						else{
							app.println("Unknown command: " + cmd);
						}
					}
				}
				catch(Exception ex){
					if(ex.getClass() == NoSuchElementException.class){
						throw new NoSuchElementException(ex.getMessage()); // Program has been terminated by using CTRL+C
					}
					System.out.println("Critical Error: " + ex.getMessage() + "\n");
					ex.printStackTrace(System.out);
					System.out.println("\nAn unexpected error has occured. This should not have happened.\n"
									 + "It is recommended to be careful with the next steps, they can have unpredictable effects.\n"
									 + "The suggested way is to save your password file to another location (use \"file saveas <filepath>\" to do this) to prevent any data loss and restart " + ApplicationInstance.APP_NAME + "."
									 + "\nIf you are able to, take a look at the error message and the stack trace - it's possible that this error is more or less harmless.");
				}
			}
		}
		catch (NoSuchElementException ex){} // Program has been terminated by using CTRL+C
	}

	public static boolean askYesNo(String question){
		if(in == null){System.err.println(ApplicationInstance.APP_NAME + " console interface is not initialized."); return false;}
		System.out.print(question + "\n[Yes/No]: ");
		String input = in.nextLine().toLowerCase();
		if(input.equals("y") || input.equals("yes") || input.equals("j") || input.equals("ja")){return true;}
		return false;
	}

	public static String readLineFromSystemIn(){
		if(in == null){System.err.println(ApplicationInstance.APP_NAME + " console interface is not initialized."); return "";}
		String input = in.nextLine();
		return input;
	}

	public static String readPasswordFromSystemIn(){
		try{
			return new String(System.console().readPassword());
		}
		catch(NullPointerException nullPointEx){
			return "";
		}
	}

	/*
	 * ========================================================================================================================================================
	 * Add some default commands
	 * ========================================================================================================================================================
	 */
	public static void provideDefaultCommands(ApplicationInstance instance){
		/*
		 *  ================================================ license ================================================
		 */
		instance.provideNewCommand("license", (out, app, args) -> {
			String filename = "/GPLv3";
			try{
				if(args.length == 1){
					if(args[0].toLowerCase().equals("-w")){
						filename = "/LICENSE";
					}
				}
				java.util.Scanner s = new java.util.Scanner(ApplicationInstance.class.getResourceAsStream(filename), "UTF-8");
				String licese = s.useDelimiter("\\A").next();
				s.close();
				out.println(licese);
				return licese;
			}
			catch(Exception ex){
				out.println(String.format("ERROR, cannot find resource '%s'", filename));
				return null;
			}
		});

		/*
		 *  ================================================ cd ================================================
		 */
		instance.provideNewCommand("cd", new Command() {
			@Override
			public String runCommand(CommandOutputProvider out, ApplicationInstance instance, String[] args) {
				if(args.length == 1){
					TreeNode node = instance.getTree().getNodeByPath(args[0]);

					if(node == null){
						out.println("Cannot find node: " + args[0]);
					}
					else{
						instance.getTree().setSelectedNode(node);
						instance.execute(out, "pwd", new String[0]);
					}
				}
				else{
					out.println("Invalid Syntax.\nUsage: \"cd [nodepath]\"");
				}
				return null;
			}}, "Selects a node by its path.\nUsage:\n\t\"cd [nodename]\" to select a childnode.\n\t\""
					+ "cd ..\" to select the parent node\n\t\""
					+ "cd /\" to select the rootnode.\n\n\t"
					+ "You can also specify a path: \"cd /path/to/node\"");

		instance.provideCommandAlias("select", "cd");

		/*
		 *  ================================================ cdi - change directory by Index ================================================
		 */
		instance.provideNewCommand("cdi", new Command() {
			@Override
			public String runCommand(CommandOutputProvider out, ApplicationInstance instance, String[] args) {
				if(args.length == 1){
					try {
						int index = Integer.parseInt(args[0]);
						if(index < instance.getTree().getSelectedNode().countChildNodes()){
							instance.getTree().setSelectedNode(instance.getTree().getSelectedNode().getChildNodeByIndex(index));
							instance.execute(out, "pwd", new String[0]);
						}
						else {
							out.println("Invalid Index: Node '" + instance.getTree().getSelectedNode().getText() + "' only has " + instance.getTree().getSelectedNode().countChildNodes() + " childnodes.");
						}
					}
					catch(NumberFormatException numEx){
						out.println("Error: '" + args[0] + "' is not a number.");
					}
				}
				else{
					out.println("This command requires exactly ONE Argument!");
				}
				return null;
			}}, "Selects a childnode by its index.\nUsage: cdi [nodeindex]");

		/*
		 *  ================================================ pwd ================================================
		 */
		instance.provideNewCommand("pwd",  new Command() {
			@Override
			public String runCommand(CommandOutputProvider out, ApplicationInstance instance, String[] args) {
				TreeNode node;
				if(args.length > 0){
					node = instance.getTree().getNodeByPath(args[0]);
					if(node == null){out.println("Cannot find node '" + args[0] + "'."); return null;}
				}
				else{
					node = instance.getTree().getSelectedNode();
				}

				String path = instance.getTree().getNodePath(node, "/");
				out.println(path);
				return path;
			}}, "Shows the current \"node path\".\nUsage: pwd [nodepath]");

		/*
		 *  ================================================ ll ================================================
		 */
		instance.provideNewCommand("ls", new Command() {
			@Override
			public String runCommand(CommandOutputProvider out, ApplicationInstance instance, String[] args) {
				TreeNode node;
				if(args.length >= 1){
					node = instance.getTree().getNodeByPath(args[0]);
					if(node == null){
						out.println("Cannot find node: " + args[0]);
						return null;
					}
				}
				else{
					node = instance.getTree().getSelectedNode();
				}

				out.println("Childnodes of \"" + node.getText() + "\":\n\n" +
							"Index\tCreation date\tModification date\tNode name\n" +
							"-----\t-------------\t-----------------\t---------");

				int i = 0;
				for(TreeNode n: node.getChildNodes()){
					String creationDate = de.akubix.keyminder.lib.Tools.getTimeFromEpochMilli(n.getAttribute(de.akubix.keyminder.core.ApplicationInstance.NODE_ATTRIBUTE_CREATION_DATE), true, "-\t");
					String modificationDate = de.akubix.keyminder.lib.Tools.getTimeFromEpochMilli(n.getAttribute(de.akubix.keyminder.core.ApplicationInstance.NODE_ATTRIBUTE_MODIFICATION_DATE), true, "-\t");

					out.println(i++ + "\t" + creationDate + "\t" + modificationDate + "\t\t" + n.getText());
				}
				return null;
			}}, "Lists all childnodes of a Node.\nUsage: ls [nodename]");

		instance.provideCommandAlias("ll", "ls");

		/*
		 * ================================================ exit ================================================
		 */
		instance.provideNewCommand("exit", new Command() {
			@Override
			public String runCommand(CommandOutputProvider out, ApplicationInstance instance, String[] args) {
				instance.terminate();
				return null;
			}}, "Terminates the application.");

		/*
		 * ================================================ vi ================================================
		 */
		instance.provideNewCommand("vi", new Command() {
			@Override
			public String runCommand(CommandOutputProvider out, ApplicationInstance instance, String[] args) {
				TreeNode node;
				if(args.length > 0){
					node = instance.getTree().getNodeByPath(args[0]);
					if(node == null){out.println("Cannot find node '" + args[0] + "'."); return null;}
				}
				else{
					node = instance.getTree().getSelectedNode();
				}

				out.println("Attributes of '" + node.getText() + "'");

				int i = 0;
				out.println("#" + i++ +"\tName:\tid\n\tValue:\t" + node.getId() + "\n");

				for(String key: de.akubix.keyminder.lib.Tools.asSortedList(node.listAttributes())){
					out.println("#" + i++ +"\tName:\t" + key + "\n\tValue:\t" + node.getAttribute(key) + "\n");
				}

				if(!node.getColor().equals("")){
					out.println("#" + i++ +"\tName:\tcolor\n\tValue:\t" + node.getColor() + "\n");
				}

				return null;
			}}, "Views all attributes of a node.\nUsage: vi [nodepath]");

		instance.provideCommandAlias("view", "vi");

		/*
		 * ================================================ set (Attribute) ================================================
		 */
		instance.provideNewCommand("set", new Command() {
			@Override
			public String runCommand(CommandOutputProvider out, ApplicationInstance instance, String[] args) {
				// Verify that this entry is available
				TreeNode node = null;
				if(args.length == 2){
					node = instance.getTree().getSelectedNode();
				}
				else if(args.length == 3){
					node = instance.getTree().getNodeByPath(args[0]);
				}

				if(node == null){
					out.println("Usage: set [nodename] <attributename> <value>");
					return null;
				}

				// Entry is available
				if(args[args.length - 2].toLowerCase().equals("text")){
					node.setText(args[args.length - 1]);
				}
				else {
					node.setAttribute(args[args.length - 2], args[args.length - 1]);
				}
				return null;
			}}, "Set the value of a node attribute or changes the text of a node.\n" +
				"Usage:\t\"set [nodename] <attributename> <value>\" to change a node attribute\n"+
				"\t\"set [nodename] text <new node text>\" to change the text of a node");

		/*
		 * ================================================ rmattrib (remove Attribute) ================================================
		 */
		instance.provideNewCommand("rmattrib",  new Command() {
			@Override
			public String runCommand(CommandOutputProvider out, ApplicationInstance instance, String[] args) {
				TreeNode node;

				if(args.length == 2){
					node = instance.getTree().getNodeByPath(args[0]);
					if(node == null){out.println("Cannot find node '" + args[0] + "'."); return null;}
				}
				else{
					node = instance.getTree().getSelectedNode();
				}

				if(args.length == 2 || args.length == 1){
					if(node.hasAttribute(args[args.length -1])) {
						node.removeAttribute(args[args.length -1]);
					}
					else {
						out.println("Attribute '" + args[args.length -1]+ "' does not extist.");
					}
				}
				else {
					out.println("Usage: rmattrib [nodename] attributename");
				}
				return null;
			}}, "Removes an attribute of a node.\nUsage: rmattrib [nodename] attributename");

		/*
		 * ================================================ add (node) ================================================
		 */
		instance.provideNewCommand("add", new Command() {
			@Override
			public String runCommand(CommandOutputProvider out, ApplicationInstance instance, String[] args) {
				if(instance.currentFile != null){
					String nodename;
					if(args.length == 1){
						nodename = args[0];
					}
					else {
						out.println("Please enter the name of the new node:");
						nodename = readLineFromSystemIn();
					}
					if(!nodename.equals("")){instance.getTree().addNode(instance.getTree().createNode(nodename), instance.getTree().getSelectedNode());}
				}
				else{
					out.println("Please open a password file before you append nodes the tree.");
				}
				return null;
			}
		}, "Adds a node to the tree.\nUsage: \"add\" or \"add [nodetext]\"");

		/*
		 * ================================================ rm (remove node) ================================================
		 */
		instance.provideNewCommand("rm", new Command() {
			@Override
			public String runCommand(CommandOutputProvider out, ApplicationInstance instance, String[] args) {
				if(args.length > 0)	{
					int removedNodeCnt = 0;
					for(String arg: args){
						TreeNode node = instance.getTree().getNodeByPath(arg);
						if(node == null){
							out.println("Cannot find node: '" + arg + "'");
						}
						else if(node.getId() != 0){
							out.println("Removing node '" + arg + "'...");
							instance.getTree().removeNode(node);
							removedNodeCnt++;
						}
						else{
							out.println("Cannot remove root node.");
							return null;
						}
					}
					out.println(removedNodeCnt + " nodes has been removed.");
				}

				return null;
			}}, "Removes a tree node from the tree.\nUsage: rm <nodename>");

		/*
		 * ================================================ find (node) ================================================
		 */
		instance.provideNewCommand("find", new Command() {
			@Override
			public String runCommand(CommandOutputProvider out, ApplicationInstance instance, String[] args) {
				if(args.length == 1){
					if(TreeSearch.find(args[0], instance.getTree(), true) == TreeSearch.SearchResult.FOUND){
						out.println("Found matching node: " + instance.getTree().getSelectedNode().getText());
						return instance.execute("pwd");
					}
					else{
						out.println("No matching node found.");
						return "";
					}
				}
				else{
					// Searching with advanced parameters
					try{
						ArrayList<de.akubix.keyminder.lib.NodeTimeCondition> conditions = new ArrayList<de.akubix.keyminder.lib.NodeTimeCondition>();

						for(int i = 1; i < args.length; i++){
							if(args[i].toLowerCase().equals("created") || args[i].toLowerCase().equals("modified")){
								conditions.add(new de.akubix.keyminder.lib.NodeTimeCondition(args[i], de.akubix.keyminder.lib.NodeTimeCondition.getCompareTypeFromString(args[i + 1]), args[i + 2]));
								i += 2;
							}
							else{
								throw new IndexOutOfBoundsException();
							}
						}

						if(TreeSearch.find(args[0], instance.getTree(), true, conditions.toArray(new de.akubix.keyminder.lib.NodeTimeCondition[conditions.size()])) == TreeSearch.SearchResult.FOUND){
							out.println("Found matching node: " + instance.getTree().getSelectedNode().getText());
							return instance.execute("pwd");
						}
						else{
							out.println("No matching node found.");
							return "";
						}
					}
					catch(ParseException ex){
						ex.printStackTrace();
						out.println("Invalid date.");
					}
					catch (IndexOutOfBoundsException | IllegalArgumentException ex){
						out.println("Inavlid parameters. Usage: find <keyword> [[created/modified] [before/after/at] [date]]");
					}
				}
				return null;
			}}, "Finds a node if it matches a specified string.\nUsage: find <keyword> [[created/modified] [before/after/at] [date]]\n\n" +
				"Example: \"find * modified after 1.8.2015\" will return all nodes that have been modified since the first august.");

		/*
		 * ================================================ substitute ================================================
		 */
		instance.provideNewCommand("substitute", new Command() {
			@Override
			public String runCommand(CommandOutputProvider out, ApplicationInstance instance, String[] args) {
				String find = ""; String replaceWith = "";
				boolean recursive = false; boolean ignoreCase = true;

				// The syntax of this command is rubbish - one time it should be replaced by something else

				for(int i = 0; i < args.length; i++){
					if(args[i].toLowerCase().equals("-r") && !recursive){ // Maybe someone wants to run a command like this: substitude -r -r something
						recursive = true;
					}
					else if(args[i].toLowerCase().equals("-m") && ignoreCase){
						ignoreCase = false;
					}
					else{
						if(find.equals("")){
							find = args[i];
						}
						else{
							if(replaceWith.equals("")){
								replaceWith = args[i];
							}
							else{
								out.println("Unknown Parameter: '" + args[i] + "'");
								return null;
							}
						}
					}
				}

				int replacement_cnt = 0;
				if(TreeSearch.replaceTextOfNode(instance.getTree().getSelectedNode(), find, replaceWith, ignoreCase)){replacement_cnt++;}

				if(recursive){replacement_cnt += rekursiveSubstitute(instance.getTree().getSelectedNode(), find, replaceWith, ignoreCase);}
				out.println("Substituted occurrences: " + replacement_cnt);
				return Integer.toString(replacement_cnt);

			}}, "Substitutes the text of a treenode with another.\n\tUsage: substitute [-r] [-m] <find> <replacement>\n\n" +
				"\t-r\tRekursive to all childnodes\n" +
				"\t-m\tMatch case\n");

		/*
		 * ================================================ passwd (change file password) ================================================
		 */
		instance.provideNewCommand("passwd", new Command() {
			@Override
			public String runCommand(CommandOutputProvider out, ApplicationInstance instance, String[] args) {
				if(instance.currentFile != null){
				  try{
					if(args.length == 1){
						if(args[0].toLowerCase().equals("reset")){
							if(!instance.currentFile.isEncrypted()){out.println("Encryption is already disabled."); return null;}

							if(instance.currentFile.getEncryptionManager().checkPassword(instance.requestStringInput("Change file password", "Please enter your current file password: ", "", true).toCharArray())){
								instance.currentFile.disableEncryption();
								out.println("Encryption of passwordfile disabled (not recommended).");
								instance.saveFile();
							}
							else{
								out.println("Incorrect password. Canceling...");
							}
						}
						else{
							out.println("Invalid parameter: '" + args[0] + "'.");
						}
					}
					else{
						boolean wasEncrypted = instance.currentFile.isEncrypted();
						boolean enableChangePw = !instance.currentFile.isEncrypted();
						if(!enableChangePw){enableChangePw = (instance.currentFile.getEncryptionManager().checkPassword(instance.requestStringInput("Change file password", "Please enter your current file password: ", "", true).toCharArray()));}
						if(enableChangePw){
							if(!instance.currentFile.isEncrypted()){instance.currentFile.encryptFile(new de.akubix.keyminder.core.encryption.EncryptionManager(true));}
							if(instance.currentFile.getEncryptionManager().requestPasswordInputWithConfirm(instance, wasEncrypted ? "Change file password" : "Set file password",
																										  "Please enter your " + (wasEncrypted ? "new " : "") + "file password: ", "Please enter your password again: ")){
								// New password has been set
								out.println("Password changed.");
								instance.saveFile();
							}
							else{
								if(!wasEncrypted){
									// Undo everything -> delete the created encryption manager
									instance.currentFile.disableEncryption();
								}

								// Operation has been canceled
								out.println("The passwords you typed in does not match. Cancelling..."); return null;
							}
						}
						else{
							out.println("Incorrect password. Canceling...");
						}
					}
				  }
				  catch(UserCanceledOperationException e){
					  out.println("Operation canceled.");
				  }
				}
				return null;
			}}, "Changes the current file password. To disable encryption use \"passwd reset\"\n" +
				"WARNING: After this operation your current passwordfile will be saved.");

		/*
		 * ================================================ copy/ move  ================================================
		 */
		instance.provideNewCommand("cp", new Command() {
			@Override
			public String runCommand(CommandOutputProvider out, ApplicationInstance instance, String[] args) {
				if(args.length >= 2){
					TreeNode src = instance.getTree().getNodeByPath(args[0]);
					TreeNode dest = instance.getTree().getNodeByPath(args[1]);

					if(src == null){out.println(String.format("Cannot find node '%s'.", args[0])); return null;}
					if(dest == null){out.println(String.format("Cannot find node '%s'.", args[1])); return null;}

					boolean includeChildNodes = true;
					if(args.length >= 3){if(args[2].equals("--no-child-nodes") || args[2].equals("--n")){includeChildNodes = false;}}

					nodeCopy(src, dest, false, includeChildNodes);
					out.println("Node successfully copied.");
				}
				return null;
			}}, "Creates a copy of a node and adds it to anther location.\n" +
				"\tUsage:\t\t cp [source] [target] <--no-child-nodes*>\n" +
				"\tExample:\t cp . /some/node\n\n\t* Optional parameter; Another short variant is '--n'");

		instance.provideNewCommand("mv", new Command() {
			@Override
			public String runCommand(CommandOutputProvider out, ApplicationInstance instance, String[] args) {
				if(args.length >= 2){
					TreeNode src = instance.getTree().getNodeByPath(args[0]);
					TreeNode dest = instance.getTree().getNodeByPath(args[1]);

					if(src == null){out.println(String.format("Cannot find node '%s'.", args[0])); return null;}
					if(dest == null){out.println(String.format("Cannot find node '%s'.", args[1])); return null;}
					if(src.getId() == 0){out.println("The root node cannot be moved to another location!"); return null; }
					boolean includeChildNodes = true;
					if(args.length >= 3){if(args[2].equals("--no-child-nodes") || args[2].equals("--n")){includeChildNodes = false;}}

					nodeCopy(src, dest, true, includeChildNodes);
					out.println("Node successfully moved.");
				}
				return null;
			}}, "Moves a node to anther location.\n" +
				"\tUsage:\t\t mv [source] [target] <--no-child-nodes*>\n" +
				"\tExample:\t mv . /some/node\n\n\t* Optional parameter; Another short variant is '--n'");

		/*
		 * ================================================ sort  ================================================
		 */
		instance.provideNewCommand("sort", new Command() {
			@Override
			public String runCommand(CommandOutputProvider out, ApplicationInstance instance, String[] args) {
				TreeNode parentNode = null;
				boolean recursive = false;
				if(args.length == 0){
					parentNode = instance.getTree().getSelectedNode();
				} else if(args.length == 1){
					if(args[0].toLowerCase().equals("-r")){
						recursive = true;
					}
					else{
						parentNode = instance.getTree().getNodeByPath(args[0]);
					}
				}
				else if(args.length == 2)
				{
					if(args[0].toLowerCase().equals("-r")){
						recursive = true;
					}
					else{
						out.println("Unkown parameter: '" + args[0] + "'");
						return null;
					}

					parentNode = instance.getTree().getNodeByPath(args[1]);
				}

				if(parentNode != null){
					instance.getTree().sortChildNodes(parentNode, recursive);
					return "ok";
				}
				return null;
			}}, "Sort all child nodes of a tree node. If you even want to sort child nodes of child nodes use '-r'\nUsage: sort [-r] [tree node]");

		/*
		 * ================================================ undo  ================================================
		 */
		instance.provideNewCommand("undo", (CommandOutputProvider out, ApplicationInstance app, String[] args) -> {
				if(app.currentFile != null){
					if(!app.getTree().undo()){
						out.println("Cannot undo more operations.");
					}
				}
				else{
					out.println("Please open a password file first.");
				}
				return null;
			});
	}

	private static void nodeCopy(TreeNode src, TreeNode dest, boolean move, boolean includeChildNodes)
	{
		//src.getTree() and dest.getTree() will point to the same reference
		if(move){src.getTree().beginUpdate();}
		TreeNode clone = src.getTree().cloneTreeNode(src, includeChildNodes);
		dest.getTree().addNode(clone, dest);
		if(move){
			src.getTree().removeNode(src);
			dest.getTree().endUpdate();
		}
	}

	private static int rekursiveSubstitute(TreeNode node, String find, String replaceWith, boolean ignoreCase)
	{
		int replacements = 0;
		for(TreeNode n: node.getChildNodes()){
			if(TreeSearch.replaceTextOfNode(n, find, replaceWith, ignoreCase)){replacements++;}
			replacements += rekursiveSubstitute(n, find, replaceWith, ignoreCase);
		}
		return replacements;
	}

	public static void setClipboardText(String str){
		try{
			java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(str);
			java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
		    clipboard.setContents(selection, selection);
		}
		catch(Exception ex){
		}
	}

	public static String getClipboardText(){
		try{
			return (String) java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().getData(java.awt.datatransfer.DataFlavor.stringFlavor);
		}
		catch(Exception ex){
			return "";
		}
	}
}
