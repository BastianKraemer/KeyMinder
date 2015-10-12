package de.akubix.keyminder.ui.fx.shadow;

public abstract class FxHotKeyEvent {
	Precondition condition;
	public FxHotKeyEvent()
	{
		this.condition = null;
	}
	public FxHotKeyEvent(Precondition condition)
	{
		this.condition = condition;
	}
	public void fireEvent(){
		if(this.condition == null)
		{
			onKeyDown();
		}
		else if(this.condition.check()){onKeyDown();}
	}

	public abstract void onKeyDown();
}
