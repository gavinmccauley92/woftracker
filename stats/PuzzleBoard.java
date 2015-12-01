package woftracker.stats;

import woftracker.record.Puzzle;
import woftracker.record.BonusPuzzle;
import woftracker.record.Letter;
import java.util.*;
import java.io.Serializable;
import java.util.stream.Collectors;

public class PuzzleBoard implements Serializable {
	private static final long serialVersionUID = 1L;	//version number.
	
	private static final int[] LINE_LENGTH = { 12, 14, 14, 12 };
	
	private String simplePuzzle, finalState;
	
	public PuzzleBoard(Puzzle pu) {
		simplePuzzle = pu.toString().replace("-<br>", "-").replace("<br>", " ");
		finalState = pu.getPuzzle().replace("<br>", "\n");
	}
	
	//package-private constructor for retroactive support
	PuzzleBoard(String full, Collection<Letter> called, int lines) {
		simplePuzzle = full;
		if(called == null)
			finalState = null;
		else {
			int lineIndex = lines > 2  ? 0 : 1;
			LinkedList<String> l = new LinkedList<>(Arrays.asList(full.replace("-", "- ").split(" ")));
			
			for(int j = 0; l.size() > lines; ) {
				//slowly eating one word at a time helps to avoid a two-line, three-small word puzzle from getting wrongly compressed into one line.
				String twoWords = String.join(" ", l.subList(j, j+2)).replace("- ", "-");
				if(j+2 <= l.size() && twoWords.length() <= LINE_LENGTH[lineIndex]) {
					l.set(j, twoWords);
					l.remove(j+1);
				} else {
					j++;
					lineIndex++;
				}
			}
			
			finalState = String.join("\n", l.stream().map(s -> String.join(" ", s.replace(' ', '/').split("")).trim()).toArray(String[]::new));
			
			Set<Letter> uncalled = EnumSet.complementOf(EnumSet.copyOf(called));
			if(uncalled.size() > 0)
				finalState = finalState.replaceAll('[' + String.join("", uncalled.stream().map(le -> le.name()).collect(Collectors.joining(""))) + ']', "_");
		}
	}
	
	public String getFullPuzzle() {
		return simplePuzzle;
	}
	
	public String getFinalState() {
		return finalState;
	}
	
	public String toString() {
		return simplePuzzle + (finalState != null ? "\n\t" + finalState.replace("\n", "\n\t") : "");
	}
	
	public static void main(String[] args) {
		Puzzle p = new BonusPuzzle("BRAINS AND<br>BRAWN");
		p.updatePuzzle(EnumSet.of(Letter.B, Letter.G, Letter.H, Letter.O));
		System.out.println(new PuzzleBoard("CHICKEN-FRIED STEAK SMOTHERED IN GRAVY", EnumSet.complementOf(BonusPuzzle.GIVEN), 4));
		System.out.println(new PuzzleBoard("COMMEMORATIVE COIN COLLECTION", EnumSet.noneOf(Letter.class), 3));
	}
}