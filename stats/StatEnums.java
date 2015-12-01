package woftracker.stats;

import java.util.*;
import java.util.stream.*;
import java.io.File;
import woftracker.record.*;

/* the following is useful enums for GameAnalysis */

/** UNFORTUNATELY, ORIGINAL ENUMS COULD NOT BE PUT HERE SINCE GAMES WERE RECORDED WITH THE GAMEANALYSIS SIGNATURE. SHOOT. */

enum DerailReason {
	Dud, Repeat, Missolve, Buzzout;
}

/* end enums. */

/* enums for MultiGameAnalysis */

//note the integer freq can be reobtained from the ordinal for non-repeat cases.
enum Freq {
	ZERO, ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, ELEVEN, REPEAT;
	
	private static final String[] MULT_STRINGS = { "Dud", "Single", "Double", "Triple", "Quadruple", "Quintuple", "Sextuple", "Septuple", "Octuple", "Nonuple", "Decuple", "Undecuple", "Repeat" };
	
	public static final Set<Freq> FAIL = EnumSet.of(ZERO, REPEAT);
	
	static Freq getFreq(Number n) {
		try {
			return values()[n.intValue()];
		} catch(IndexOutOfBoundsException e) {
			return null;
		} catch(NullPointerException e) {
			return REPEAT;	//consistent with null = repeat, compared to 0 = dud
		}
	}
	
	public String toString() {
		return MULT_STRINGS[ordinal()];
	}
}

enum WCUseType { Maingame, BR };

enum WheelPosition {
	ZERO("Turquoise $2500, Ruby $3500, Silver $5000, Wheel 6000"), THREE("BANKRUPT #1"), SIX("S29-31: Orange $300, S32: Orange $900"), NINE("Green 1/2 Car & $500 #1"),
	TWELVE("S29-31: Pink $450, S32: Pink $650"), FIFTEEN("Prize on Wheel, S29: Purple $350, S30+: Purple $500 #1"), EIGHTEEN("Red $800"), TWENTY_ONE("LOSE A TURN"),
	TWENTY_FOUR("S29: Blue $300, S30+: Blue $700; & $1000 #1"), TWENTY_SEVEN("Free Play"), THIRTY("S29: Purple $600, S30+: Purple $650"), THIRTY_THREE("BANKRUPT #2"),
	THIRTY_SIX("Pink $900 (S29: & Wild Card)"), THIRTY_NINE("S29: Green $300, S30+: Green 1/2 Car & $500 #2"), FORTY_TWO("S29: Blue 1/2 Car, S30-31: Blue $350, S32: Blue $550"),
	FORTY_FIVE("S29: Red $900, S30+: Red $600"), FORTY_EIGHT("S29: GT/Pink $300, S30: GT/Pink $500, S31+: MDW/Pink $500"), FIFTY_ONE("S29-31: Yellow $400, S32: Yellow $700"),
	FIFTY_FOUR("S29-30: Purple $550, S31+: GT & Purple $500 #2"), FIFTY_SEVEN("Orange $800 (S29-30: & MDW)"), SIXTY("$1000 #2 & S29 Blue $500, S30-31 Blue $300, S32 Blue $600"),
	SIXTY_THREE("S29 Red $300, S30+ Red $700; S29-30 Jackpot, S31 Express"), SIXTY_SIX("S29: Yellow $500, S30+: Yellow $900"), SIXTY_NINE("S29: Green $600, S30+: Wild Card & Green $500 #3");
	
	private String s;
	
	WheelPosition(String s) {
		this.s = s;
	}
	
	static BarChart<WheelPosition> transformChart(BarChart<Wedge> bCW, int season) {
		BarChart<WheelPosition> bCWP = new BarChart<>(EnumSet.allOf(WheelPosition.class));
		
		for(Wedge w : bCW.getXs())
			bCWP.addY(WheelPosition.values()[w.getLocation(season)/3], bCW.getY(w));
		bCWP.remove0s();
		
		return bCWP;
	}
	
	public String toString() {
		return s;
	}
}

enum WinnerRelativeToStarter {
	STARTER, NEXT, LAST;
	
	public static WinnerRelativeToStarter get(Player starter, Player winner) {
		int diff = winner.ordinal() - starter.ordinal();
		return WinnerRelativeToStarter.values()[diff < 0 ? diff + 3 : diff];
	}
}

/* end enums */

/* the following is final EnumSets for various uses. */

public interface StatEnums {
	Set<BonusAmount> ALL_BONUS_AMOUNTS = EnumSet.allOf(BonusAmount.class);
	Set<BonusWedge> ALL_BONUS_WEDGES = EnumSet.allOf(BonusWedge.class);
	Set<Category> ALL_CATEGORIES = EnumSet.allOf(Category.class);
	Set<GameAnalysis.FreePlayEvent> ALL_FREEPLAY_EVENTS = EnumSet.allOf(GameAnalysis.FreePlayEvent.class);
	Set<Letter> ALL_LETTERS = EnumSet.allOf(Letter.class);
	Set<Player> ALL_PLAYERS = EnumSet.allOf(Player.class);
	Set<GameAnalysis.PrizeEvent> ALL_PRIZE_EVENTS = EnumSet.allOf(GameAnalysis.PrizeEvent.class);
	Set<GameAnalysis.Round> MAINGAME = EnumSet.of(GameAnalysis.Round.R1, GameAnalysis.Round.R2, GameAnalysis.Round.R3, GameAnalysis.Round.R4, GameAnalysis.Round.R5, GameAnalysis.Round.R6, GameAnalysis.Round.R7),
		TOSSUPS = EnumSet.of(GameAnalysis.Round.T1, GameAnalysis.Round.T2, GameAnalysis.Round.T3, GameAnalysis.Round.TT),
		ALL_ROUNDS_EXCEPT_TOSS = EnumSet.complementOf(EnumSet.copyOf(TOSSUPS)),
		GUARANTEED_ROUNDS = EnumSet.complementOf(EnumSet.of(GameAnalysis.Round.R5, GameAnalysis.Round.R6, GameAnalysis.Round.R7, GameAnalysis.Round.TT)),
		BONUS_ROUND = EnumSet.of(GameAnalysis.Round.BR),
		TIEBREAKER_TOSSUP = EnumSet.of(GameAnalysis.Round.TT),
		ALL_ROUNDS_EXCEPT_BR = EnumSet.complementOf(EnumSet.copyOf(BONUS_ROUND)),
		ALL_ROUNDS = EnumSet.allOf(GameAnalysis.Round.class);
	Set<Wedge> ALL_WEDGES = EnumSet.allOf(Wedge.class),
		BIG_MONEY = EnumSet.of(Wedge.Turquoise2500, Wedge.Ruby3500, Wedge.Silver5000),
		CARDBOARD = EnumSet.of(Wedge.WildCard, Wedge.PrizeOnWheel, Wedge.GiftTag, Wedge.MillionDollarWedge, Wedge.Purple1000_1, Wedge.Purple1000_2,
			Wedge.HalfCar_1, Wedge.HalfCar_2, Wedge.HalfCar_3, Wedge.HalfCar_4, Wedge.HalfCar_5, Wedge.HalfCar_6
		),
		WASTE = EnumSet.of(Wedge.MillionDollarWedge),
		MYSTERY = EnumSet.of(Wedge.Purple1000_1, Wedge.Purple1000_2),
		BW = EnumSet.of(Wedge.LOSE_A_TURN, Wedge.BANKRUPT_1, Wedge.BANKRUPT_2);
	Set<Freq> ALL_FREQS = EnumSet.allOf(Freq.class);
	Set<WCUseType> ALL_WC_TYPES = EnumSet.allOf(WCUseType.class);
	Set<DerailReason> ALL_REASONS = EnumSet.allOf(DerailReason.class);
	Set<WinnerRelativeToStarter> ALL_WINNERS_RELATIVE_TO_STARTER = EnumSet.allOf(WinnerRelativeToStarter.class);
	
	default <E extends Enum<E>> Map<String, Set<E>> createGroupingOfEnum(Class<E> enumClass, int binSize) {
		if(binSize <= 1)
			throw new IllegalArgumentException("Bin size must be at least 2");
		if(enumClass == null)
			throw new NullPointerException();
		
		Map<String, Set<E>> groupingMap = new HashMap<>();
		EnumSet.allOf(enumClass).stream().collect(Collectors.groupingBy(e -> binSize * (e.ordinal() / binSize), Collectors.toCollection(TreeSet::new)))
			.forEach((i, ts) -> {
				E e1 = ts.first(), e2 = ts.last();
				groupingMap.put(e1 + (e1 != e2 ? "-" + e2 : ""), EnumSet.copyOf(ts));
			});
		
		return groupingMap;
	}
}

/* end of setting up EnumSets. */

/* Exception class for GameAnalysis. RuntimeExceptions don't need to be thrown explicitly in the method declaration. */

class AnalysisDoneException extends RuntimeException {
	AnalysisDoneException() {
		super("This analysis is marked done, cannot use analyze or set methods anymore");
	}
}