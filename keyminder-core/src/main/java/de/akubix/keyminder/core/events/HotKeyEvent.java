package de.akubix.keyminder.core.events;

import de.akubix.keyminder.core.interfaces.Precondition;

public abstract class HotKeyEvent {
	Precondition condition;
	public HotKeyEvent(){
		this.condition = null;
	}
	public HotKeyEvent(Precondition condition){
		this.condition = condition;
	}
	public void fireEvent(){
		if(this.condition == null){
			onKeyDown();
		}
		else if(this.condition.check()){onKeyDown();}
	}

	public abstract void onKeyDown();
}
