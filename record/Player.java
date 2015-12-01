package woftracker.record;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.EnumSet;
import java.util.Comparator;
import java.util.Arrays;

public enum Player {
	RED, YELLOW, BLUE;
	
	private static Player currentPlayer, tossUpWinner;	//reference only
	
	private int roundScore, totalScore, preMinimumScore;
	private static NumberFormat nF = NumberFormat.getCurrencyInstance(Locale.US);
	private EnumSet<Wedge> tempCardboard = EnumSet.noneOf(Wedge.class), permCardboard = EnumSet.noneOf(Wedge.class);
	private boolean failTossUp = false;
	private static boolean showRound = false;
	private String name, abbrName = "";
	
	public static final Comparator<Player> SCORE_COMPARATOR = Comparator.comparingInt(p -> p.getTotalScore());
	public static boolean isTied(Player p1, Player p2) {
		return SCORE_COMPARATOR.compare(p1, p2) == 0;
	}
	public static boolean areLosersTied(Player winner) {
		Player[] p = Arrays.stream(Player.values()).filter(pl -> pl != winner).toArray(Player[]::new);
		return isTied(p[0], p[1]);
	}
	
	Player() {
		roundScore = 0;
		totalScore = 0;
		preMinimumScore = 0;
		name = this.name();
		abbrName = name.substring(0, 1) + '*';		//initial only, * used to avoid unnecessary replacing / denote default
	}
	
	public static void setCurrentPlayer(Player player) {
		currentPlayer = player;
	}
	
	//R5 & beyond are taken care of as far as their starting points go. Will R7 ever happen again?
	public static void setRoundStarter(int round) {	//call setRoundPattern before this
		if(round > 4) {
			Player desiredPlayer = tossUpWinner;
			for(int i = 0; i < RecordPanel.roundNumber - 4; i++)
				desiredPlayer = passTurn(desiredPlayer);
			
			for(Player thisPlayer = currentPlayer; thisPlayer != desiredPlayer; thisPlayer = passTurn(thisPlayer))
				Wedge.adjustPrevWedge(RecordGamePanel.getPrev(), thisPlayer);
		}
		
		switch(round % 3) {	//adjusted values below
			case 1:
				setCurrentPlayer(tossUpWinner);
				break;
			case 2:
				setCurrentPlayer(passTurn(tossUpWinner));
				break;
			case 0:
				setCurrentPlayer(passTurn(passTurn(tossUpWinner)));
		}
	}
	
	public static void setRoundStarter() {
		setRoundStarter(RecordPanel.roundNumber);
	}
	
	public static void setRoundPattern(Player player) {
		if(player != null)
			tossUpWinner = player;
	}
	
	
	public static void loseTurn() {
		Wedge.adjustPrevWedge(RecordGamePanel.getPrev(), currentPlayer, RecordGamePanel.isFinalSpin());
		setCurrentPlayer(passTurn(currentPlayer));
	}
	
	public static Player passTurn(Player p) {
		switch(p) {
			case RED:
				return YELLOW;
			case YELLOW:
				return BLUE;
			case BLUE:
				return RED;
			default:
				return null;	//suffices compiler
		}
	}
	
	public static Player getCurrentPlayer() {
		return currentPlayer;
	}
	
	public void setName(String name) {
		this.name = name;
		
		String s = "";
		for(char l : this.name.toCharArray()) 
			if(Character.isUpperCase(l) || l == '&')
				s += l;
		
		abbrName = s;
	}
	
	public boolean isCouples() {
		return name.indexOf(" & ") != -1;
	}
	
	private void adjustAbbrName(int lowerCaseAdjuster) {
		if(abbrName.indexOf('&') == -1)
			abbrName += name.charAt(lowerCaseAdjuster);
		else {	//old way still needed for couples, can't really be improved much, fortunately very rare
			try {
				String s = "";
				
				for(char l : this.name.toCharArray()) {
					if(Character.isUpperCase(l))
						s += name.substring(name.indexOf(l), name.indexOf(l) + lowerCaseAdjuster + 1);
					else if(l == '&')
						s += '&';
				}
					
				abbrName = s;
			} catch(IndexOutOfBoundsException e) {
				abbrName = name;
			}
		}
	}
	
	public static void checkAbbrNames() {	//safe to call anytime since constructor gives these fields default values
		EnumSet<Player> matchingAbbr = EnumSet.noneOf(Player.class);
		
		for(byte abbrNameAdjuster = 0; abbrNameAdjuster < 3; matchingAbbr.clear()) {
			String r = RED.getAbbrName(), y = YELLOW.getAbbrName(), b = BLUE.getAbbrName();
			
			if(r.equals(y)) {
				matchingAbbr.add(RED);
				matchingAbbr.add(YELLOW);
				if(y.equals(b))
					matchingAbbr.add(BLUE);
			} else if(y.equals(b)) {
				matchingAbbr.add(YELLOW);
				matchingAbbr.add(BLUE);
			} else if (r.equals(b)) {
				matchingAbbr.add(RED);
				matchingAbbr.add(BLUE);
			} else
				break;
			
			abbrNameAdjuster++;
			
			if(abbrNameAdjuster < 3)
				for(Player p : matchingAbbr)
					p.adjustAbbrName(abbrNameAdjuster);
			else {
				char c = '1';
				for(Player p : matchingAbbr)
					p.abbrName += c++;
			}
		}
	}
	
	public String getName() {
		return name;
	}
	
	public String getAbbrName() {
		return abbrName;
	}
	
	public void addMoney(Wedge w, byte multiple) {
		roundScore += (w.getAmount() * multiple);
	}
	
	public void addMoney(Wedge w, int multiple) {
		roundScore += (w.getAmount() * multiple);
	}
	
	public void addMoney(int amount, byte multiple) {
		roundScore += amount * multiple;
	}
	
	public void addMoney(int amount, int multiple) {
		roundScore += amount * multiple;
	}
	
	public void addMoney(int amount) {
		roundScore += amount;
	}
	
	//should not be used much
	public void setMoney(int amount) {
		roundScore = amount;
	}
	
	public void buyVowel() {
		roundScore -= 250;
	}

	public void addRoundStatus(Wedge w) {
		tempCardboard.add(w);
	}
	
	public String getRoundStatus() {
		int n;
		return (hasPrize() ? " + Prize" : "") + (hasMystery() ? " + Mystery" : "") + (hasGiftTag() ? " + Gift Tag" : "") + (hasMDW(false) ? " + MDW" : "") + getHalfCarStatus(false);
	}
	
	public void setTotalStatus(Wedge w, boolean b) {
		if(b)
			permCardboard.add(w);
		else
			permCardboard.remove(w);
	}
	
	public String getTotalStatus() {
		return (hasWC() ? " + WC" : "") + (hasMDW(true) ? " + MDW" : "") + getHalfCarStatus(true);
	}
	
	private String getHalfCarStatus(boolean b) {
		int n = numHalfCars(b);
		return (n >= 1 ? " + " + (n == 2 ? "" : "1/2 ") + "Car" : "");
	}
	
	public void bankrupt() {
		tempCardboard.clear();
		permCardboard.clear();	//permCardboard.retainAll(Wedge.HALFCARS);
		
		roundScore = 0;
	}
	
	private void resetHalfCar() {
		permCardboard.removeAll(Wedge.HALFCARS);
	}
	
	public static void resetHalfCars() {
		RED.resetHalfCar();
		YELLOW.resetHalfCar();
		BLUE.resetHalfCar();
	}
	
	public void winRound(boolean didWin) {
		if(didWin) {
			totalScore += roundScore;
			
			if(hasMDW(false))
				permCardboard.add(Wedge.MillionDollarWedge);
			
			EnumSet<Wedge> tempHalfCar = EnumSet.copyOf(tempCardboard);
			tempHalfCar.retainAll(Wedge.HALFCARS);
			permCardboard.addAll(tempHalfCar);
		}
		
		tempCardboard.clear();
		roundScore = 0;
	}
	
	public void winCustomAmount(int cost) {
		totalScore += cost;
	}
	
	public void winTossUp() {
		totalScore += (RecordPanel.tossUpNumber) * 1000;
	}
	
	public void winTossUp(int i) {
		totalScore += i * 1000;
	}
	
	public boolean hasPrize() {
		return tempCardboard.contains(Wedge.PrizeOnWheel);
	}

	public boolean hasMystery() {
		return tempCardboard.contains(Wedge.Purple1000_1) || tempCardboard.contains(Wedge.Purple1000_2);	//only one of two can be banked, so this is safe
	}
	
	public boolean hasGiftTag() {
		return tempCardboard.contains(Wedge.GiftTag);
	}
	
	public boolean hasWC() {
		return permCardboard.contains(Wedge.WildCard);
	}
	
	public boolean hasMDW() {
		return tempCardboard.contains(Wedge.MillionDollarWedge) || permCardboard.contains(Wedge.MillionDollarWedge);
	}
	
	public boolean hasMDW(boolean b) {	//separate two, variable is for perma-keep specification.
		if(b) return permCardboard.contains(Wedge.MillionDollarWedge);
		else return tempCardboard.contains(Wedge.MillionDollarWedge);
	}
	
	public int numHalfCars() {
		EnumSet<Wedge> c = EnumSet.copyOf(permCardboard);
		c.addAll(tempCardboard);
		c.retainAll(Wedge.HALFCARS);
		
		return c.size();	
	}
	
	public int numHalfCars(boolean b) {	//separate two, variable is for perma-keep specification.
		EnumSet<Wedge> c = EnumSet.copyOf(b ? permCardboard : tempCardboard);
		c.retainAll(Wedge.HALFCARS);
		
		return c.size();
	}
	
	//the following two methods return data not "backed" by the Player.
	public EnumSet<Wedge> getCardboard(boolean b) {	//separate two
		return EnumSet.copyOf(b ? permCardboard : tempCardboard);
	}
	
	public EnumSet<Wedge> getAllCardboard() {
		EnumSet<Wedge> c = EnumSet.copyOf(permCardboard);
		c.addAll(tempCardboard);
		return c;
	}
	
	public int getRoundScore() {
		return roundScore;
	}
	
	public int getTotalScore() {
		return totalScore;
	}
	
	public boolean checkTotalMinimum() {
		return checkTotalMinimum(RecordGamePanel.getRoundMinimum());
	}
	
	public boolean checkTotalMinimum(int minimum) {	//maybe cleaner method later is to incorporate round minimum here too, but OK for now
		if (totalScore < minimum) {
			preMinimumScore = totalScore;
			totalScore = minimum;
			return true;
		} else
			return false;
	}
	
	public int getPreMinimumScore() {
		return preMinimumScore;
	}
	
	public void setFailTossUp(boolean b) {
		failTossUp = b;
	}
	
	public boolean didFailTossUp() {
		return failTossUp;
	}
	
	public String getFormattedRoundScore(boolean includeStatus) {
		String s = nF.format(roundScore);
		
		return s.substring(0, s.length() - 3) + (includeStatus ? getRoundStatus() : "");	// ".00" removed :)
	}
	
	public String getFormattedTotalScore(boolean includeStatus) {
		String s = nF.format(totalScore);
		
		return s.substring(0, s.length() - 3) + (includeStatus ? getTotalStatus() : "");	// ".00" removed :)
	}
	
	public static void setRoundActive(boolean b) {
		showRound = b;
	}
	
	public void reset() {
		bankrupt();
		totalScore = 0;
		tempCardboard.clear();
		permCardboard.clear();
		name = this.name();
		abbrName = name.substring(0, 1) + '*';
	}
	
	public String toLabelString() {
		return "<html><font family = \"Verdana\" size = \"36pt\"><b>" + name + ": " + (showRound ? getFormattedRoundScore(true) + "<p>" : "") +
		getFormattedTotalScore(true) + "</b></font>";
	}
	
	public static void main(String[] args) {
		RED.setName("Trent");
		YELLOW.setName("Joann");
		BLUE.setName("Joann");
		checkAbbrNames();
		for(Player p : Player.values())
			System.out.println(p.getName() + "\t" + p.getAbbrName());
	}
}