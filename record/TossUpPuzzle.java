package woftracker.record;

import java.util.EnumSet;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

public class TossUpPuzzle extends Puzzle {

	public TossUpPuzzle(String answer) {
		super(answer);
	}
	
	public void setTossUpState(String s) {
		currentState = s.toUpperCase().replace("<BR>", "<br>");
		List<Character> currentStateList = currentState.replace("<br>", "/").chars().mapToObj(i -> (char) i).filter(c -> c != ' ').collect(Collectors.toList()),
			solvedList = SOLVED.replace("<br>", "/").chars().mapToObj(i -> (char) i).collect(Collectors.toList());
		
		if(currentStateList.size() != solvedList.size())
			throw new IllegalArgumentException("Toss-up state & solved puzzle do not match in length");
		else {
			for(int i = 0; i < solvedList.size(); i++) {
				char c1 = currentStateList.get(i), c2 = solvedList.get(i);
				if(c1 != c2 && c1 != '_' && !(c1 == '/' && c2 == ' '))
					throw new IllegalArgumentException("Character mismatch in toss-up: \'" + c1 + "\' and \'" + c2 + "\'");
			}
		}
	}
	
	public String updatePuzzle(EnumSet<Letter> called) {
		throw new UnsupportedOperationException("Toss-ups do not reveal all of one letter at one time, this class uses setTossUpState instead");
	}
	
	public static void main(String[] args) {
		TossUpPuzzle t1 = new TossUpPuzzle("THIS IS NOT<br>A DRILL");
		t1.setTossUpState("_ _ I S / I _ / N _ _<br>_ / D _ _ L _");
		TossUpPuzzle t2 = new TossUpPuzzle("BITE-SIZE<br>PIECES");
		t2.setTossUpState("_ _ T E - S _ _ E<br>_ _ E _ E S");
		TossUpPuzzle t3 = new TossUpPuzzle("FLIP-FLOPS");
		t3.setTossUpState("_ L _ P - _ L _ P S");
		TossUpPuzzle t4 = new TossUpPuzzle("FLIP-<br>FLOPS");
		t4.setTossUpState("_ L _ P -<br>_ L _ P S");
		TossUpPuzzle t5 = new TossUpPuzzle("FAITHFUL<br>FRIENDS");
		t5.setTossUpState("_ A _ T H F U L<br>_ _ I E N D S");
		
		System.out.println("All OK!");
	}
}