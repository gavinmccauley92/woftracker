package woftracker.stats;

import woftracker.record.*;
import woftracker.util.*;
import java.io.*;
import java.util.*;
import java.util.stream.*;
import java.util.function.*;
import java.math.*;
import java.lang.reflect.*;

import static woftracker.stats.GameAnalysis.*;	//makes using some poorly-laid out enums bearable

public class MultiGameAnalysis implements Serializable, StatEnums {
	private static final long serialVersionUID = 1L;
	
	private TreeSet<GameAnalysis> analyses;
	private Map<Integer, Set<GameAnalysis>> analysesBySeason;
	private int n, mgRoundTotal;
	private StringBuilder report;
	
	private static final String LINE = "\n\n--------------###--------------\n\n";
	
	/* link variable names to their actual contents for AnalysisPanel, same as GameAnalysis */
	
	static final Vector<String> STAT_NAMES = new Vector<>();
	static final Map<String, Field> STAT_MAP = new HashMap<>();
	static {
		for(Field f : MultiGameAnalysis.class.getDeclaredFields()) {
			int modifiers = f.getModifiers();
			if(!Modifier.isPrivate(modifiers) && !Modifier.isStatic(modifiers)) {
				String s = f.getName();
				s = s.substring(s.lastIndexOf('.') + 1);
				STAT_NAMES.add(s);
				STAT_MAP.put(s, f);
			} else if(f.getType() == StringBuilder.class) {
				STAT_NAMES.add(0, "report");
				STAT_MAP.put("report", f);
			}
		}
	}
	
	/* every package private field has an analog (not necessarily the same name) to its GameAnalysis equivalent, in the order it was DECLARED in the GameAnalysis code.
	   there are also new stats! */
	
	BarChartPartition<Wedge, PrizeEvent> prizeResults;
	BarChartPartition<Round, Wedge> spinLocations;
	BarChart<WheelPosition> allSpins;
	BarChart<Round> leftRightMDWHits;
	BarChartPartition<Round, Player> lachTrash;
	BarChartPartition<Wedge, Freq> bigMoneyFreq;
	BarChartPartition<Wedge, Freq> wasteFreq;
	OneVarStats spinStrengthStats, finalSpinStrengthStats;
	BarChart<Wedge> finalSpinLocations;
	BarChart<FreePlayEvent> freePlayActions; 
	WCUses wcUses;
	class WCUses implements Serializable {
		private static final long serialVersionUID = 1L;
		
		BarChart<Wedge> wedges;
		BarChartPartition<WCUseType, Letter> letters;
		BarChartPartition<WCUseType, Freq> freqs;
		short mg, br, buzz;
		
		WCUses() {
			wedges = new BarChart<>(ALL_WEDGES);
			letters = new BarChartPartition<>(ALL_WC_TYPES, ALL_LETTERS);
			freqs = new BarChartPartition<>(ALL_WC_TYPES, ALL_FREQS);
			mg = 0;
			br = 0;
			buzz = 0;
			
			for(GameAnalysis gA : analyses) {
				if(gA.wcUse != null) {
					if(gA.wcUse.l != null) {
						letters.add(gA.wcUse.w != null ? WCUseType.Maingame : WCUseType.BR, gA.wcUse.l);
						freqs.add(gA.wcUse.w != null ? WCUseType.Maingame : WCUseType.BR, Freq.getFreq(gA.wcUse.freq));
					} else
						buzz++;
					
					if(gA.wcUse.w != null) {
						wedges.addY(gA.wcUse.w);
						mg++;
					} else
						br++;
				}
			}
			
			wedges.remove0s();
			letters.remove0s();
			freqs.remove0s();
		}
		
		public String toString() {
			return mg + " maingame uses, " + br + " BR uses:\n\nMaingame wedges:\n" + wedges + "\nLetters:\n" + letters + "\nFreqs:\n" + freqs + "\nBuzzouts: " + buzz;
		}
	}
	MysteryRisks mysteryRisks;
	class MysteryRisks  implements Serializable {
		private static final long serialVersionUID = 1L;
		
		OneVarStats riskedMoneyStats;
		BarChart<Wedge> riskedCardboard;
		BarChart<Freq> costsOfRisk;
		int total, success;
		
		MysteryRisks() {
			Collection<Integer> riskedMonies = new LinkedList<>();
			riskedCardboard = new BarChart<>(CARDBOARD);
			costsOfRisk = new BarChart<>(ALL_FREQS);
			total = 0;
			success = 0;
			
			for(GameAnalysis gA : analyses) {
				if(gA.mysteryRisk != null) {
					riskedMonies.add(gA.mysteryRisk.riskedMoney);
					for(Wedge w : gA.mysteryRisk.riskedCardboard)
						riskedCardboard.addY(w);
					costsOfRisk.addY(Freq.getFreq(gA.mysteryRisk.costOfRisk/1000));
					if(gA.mysteryRisk.wasSuccessful)
						success++;
					total++;
				}
			}
			
			riskedMoneyStats = new OneVarStats(riskedMonies);
			riskedCardboard.remove0s();
			costsOfRisk.remove0s();
		}
		
		public String toString() {
			return success + " of " + total + " risks succeeded:\n\nRisked money stats:\n" + riskedMoneyStats + "\n\nRisked cardboard:\n" + riskedCardboard +
				"\nCosts of risk (multiply frequency of letter by $1000):\n" + costsOfRisk
			;
		}
	}
	ExpressRides expressRides;
	class ExpressRides implements Serializable {
		private static final long serialVersionUID = 1L;
		
		OneVarStats riskedMoneyStats, rideEarningStats;
		SingleQuotient consonantPercentage;
		BarChart<Wedge> riskedCardboard;
		BarChart<DerailReason> derailReasons;
		BarChart<Letter> duds;
		BarChart<IntegerEnum> consonantsLeft, vowelsLeft, vowelsLeftWithZeroC, vowelsCalledWithZeroC;
		List<Integer> optimalRideEarnings;
		int total, success, fullCarRisked;
		
		public ExpressRides() {
			Collection<Integer> riskedMonies = new LinkedList<>(), rideEarnings = new LinkedList<>();
			consonantPercentage = new SingleQuotient();
			riskedCardboard = new BarChart<>(CARDBOARD);
			derailReasons = new BarChart<>(ALL_REASONS);
			duds = new BarChart<>(ALL_LETTERS);
			consonantsLeft = new BarChart<>(IntegerEnum.createRange(0, 52));	//52 letters on puzzleboard
			vowelsLeft = new BarChart<>(IntegerEnum.createRange(0, 5));
			vowelsLeftWithZeroC = new BarChart<>(IntegerEnum.createRange(0, 5));
			vowelsCalledWithZeroC = new BarChart<>(IntegerEnum.createRange(0, 5));
			optimalRideEarnings = new LinkedList<>();
			total = 0;
			fullCarRisked = 0;
			
			for(GameAnalysis gA : analyses)
				Optional.ofNullable(gA.expressRides).ifPresent(eRs -> {
					for(ExpressRide eR : eRs) {
						riskedMonies.add(eR.riskedMoney);
						rideEarnings.add(eR.rideEarnings);
						for(Wedge w : eR.riskedCardboard)
							riskedCardboard.addY(w);
						if(eR.riskedCardboard.stream().filter(w -> Wedge.HALFCARS.contains(w)).count() >= 2L)
							fullCarRisked++;
						
						Set<Letter> rideLetters = eR.ride.keySet();
						for(Letter l : rideLetters)
							consonantPercentage.add(Letter.CONSONANTS.contains(l));
						
						Optional<DerailReason> dR = Optional.ofNullable(eR.derailReason);
						if(dR.isPresent()) {
							derailReasons.addY(dR.get());
							if(dR.get() == DerailReason.Dud)
								duds.addY(new LinkedList<Letter>(rideLetters).getLast());
						} else {
							BarChart<Letter> leftOverDissection = gA.puzzleDissections.getY(Round.R3).subChart(EnumSet.complementOf(EnumSet.copyOf(gA.calledByRound.get(Round.R3))));
							//note the diff in cl vs. vl
							int cl = leftOverDissection.getY(Letter.CONSONANTS), vl = leftOverDissection.subChart(Letter.VOWELS).getXs().size();
							
							if(gA.allConsonantsCalled.contains(Round.R3))
								assert cl == 0;
							if(gA.allVowelsBought.contains(Round.R3))
								assert vl == 0;
							
							consonantsLeft.addY(IntegerEnum.getEnum(cl));
							vowelsLeft.addY(IntegerEnum.getEnum(vl));
							if(cl == 0) {
								vowelsLeftWithZeroC.addY(IntegerEnum.getEnum(vl));
								int i = (int) rideLetters.stream().filter(l -> Letter.VOWELS.contains(l)).count();
								vowelsCalledWithZeroC.addY(IntegerEnum.getEnum(i));
								if(i == 0)
									optimalRideEarnings.add(eR.rideEarnings);
							}
						}
						
						total++;
					}
				});
				
			riskedCardboard.remove0s();
			derailReasons.remove0s();
			duds.remove0s();
			consonantsLeft.remove0s();
			vowelsLeft.remove0s();
			vowelsLeftWithZeroC.remove0s();
			vowelsCalledWithZeroC.remove0s();
			Collections.sort(optimalRideEarnings);
			riskedMoneyStats = new OneVarStats(riskedMonies);
			rideEarningStats = new OneVarStats(rideEarnings);
			success = total - derailReasons.getTotalY();
		}
		
		public String toString() {
			if(total == 0)
				return "No Express rides in any of these episodes.";
			
			return success + " of " + total + " rides succeeded:\n\nRisked money stats:\n" + riskedMoneyStats +
			"\n\nRisked cardboard*:\n" + riskedCardboard + "*Obviously, single 1/2 Car tags are uniquely made valueless when risked on Express. However, " +
			fullCarRisked + " full car(s) (2+ tags above) were risked on Express.\n" +
			"\nRide earning stats:\n" + rideEarningStats + "\n\nDerail reasons:\n" + derailReasons + "\nDud distribution:\n" + duds +
			"\n\nNumber of consonants in the puzzle left uncalled on successful rides (a measure of inefficiency):\n" + consonantsLeft +
			"\nNumber of unique, non-dud vowels NOT called IN THE WHOLE ROUND with a successful ride\n" +
				"(i.e. for \"1\", one of A/E/I/O/U was present any number of times in the puzzle, but was not called in the ENTIRE round):\n" + vowelsLeft +
			"\nSubchart of the immediately preceding chart when ALL consonants were filled in the puzzle\n(for 0, that many puzzles were thus fully filled in due to an Express Ride):\n" + 		
			vowelsLeftWithZeroC + "\nNumber of vowels that WERE called DURING successful rides when ALL consonants were filled in the puzzle\n(not just 5 minus the chart above, " +
			"since any number of vowels could have been bought before the successful ride):\n" + vowelsCalledWithZeroC + "Therefore, " + vowelsCalledWithZeroC.getY(IntegerEnum.getEnum(0)) +
			" rides were \"optimal\" for ride earnings: all remaining consonants filled in, nothing spent for vowels. These individual ride earnings:\n" +
			FormatFactory.formatCollection(optimalRideEarnings) + "\n";
		}
	}
	int timesNotRisked, timesNotRidden, savedTurns;
	
	BarChartPartition<Player, Letter> calledByPlayer;
	BarChartPartition<Round, Letter> calledByRound, repeatedLetters, calledFreqs, firstLetters;
	BarChartPartitionQuotient<Letter, Freq> mgEfficiencyByLetter, brEfficiencyByLetter;	//quotient: calledFreqs over puzzle dissections
	BarChartPartitionQuotient<Freq, Letter> mgEfficiencyByFreq, brEfficiencyByFreq;	////quotient: calledFreqs over puzzle dissections, Freq.ZERO covers calledDuds / allDuds
	
	BarChart<Player> mainGameWinnings;	//might care to split this by round later
	BarChart<Round> winnings, missolves, lostTurns;
	Map<Round, OneVarStats> mainGameWinningsStats, lostTurnStats;
	BarChartPartition<Round, Letter> puzzleDissections, highestMultLetters;
	BarChart<Letter> goodBRCalls;
	OneVarStats brLetterCountStats;	// # of non-RSTLNE letters that are not duds
	BarChartPartition<Round, Player> lostBanks;
	BarChartPartition<Round, Freq> highestMultFreqs;
	BarChart<Round> perfectRounds, allVowelsBought, allConsonantsCalled, fullyFilledIn;
	BarChartPartition<Round, Player> roundWinners;
	BarChartPartition<Round, WinnerRelativeToStarter> winnersRelativeToStarter;
	
	//EXPRESS Kevin Fox (10:14:20 PM): # of early solves, average letters away of early solves
	
	BarChart<Round> mainBuzzes, tossUpBlanks;
	
	Map<Round, OneVarStats> blankStats, filledPercentStats, vowelStats, consonantStats, vowelPercentStats, letterStats, lineStats;
	
	BarChartPartition<Round, Category> roundCat;
	BarChartPartition<Category, Round> catRound;
	OneVarStats winnerStats, secondStats, thirdStats, mainGameStats;
	BarChart<Player> winner, second, third;
	BarChartQuotient<IntegerEnum> roundsPerWinner, mgRoundsPerWinner, tosRoundsPerWinner;
	BarChart<Round> pp;
	BarChartQuotient<Round> roundsWonByWinner;	//quotient: BR winning pct
	OneVarStats pWheelStats, ppStats, winningMarginStats, secondaryMarginStats, overMinimumStats;
	SingleQuotient ppWinnerTimes, ppMatteredTimes, tossUpSweepWinners;	//quotient: BR winning pct
	int gooseEggs, tossUpSweeps;
	
	OneVarStats jackpotStats, jackpotsWonStats;
	
	BarChart<Category> triviaCat;
	int triviaWonTimes;
	
	BarChartPartitionQuotient<BonusAmount, BonusWedge> bonusPairsByAmount;	//quotient: winning pct
	BarChartPartitionQuotient<BonusWedge, BonusAmount> bonusPairsByWedge;	//quotient: winning pct
	
	int females, males;
	
	BarChart<Round> roundCountTable;
	Map<GameAnalysis, Boolean> brTruthTable;
	
	/* */
	/* end field declarations. */
	
	private static IntUnaryOperator reportSize = n -> 50000 + 250*n;	//StringBuilder size as a function of number of analyses n.
	
	/* the entire defining, analyzing and reporting is done in the constructor and helper methods - all input is already in analyses. */
	MultiGameAnalysis(TreeSet<GameAnalysis> analyses) throws NoSuchFieldException, IllegalAccessException {
		if(analyses == null || (n = analyses.size()) <= 1)
			throw new IllegalArgumentException("MultiGame means a non-null set of more than one game, ya doof.");
		this.analyses = analyses;
		report = new StringBuilder(reportSize.applyAsInt(n));
		
		GameAnalysis firstShow = analyses.first(), lastShow = analyses.last();
		TreeSet<GameAnalysis> endOfSpanShows = analyses.stream().limit(Long.valueOf(n-1L)).filter(gA -> analyses.higher(gA).getShowNumber() != gA.getShowNumber() + 1
			|| gA.getSeasonNumber() == 195).collect(Collectors.toCollection(TreeSet::new));
		endOfSpanShows.add(lastShow);
		
		report.append("Multi-analysis of " + n + " shows, consisting of:\n");
		for(GameAnalysis endShow : endOfSpanShows) {
			int season = endShow.getSeason(), fSeason = firstShow.getSeasonNumber(), eSeason = endShow.getSeasonNumber();
			report.append("-" + firstShow.showDateString());
			if(firstShow != endShow)
				report.append(" to " + endShow.showDateString() + " (" + firstShow.getShowNumber() + "-" + endShow.getShowNumber() + ")");
			report.append(" (" + (fSeason == 1 && eSeason == 195 ? "all of S" + season : "S" + season + " E" +
				(firstShow != endShow ? String.format("%03d-%03d", fSeason, eSeason) : String.format("%03d", fSeason)) ) + ")\n");
			
			firstShow = analyses.higher(endShow);
		}
		
		analysesBySeason = analyses.stream().collect(Collectors.groupingBy(gA -> gA.getSeason(), Collectors.toSet()));
		setCA(OptionalInt.empty());
		
		/* below is stats that need unique processing. */
		
		//initialization of variables.
		mgEfficiencyByLetter = new BarChartPartitionQuotient<>(ALL_LETTERS, ALL_FREQS, true);
		brEfficiencyByLetter = new BarChartPartitionQuotient<>(ALL_LETTERS, ALL_FREQS, true);
		bigMoneyFreq = new BarChartPartition<>(BIG_MONEY, ALL_FREQS);
		wasteFreq = new BarChartPartition<>(CARDBOARD, ALL_FREQS);
		finalSpinLocations = new BarChart<>(ALL_WEDGES);
		calledByRound = new BarChartPartition<>(ALL_ROUNDS_EXCEPT_TOSS, ALL_LETTERS);
		repeatedLetters = new BarChartPartition<>(MAINGAME, ALL_LETTERS);
		firstLetters = new BarChartPartition<>(ALL_ROUNDS_EXCEPT_TOSS, ALL_LETTERS);
		mainGameWinningsStats = new EnumMap<>(Round.class);
		lostTurnStats = new EnumMap<>(Round.class);
		blankStats = new EnumMap<>(Round.class);
		filledPercentStats = new EnumMap<>(Round.class);
		vowelStats = new EnumMap<>(Round.class);
		consonantStats = new EnumMap<>(Round.class);
		vowelPercentStats = new EnumMap<>(Round.class);
		letterStats = new EnumMap<>(Round.class);
		lineStats = new EnumMap<>(Round.class);
		goodBRCalls = new BarChart<>(BonusPuzzle.NOT_GIVEN);
		highestMultLetters = new BarChartPartition<>(MAINGAME, ALL_LETTERS);
		highestMultFreqs = new BarChartPartition<>(MAINGAME, ALL_FREQS);
		fullyFilledIn = new BarChart<>(MAINGAME);
		winnersRelativeToStarter = new BarChartPartition<>(MAINGAME, ALL_WINNERS_RELATIVE_TO_STARTER);
		triviaCat = new BarChart<>(Category.TRIVIA);
		triviaWonTimes = 0;
		roundsPerWinner = new BarChartQuotient<>(IntegerEnum.createRange(1, 9), true);
		mgRoundsPerWinner = new BarChartQuotient<>(IntegerEnum.createRange(0, 6), true);
		tosRoundsPerWinner = new BarChartQuotient<>(IntegerEnum.createRange(0, 3), true);
		ppWinnerTimes = new SingleQuotient();
		ppMatteredTimes = new SingleQuotient();
		tossUpSweepWinners = new SingleQuotient();
		roundsWonByWinner = new BarChartQuotient<>(ALL_ROUNDS_EXCEPT_BR, true);
		bonusPairsByAmount = new BarChartPartitionQuotient<>(ALL_BONUS_AMOUNTS, ALL_BONUS_WEDGES, true);
		roundCountTable = new BarChart<>(ALL_ROUNDS);
			for(Round r : GUARANTEED_ROUNDS)
				roundCountTable.addY(r, n);
		brTruthTable = new HashMap<>();
		
		//intermediate variables - to be processed below.
		Collection<OneVarStats> spinStrengths = new LinkedList<>();
		Collection<Number> winnerTotals = new LinkedList<>(), secondTotals = new LinkedList<>(), thirdTotals = new LinkedList<>(), mainGameTotals = new LinkedList<>(), winningJackpots = new LinkedList<>(),
			brLetterCounts = new LinkedList<>();
		Map<Round, Collection<Number>> winningsByRound = new EnumMap<>(Round.class), lostTurnsByRound = new EnumMap<>(Round.class),
			blanksByRound = new EnumMap<>(Round.class), filledPercentByRound = new EnumMap<>(Round.class), vowelsByRound = new EnumMap<>(Round.class),
			consonantsByRound = new EnumMap<>(Round.class),
			vowelPercentByRound = new EnumMap<>(Round.class), lettersByRound = new EnumMap<>(Round.class), linesByRound = new EnumMap<>(Round.class);
		for(Round r : MAINGAME) {
			winningsByRound.put(r, new LinkedList<Number>());
			lostTurnsByRound.put(r, new LinkedList<Number>());
		}
		for(Round r : ALL_ROUNDS) {
			blanksByRound.put(r, new LinkedList<Number>());
			filledPercentByRound.put(r, new LinkedList<Number>());
			vowelsByRound.put(r, new LinkedList<Number>());
			consonantsByRound.put(r, new LinkedList<Number>());
			vowelPercentByRound.put(r, new LinkedList<Number>());
			lettersByRound.put(r, new LinkedList<Number>());
			linesByRound.put(r, new LinkedList<Number>());
		}
		mgRoundTotal = 0;
		
		//processing.
		for(GameAnalysis gA : analyses) {
			int mgForThisGA = gA.getNumberOfMGRounds();
			mgRoundTotal += mgForThisGA;	//in retrospect, perhaps an enum denoting "got only to R4, got to R5, got to R6" would have been useful.
			for(int i = mgForThisGA; i > 4; i--)
				roundCountTable.addY(Enum.valueOf(Round.class, "R" + i));
			
			brTruthTable.put(gA, gA.roundWinners.get(Round.BR) != null);
			
			for(Map.Entry<Wedge, List<Byte>> bmEntry : gA.bigMoneyFreq.entrySet()) {
				Wedge w = bmEntry.getKey();
				for(Byte b : bmEntry.getValue())
					bigMoneyFreq.add(w, Freq.getFreq(b));
			}
			
			for(Map.Entry<Wedge, Byte> wasteEntry : gA.wasteFreq.entrySet())
				wasteFreq.add(wasteEntry.getKey(), Freq.getFreq(wasteEntry.getValue()));
			
			gA.spinStrengthStats.entrySet().stream().map(e -> e.getValue().withOT(OutlierTechnique.MILD)).forEach(ovs -> spinStrengths.add(ovs));
			finalSpinLocations.addY(gA.finalSpinLocation);
			
			for(Map.Entry<Round, List<Letter>> letterEntry : gA.calledByRound.entrySet()) {
				Round r = letterEntry.getKey();
				List<Letter> letters = letterEntry.getValue();
				
				for(Letter l : letters)
					calledByRound.add(r, l);
				
				if(!letters.isEmpty())	//will an immediate solve ever happen? I think not, but still the check
					firstLetters.add(r, letters.get(0));
			}
			
			for(Map.Entry<Round, List<Letter>> letterEntry : gA.repeatedLetters.entrySet()) {
				Round r = letterEntry.getKey();
				List<Letter> letters = letterEntry.getValue();
				
				for(Letter l : letters)
					repeatedLetters.add(r, l);
			}
			
			for(DataPoint<Round, BarChart<Letter>> pddp : gA.puzzleDissections) {
				Round r = pddp.x;
				if(!TOSSUPS.contains(r)) {
					BarChartPartitionQuotient<Letter, Freq> thisEfficiencyByLetter = pddp.x != Round.BR ? mgEfficiencyByLetter : brEfficiencyByLetter;
					
					BarChart<Letter> pD = pddp.y;
					Set<Letter> nonDuds = pD.getXs();
					
					for(Letter l : Letter.values())
						thisEfficiencyByLetter.add(l, nonDuds.contains(l) ? Freq.getFreq(pD.getY(l)) : Freq.ZERO, gA.calledFreqs.contains(r, l));
				}
			}
			
			for(Map.Entry<Round, GameAnalysis.PuzzleInfo> pIEntry : gA.puzzleDetails.entrySet()) {
				Round r = pIEntry.getKey();
				GameAnalysis.PuzzleInfo pI = pIEntry.getValue();
				blanksByRound.get(r).add(pI.blanks);
				filledPercentByRound.get(r).add(pI.filledPercent);
				vowelsByRound.get(r).add(pI.vowels);
				consonantsByRound.get(r).add(pI.total - pI.vowels);
				vowelPercentByRound.get(r).add(pI.vowelPercent);
				lettersByRound.get(r).add(pI.total);
				linesByRound.get(r).add(pI.lines);
			}
			
			Set<Letter> thisGoodBRCalls = gA.puzzleDissections.getY(Round.BR).getXs();
			thisGoodBRCalls.removeAll(BonusPuzzle.GIVEN);
			for(Letter l : thisGoodBRCalls)
				goodBRCalls.addY(l);
			brLetterCounts.add(thisGoodBRCalls.size());
			
			
			byte mgWinnerSubTotal = 0, tosWinnerSubTotal = 0;
			Player starter = Optional.ofNullable(gA.roundWinners.get(Round.T2)).orElse(Player.RED);
			
			for(Round r : MAINGAME) {			
				if(gA.roundWinners.containsKey(r)) {
					int lostTurns = gA.lostTurns.getY(r);
					winningsByRound.get(r).add(gA.winnings.getY(r));
					lostTurnsByRound.get(r).add(lostTurns);
					
					if(r == Round.R4)
						starter = Optional.ofNullable(gA.roundWinners.get(Round.T3)).orElse(Player.RED);	//set
					winnersRelativeToStarter.add(r, WinnerRelativeToStarter.get(starter, gA.roundWinners.get(r)));
					starter = Player.passTurn(starter);	//update
					
					for(Letter l : gA.highestMultLetters.get(r).multiples)
						highestMultLetters.add(r, l);
					highestMultFreqs.add(r, Freq.getFreq(gA.highestMultLetters.get(r).max));
					
					if(gA.roundWinners.get(r) == gA.winner) {
						mgWinnerSubTotal++;
						roundsWonByWinner.addY(r, brTruthTable.get(gA));
					}
				}
			}
			for(Round r : TOSSUPS)
				if(gA.roundWinners.get(r) == gA.winner) {
					tosWinnerSubTotal++;
					roundsWonByWinner.addY(r, brTruthTable.get(gA));
				}
			
			mgRoundsPerWinner.addY(IntegerEnum.getEnum(mgWinnerSubTotal), brTruthTable.get(gA));
			tosRoundsPerWinner.addY(IntegerEnum.getEnum(tosWinnerSubTotal), brTruthTable.get(gA));
			roundsPerWinner.addY(IntegerEnum.getEnum(mgWinnerSubTotal + tosWinnerSubTotal), brTruthTable.get(gA));
			
			Set<Round> thisFullyFilledIn = new HashSet<>(gA.allVowelsBought);
			thisFullyFilledIn.retainAll(gA.allConsonantsCalled);
			for(Round r : thisFullyFilledIn)
				fullyFilledIn.addY(r);
			
			winnerTotals.add(gA.finalMainWinnings.getY(gA.winner));
			secondTotals.add(gA.finalMainWinnings.getY(gA.second));
			thirdTotals.add(gA.finalMainWinnings.getY(gA.third));
			mainGameTotals.add(gA.finalMainWinnings.getTotalY());
			
			if(gA.ppIsWinner) {
				ppWinnerTimes.add(brTruthTable.get(gA));
				if(gA.didPPMatter)
					ppMatteredTimes.add(brTruthTable.get(gA));
			}
			
			if(gA.tossUpSweep && gA.winner == gA.roundWinners.get(Round.T1))
					tossUpSweepWinners.add(brTruthTable.get(gA));
			
			if(gA.wasJackpotWon)
				winningJackpots.add(gA.jackpotAmount);
				
			if(gA.triviaBonus != null) {
				triviaCat.addY(gA.triviaBonus.triviaCat);
				if(gA.triviaBonus.wasTriviaRight)
					triviaWonTimes++;
			}
			
			bonusPairsByAmount.add(gA.bonusAmount, gA.bonusWedge, brTruthTable.get(gA));
		}
		
		//combine intermediate variables.
		spinStrengthStats = OneVarStats.combine(spinStrengths, OutlierTechnique.NONE);
		for(Round r : MAINGAME) {
			try {
				mainGameWinningsStats.put(r, new OneVarStats(winningsByRound.get(r)));
				lostTurnStats.put(r, new OneVarStats(lostTurnsByRound.get(r)));
			} catch(IllegalArgumentException e) {
				continue;	//the constructor error - for n = 0 data - works well here
			}
		}
		for(Round r : ALL_ROUNDS) {
			try {
				blankStats.put(r, new OneVarStats(blanksByRound.get(r)));
				filledPercentStats.put(r, new OneVarStats(filledPercentByRound.get(r)));
				vowelStats.put(r, new OneVarStats(vowelsByRound.get(r)));
				consonantStats.put(r, new OneVarStats(consonantsByRound.get(r)));
				vowelPercentStats.put(r, new OneVarStats(vowelPercentByRound.get(r)));
				letterStats.put(r, new OneVarStats(lettersByRound.get(r)));
				lineStats.put(r, new OneVarStats(linesByRound.get(r)));
			} catch(IllegalArgumentException e) {
				continue;	//the constructor error - for n = 0 data - works well here
			}
		}
		winnerStats = new OneVarStats(winnerTotals);
		secondStats = new OneVarStats(secondTotals);
		thirdStats = new OneVarStats(thirdTotals);
		mainGameStats = new OneVarStats(mainGameTotals);
		jackpotsWonStats = new OneVarStats(winningJackpots);
		brLetterCountStats = new OneVarStats(brLetterCounts);
		
		//now manual clean-up...
		mgEfficiencyByLetter.remove0s();
		brEfficiencyByLetter.remove0s();
		bigMoneyFreq.remove0s();
		wasteFreq.remove0s();
		finalSpinLocations.remove0s();
		calledByRound.remove0s();
		repeatedLetters.remove0s();
		winnersRelativeToStarter.remove0s();
		firstLetters.remove0s();
		highestMultLetters.remove0s();
		highestMultFreqs.remove0s();
		fullyFilledIn.remove0s();
		triviaCat.remove0s();
		bonusPairsByAmount.remove0s();
		
		//and now all the reporting.
		Category.setBoth(true);
		
		reportStat("CRD", "What happened to the cardboard:", (prizeResults = combineMap("prizeResults", true)) + "\nSubset for 1/2 Cars only:\n" + prizeResults.getY(Wedge.HALFCARS));
		
		List<BarChartPartition<Round, Wedge>> seasonSpinLocations = new LinkedList<>();
		List<BarChart<WheelPosition>> seasonWheelPositions = new LinkedList<>();
		IntStream.rangeClosed(29, 32).forEach(i -> {
			setCA(OptionalInt.of(i));
			BarChartPartition<Round, Wedge> ssl = null;
			try { ssl = combineBCP("spinLocations"); } catch(Exception e) { System.err.println("spinLocations failed\n"); e.printStackTrace(); return; }
			seasonSpinLocations.add(ssl);
			seasonWheelPositions.add(WheelPosition.transformChart(ssl.synthesizeTotalY(), i));
		});
		reportStat("SPN", "All spins", (spinLocations = BarChartPartition.combine(seasonSpinLocations)).average(roundCountTable.subChart(MAINGAME), 2, Wedge.GROUPING_MAP) + "\nTotal:\n\n" +
			(allSpins = BarChart.combine(seasonWheelPositions)).average(n, 2) + "One-var stats on each 1/24th:\n" + new OneVarStats(allSpins.getYs())
		);
		setCA(OptionalInt.empty());	//reset to all analyses.
		
		reportStat("MIL", "All left/right MDW hits:", (leftRightMDWHits = combineBC("leftRightMDWHits", true)));
		reportStat("BKT", "All bankrupt trash by player:", (lachTrash = combineBCP("lachTrash")) + "\n" + lachTrash.synthesizeTotalY());
		reportStat("FPL", (savedTurns = combineNumber("savedTurns")) + " of these FP actions saved turns:", (freePlayActions = combineBC("freePlayActions", false)));
		
		reportStat("CL1", "All players' called letters (INCLUDING REPEATS) and average per maingame", (calledByPlayer = combineBCP("calledByPlayer")).synthesizeTotalY().average(n, 2));
		reportStat("CL2", "The above split up, OMITTING REPEATS, by maingame round", calledByRound.toPartialString(MAINGAME) +
			"\n\nAgain, the total of the above is the same chart as the previous stat, BUT WITHOUT REPEATS, so no total chart is given here."
		);
		reportStat("CLF", "All players' called letters' frequencies and average per maingame", (calledFreqs = combineBCP("calledFreqs")).synthesizeY(MAINGAME).average(n, 2));
		reportStat("FIL", "Total first letters called in maingame + BR", firstLetters);
		reportStat("RPT", "Total repeated letters", repeatedLetters + "Total:\n" + repeatedLetters.synthesizeTotalY());
		reportStat("PUD", "Total puzzle dissections", (puzzleDissections = combineBCP("puzzleDissections")));
		reportStat("MEF", "Maingame efficiency chart by letter: called freqs over puzzle dissections in " + mgRoundTotal + " R1-6 puzzles",
			"Cross-check this data with waste stats for a true effiency image.\n\n" + mgEfficiencyByLetter
		);
		reportStat("BNR", "BR analysis", "Number of BRs a letter is called in:\n" + calledByRound.getY(Round.BR) + "\nExpansion of above by freqs over dissections (note RSTLNE have no data on freqs, " + 
		"but are included for dissections):\n\n" + brEfficiencyByLetter + "\n\nNumber of BRs a non-RSTLNE letter was actually in:\n" + goodBRCalls.average(n, 2) +
			"\n\nStats of above as # of good letters per BR:\n" + brLetterCountStats + "\n\n" + brLetterCountStats.intBarChart()
		);
		reportStat("EFF", "Efficiency charts above reversed (by freq) for " + mgRoundTotal + " MAINGAME rounds/puzzles",
			"R1-6:\n" +(mgEfficiencyByFreq = new BarChartPartitionQuotient<>(BarChartPartition.reverse(mgEfficiencyByLetter.getNum()), BarChartPartition.reverse(mgEfficiencyByLetter.getDen()), true)) +
			"\nBR:\n" +(brEfficiencyByFreq = new BarChartPartitionQuotient<>(BarChartPartition.reverse(brEfficiencyByLetter.getNum()), BarChartPartition.reverse(brEfficiencyByLetter.getDen()), true))
		);
		//I need to also check 1000->2000 on 6-person shows I think!
		reportStat("TEW", "Total earned winnings", (winnings = combineBC("winnings", false)).average(n, 0) + "\nGoose-eggs: " + (gooseEggs = combineNumber("gooseEggs")) +
			"\n\nTotal maingame winnings (including goosings):\n" + (mainGameWinnings = combineBC("finalMainWinnings", false)) +
			"\nOverall grand total (total line above + BR): " + (mainGameWinnings.getTotalY() + winnings.getY(Round.BR))
		);
		reportStat("MWS", "Maingame winnings stats", FormatFactory.formatMap(mainGameWinningsStats));
		reportStat("MSV", "Total missolves", (missolves = combineBC("missolves", false)));
		reportStat("TLT", "Total lost turns", (lostTurns = combineBC("lostTurns", false)));
		reportStat("LTS", "Lost turn stats", FormatFactory.formatMap(lostTurnStats));
		
		reportStat("TLB", "Total lost banks", (lostBanks = combineBCP("lostBanks")));
		reportStat("HM1", "Total highest multiples", "Letters (Doubles or more):\n" + highestMultLetters + "\nFreqs (Dud = fully filled puzzle, Single = only single(s) left, no highest multiple recorded above):\n" + highestMultFreqs);
		reportStat("HM2", "Combined letter/freq totals of previous section", "Letters:\n" + highestMultLetters.synthesizeTotalY() + "\nFreqs:\n" + highestMultFreqs.synthesizeTotalY());
		reportStat("ALC", "Total perfect rounds, all vowels bought, all consonants called, fully filled in", "Perfect rounds:\n" + (perfectRounds = combineSet("perfectRounds", true)) +
			"\nAll vowels bought:\n" + (allVowelsBought = combineSet("allVowelsBought", true)) + "\nAll consonants called:\n" + (allConsonantsCalled = combineSet("allConsonantsCalled", true)) +
			"\nFully filled in (union of above two) (should match total highest multiple freqs' dud entry):\n" + fullyFilledIn
		);
		reportStat("TRW", "Total round winners", (roundWinners = combineMap("roundWinners", true)));
		reportStat("REL", "Total winners relative to the starter of each round", winnersRelativeToStarter + "Total:\n" + winnersRelativeToStarter.synthesizeTotalY());
		
		reportStat("BMF", "Total big money frequencies", bigMoneyFreq);
		BarChart<Freq> wasteTotalFreq;	//see next line
		reportStat("WFR", "Total waste frequencies on WC, GT, Prize, MDW (MDW only in S30+)", wasteFreq + "\nTotal:\n" + (wasteTotalFreq = wasteFreq.synthesizeTotalY()) +
			"Average: " + weightedAverageFromFreq(wasteTotalFreq)	//I hate wasting time!
		);
		reportStat("SSS", "Total spin strength stats for " + 3*n + " players/teams (with individual mild outliers removed)", spinStrengthStats +
			"\n\nFull distribution by 5s:\n" + spinStrengthStats.intBarChart().toGroupedString(createGroupingOfEnum(IntegerEnum.class, 5)));
		reportStat("FSL", "Total Pat final spin locations", finalSpinLocations);
		reportStat("FSS", "Pat final spin strength stats (with extreme outliers removed):", (finalSpinStrengthStats = oneVarStatsFromNumber("finalSpinStrength").withOT(OutlierTechnique.EXTREME)) +
			"\n\nFull distribution by 5s:\n" + finalSpinStrengthStats.intBarChart().toGroupedString(createGroupingOfEnum(IntegerEnum.class, 5)));
		reportStat("WCU", "Total WC uses:", (wcUses = new WCUses()));
		reportStat("MYS", "Total Mystery risks:", "After " + (timesNotRisked = combineNumber("timesNotRisked")) + " times contestants played it safe, " + (mysteryRisks = new MysteryRisks()));
		if(containsAnyOfSeasons(i -> i >= 31))
			reportStat("EXP", "Express info:", "After " + (timesNotRidden = combineNumber("timesNotRidden")) + " times contestants played it safe:\n" + (expressRides = new ExpressRides()));
		
		reportStat("BUZ", "Total \"taking too long\" times", "Maingame buzzes (excludes natural speed-up 3 seconds)\n" + (mainBuzzes = combineBC("mainBuzzes", true)) + "\nToss-up \"blanks\"\n" +
			(tossUpBlanks = combineBC("tossUpBlanks", true)) + "\nWC buzzouts: " + wcUses.buzz
		);
		
		reportStat("PUI", "Total puzzleInfo stats", "Blanks (number of letters unfilled, 0 for toss-ups = triple stumper):\n" + FormatFactory.formatMap(blankStats) +
			"\n\nFilled percent:\n" + FormatFactory.formatMap(filledPercentStats) + "\n\nVowels:\n" + FormatFactory.formatMap(vowelStats) +
			"\n\nConsonants:\n" + FormatFactory.formatMap(consonantStats) +
			"\n\nVowel percent:\n" + FormatFactory.formatMap(vowelPercentStats) + "\n\nTotal:\n" + FormatFactory.formatMap(letterStats) +
			"\n\nLines:\n" + new BarChartPartition<Round, IntegerEnum>(lineStats.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().intBarChart())), Round.class)
		);

		reportStat("CAT", "Total categories", "By round:\n\n" + (roundCat = combineMap("roundCat", true)) + "\nBy category:\n\n" + (catRound = BarChartPartition.reverse(roundCat)));
		reportStat("PLA", "First, second, third stats", "Winners:\n" + (winner = barChartFromEnum("winner", true)) + "  " + winnerStats + "\n\nSecond:\n" + (second = barChartFromEnum("second", true)) +
			"  " + secondStats + "\n\nThird:\n" + (third = barChartFromEnum("third", true)) + "  " + thirdStats + "\n\nOverall maingame: " + mainGameStats
		);
		reportStat("MAR", "Margin stats", "Winning margin:\n" + (winningMarginStats = oneVarStatsFromNumber("winningMargin")) + "\n\nSecondary margin:\n" +
			(secondaryMarginStats = oneVarStatsFromNumber("secondaryMargin")) + "\n\nOver minimum (every round minimum + PP):\n" + (overMinimumStats = oneVarStatsFromNumber("amountOverMinimum"))
		);
		
		reportStat("MC1", "# of rounds solved by winners in denominator, with BR winners in numerator:", "Main rounds:\n" + mgRoundsPerWinner + "Average: " + weightedAverage(mgRoundsPerWinner.getNum()) +
			" / " + weightedAverage(mgRoundsPerWinner.getDen()) + " main rounds / winner\n\nTossups:\n" + tosRoundsPerWinner + "Average: " + weightedAverage(tosRoundsPerWinner.getNum()) + " / " +
			weightedAverage(tosRoundsPerWinner.getDen()) + " tossups / winner\n\nTotal:\n" + roundsPerWinner + "Average: " + weightedAverage(roundsPerWinner.getNum()) + " / " + 
			weightedAverage(roundsPerWinner.getDen()) + " puzzles solved / winner"
		);
		
		reportStat("MC2", "Number of times maingame winner won each round:", BarChart.quotient(roundsWonByWinner.getDen(), roundCountTable.subChart(ALL_ROUNDS_EXCEPT_BR), true) +
			"Now how BR winners correlate:\n\n" + roundsWonByWinner + "\nNote how for total rounds in immediately preceding, that total line (BR winning pct) times that average line equals this total line."
		);
		
		reportStat("PRZ", "Trip / big GC stats", "Prize on Wheel:\n" + (pWheelStats = oneVarStatsFromNumber("pWheelAmount")) + "\n\nPrize Puzzle:\n" + (ppStats = oneVarStatsFromNumber("ppAmount")));
		reportStat("BRC", "Single BR / winner correlations", "Number of times out of " + n + " PP winner won BR / won maingame: " + ppWinnerTimes +
			"\n\tand of those times, PP mattered (BR/all again): " + ppMatteredTimes +
			"\n\nNumber of toss up sweepers: " + (tossUpSweeps = combineBoolean("tossUpSweep")) + "\n\tand of those times, the sweeper won BR / won game: " + tossUpSweepWinners
		);
		reportStat("JKP", "Jackpot stats", (jackpotStats = oneVarStatsFromNumber("jackpotAmount")) + "\n\nSubset for Jackpots won:\n\n" + jackpotsWonStats);
		if(containsAnyOfSeasons(i -> i == 29))
			reportStat("TRI", "Trivia bonus stats", "Categories:\n" + triviaCat + "\nConversion rate: " + triviaWonTimes + "/" + triviaCat.getTotalY());
		
		reportStat("BRA", "Bonus \"pairs\" by amount", bonusPairsByAmount);
		reportStat("BRW", "Bonus \"pairs\" by wedge", (bonusPairsByWedge = new BarChartPartitionQuotient<>(BarChartPartition.reverse(bonusPairsByAmount.getNum()),
			BarChartPartition.reverse(bonusPairsByAmount.getDen()), true))
		);
		
		BarChartQuotient<BonusAmount> bcqa = bonusPairsByWedge.synthesizedQuotient();	BarChartQuotient<BonusWedge> bcqw = bonusPairsByAmount.synthesizedQuotient();
		reportStat("BRT", "Summary of above (\"TOTAL\" lines into BarCharts):", bcqw + "\n\n" + bcqw.toGroupedString(BonusWedge.GROUPING_MAP) + "\n\n" +
			bcqa + "\n\n" + bcqa.toGroupedString(BonusAmount.GROUPING_MAP)
		);
		
		reportStat("GEN", "Gender totals:", "Females: " + (females = combineNumber("females")) + "\nMales: " + (males = combineNumber("males")) + "\nRatio: " + 
			new BigDecimal((double) females / males).setScale(2, RoundingMode.HALF_UP) + " females per male"
		);
		
		reportStat("DBG", "Total length before this last part compared to reportSize function (see just before constructor):", report.length() + " | " + reportSize.applyAsInt(n) + "\n\n");
		
		FormatFactory.fixStringBuilder(report, "null", "none");
		
		Category.setBoth(false);	//clean.
	}
	
	/* helper methods: combining like types; some math with number-oriented enum BarCharts, OneVarStats to BarChart. */
	
	private Collection<GameAnalysis> combinedAnalyses;
	private void setCA(OptionalInt oi) {
		combinedAnalyses = oi.isPresent() ? Optional.ofNullable(analysesBySeason.get(oi.getAsInt())).orElse(new TreeSet<GameAnalysis>()) : analyses;
	}
	
	@SuppressWarnings("unchecked")
	private <Y extends Enum<Y>> BarChart<Y> combineBC(String gaName, boolean remove0s) throws NoSuchFieldException, IllegalAccessException {
		Collection<BarChart<Y>> bCs = new LinkedList<>();
		Field source = GameAnalysis.class.getDeclaredField(gaName);
		
		for(GameAnalysis gA : combinedAnalyses)
			bCs.add((BarChart<Y>) source.get(gA));
		
		BarChart<Y> combination = BarChart.combine(bCs);
		if(remove0s)
			try {
				combination.remove0s();
			} catch(NullPointerException e) {
				System.out.println(e + "\n\n" + combination.getClass() + "\n\n" + combination.getXs());
				System.exit(1);
			}
		
		return combination;
	}
	
	@SuppressWarnings("unchecked")
	private <X extends Enum<X>, Y extends Enum<Y>> BarChartPartition<X,Y> combineBCP(String gaName) throws NoSuchFieldException, IllegalAccessException {
		Collection<BarChartPartition<X,Y>> bCPs = new LinkedList<>();
		Field source = GameAnalysis.class.getDeclaredField(gaName);
		
		for(GameAnalysis gA : combinedAnalyses)
			bCPs.add((BarChartPartition<X,Y>) source.get(gA));
		
		BarChartPartition<X,Y> combination = BarChartPartition.combine(bCPs);
		
		return combination;
	}
	
	@SuppressWarnings("unchecked")
	private <Y extends Enum<Y>> BarChart<Y> combineSet(String gaName, boolean remove0s) throws NoSuchFieldException, IllegalAccessException {
		BarChart<Y> combination = null;
		Field source = GameAnalysis.class.getDeclaredField(gaName);
		
		for(GameAnalysis gA : combinedAnalyses)
			for(Y y : ((Set<Y>) source.get(gA))) {
				if(combination == null)
					combination = new BarChart<>(EnumSet.allOf(y.getClass()));
				
				combination.addY(y);
			}
		
		if(remove0s && combination != null)
			combination.remove0s();
		
		return combination;
	}
	
	@SuppressWarnings("unchecked")
	private <X extends Enum<X>, Y extends Enum<Y>> BarChartPartition<X,Y> combineMap(String gaName, boolean remove0s) throws NoSuchFieldException, IllegalAccessException {
		BarChartPartition<X,Y> combination = null;
		Field source = GameAnalysis.class.getDeclaredField(gaName);
		
		for(GameAnalysis gA : combinedAnalyses)
			for(Map.Entry<X, Y> entry : ((Map<X,Y>) source.get(gA)).entrySet()) {
				if(combination == null)
					combination = new BarChartPartition<>(EnumSet.allOf(entry.getKey().getClass()), EnumSet.allOf(entry.getValue().getClass()));
				
				try {
					combination.add(entry.getKey(), entry.getValue());
				} catch(IllegalArgumentException e) {	//this check is necessary for null values like roundWinners
					continue;
				}
			}
			
		if(remove0s)
			combination.remove0s();
		
		return combination;
	}
	
	@SuppressWarnings("unchecked")
	private <Y extends Enum<Y>> BarChart<Y> barChartFromEnum(String gaName, boolean remove0s) throws NoSuchFieldException, IllegalAccessException {
		BarChart<Y> combination = null;
		Field source = GameAnalysis.class.getDeclaredField(gaName);
		
		for(GameAnalysis gA : combinedAnalyses) {
			Y y = (Y) source.get(gA);
			if(combination == null)
				combination = new BarChart<>(EnumSet.allOf(y.getClass()));
				
			combination.addY(y);
		}
		
		if(remove0s)
			combination.remove0s();
		
		return combination;
	}
	
	private OneVarStats oneVarStatsFromNumber(String gaName) throws NoSuchFieldException, IllegalAccessException {
		Collection<Number> c = new LinkedList<>();
		Field source = GameAnalysis.class.getDeclaredField(gaName);
		
		for(GameAnalysis gA : combinedAnalyses)
			c.add((Number) source.get(gA));
		
		return new OneVarStats(c);
	}
	
	private int combineNumber(String gaName) throws NoSuchFieldException, IllegalAccessException {
		int i = 0;
		Field source = GameAnalysis.class.getDeclaredField(gaName);
		
		for(GameAnalysis gA : combinedAnalyses)
			i += ((Number) source.get(gA)).intValue();
			
		return i;
	}
	
	private int combineBoolean(String gaName) throws NoSuchFieldException, IllegalAccessException {
		int i = 0;
		Field source = GameAnalysis.class.getDeclaredField(gaName);
		
		for(GameAnalysis gA : combinedAnalyses)
			if(((Boolean) source.get(gA)).booleanValue())
				i++;
			
		return i;
	}
	
	private static BigDecimal weightedAverage(BarChart<IntegerEnum> m) {
		if(m.getTotalY() == 0)
			return new BigDecimal(0);
		
		double sum = 0.0;
		
		for(IntegerEnum n : EnumSet.copyOf(m.getXs()))
			sum += n.ordinal() * m.getY(n);
			
		return new BigDecimal(sum / m.getTotalY()).setScale(2, RoundingMode.HALF_UP);
	}
	
	//old method before IntegerEnum - still useful, but in general should use above now.
	private static BigDecimal weightedAverageFromFreq(BarChart<Freq> m) {
		if(m.getTotalY() == 0)
			return new BigDecimal(0);
		
		double sum = 0.0;
		Set<Freq> fs = EnumSet.copyOf(m.getXs());
		fs.removeAll(Freq.FAIL);
		
		for(Freq f : fs)
			sum += f.ordinal() * m.getY(f);
			
		return new BigDecimal(sum / m.getTotalY()).setScale(2, RoundingMode.HALF_UP);
	}
	
	public boolean containsAnyOfSeasons(IntPredicate ip) {
		return analysesBySeason.keySet().stream().mapToInt(i -> i.intValue()).anyMatch(ip);
	}
	
	/* end helpers. */
	
	private Set<String> sectionHeaders = new LinkedHashSet<>(60);	public Set<String> getSectionHeaders() { return sectionHeaders; }
	private void reportStat(String abbr, String title, Object o) {
		if(!sectionHeaders.add(abbr))
			System.err.println("Repeat header: " + abbr);
		report.append(LINE.replace("###", abbr) + title + "\n\n" + o);
	}
	
	public Set<GameAnalysis> getAnalyses() {
		return analyses;
	}
	
	public boolean equals(Object o) {
		if(!(o instanceof MultiGameAnalysis))
			return false;
		
		return analyses.equals(((MultiGameAnalysis) o).getAnalyses());
	}
	
	public String toString() {
		return report.toString();
	}
	
	//test a full season analysis (season given in command line). else, do it all!
	public static void main(String[] args) throws Exception {
		if(args.length == 1) {
			try(FileWriter writer = new FileWriter(new File("analysis\\s" + args[0] + " full.txt"))) {
				TreeSet<GameAnalysis> gAs = new TreeSet<>();
				
				for(File f : new File("analysis\\s" + args[0]).listFiles())
					if(!f.isDirectory()) {
						GameAnalysis gA;
						if(!gAs.add(gA = readAnalysis(f)))
							System.out.println(gA.getShowNumber());
						//if(gA.missolves.getY(MAINGAME) != 0)
							//System.out.println(gA.getShowNumber() + ", " + gA.missolves.getY(MAINGAME) + "\n" + gA.missolves);
						//if(gA.winnings.getY(Round.T1) != 1000)
							//System.out.println(gA.getTitle());
					}
				
				writer.write(new MultiGameAnalysis(gAs).toString());
			}
		} else {
			try(FileWriter writer = new FileWriter(new File("analysis\\s29-33_full.txt"))) {
				TreeSet<GameAnalysis> gAs = new TreeSet<>();
				
				for(File f : new File("analysis\\s29").listFiles())
					if(!f.isDirectory())
						gAs.add(GameAnalysis.readAnalysis(f));
				for(File f : new File("analysis\\s30").listFiles())
					if(!f.isDirectory()) 
						gAs.add(GameAnalysis.readAnalysis(f));
				for(File f : new File("analysis\\s31").listFiles())
					if(!f.isDirectory()) 
						gAs.add(GameAnalysis.readAnalysis(f));
				for(File f : new File("analysis\\s32").listFiles())
					if(!f.isDirectory()) 
						gAs.add(GameAnalysis.readAnalysis(f));
				for(File f : new File("analysis\\s33").listFiles())
					if(!f.isDirectory()) 
						gAs.add(GameAnalysis.readAnalysis(f));
				
				writer.write(new MultiGameAnalysis(gAs).toString());
			}
		}
	}
}