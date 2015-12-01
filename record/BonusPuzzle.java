package woftracker.record;

import java.util.EnumSet;

public class BonusPuzzle extends MainPuzzle {
	public static final EnumSet<Letter> GIVEN = EnumSet.of(Letter.R, Letter.S, Letter.T, Letter.L, Letter.N, Letter.E), NOT_GIVEN = EnumSet.complementOf(GIVEN);
	
	public BonusPuzzle(String answer) {
		super(answer);
		updatePuzzle(null);
	}
	
	public String updatePuzzle(EnumSet<Letter> called) {
		EnumSet<Letter> copyCalled = called != null ? EnumSet.copyOf(called) : EnumSet.noneOf(Letter.class);
		copyCalled.addAll(GIVEN);
		return super.updatePuzzle(copyCalled);
	}
}