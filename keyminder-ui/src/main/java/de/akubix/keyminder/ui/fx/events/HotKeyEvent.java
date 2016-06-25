package de.akubix.keyminder.ui.fx.events;

import java.util.function.BooleanSupplier;

public abstract class HotKeyEvent {
	BooleanSupplier condition;
	public HotKeyEvent(){
		this.condition = null;
	}
	public HotKeyEvent(BooleanSupplier condition){
		this.condition = condition;
	}
	public void fireEvent(){
		if(this.condition == null){
			onKeyDown();
		}
		else if(this.condition.getAsBoolean()){onKeyDown();}
	}

	public abstract void onKeyDown();
}
