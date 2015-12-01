package woftracker.record;

import java.util.EnumSet;

abstract public class Puzzle {
	protected final String SOLVED;
	protected String currentState, finalState;
	
	Puzzle(String answer) {
		SOLVED = answer.toUpperCase().replace("<BR>", "<br>");
		finalState = formatPuzzle(SOLVED.replace(' ', SOLVED.indexOf('/') != -1 ? '|' : '/'));
	}
	
	protected String formatPuzzle(String s) {
		StringBuilder fP = new StringBuilder(s);
		for(int i = 0; i < fP.length(); i++) {
			char l = fP.charAt(i);
			if ((l >= 'A' && l <= 'Z') || (l == '\'') || (l == '/') || (l == '&') || (l == ':') || (l == '.') || (l == '|') || (l == '-') || (l == '!') || (l == '?'))
				fP.insert(++i, ' ');	//adjust for newly added space character
		}
		
		return fP.toString();
	}
	
	public String getPuzzle() {
		return currentState;
	}
	
	abstract public String updatePuzzle(EnumSet<Letter> called);
	
	public String toString() {
		return SOLVED;
	}
	
	public static void main(String[] args) {
		Puzzle p = new MainPuzzle("TEST TEST");
		System.out.println(p.updatePuzzle(EnumSet.of(Letter.E, Letter.S)));
	}
}