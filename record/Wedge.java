package woftracker.record;

import java.util.*;

public enum Wedge {
	/* all wedges used since the start of S29 are kept for analysis. */
	/* identical wedges mostly listed in spinning direction, starting at big money. Exception is the gift tagS */
	
	Turquoise2500(2500, 0),
	Ruby3500(3500, 0),
	Silver5000(5000, 0),
	BANKRUPT_1(0, 3),
	Orange300(300, 6),	//INACTIVE AS OF S32
	Orange900(900, 6),
	Green500_1(500, 9, false),
	Green500(500, 9, false),	//INACTIVE AS OF S30 (more Green 500s)
	HalfCar_1(500, 9, true),
	HalfCar_3(500, 9, false),
	HalfCar_5(500, 9, false),
	Pink450(450, 12),	//INACTIVE AS OF S32
	Pink650(650, 12),
	Purple500(500, 15, false),
	Purple500_1(500, 15, true),	//see PRIZE_REPLACEMENT
	Purple350(350, 15, false),	//INACTIVE AS OF S30
	PrizeOnWheel(500, 15),	//usual location, see PRIZE_REPLACEMENT below
	Red800(800, 18),
	LOSE_A_TURN(0, 21),
	Blue700(700, 24),
	Purple1000_1(1000, 24),
	FreePlay(500, 27),
	Purple650(650, 30),
	Purple600(600, 30, false),	//INACTIVE AS OF S30
	BANKRUPT_2(0, 33),
	Pink900(900, 36),	//INACTIVE AS OF S33
	Pink600(600, 36),
	Green500_2(500, 39, false),
	Green300(300, 39),	//INACTIVE AS OF S30
	HalfCar_2(500, 39, true, 29, 42),
	HalfCar_4(500, 39, false, 29, 42),
	HalfCar_6(500, 39, false, 29, 42),
	Blue350(350, 42),	//INACTIVE AS OF S32
	Blue550(550, 42),
	Blue500_1(500, 42, false),	//INACTIVE AS OF S30
	Red600(600, 45),
	Red900(900, 45),	//INACTIVE AS OF S30
	Pink500(500, 48),	//NOW COVERED BY MDW IN S31
	Pink300(300, 48, false),	//INACTIVE AS OF S30
	GiftTag(500, 54, 30, 48, 29, 48),	//WAS 48 IN S30-
	GiftTag_2(500, 15),	//SECOND GIFT TAG, USED FIRST IN NBA WEEK IN S30, OVER PURPLE 500 #1 (PRIZE ON WHEEL MOVED TO 650), COULD BE USED AGAIN. SEE LOOKUP BELOW.
	Yellow400(400, 51),	//INACTIVE AS OF S32
	Yellow700(700, 51),
	Purple550(550, 54, false),	//INACTIVE AS OF S31
	Purple500_2(500, 54, false),
	Orange800(800, 57),	//THIS BECAME FULL TIME IN S31, INACTIVE AS OF S33
	Orange650(650, 57),
	MillionDollarWedge(0, 48, 30, 57, 29, 57),	//WAS 57 IN S30-
	Blue300(300, 60, false, 29, 24),	//note this wedge exists in S29 next to LAT, double switch now moves it here; 	INACTIVE AS OF S32
	Blue500_2(500, 60, false),	//INACTIVE AS OF S30
	Blue600(600, 60),
	Purple1000_2(1000, 60),
	Red700(700, 63),
	Red300(300, 63, false),	//INACTIVE AS OF S30
	Jackpot(500, 63, false),	//INACTIVE AS OF S31
	Express(1000, 63, false),	//ACTIVE IN R3 IN S31
	Yellow900(900, 66),
	Yellow600(600, 66),	//R3 IN S33
	Yellow500(500, 66, false),	//INACTIVE AS OF S30
	Green600(600, 69, false),	//INACTIVE AS OF S30
	Green500_3(500, 69, false),
	WildCard(500, 69, 29, 36);
	
	private int amount, location, wedgePosition;
	private boolean active;	//sum '11 programming has made this variable kind of obsolete with roundConfiguration, but it is still useful on permanent cardboard changes. An update may be done at some point
	private String wedgeName;
	private Map<Integer, Integer> pastSeasonLocations;
	public static final int LEFT = 1, CENTER = 0, RIGHT = -1;	//constants are explicitly defined for index adjusting
	
	//in a perfect world, Wheel would have the same wedges on the same locations every night. But sometimes, color fucking matters to them.
	private static Set<Wedge> roundConfiguration = new TreeSet<>((w1, w2) -> w1.location - w2.location);
	
	public static final EnumSet<Wedge> HALFCARS = EnumSet.of(HalfCar_1, HalfCar_2, HalfCar_3, HalfCar_4, HalfCar_5, HalfCar_6),
		INACTIVE = EnumSet.of(Green500, Purple350, Purple600, Green300, Blue500_1, Red900, Pink300, Blue500_2, Red300, Yellow500, Green600, Purple550),
		ACTIVE = EnumSet.complementOf(INACTIVE)
	;
	
	public static final Map<String, Set<Wedge>> GROUPING_MAP = new HashMap<>();
	static {
		GROUPING_MAP.put("Purple $500 (#1)", EnumSet.of(Purple500, Purple500_1));
	}
	
	public static final int MYSTERY_ROUND = 2;
	public static final boolean NO_HALF_CARS = true;
	
	public static final Map<String, Wedge> LOOKUP = new HashMap<>();
	
	Wedge(int amount, int location, boolean active, int ... differences) {
		this.amount = amount;
		this.location = location;
		this.active = active;
		wedgeName = formalize(this);
		
		pastSeasonLocations = new HashMap<>();
		for(int i = 0; i < differences.length; i += 2)
			pastSeasonLocations.put(differences[i], differences[i+1]);
	}
	
	Wedge(int amount, int location, int ... differences) {
		this(amount, location, true, differences);
	}
	
	public static void setRoundConfiguration(int round) {
		if(round >= 5)
			return;
		
		roundConfiguration.clear();
		boolean b = RecordGamePanel.isCarWon();
		
		//other cardboard, that is, one-time replacements, is taken care of in the game or in startMainRound of RecordGamePanel, R4.
		for(Wedge w : ACTIVE) {
			switch(w) {
				case Turquoise2500:
				case Jackpot:
					w.active = round == 1;
					break;
				case HalfCar_1:
				case HalfCar_2:
					w.active = round == 1 && !NO_HALF_CARS;
					break;
				case Red700:
					w.active = round != 1;
					break;
				case Ruby3500:
					w.active = round == 2 || round == 3;
					break;
				case HalfCar_3:
				case HalfCar_4:
					w.active = round == 2 && !b && !NO_HALF_CARS;
					break;
				case Purple1000_1:
				case Purple1000_2:
					w.active = round == MYSTERY_ROUND;
					break;
				case Blue700:
				case Blue300:
					w.active = round != MYSTERY_ROUND;
					break;
				case HalfCar_5:
				case HalfCar_6:
					w.active = round == 3 && !b && !NO_HALF_CARS;
					break;
				case Green500_1:
				case Green500_2:
					if(b || NO_HALF_CARS) {
						w.active = true;
						break;
					}
				case Silver5000:
					w.active = round == 4;	//no need to reset Wheel on R5+
			}
			
			if(w.active)
				roundConfiguration.add(w);
		}
	}
	
	static Set<Wedge> getRoundConfiguration() {
		return roundConfiguration;
	}
	
	private static Wedge PRIZE_REPLACEMENT = Purple500_1;
	static {
		PrizeOnWheel.location = PRIZE_REPLACEMENT.location;
		PRIZE_REPLACEMENT.active = false;
	}
	
	public static void setPrizeReplacement(Wedge w) {
		PRIZE_REPLACEMENT = w;
		PrizeOnWheel.location = PRIZE_REPLACEMENT.location;
		PRIZE_REPLACEMENT.active = false;
	}
	
	public static Wedge getPrizeReplacement() {
		return PRIZE_REPLACEMENT;
	}
	
	private static final Map<Wedge, Wedge> REPLACEMENTS = new EnumMap<>(Wedge.class);
	static {
		REPLACEMENTS.put(WildCard, Green500_3);
		REPLACEMENTS.put(PrizeOnWheel, PRIZE_REPLACEMENT);
		REPLACEMENTS.put(Purple1000_1, Blue700);
		REPLACEMENTS.put(GiftTag, Purple500_2);	//as of S31
		REPLACEMENTS.put(MillionDollarWedge, Pink500);	//as of S31
		REPLACEMENTS.put(Purple1000_2, Blue300);
		REPLACEMENTS.put(HalfCar_1, Green500);
		REPLACEMENTS.put(HalfCar_2, Green500_2);
		REPLACEMENTS.put(HalfCar_3, Green500);
		REPLACEMENTS.put(HalfCar_4, Green500_2);
		REPLACEMENTS.put(HalfCar_5, Green500);
		REPLACEMENTS.put(HalfCar_6, Green500_2);
		
		//for resetting only
		for(Map.Entry<Wedge, Wedge> entry : REPLACEMENTS.entrySet())
			if(entry.getKey().name().indexOf("HalfCar") == -1)
				REPLACEMENTS.put(entry.getValue(), entry.getKey());
				
		REPLACEMENTS.put(Silver5000, Turquoise2500);
		REPLACEMENTS.put(Green500, HalfCar_1);
		REPLACEMENTS.put(Green500_2, HalfCar_2);
	}
	
	public static void replaceWedge(Wedge oldW) {
		Wedge newW = REPLACEMENTS.get(oldW);
		
		if(newW != null) {
			oldW.active = false;
			roundConfiguration.remove(oldW);
			
			newW.active = true;
			roundConfiguration.add(newW);
		}
	}
	
	//should never need
	public static void replaceWedge(Wedge oldW, Wedge newW) {
		oldW.active = false;
		roundConfiguration.remove(oldW);
		
		newW.active = true;
		roundConfiguration.add(newW);
	}
	
	public static void resetWedges() {
		replaceWedge(Wedge.Green500_1);
		replaceWedge(PRIZE_REPLACEMENT);
		replaceWedge(Wedge.Pink500);
		replaceWedge(Wedge.Silver5000);
		replaceWedge(Wedge.Orange800);
		replaceWedge(Wedge.Green500_2);
		replaceWedge(Wedge.Green500_3);
	}
	
	public void setPosition(int wP) {
		if(!(wP < RIGHT || wP > LEFT))
			wedgePosition = wP;
	}
	
	public int getPosition() {
		return wedgePosition;
	}
	
	public static int determineStrength(Wedge prev, Wedge now, boolean overRotation) {
		return determineStrength(prev, now, overRotation, RecordGamePanel.getPrevPosition());
	}
	
	public static int CURRENT_SEASON;
	public static int determineStrength(Wedge prev, Wedge now, boolean overRotation, int samePrevPosition) {
		int nowIndex = now.getLocation(CURRENT_SEASON) + now.getPosition();
		int prevIndex = prev.getLocation(CURRENT_SEASON) + (prev == now ? samePrevPosition : prev.getPosition());
		
		//adjustment: -1 to 71.
		if(nowIndex == -1)
			nowIndex = 71;
		if(prevIndex == -1)
			prevIndex = 71;
		
		int displacement = nowIndex - prevIndex;
		//from right to left of big money is 1 - 71 = -70, should be 2; from left to right of same wedge is 71 - 1 = 70, it is 70
		
		if(displacement < 0)
			displacement += 72;
		
		return (overRotation ? 72 : 0) + displacement;
	}
	
	public static void adjustPrevWedge(Wedge prev, Player p, boolean isFinal) {	//for lost turns or final spin, adjust the starting point of a spin. p is the OLD player who lost the turn.
		int currentLocation = prev.getAdjustedLocation(CURRENT_SEASON);
		
		if(p == Player.BLUE)
			currentLocation += 14;
		else if(p == Player.YELLOW)
			currentLocation += (isFinal ? 7 : -7);
		else {
			if(!isFinal)
				currentLocation -= 7;
			else
				return;	//no adjustment needed
		}
		
		if(currentLocation >= 71)	//right side of big money technically has adjustedLocation of -1, not 71
			currentLocation -= 72;
		else if(currentLocation < 0)
			currentLocation += 72;
		//System.out.println(prev + "\t" + p + "\t" + currentLocation);
		for(Wedge w : getRoundConfiguration()) {	//exhaustive search isn't the best, but just an assignment, subtraction and two comparisions throughout loop
			int location = currentLocation - w.getLocation(CURRENT_SEASON);
			
			if(location >= RIGHT && location <= LEFT) {
				w.setPosition(location);
				RecordGamePanel.setPrev(w);
				break;
			}
		}
	}
	
	public static void adjustPrevWedge(Wedge prev, Player p) {
		adjustPrevWedge(prev, p, RecordGamePanel.isFinalSpin());
	}
	
	public static boolean isBlack(Wedge w) {
		return (w == Wedge.BANKRUPT_1 || w == Wedge.BANKRUPT_2 || (w == Wedge.MillionDollarWedge && w.getPosition() != Wedge.CENTER));
	}
	
	public int getAmount() {
		return amount;
	}
	
	public int getLocation() {
		return location;
	}
	
	public int getLocation(int season) {
		return Optional.ofNullable(pastSeasonLocations.get(season)).orElse(location);
	}
	
	public int getAdjustedLocation() {
		return location + wedgePosition;
	}
	
	public int getAdjustedLocation(int season) {
		return Optional.ofNullable(pastSeasonLocations.get(season)).orElse(location) + wedgePosition;
	}
	
	public void setActive(boolean b) {
		active = b;
	}
	
	public boolean isActive() {
		return active;
	}
	
	private static String formalize(Wedge w) {		
		StringBuilder sB = new StringBuilder(w.name().replace('_', ' ').replace("Half", "1/2 "));
		for(int i = 0; i < sB.length() - 1; i++) {
			char j = sB.charAt(i);
			char k = sB.charAt(i+1);
			if (Character.isLowerCase(j)) {
				if(Character.isDigit(k))
					sB.insert(++i, " $");	//adjust for newly added space character
				if(Character.isUpperCase(k))
					sB.insert(++i, ' ');
			}
			else if (j == ' ') {
				if(Character.isDigit(k))
					sB.insert(i+1, "#");	// "#x"
			}
		}
		return sB.toString();
	}
	
	public String toTrimmedString() {
		int indexOf$ = wedgeName.indexOf('$'), indexOfPound = wedgeName.indexOf('#');
		if(indexOf$ == -1) indexOf$ = 0;
		if(indexOfPound == -1) indexOfPound = wedgeName.length();
		else indexOfPound--;
		
		return wedgeName.substring(indexOf$, indexOfPound);
	}
	
	public String toString() {
		return wedgeName;
	}
	
	//static within enum is as valid as static within any class. pretty deft
	static {
		for(Wedge w : ACTIVE)
			switch(w.amount) {
				case 0:
					LOOKUP.put(w.wedgeName, w);
					break;
				case 350: case 400: case 450: case 550: /*case 600: case 650:*/ case 2500: case 3500: case 5000:	//unique amount wedges
					LOOKUP.put(w.wedgeName.substring(w.wedgeName.indexOf('$')), w);
					break;
				default:
					LOOKUP.put(w.wedgeName, w);
					if(w.wedgeName.indexOf('$') == -1)
						break;
					char c = w.wedgeName.charAt(0);	//only capital letter in name, safe to replace below
					LOOKUP.put(w.wedgeName.replace(c, Character.toLowerCase(c)), w);
			}
		
		//now some extras
		LOOKUP.put("purple $550", Wedge.Purple550);	//S30-
		LOOKUP.put("blue $550", Wedge.Blue550);	//S32+
		LOOKUP.put("purple $500 #1", Wedge.Purple500_1);	//due to S31's doubling of purple 500
		LOOKUP.put("$600", Wedge.Red600);	//default for <S32
		LOOKUP.put("$650", Wedge.Purple650);	//default for <S32
		LOOKUP.put("$800", Wedge.Red800);	//when MDW is on Wheel, 800 is unique
		LOOKUP.put("gift tag", Wedge.GiftTag);
		LOOKUP.put("tag", Wedge.GiftTag);
		LOOKUP.put("gift tag #2", Wedge.GiftTag_2);
		LOOKUP.put("tag #2", Wedge.GiftTag_2);
		LOOKUP.put("MDW", Wedge.MillionDollarWedge);
		LOOKUP.put("LEFT MDW BANKRUPT", Wedge.MillionDollarWedge);
		LOOKUP.put("RIGHT MDW BANKRUPT", Wedge.MillionDollarWedge);
		LOOKUP.put("WC", Wedge.WildCard);
		LOOKUP.put("$1000 #1", Wedge.Purple1000_1);
		LOOKUP.put("$1000 next to LAT", Wedge.Purple1000_1);
		LOOKUP.put("$1000 #2", Wedge.Purple1000_2);
		LOOKUP.put("$1000 next to MDW", Wedge.Purple1000_2);
		LOOKUP.put("$1000 next to where MDW was", Wedge.Purple1000_2);
		LOOKUP.put("LAT", Wedge.LOSE_A_TURN);
	}
	
	public static void main(String[] args) {
		for(Map.Entry<String, Wedge> w : LOOKUP.entrySet())
			System.out.println(w);
	}
}
