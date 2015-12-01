package woftracker.record;

import java.util.EnumSet;

public class MainPuzzle extends Puzzle {

	public MainPuzzle(String answer) {
		super(answer);
	}
	
	public String updatePuzzle(EnumSet<Letter> called) {
		currentState = new String(finalState);
		
		for(Letter l : EnumSet.complementOf(called))
			currentState = currentState.replace(l.getChar(), '_');
			
		return currentState;
	}
}