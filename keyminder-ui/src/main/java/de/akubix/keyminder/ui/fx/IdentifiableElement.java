package de.akubix.keyminder.ui.fx;

public enum IdentifiableElement {

	SIDEBAR_TAB_PANEL("sidebar-tab-panel", false),
	SIDEBAR_HEADER("sidebar-header", true);

	private String nodeId;
	private boolean allowAddCustomElementOperation;

	private IdentifiableElement(String id, boolean allowAddCustomElementOperation){
		this.nodeId = id;
		this.allowAddCustomElementOperation = allowAddCustomElementOperation;
	}

	public String getId(){
		return this.nodeId;
	}

	public String getLookupId(){
		return "#" + this.nodeId;
	}

	boolean allowAddCustomElementOperation(){
		return this.allowAddCustomElementOperation;
	}
}
