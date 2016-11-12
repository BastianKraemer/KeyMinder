package de.akubix.keyminder.shell.io;

import java.util.Map;

import de.akubix.keyminder.core.tree.TreeNode;
import de.akubix.keyminder.shell.annotations.Operands;

/**
 * <p>An object of this class is given to each command when it is executed.</p>
 * <p>All command line arguments are stored in the {@link CommandInput#parameters} map,
 * furthermore there could be an input from the previous command stored in the {@link CommandInput#inputData} object.
 * <br>In most cases, the {@link CommandInput#treeNode} field will contain the currently selected {@link TreeNode}, but
 * it is possible to use the {@link Operands} annotation to reference another tree node.</p>
 */
public class CommandInput {

	private Map<String, String[]> parameters;
	private Object inputData;
	private TreeNode treeNode;
	private boolean pipedOutput;

	/**
	 * Create a new command input object. The value for {@link CommandInput#inputData} is set to {@code null} by default.
	 * @param parameters The parameters of the command
	 * @param treeNode a tree node the command my work with (this is the currently selected node by default)
	 */
	public CommandInput(Map<String, String[]> parameters, TreeNode treeNode){
		this.parameters = parameters;
		this.inputData = null;
		this.treeNode = treeNode;
		this.pipedOutput = false;
	}

	/**
	 * @return the command parameters
	 */
	public Map<String, String[]> getParameters() {
		return this.parameters;
	}

	/**
	 * @return the input data
	 */
	public Object getInputData() {
		return this.inputData;
	}

	/**
	 * @param inputData the inputData to set
	 */
	public void setInputData(Object inputData) {
		this.inputData = inputData;
	}

	public void setPipedOutputInidcator(){
		this.pipedOutput = true;
	}

	/**
	 * @return the tree node for this command
	 */
	public TreeNode getTreeNode() {
		return this.treeNode;
	}

	/**
	 * Returns {@code true} if the output is piped to the next command
	 * @return the pipe status
	 */
	public boolean outputIsPiped(){
		return this.pipedOutput;
	}
}
