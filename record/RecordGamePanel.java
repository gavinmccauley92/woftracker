package woftracker.record;

import woftracker.*;
import woftracker.stats.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import java.util.*;
import java.io.Serializable;

/* this class was bulky written before I took CS210 and beyond, leading to longness. lazy also means no real documentation... hehe.
	however, it has been renovated decently since then. */

public class RecordGamePanel extends EditablePanel implements Recordable {
	private  JPanel
		TurnRowPanel, TurnColumnOnePanel, TurnColumnTwoPanel,	//sub-components needed
		LetterPanel, SolvePanel,	//more sub-components
		SpecialRowPanel,	//simply some special buttons :)
		WedgeColumnPanel;
	
	private JToggleButton tripleStumper, riskMystery, finalSpin, noMoreVowels, noMoreConsonants, useWildCard, prizePuzzle;
	private JLabel defaultRound, TurnPlayer, jackpotCounter;
	private JRadioButton spin, buy, solve, missolve, buzzed, wedgeLeft, wedgeCenter, wedgeRight;
	private JCheckBox overRotation;
	private ButtonGroup TurnChoices, WedgePosition;
	private JComboBox<Wedge> wedgeBox;
	private JComboBox<BonusWedge> bonusWedgeBox;
	private JComboBox<BonusAmount> bonusAmountBox;
	private JComboBox<Letter> letterBox;
	private JComboBox<Byte> freqBox;
	private JTextPane puzzleBoard, solveAttempt;
	
	private static final String PUZZLEBOARD_DEFAULT = "What the puzzle looks like",
		SOLVEATTEMPT_DEFAULT = "What the contestant says";
	private static final Byte[] FREQ_OPTIONS = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 /*, 10 */ };
	
	private static final Font ABBR_FONT = new Font("Verdana", Font.BOLD, 12);
																			//ALSO EDIT: BOTTOM OF HERE, GAMEANALYSIS
	private static int boardCount = 0, prizeOnWheelAmount = 0, jackpot = 5000, roundMinimum = 1000, prevPosition = 0, editAdjustment = 0;
	private static boolean isFreePlay = false, canGetJackpot = false, donePrizePuzzle = false, isFinalSpin = false, isBonusRound = false, isCarWon = false;
	private static String prizeOnWheelDescription;
	private static Wedge prev;
	
	private static EnumSet<Letter> calledLetters = EnumSet.noneOf(Letter.class);
	private static Queue<EnumSet<Letter>> puzzleBoards = new LinkedList<>();
	
	RecordGamePanel() {
		
		TurnRowPanel = new JPanel();
		TurnRowPanel.setLayout(new BoxLayout(TurnRowPanel, BoxLayout.LINE_AXIS));
		
		TurnPlayer = new JLabel("-");
		TurnPlayer.setFont(ABBR_FONT);
		TurnRowPanel.add(Box.createHorizontalGlue());
		TurnRowPanel.add(Box.createHorizontalStrut(10));
		TurnRowPanel.add(TurnPlayer);
		TurnRowPanel.add(Box.createHorizontalStrut(15));
		
		TurnColumnOnePanel = new JPanel();
		TurnColumnOnePanel.setLayout(new GridLayout(5, 1, 0, 10));
		spin = new JRadioButton("spins");
		spin.setEnabled(false);
		spin.setOpaque(false);
		TurnColumnOnePanel.add(spin);
		buy = new JRadioButton("buys");
		buy.setEnabled(false);
		buy.setOpaque(false);
		TurnColumnOnePanel.add(buy);
		solve = new JRadioButton("solves");
		solve.setEnabled(false);
		solve.setOpaque(false);
		TurnColumnOnePanel.add(solve);
		missolve = new JRadioButton("missolves");
		missolve.setEnabled(false);
		missolve.setOpaque(false);
		TurnColumnOnePanel.add(missolve);
		buzzed = new JRadioButton("got buzzed");
		buzzed.setEnabled(false);
		buzzed.setOpaque(false);
		TurnColumnOnePanel.add(buzzed);
		TurnChoices = new ButtonGroup();
			TurnChoices.add(spin);
			TurnChoices.add(buy);
			TurnChoices.add(solve);
			TurnChoices.add(missolve);
			TurnChoices.add(buzzed);
			
			TurnListener tL = new TurnListener();
			spin.addActionListener(tL);
			buy.addActionListener(tL);
			solve.addActionListener(tL);
			missolve.addActionListener(tL);
			buzzed.addActionListener(tL);
		
		TurnRowPanel.add(TurnColumnOnePanel);
		TurnRowPanel.add(Box.createHorizontalStrut(25));
		
		LetterPanel = new JPanel();
		LetterPanel.setLayout(new BoxLayout(LetterPanel, BoxLayout.LINE_AXIS));
		LetterPanel.setOpaque(false);
		wedgeBox = new JComboBox<Wedge>();
		wedgeBox.setEnabled(false);
		wedgeBox.setMaximumSize(new Dimension(35, 30));
		
		HiddenEdit.addHiddenEdit(wedgeBox, new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				try {
					editAdjustment = Integer.parseInt(JOptionPane.showInputDialog(wedgeBox, "ONLY USE WHEN A DIFFERENT SPOT THAN EXPECTED IS CLEAR.\nTo the left of the expected should be a " +
					"negative adjustment and vice versa.", "Hidden: Spin Strength Adjustment", JOptionPane.QUESTION_MESSAGE, null, null, String.valueOf(editAdjustment)).toString());
				} catch(Exception ex) {}
			}
		});
		
		bonusWedgeBox = new JComboBox<BonusWedge>(new Vector<>(new TreeSet<>(EnumSet.allOf(BonusWedge.class)).descendingSet()));	//only reverse that returns a collection needed for Vector
		bonusWedgeBox.setVisible(false);
		bonusWedgeBox.setMaximumSize(new Dimension(35, 30));
		bonusAmountBox = new JComboBox<BonusAmount>();
		bonusAmountBox.setVisible(false);
		bonusAmountBox.setMaximumSize(new Dimension(35, 30));

		LetterPanel.add(Box.createHorizontalGlue());
		LetterPanel.add(wedgeBox);
		LetterPanel.add(bonusWedgeBox);
		LetterPanel.add(bonusAmountBox);
		LetterPanel.add(Box.createHorizontalStrut(5));
		
		WedgeColumnPanel = new JPanel();
		WedgeColumnPanel.setLayout(new BoxLayout(WedgeColumnPanel, BoxLayout.PAGE_AXIS));
		WedgeColumnPanel.setOpaque(false);
		wedgeLeft = new JRadioButton("L");
		wedgeLeft.setEnabled(false);
		wedgeLeft.setOpaque(false);
		wedgeLeft.setToolTipText("The left third of the wedge.");
		WedgeColumnPanel.add(wedgeLeft);
		wedgeCenter = new JRadioButton("C");
		wedgeCenter.setEnabled(false);
		wedgeCenter.setOpaque(false);
		wedgeCenter.setToolTipText("The center third of the wedge.");
		WedgeColumnPanel.add(wedgeCenter);
		wedgeRight = new JRadioButton("R");
		wedgeRight.setEnabled(false);
		wedgeRight.setOpaque(false);
		wedgeRight.setToolTipText("The right third of the wedge.");
		WedgeColumnPanel.add(wedgeRight);
		WedgePosition = new ButtonGroup();
			WedgePosition.add(wedgeLeft);
			WedgePosition.add(wedgeCenter);
			WedgePosition.add(wedgeRight);
		
		WedgeListener wL = new WedgeListener();
			wedgeBox.addActionListener(wL);
			wedgeLeft.addActionListener(wL);
			wedgeCenter.addActionListener(wL);
			wedgeRight.addActionListener(wL);
		
		LetterPanel.add(WedgeColumnPanel);
		LetterPanel.add(Box.createHorizontalStrut(5));
		overRotation = new JCheckBox("OR");
		overRotation.setEnabled(false);
		overRotation.setOpaque(false);
		overRotation.setToolTipText("Check if the Wheel was spun back to its starting point or greater.");
		LetterPanel.add(overRotation);
		LetterPanel.add(Box.createHorizontalStrut(2));
		LetterPanel.add(new JLabel(","));
		LetterPanel.add(Box.createHorizontalStrut(15));
		letterBox = new JComboBox<Letter>();
		letterBox.addActionListener(new LetterListener());
		letterBox.setEnabled(false);
		letterBox.setMaximumSize(new Dimension(150, 25));
		LetterPanel.add(letterBox);
		LetterPanel.add(Box.createHorizontalStrut(2));
		LetterPanel.add(new JLabel(","));
		LetterPanel.add(Box.createHorizontalStrut(15));
		freqBox = new JComboBox<Byte>(FREQ_OPTIONS);
		freqBox.setSelectedItem(null);
		freqBox.setEnabled(false);
		freqBox.setMaximumSize(new Dimension(150, 25));
		LetterPanel.add(freqBox);
		LetterPanel.add(Box.createHorizontalStrut(2));
		LetterPanel.add(new JLabel("."));
		LetterPanel.add(Box.createHorizontalStrut(10));
		submit = new JButton("Submit");
		submit.setEnabled(false);
		LetterPanel.add(submit);
		LetterPanel.add(Box.createHorizontalStrut(10));
		LetterPanel.add(Box.createHorizontalGlue());
		
		SolvePanel = new JPanel();
		SolvePanel.setLayout(new BoxLayout(SolvePanel, BoxLayout.LINE_AXIS));
		SolvePanel.setOpaque(false);
		puzzleBoard = new JTextPane();
		puzzleBoard.setText(PUZZLEBOARD_DEFAULT);
		puzzleBoard.setPreferredSize(new Dimension(200, 80));
		puzzleBoard.setMaximumSize(new Dimension(200, 80));
		puzzleBoard.setEnabled(false);
		puzzleBoard.setOpaque(false);
		SolvePanel.add(Box.createHorizontalGlue());
		SolvePanel.add(puzzleBoard);
		SolvePanel.add(Box.createHorizontalGlue());
		solveAttempt = new JTextPane();
		solveAttempt.setText(SOLVEATTEMPT_DEFAULT);
		solveAttempt.setPreferredSize(new Dimension(200, 80));
		solveAttempt.setMaximumSize(new Dimension(200, 80));
		solveAttempt.setEnabled(false);
		solveAttempt.setOpaque(false);
		SolvePanel.add(solveAttempt);
		SolvePanel.add(Box.createHorizontalStrut(10));
		SolvePanel.add(Box.createHorizontalGlue());
		
		TurnColumnTwoPanel = new JPanel();
		TurnColumnTwoPanel.setLayout(new BoxLayout(TurnColumnTwoPanel, BoxLayout.PAGE_AXIS));
		TurnColumnTwoPanel.add(LetterPanel);
		TurnColumnTwoPanel.add(Box.createVerticalGlue());
		TurnColumnTwoPanel.add(SolvePanel);
		TurnColumnTwoPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));
		TurnRowPanel.add(TurnColumnTwoPanel);
		
		SpecialRowPanel = new JPanel();
		SpecialRowPanel.setLayout(new BoxLayout(SpecialRowPanel, BoxLayout.LINE_AXIS));
		SpecialRowPanel.setOpaque(false);
		SpecialRowPanel.add(Box.createHorizontalGlue());
		defaultRound = new JLabel("N/A");
		defaultRound.setToolTipText("No round-specific actions during toss-ups or R2.");
		SpecialRowPanel.add(defaultRound);
		jackpotCounter = new JLabel("Jackpot: $" + jackpot);
		jackpotCounter.setVisible(false);
		SpecialRowPanel.add(jackpotCounter);
		tripleStumper = new JToggleButton("Triple Stumper!");
		tripleStumper.setEnabled(false);
		tripleStumper.setVisible(false);
		tripleStumper.setToolTipText("Enabled automatically if everyone missolved / blanked. Enable manually while submitting the last fail if it's not third.");
		SpecialRowPanel.add(tripleStumper);
		riskMystery = new JToggleButton("Go for Mystery...");
		riskMystery.setEnabled(false);
		riskMystery.setVisible(false);
		SpecialRowPanel.add(riskMystery);
		finalSpin = new JToggleButton("Final Spin!");
		finalSpin.setVisible(false);
		SpecialRowPanel.add(finalSpin);
		SpecialRowPanel.add(Box.createHorizontalStrut(10));
		noMoreVowels = new JToggleButton("No More Vowels");
		noMoreVowels.setEnabled(false);
		SpecialRowPanel.add(noMoreVowels);
		SpecialRowPanel.add(Box.createHorizontalStrut(10));
		noMoreConsonants = new JToggleButton("No More Consonants");
		noMoreConsonants.setEnabled(false);
		SpecialRowPanel.add(noMoreConsonants);
		SpecialRowPanel.add(Box.createHorizontalStrut(10));
		useWildCard = new JToggleButton("Use Wild Card");
		useWildCard.setEnabled(false);
		SpecialRowPanel.add(useWildCard);
		SpecialRowPanel.add(Box.createHorizontalStrut(10));
		prizePuzzle = new JToggleButton("Prize Puzzle!");
		prizePuzzle.setVisible(false);	//as of S29, PP is always R3, so hide this away and do work automatically
		prizePuzzle.setEnabled(false);
		SpecialRowPanel.add(prizePuzzle);
		SpecialRowPanel.add(Box.createHorizontalGlue());
		SpecialRowPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		
		SpecialListener sL = new SpecialListener();
		riskMystery.addItemListener(sL);
		finalSpin.addItemListener(sL);
		noMoreVowels.addItemListener(sL);
		noMoreConsonants.addItemListener(sL);
		useWildCard.addItemListener(sL);
		//prizePuzzle.addItemListener(sL);
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		
		setOpaque(false);
		add(Box.createVerticalGlue());
		add(TurnRowPanel);
		add(Box.createVerticalStrut(10));
		add(SpecialRowPanel);
		add(Box.createVerticalGlue());
	}
	
	private class RoundListener implements ActionListener, Serializable {
		private static final long serialVersionUID = 1L;
		
		int calledVowels = 0;	//all 5 Letter.VOWELS called is common. all 21 consonants never happens.
		EnumSet<Letter> previousBoard;
		
		private boolean isPerfect = true;
		
		//here comes the loooooooong method!
		public void actionPerformed(ActionEvent aE) {
			Player p = Player.getCurrentPlayer();
			String abbr = p.getAbbrName(), name = p.getName(), currentScore = p.getFormattedRoundScore(true), wedgeString = "", strengthString = "";
			Wedge w = null;
			int indexOf$ = -1, spinStrength = -1;
			Letter l = null;
			boolean isBlackAndWhite = false, isBlack = false, isExempt = false, isCombo = false;
			Byte freq = null;
			
			try {
				if(wedgeBox.getSelectedIndex() != -1) {
					w = (Wedge) wedgeBox.getSelectedItem();
					
					if(prev == w)
						prevPosition = prev.getPosition();	//save the old position in another variable before being overwritten. see Wedge.determineStrength()
					
					if(wedgeLeft.isSelected()) {
						w.setPosition(Wedge.LEFT);
					} else if(wedgeCenter.isSelected()) {
						w.setPosition(Wedge.CENTER);
					} else if(wedgeRight.isSelected()) {
						w.setPosition(Wedge.RIGHT);
					} else  { return; }
					
					wedgeString = w.toTrimmedString();
					strengthString = "<span class = \"strength\">(" + (spinStrength = Wedge.determineStrength(prev, w, overRotation.isSelected()) + editAdjustment) + ")</span>";
					editAdjustment = 0;
					
					if(spinStrength <= 36 || spinStrength >= 100)
						if(JOptionPane.showOptionDialog(MainWindow.getWindow(), "This spin strength is irregular (" + spinStrength + "). Are you sure this is right? Common fix: verify OR.",
						"Spin Strength Check", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, Arrays.asList("Yes", "No").toArray(), "No") != JOptionPane.YES_OPTION)
							return;
					
					if(w == Wedge.MillionDollarWedge) {		//special cases
						switch(w.getPosition()) {
							case Wedge.CENTER:
								wedgeString = "MDW";
								break;
							default:
								wedgeString = (wedgeLeft.isSelected() ? "LEFT" : "RIGHT") + " MDW BANKRUPT";
						}
					} if(w == Wedge.PrizeOnWheel)
						wedgeString = prizeOnWheelDescription;	//override.
					
					isBlackAndWhite = (isBlack = Wedge.isBlack(w)) || w == Wedge.LOSE_A_TURN;
					isExempt = (w == Wedge.FreePlay);
					isCombo = ((solve.isSelected() || missolve.isSelected()) && isExempt) || buzzed.isSelected();
				}
				
				//do not reassign l and freq if it is black and white, a buzzout, or a FP-solve attempt. Also requires a letter filled in and proper button
				if(!isBlackAndWhite && !isCombo && letterBox.getSelectedIndex() != -1 && (spin.isSelected() || buy.isSelected() || useWildCard.isSelected())) {
					if(!calledLetters.contains(l = (Letter) letterBox.getSelectedItem())) {
						freq = (Byte) freqBox.getSelectedItem();
						if(freq == null)	//a non-repeat letter cannot have a freq of null, which signals repeated letter
							return;
						calledLetters.add(l);
					}
				}
				
				if(RecordPanel.roundNumber == 1 && spin.isSelected() && !useWildCard.isSelected()) {
					jackpot += w.getAmount();
					jackpotCounter.setText("Jackpot: $" + jackpot);
					if(!solve.isSelected())
						canGetJackpot = false;	//reset if player is on Jackpot and does not solve immediately after
				}
			} catch (ClassCastException cCE) { System.err.println("Class cast"); return; }	//occurs when loading the box
			  catch (NullPointerException nPE) { System.err.println("Box set to null"); return; }
			
			write();
			
			if(useWildCard.isSelected()) {
				boolean didFail = freq == null || freq == 0, ultraFail = buzzed.isSelected();	//2/1/12; can't use isCombo, that's only defined on a spin
				
				RecordPanel.GAME_TEXT.append("<span" + (didFail || ultraFail ? " class = \"fail\"" : (prev.getAmount() >= 2500 ? " class = \"epic\"" : "")) + ">"
					+ abbr + " use" + (roundMinimum == 1000 ? "s" : "") + " Wild Card, " + (!ultraFail ? (l + ", " + (freq == null ? "called" : freq) + '.' +
					(didFail ? " (" + currentScore + ") " : "")) : "and got buzzed out. (" + currentScore + ")") + "</span><br>"
				);
				
				p.setTotalStatus(Wedge.WildCard, false);
				useWildCard.setSelected(false);
				
				RecordPanel.gameAnalysis.analyzeSpin(p, prev, l, freq, (short) 0, false);	//This must be analyzed first because on a dud, prev is reset on loseTurn! 5/26/12
				
				if(didFail || ultraFail)
					Player.loseTurn();
				else
					p.addMoney(prev, freq);
			} else if(spin.isSelected()) {
				boolean didFail = freq == null || freq == 0;
				RecordPanel.GAME_TEXT.append("<span" + (didFail ? " class = \"fail\"" : ((w.getAmount() >= 2500 ? " class = \"epic\"" : ""))) + ">" + abbr + ' ' + spin.getText() + ' '
					+ wedgeString + (!isBlackAndWhite ? ", " + l + ", " + (freq == null ? "called" : freq) : "") + '.' +
					(didFail ? " (" + currentScore + (isBlack ? p.getTotalStatus() : "") + ") " : ' ') + strengthString + "</span><br>"
				);
				
				prev = w;
				
				boolean didBankrupt = false;
				
				if(didFail && !isBlackAndWhite && !isExempt)
					Player.loseTurn();
				else {
					switch(w) {
						case WildCard:	//as of S30
						case HalfCar_1:
						case HalfCar_2:
						case HalfCar_3:
						case HalfCar_4:
						case HalfCar_5:
						case HalfCar_6:
						case PrizeOnWheel:	//as of S30
						case GiftTag:	//as of S30
							p.addMoney(w, freq);
							if(w == Wedge.WildCard)
								p.setTotalStatus(w, true);
							else
								p.addRoundStatus(w);
							Wedge.replaceWedge(w);
							break;
						case MillionDollarWedge:
							if(w.getPosition() == Wedge.CENTER) {
								p.addRoundStatus(w);
								Wedge.replaceWedge(w);
							} else {
								didBankrupt = true;
								Player.loseTurn();
							}
							break;
						case Purple1000_1:
						case Purple1000_2:
							if(riskMystery.isSelected()) {
								if(JOptionPane.showOptionDialog(MainWindow.getWindow(), "Mystery successful?", "Mystery Check", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
								Arrays.asList("Yes", "No").toArray(), null) == JOptionPane.YES_OPTION) {
									RecordPanel.GAME_TEXT.append("<span class = \"epic\">" + abbr + " risks the entire bank and gets the cash!</span><br>");
									p.addRoundStatus(w);
									Wedge.replaceWedge(w);
								} else
									RecordPanel.GAME_TEXT.append("<span class = \"fail\">" + abbr + " risks the entire bank and loses it all. (" + currentScore + p.getTotalStatus() + ")</span><br>");
								
								Wedge.replaceWedge(w);
								riskMystery.setEnabled(false);
								riskMystery.setSelected(false);
								riskMystery.setText("Mystery Risked");
								if(p.hasMystery())
									break;
							} else {
								p.addMoney(w, freq);
								break;
							}
						case BANKRUPT_1:
						case BANKRUPT_2:
							didBankrupt = true;
						case LOSE_A_TURN:
							Player.loseTurn();
							break;
						case FreePlay:
							if(Letter.CONSONANTS.contains(l)) {
								if(freq != null)	//in the very rare case a repeat is called on Free Play...
									p.addMoney(w, freq);
							} else
								if(++calledVowels == Letter.VOWELS.size())
									noMoreVowels.setSelected(true);	//will generate event like usual
							break;
						case Jackpot:
							canGetJackpot = true;
						default:
							p.addMoney(w, freq);
					}
				}
				
				RecordPanel.gameAnalysis.analyzeSpin(p, w, l, freq, (short) spinStrength, RecordPanel.roundNumber == Wedge.MYSTERY_ROUND && !riskMystery.isEnabled() && didBankrupt);
					
				if(didBankrupt)
					p.bankrupt();
				
				useWildCard.setEnabled(Player.getCurrentPlayer().hasWC());
				
				setSpinChoices(prev.getLocation());
			} else if(buy.isSelected()) {
				if(p.getRoundScore() < 250) return;
				p.buyVowel();
				
				boolean didFail = freq == null || freq == 0;
				RecordPanel.GAME_TEXT.append("<span" + (didFail ? " class = \"fail\"" : "") + ">" + abbr + ' ' + buy.getText() + ' ' + l.getChar() + ", " + (freq == null ? "called" : freq) + "." +
					(didFail ? " (" + p.getFormattedRoundScore(true) + ")" : "") + "</span><br>"	//here, re-do currentScore with -250
				);
				
				if(freq != null && ++calledVowels == Letter.VOWELS.size())
					noMoreVowels.setSelected(true);	//will generate event like usual
				
				if(didFail) {
					Player.loseTurn();
					setSpinChoices(prev.getLocation());
				}
				
				RecordPanel.gameAnalysis.analyzeCall(p, l, freq);
			} else if(solve.isSelected()) {
				String solved = solveAttempt.getText().replaceAll("\n", "<br>");
				Puzzle puzzle = new MainPuzzle(solved);
				
				EnumSet<Letter> board = EnumSet.copyOf(calledLetters);
				
				boolean isConsecutive;
				if(!(isConsecutive = board.equals(previousBoard))) {
					boardCount++;
					puzzleBoards.offer(board);
					previousBoard = board;
				}
				
				if(isCombo) {
					RecordPanel.GAME_TEXT.append("<span>" + abbr + " spins Free Play, solve attempt: " + strengthString + "</span><br>");
					prev = w;	//needed if end of R4
					RecordPanel.gameAnalysis.analyzeSpin(p, w, null, null, (short) spinStrength, true);
				}
				
				RecordPanel.GAME_TEXT.append((isConsecutive ? "" : "<br><span>" + PB_ID + boardCount + "</span><br><br>") + "<span class = \"" +
					(p.hasMystery() || (p.getRoundScore() + (canGetJackpot ? jackpot : 0) + (p.hasGiftTag() ? 1000 : 0)) >= 10000 ? "epic" : "solve") + "\"><b>" + name + ' ' +
					solve.getText() + ' ' + solved.replaceAll("<br>", " ") + " for $" + p.getRoundScore() + p.getRoundStatus()
				);
				
				boolean more = false, isCarWonInOneRound = false;
				
				if(p.hasGiftTag()) {
					p.addMoney(1000);
					more = true;
				} if(p.hasPrize()) {
					p.addMoney(prizeOnWheelAmount);
					more = true;
				} if(p.hasMystery()) {
					p.addMoney(10000);
					more = true;
				} if(canGetJackpot) {
					p.addMoney(jackpot);
					more = true;
				} if(prizePuzzle.isSelected()) {
					donePrizePuzzle = true;
					more = true;
				} if(p.numHalfCars() >= 2) {
					isCarWon = true;
					if(p.numHalfCars(true) == 0)
						isCarWonInOneRound = true;
					more = true;
				}
				
				boolean isRoundMinimum = (p.getRoundScore() < roundMinimum && !prizePuzzle.isSelected());
				if(isRoundMinimum) {
					p.addMoney(roundMinimum - p.getRoundScore());
				}
				
				currentScore = p.getFormattedRoundScore(false);	//re-initialize to update above, but also w/o redundant status
				
				//isCarWon is redone separately here because it needs to remain true for roundConfiguration
				RecordPanel.GAME_TEXT.append((more ? "," + (p.numHalfCars() >= 2 ? (!isCarWonInOneRound ? " and completing the car," : "") + " a " + VEHICLE_ID + "," : "") +
					(canGetJackpot ? " and a Jackpot of $" + jackpot + "," : "") + (prizePuzzle.isSelected() ? " and the Prize Puzzle, " + PP_ID + "," : "") + " a total of " +
					currentScore : "") + (isRoundMinimum ? ", bumped up to " + currentScore : "") + ".</b></span><br><br>"
				);
				
				if(isPerfect)
					RecordPanel.GAME_TEXT.append("<span class = \"epic\"><b>PERFECT ROUND!</b></span><br><br>");
				
				writePuzzleBoards(puzzle);
				
				RecordPanel.gameAnalysis.analyzeSolveAttempt(p, puzzle);
				
				if(RecordPanel.roundNumber == 1) {
					RecordPanel.gameAnalysis.analyzeJackpot(jackpot, canGetJackpot);
					if(!canGetJackpot)
						RecordPanel.GAME_TEXT.append("<span><b>LEFT IN JACKPOT:</b> $" + jackpot + "</span><br><br>");
				}
				
				canGetJackpot = false;
				
				spin.setEnabled(false);
				buy.setEnabled(false);
				solve.setEnabled(false);
				missolve.setEnabled(false);
				buzzed.setEnabled(false);
				wedgeBox.setEnabled(false);
				wedgeLeft.setEnabled(false);
				wedgeCenter.setEnabled(false);
				wedgeRight.setEnabled(false);
				overRotation.setEnabled(false);
				letterBox.setEnabled(false);
				freqBox.setEnabled(false);
				submit.setEnabled(false);
				solveAttempt.setText(SOLVEATTEMPT_DEFAULT);
				solveAttempt.setEnabled(false);
				noMoreVowels.setEnabled(false);
				noMoreConsonants.setEnabled(false);
				noMoreVowels.setSelected(false);
				noMoreConsonants.setSelected(false);
				useWildCard.setEnabled(false);
				prizePuzzle.setEnabled(false);
				
				boardCount = 0;
				
				Player.RED.winRound(p == Player.RED);
				Player.YELLOW.winRound(p == Player.YELLOW);
				Player.BLUE.winRound(p == Player.BLUE);
				
				prizePuzzle.setSelected(false);
				RecordPanel.UTILITY.endRound(true);
			} else if(missolve.isSelected()) {
				//this duplicates code from solve branch, but is more convenient inline
				EnumSet<Letter> board = EnumSet.copyOf(calledLetters);
				
				boolean isConsecutive;
				if(!(isConsecutive = board.equals(previousBoard))) {
					boardCount++;
					puzzleBoards.offer(board);
					previousBoard = board;
				}
				
				if(isCombo) {
					RecordPanel.GAME_TEXT.append("<span>" + abbr + " spins Free Play, solve attempt: " + strengthString + "</span><br>");
					setSpinChoices(prev.getLocation());
					prev = w;
					isExempt = true;
					RecordPanel.gameAnalysis.analyzeSpin(p, w, null, null, (short) spinStrength, true);
				}
				
				RecordPanel.GAME_TEXT.append((isConsecutive ? "" : "<br><span>" + PB_ID + boardCount + "</span><br><br>") + "<span class = \"fail\">" + name + ' ' + missolve.getText() + ' ' +
					solveAttempt.getText() + ". (" + currentScore + ")</span><br>"
				);
				
				if(!isExempt) {
					Player.loseTurn();
					setSpinChoices(prev.getLocation());
				}
					
				RecordPanel.gameAnalysis.analyzeSolveAttempt(p, null);
			} else if(buzzed.isSelected()) {
				RecordPanel.GAME_TEXT.append("<span class = \"fail\">" + abbr);
				
				if(isCombo) {
					RecordPanel.GAME_TEXT.append(" spins " + wedgeString + " and");
					prev = w;
					RecordPanel.gameAnalysis.analyzeSpin(p, w, null, null, (short) spinStrength, false);
				}
					
				RecordPanel.GAME_TEXT.append(" got buzzed out. (" + currentScore + ')' + (isCombo ? ' ' + strengthString : "") + "</span><br>");
				
				if(!isExempt) {
					Player.loseTurn();
					setSpinChoices(prev.getLocation());
				}
				
				RecordPanel.gameAnalysis.analyzeBuzz(p);
			} else return;
			
			RecordPanel.TOSS_INFO.updateBanks();
			RecordPanel.update();
			letterBox.setSelectedIndex(-1);
			freqBox.setSelectedIndex(-1);
			overRotation.setSelected(false);
			
			String perfectCheck = TurnPlayer.getText();
			setPlayerText();
			isPerfect &= perfectCheck.equals(TurnPlayer.getText());
			
			TurnChoices.clearSelection();
			WedgePosition.clearSelection();
		}
	}
	
	private static void writePuzzleBoards(Puzzle puzzle) {
		int i = 0;
		while(puzzleBoards.peek() != null) {
			String id = PB_ID + ++i;
			int index = RecordPanel.GAME_TEXT.indexOf(id);
			RecordPanel.GAME_TEXT.replace(index, index + id.length(), puzzle.updatePuzzle(puzzleBoards.remove()));
		}
	}
	
	private class FinalListener implements ActionListener, Serializable {
		private static final long serialVersionUID = 1L;
		
		int AMOUNT;
		boolean hasAttempted = false;	//in the final spin sequence, a simple boolean can be used instead of previousBoard to determine two identical boards.
	
		public void actionPerformed(ActionEvent aE) {
			Player p = Player.getCurrentPlayer();
			String abbr = p.getAbbrName(), name = p.getName(), currentScore = p.getFormattedRoundScore(true);
			int indexOf$ = -1;
			
			write();
			
			if(spin.isSelected() && wedgeBox.getSelectedIndex() != -1) {
				Wedge w = (Wedge) wedgeBox.getSelectedItem();
				
				if(prev == w)
					prevPosition = prev.getPosition();	//save the old position in another variable before being overwritten. see Wedge.determineStrength()
				
				if(wedgeLeft.isSelected()) {
					w.setPosition(Wedge.LEFT);
				} else if(wedgeCenter.isSelected()) {
					w.setPosition(Wedge.CENTER);
				} else if(wedgeRight.isSelected()) {
					w.setPosition(Wedge.RIGHT);
				} else return;
				
				write();

				AMOUNT = w.getAmount() + 1000;
				
				
				String wedgeString = w.toTrimmedString(), strengthString;
				int spinStrength;
				strengthString = "<span class = \"strength\">(" + (spinStrength = Wedge.determineStrength(prev, w, overRotation.isSelected()) + editAdjustment) + ")</span>";
				editAdjustment = 0;
				
				RecordPanel.GAME_TEXT.append("<span" + (AMOUNT == 6000 ? " class = \"epic\"" : "") + ">Pat " + spin.getText() + ' ' + wedgeString + " --> $" + AMOUNT + ". " + strengthString + "</span><br>");
				RecordPanel.gameAnalysis.analyzeSpin(null, w, null, null, (short) spinStrength, false);
				
				spin.setEnabled(false);
				buy.setEnabled(true);
				solve.setEnabled(true);
				missolve.setEnabled(true);
				buzzed.setEnabled(true);
				wedgeBox.setEnabled(false);
				wedgeLeft.setEnabled(false);
				wedgeCenter.setEnabled(false);
				wedgeRight.setEnabled(false);
				overRotation.setEnabled(false);
				overRotation.setSelected(false);
				letterBox.setEnabled(true);
				freqBox.setEnabled(true);
				solveAttempt.setEnabled(true);
				noMoreVowels.setEnabled(true);
				noMoreConsonants.setEnabled(true);
				WedgePosition.clearSelection();
			} else if(buy.isSelected() && letterBox.getSelectedIndex() != -1) {
				Letter l = (Letter) letterBox.getSelectedItem();
				Byte freq = null;
				
				if(!calledLetters.contains(l)) {
					freq = (Byte) freqBox.getSelectedItem();
					if(freq == null)	//a non-repeat letter cannot have a freq of null, which signals repeated letter
						return;
					calledLetters.add(l);
				}
				
				RecordPanel.GAME_TEXT.append("<span>" + abbr + ' ' + buy.getText() + ' ' + l + ", " + (freq == null ? "called" : freq) + ".</span><br>");
				
				if(freq == null || freq == 0)
					Player.loseTurn();
				else {
					p.addMoney((Letter.VOWELS.contains(l) ? 0 : AMOUNT), freq);
					buy.setEnabled(false);
				}
					
				RecordPanel.gameAnalysis.analyzeCall(p, l, freq);
			} else if(solve.isSelected()) {
				String solved = solveAttempt.getText().replaceAll("\n", "<br>");
				Puzzle puzzle = new MainPuzzle(solved);
				
				if(!hasAttempted) {		//occasionally people will solve after missolving, so don't repeat certainly same puzzleboard here either. 3/16/12
					puzzleBoards.offer(EnumSet.copyOf(calledLetters));
					RecordPanel.GAME_TEXT.append("<br><span>" + PB_ID + ++boardCount + "</span><br><br>");
				}
				
				RecordPanel.GAME_TEXT.append("<span class = \"" + (p.getRoundScore() >= 10000 ? "epic" : "solve") + "\"><b>" + name + ' ' + solve.getText()
					+ ' ' + solved.replaceAll("<br>", " ") + " for $" + p.getRoundScore()
				);
				
				boolean isRoundMinimum = (p.getRoundScore() < roundMinimum);
				
				if(isRoundMinimum) {
					p.addMoney(roundMinimum - p.getRoundScore());
					currentScore = p.getFormattedRoundScore(false);	//re-initialize to update above
				}
				
				
				RecordPanel.GAME_TEXT.append((isRoundMinimum ? ", bumped up to " + currentScore : "") + ".</b></span><br><br>");
				
				writePuzzleBoards(puzzle);
				RecordPanel.gameAnalysis.analyzeSolveAttempt(p, puzzle);

				buy.setEnabled(false);
				solve.setEnabled(false);
				missolve.setEnabled(false);
				buzzed.setEnabled(false);
				letterBox.setEnabled(false);
				freqBox.setEnabled(false);
				submit.setEnabled(false);
				solveAttempt.setText(SOLVEATTEMPT_DEFAULT);
				solveAttempt.setEnabled(false);
				noMoreVowels.setEnabled(false);
				noMoreConsonants.setEnabled(false);
				noMoreVowels.setSelected(false);
				noMoreConsonants.setSelected(false);
				
				Player.RED.winRound(p == Player.RED );
				Player.YELLOW.winRound(p == Player.YELLOW);
				Player.BLUE.winRound(p == Player.BLUE);
				
				RecordPanel.UTILITY.endRound(true);	
			} else if(missolve.isSelected()) {
				if(!hasAttempted) {
					puzzleBoards.offer(EnumSet.copyOf(calledLetters));
					RecordPanel.GAME_TEXT.append("<br><span>" + PB_ID + ++boardCount + "</span><br><br>");
				}

				RecordPanel.GAME_TEXT.append("<span class = \"fail\">" + name + ' ' + missolve.getText() + ' ' + solveAttempt.getText() + ".</span><br>");
				RecordPanel.gameAnalysis.analyzeSolveAttempt(p, null);
				
				hasAttempted = true;
			} else if(buzzed.isSelected()) {
				Player.loseTurn();
				hasAttempted = false;
				buy.setEnabled(true);
				RecordPanel.gameAnalysis.analyzeBuzz(p);
			} else return;
			
			RecordPanel.TOSS_INFO.updateBanks();
			RecordPanel.update();
			letterBox.setSelectedIndex(-1);
			freqBox.setSelectedIndex(-1);
			setPlayerText();
			TurnChoices.clearSelection();
		}
	}
	
	private class BonusListener implements ActionListener, Serializable {
		private static final long serialVersionUID = 1L;
		
		int numberOfLetters, letterCount = 0;
		String letterString = "", solved = "", boardTwoString;
		BonusWedge bW = null; BonusAmount bA = null;
		Player p;
		
		public void actionPerformed(ActionEvent aE) {
			if(p == null)
				p = Player.getCurrentPlayer();
			String abbr = p.getAbbrName(), name = p.getName(); //currentScore = p.getFormattedRoundScore(true);
			
			write();
			
			if(spin.isSelected() && bonusWedgeBox.getSelectedIndex() != -1) {
				bW = (BonusWedge) bonusWedgeBox.getSelectedItem();
				
				/* if(wedgeLeft.isSelected()) {
					w.setPosition(Wedge.LEFT);
				} else if(wedgeRight.isSelected()) {
					w.setPosition(Wedge.RIGHT);
				} else  { return; } */
				
				puzzleBoards.offer(BonusPuzzle.GIVEN);	//RSTLNE
				RecordPanel.GAME_TEXT.append("<span>" + abbr + ' ' + spin.getText() + " the " + bW + " and finds that the category is " + CAT_ID + ":</span><br><br><span>" + PB_ID + ++boardCount +
					"</span><br><br>"
				);
				
				spin.setEnabled(false);
				buy.setEnabled(true);
				letterBox.setEnabled(true);
				
				bonusWedgeBox.setVisible(false);
				bonusAmountBox.setVisible(true);
				
				for(BonusAmount bA : EnumSet.complementOf(EnumSet.of(p.hasMDW() ? BonusAmount.$100000 : BonusAmount.ONE_MILLION)))
					bonusAmountBox.addItem(bA);
				
				bonusAmountBox.setSelectedItem(null);
				
				numberOfLetters = p.hasWC() ? 5 : 4;
			} else if(buy.isSelected() && letterBox.getSelectedIndex() != -1) {
				Letter l = (Letter) letterBox.getSelectedItem();
				calledLetters.add(l);
				letterBox.removeItem(l);
				
				boolean done = (++letterCount == numberOfLetters);
				letterString += ((done ? "and " : "") + l + (!done ? ", " : ""));
				
				if(done) {
					EnumSet<Letter> totalLetters = EnumSet.copyOf(calledLetters);
					totalLetters.addAll(BonusPuzzle.GIVEN);
					puzzleBoards.offer(totalLetters);
					RecordPanel.GAME_TEXT.append("<span>" + letterString + " give..." + (boardTwoString = "</span><br><br><span>" + PB_ID + ++boardCount + "</span><br><br>"));
					buy.setEnabled(false);
					solve.setEnabled(true);
					missolve.setEnabled(true);
					buzzed.setEnabled(true);
					letterBox.setEnabled(false);
					solveAttempt.setEnabled(true);
				}
				
				RecordPanel.gameAnalysis.analyzeCall(p, l, null);
				
				letterBox.setSelectedIndex(-1);
				freqBox.setSelectedIndex(letterCount);
			} else if(solve.isSelected() && bonusAmountBox.getSelectedIndex() != -1) {	
				solved = solveAttempt.getText().replaceAll("\n", "<br>");
				if(solved.length() == 0) return;
				
				bA = (BonusAmount) bonusAmountBox.getSelectedItem();
				
				p.winCustomAmount(bA.getAmount());
				RecordPanel.GAME_TEXT.append("<br><span" + (bA.getAmount() >= 100000 ? " class = \"epic\"" : " class = \"solve\"") + "><b>" + name + " solve" + (roundMinimum == 1000 ? "s " : ' ') +
					solved.replaceAll("<br>", " ") + "</b>, and win" + (roundMinimum == 1000 ? "s " : ' ') + (bA != BonusAmount.VEHICLE ? bA : "the " + VEHICLE_ID) +
					"!</b></span><br><br><span><b>GRAND TOTAL: " + p.getFormattedTotalScore(false) + "</b></span><br><br>"
				);
			} else if(missolve.isSelected()) {
				RecordPanel.GAME_TEXT.append("<span class = \"fail\">" + name + ' ' + missolve.getText() + ' ' + solveAttempt.getText() + ".</span><br>");
				RecordPanel.gameAnalysis.analyzeSolveAttempt(p, null);
			} else if(buzzed.isSelected() && bonusAmountBox.getSelectedIndex() != -1) {
				solved = solveAttempt.getText().replaceAll("\n", "<br>");
				if(solved.length() == 0) return;
				
				bA = (BonusAmount) bonusAmountBox.getSelectedItem();
				RecordPanel.GAME_TEXT.append("<span><b>*BZZ BZZ*</b></span><br><br><span" + (bA.getAmount() >= 100000 ? " class = \"fail\"" : "") + ">It was <b>" + solved.replaceAll("<br>", " ") +
					"</b>, and " + name + " lose" + (roundMinimum == 1000 ? "s " : ' ') + (bA != BonusAmount.VEHICLE ? bA : "the " + VEHICLE_ID) + ".</span><br><br>"
				);
			} else return;
			
			if(solve.isSelected() || buzzed.isSelected()) {
				Puzzle puzzle = new BonusPuzzle(solved);
				RecordPanel.gameAnalysis.analyzeSolveAttempt(solve.isSelected() ? p : null, puzzle);
				
				if(RecordPanel.gameAnalysis.areBRLettersNada()) {
					int i = RecordPanel.GAME_TEXT.indexOf(boardTwoString);
					RecordPanel.GAME_TEXT.replace(i, i + boardTwoString.length(), " <span class = \"fail\">NOTHING.</span></span><br><br>");
					puzzleBoards.remove();
				}
				
				writePuzzleBoards(puzzle);
				RecordPanel.gameAnalysis.analyzeBonus(bW, bA);
				bonusAmountBox.setEnabled(false);
				solve.setEnabled(false);
				missolve.setEnabled(false);
				buzzed.setEnabled(false);
				wedgeBox.setEnabled(false);
				solveAttempt.setText("Almost there!");
				solveAttempt.setEnabled(false);
				submit.setEnabled(false);
				for(Letter l : EnumSet.complementOf(calledLetters))
					letterBox.addItem(l);
				
				RecordPanel.UTILITY.endRound(false);
			}
			
			RecordPanel.update();
			TurnChoices.clearSelection();
		}
	}
	
	private class SpecialListener implements ItemListener, Serializable {
		private static final long serialVersionUID = 1L;
		
		public void itemStateChanged(ItemEvent iE) {
			JToggleButton selected = (JToggleButton) iE.getItem();
			
			if(iE.getStateChange() == ItemEvent.SELECTED) {
				if(selected == noMoreVowels) {
					buy.setEnabled(false);
					buy.setSelected(false);
					selected.setEnabled(false);
					String s = noMoreConsonants.isEnabled() ? "NO MORE VOWELS" : "FULLY FILLED IN";
					RecordPanel.GAME_TEXT.append("<span>" + s + ".</span><br>");
					RecordPanel.update();
				} else if(selected == noMoreConsonants) {
					spin.setEnabled(false);
					spin.setSelected(false);
					selected.setEnabled(false);
					String s = noMoreVowels.isEnabled() ? "NO MORE CONSONANTS" : "FULLY FILLED IN";
					RecordPanel.GAME_TEXT.append("<span>" + s + ".</span><br>");
					RecordPanel.update();
				} else if(selected == useWildCard) {
					selected.setEnabled(false);
				} else if(selected == finalSpin) {
					isFinalSpin = true;
					selected.setEnabled(false);
					startFinalSpin();
				}
			}
		}
	}
	
	private class WedgeListener implements ActionListener, Serializable {
		private static final long serialVersionUID = 1L;
		
		public void actionPerformed(ActionEvent aE) {
			Wedge w;
			
			try {
				w = (Wedge) wedgeBox.getSelectedItem();
			} catch (ClassCastException cCE) { return; }
			
			if(w == null)
				return;
			
			if(!isFinalSpin)
				switch(w) {
					case MillionDollarWedge:
						if(wedgeCenter.isSelected()) {
							letterBox.setEnabled(true);
							freqBox.setEnabled(true);
							break;
						}
					case LOSE_A_TURN:
					case BANKRUPT_1:
					case BANKRUPT_2:
						letterBox.setSelectedIndex(-1);
						letterBox.setEnabled(false);
						freqBox.setSelectedIndex(-1);
						freqBox.setEnabled(false);
						break;
					case FreePlay:
						setFreePlayLetters(true);
						letterBox.setEnabled(true);
						freqBox.setEnabled(true);
						break;
					case Purple1000_1:
					case Purple1000_2:
						riskMystery.setEnabled(true);
						return;
					default:
						setFreePlayLetters(false);
						letterBox.setEnabled(true);
						freqBox.setEnabled(true);
				}
			
			riskMystery.setEnabled(false);
		}
		
		private void setFreePlayLetters(boolean b) {
			setLetterChoices(buy.isSelected(), isFreePlay = b);
		}
	}
	
	private class LetterListener implements ActionListener, Serializable {
		private static final long serialVersionUID = 1L;
		
		public void actionPerformed(ActionEvent aE) {
			Letter l;
			
			try {
				l = (Letter) letterBox.getSelectedItem();
			} catch (ClassCastException cCE) { return; }	//occurs when loading the box
			
			if(l == null)
				return;
			
			if(calledLetters.contains(l) || isBonusRound) {
				if(!isBonusRound)
					freqBox.setSelectedIndex(-1);
				freqBox.setEnabled(false);
			} else
				freqBox.setEnabled(true);
		}
	}
	
	private class TossUpListener implements ActionListener, Serializable {
		private static final long serialVersionUID = 1L;
		String previous;
		
		public void actionPerformed(ActionEvent aE) {
			String action = "";
			
			if(solve.isSelected())
				action = solve.getText();
			else if(missolve.isSelected()) {
				action = missolve.getText();
				RecordPanel.gameAnalysis.analyzeSolveAttempt(Player.getCurrentPlayer(), null);
			} else if(buzzed.isSelected()) {
				action = buzzed.getText();
				RecordPanel.gameAnalysis.analyzeBuzz(Player.getCurrentPlayer());
			} else if(!tripleStumper.isSelected())
				return;
			
			//if this is part of a editing reconstruct, reconstruct whose buzzed in
			if(RecordTossInfoPanel.isEditRedo()) {
				String s = TurnPlayer.getText();
				for(Player p : Player.values())
					if(s.equals(p.getAbbrName())) {
						Player.setCurrentPlayer(p);
						break;
					}
			}
				
			write();
				
			if(getPlayerText() == "-") {
				if(tripleStumper.isSelected()) {
					RecordPanel.GAME_TEXT.append("<span>The puzzle was <b>" + solveAttempt.getText() + "</b>.</span><br><br>");
					TossUpPuzzle puzzle = new TossUpPuzzle(solveAttempt.getText());
					puzzle.setTossUpState(solveAttempt.getText());
					RecordPanel.gameAnalysis.analyzeSolveAttempt(null, puzzle);
					submit.setEnabled(false);
					solveAttempt.setEnabled(false);
					solveAttempt.setText(SOLVEATTEMPT_DEFAULT);
					Player.setCurrentPlayer(Player.RED);
					RecordPanel.TOSS_INFO.resetTossUp();
					RecordPanel.update();
					RecordPanel.UTILITY.endRound(false);
				}
				return;
			}
			
			RecordPanel.GAME_TEXT.append((!puzzleBoard.getText().equals(previous) ? "<span>" + puzzleBoard.getText().replaceAll("\n", "<br>") + "</span><br><br>" : "") +
				"<span class = \"" + (solve.isSelected() ? "solve" : "fail") + "\">" + Player.getCurrentPlayer().getAbbrName() + ' ' + action + (buzzed.isSelected() ? '.' : ' ' +
				solveAttempt.getText().replaceAll("<br>", " ") + '.') + "</span><br><br>"
			);
			
			previous = puzzleBoard.getText();
			
			if(solve.isSelected()) {
				solve.setEnabled(false);
				missolve.setEnabled(false);
				buzzed.setEnabled(false);
				submit.setEnabled(false);
				puzzleBoard.setEnabled(false);
				solveAttempt.setEnabled(false);
				tripleStumper.setEnabled(false);
				
				TossUpPuzzle puzzle = new TossUpPuzzle(solveAttempt.getText());
				puzzle.setTossUpState(puzzleBoard.getText());
				RecordPanel.gameAnalysis.analyzeSolveAttempt(Player.getCurrentPlayer(), puzzle);
				
				puzzleBoard.setText(PUZZLEBOARD_DEFAULT);
				solveAttempt.setText(SOLVEATTEMPT_DEFAULT);
				Player.RED.setFailTossUp(false);
				Player.YELLOW.setFailTossUp(false);
				Player.BLUE.setFailTossUp(false);
				
				if(!RecordPanel.UTILITY.isTied())
					Player.getCurrentPlayer().winTossUp();
				RecordPanel.TOSS_INFO.resetTossUp();
				RecordPanel.TOSS_INFO.updateBanks();
				RecordPanel.UTILITY.setTied(false);
				RecordPanel.UTILITY.endRound(false);
			} else {
				Player.getCurrentPlayer().setFailTossUp(true);
				
				if(tripleStumper.isSelected() || isTripleStumper()) {
					tripleStumper.setSelected(true);
					solve.setEnabled(false);
					missolve.setEnabled(false);
					buzzed.setEnabled(false);
					tripleStumper.setEnabled(false);
					puzzleBoard.setEnabled(false);
					puzzleBoard.setText("Not needed anymore, put the full answer on the right.");
					
					RecordPanel.GAME_TEXT.append("<span class = \"fail\">Triple stumper!</span> ");
				}
			}
			
			RecordPanel.update();
			resetPlayerText();
			TurnChoices.clearSelection();
		}
	}
	
	private class TurnListener implements ActionListener, Serializable {
		private static final long serialVersionUID = 1L;
		
		public void actionPerformed (ActionEvent aE) {
			Object turn = aE.getSource();
			
			if(!isFinalSpin) {
				boolean spinEnable = turn == spin || Wedge.FreePlay == wedgeBox.getSelectedItem();
				wedgeBox.setEnabled(spinEnable);
				wedgeLeft.setEnabled(spinEnable);
				wedgeCenter.setEnabled(spinEnable);
				wedgeRight.setEnabled(spinEnable);
				overRotation.setEnabled(turn == spin);
				letterBox.setEnabled(turn == spin || turn == buy);
				freqBox.setEnabled(turn == spin || turn == buy);
			}
			
			if(turn != spin && !isFinalSpin) {
				wedgeBox.setSelectedItem(null);
				WedgePosition.clearSelection();
			}
			
			if(turn == spin && !isBonusRound)
				setLetterChoices(false, isFreePlay);
			else if (turn == buy)
				setLetterChoices(true, isFinalSpin);	//this is a shortcut way to activate Free-Play like situation when it comes time. In Maingame of course it is (true, false).
		}
	}
	
	private void setLetterChoices(boolean useVowel, boolean useFreePlay) {
		letterBox.removeAllItems();
		for(Letter l : (useFreePlay ? !isBonusRound ? Arrays.asList(Letter.values()) : BonusPuzzle.NOT_GIVEN : useVowel ? Letter.VOWELS : Letter.CONSONANTS))
			letterBox.addItem(l);
		if(isBonusRound)
			for(Letter l : calledLetters)
				letterBox.removeItem(l);
		letterBox.setSelectedItem(null);
	}
	
	private void setSpinChoices(int location) {
		wedgeBox.removeAllItems();
		java.util.List<Wedge> activeWedges = new LinkedList<>(Wedge.getRoundConfiguration());	//whoever decided a awt element should be named List as well is silly
		Collections.rotate(activeWedges, -(location < 36 ? location : location-72)/3);	//this makes the worst case n/2 (with n = 72) instead of n, since rotation can be done both ways. Still O(n)
		
		for(Wedge w : activeWedges)
			wedgeBox.addItem(w);
		
		wedgeBox.setSelectedItem(null);
	}
	
	public void startMainRound() {
		spin.setEnabled(true);
		buy.setEnabled(true);
		solve.setEnabled(true);
		missolve.setEnabled(true);
		buzzed.setEnabled(true);
		wedgeBox.setEnabled(true);
		wedgeLeft.setEnabled(true);
		wedgeCenter.setEnabled(true);
		wedgeRight.setEnabled(true);
		overRotation.setEnabled(true);
		letterBox.setEnabled(true);
		freqBox.setEnabled(true);
		submit.setEnabled(true);
		puzzleBoard.setEnabled(false);
		puzzleBoard.setText("Taken care of at the end of the round!");
		solveAttempt.setEnabled(true);
		tripleStumper.setVisible(false);
		noMoreVowels.setEnabled(true);
		noMoreConsonants.setEnabled(true);
		useWildCard.setEnabled(false);
		
		boardCount = 0;
		calledLetters.clear();	//always reset
		
		Player.setRoundActive(true);
		Wedge.setRoundConfiguration(++RecordPanel.roundNumber);
		Player.setRoundStarter();
		
		jackpotCounter.setVisible(RecordPanel.roundNumber == 1);
		defaultRound.setVisible(RecordPanel.roundNumber == (Wedge.MYSTERY_ROUND == 3 ? 2 : 3));
		riskMystery.setVisible(RecordPanel.roundNumber == Wedge.MYSTERY_ROUND);
		finalSpin.setVisible(RecordPanel.roundNumber >= 4);
		//prizePuzzle.setVisible(RecordPanel.roundNumber == 2 || RecordPanel.roundNumber == 3);	//PP is not R1 effective S28
		//prizePuzzle.setEnabled(!donePrizePuzzle);
		prizePuzzle.setSelected(RecordPanel.roundNumber == 3);
		
		if(RecordPanel.roundNumber == 3)
			if(Wedge.PrizeOnWheel.isActive())
				Wedge.replaceWedge(Wedge.PrizeOnWheel);	//a change starting on 11/1/11
		
		if(RecordPanel.roundNumber == 4)
			for(Wedge w : EnumSet.of(/*Wedge.PrizeOnWheel,*/ Wedge.WildCard, Wedge.GiftTag, Wedge.MillionDollarWedge))
				if(w.isActive())
					Wedge.replaceWedge(w);
		
		//resetting prev for R1-4.
		if(RecordPanel.roundNumber < 5) {
			if(Player.getCurrentPlayer() == Player.RED) {
				prev = Wedge.Orange300;
				prev.setPosition(Wedge.LEFT);
			} else if(Player.getCurrentPlayer() == Player.YELLOW) {
				prev = Wedge.Turquoise2500;		//this is only for determining spin strength, so we can substitute in later rounds
				prev.setPosition(Wedge.CENTER);
			} else {
				prev = Wedge.Yellow500;
				prev.setPosition(Wedge.RIGHT);
			}
		}
		
		setSpinChoices(prev.getLocation());
		
		resetListener(submit, new RoundListener());
		
		setPlayerText();
		RecordPanel.GAME_TEXT.append("<span><b>" + CAT_ID + " R" + RecordPanel.roundNumber + "</b></span><br><br>");
		RecordPanel.update();
	}
	
	private void startFinalSpin() {
		spin.setSelected(true);
		spin.setText("spins");
		buy.setEnabled(false);
		buy.setText("call" + (roundMinimum == 1000 ? 's' : ""));	//alternate way to determine isCouples()
		solve.setEnabled(false);
		missolve.setEnabled(false);
		buzzed.setEnabled(false);
		wedgeBox.setEnabled(true);
		wedgeLeft.setEnabled(true);
		wedgeCenter.setEnabled(true);
		wedgeRight.setEnabled(true);
		overRotation.setEnabled(true);
		letterBox.setEnabled(false);
		freqBox.setEnabled(false);
		submit.setEnabled(true);
		solveAttempt.setEnabled(false);
		noMoreVowels.setEnabled(false);
		noMoreConsonants.setEnabled(false);
		useWildCard.setEnabled(false);
		
		RecordPanel.GAME_TEXT.append("<span><b>*DING DING* *DING DING*</b>");
		if(!calledLetters.isEmpty()) {
			puzzleBoards.offer(EnumSet.copyOf(calledLetters));
			RecordPanel.GAME_TEXT.append("<br>Puzzle at this point: <br><br>" + PB_ID + ++boardCount);
		}
		RecordPanel.GAME_TEXT.append("</span><br><br>");
		
		resetListener(submit, new FinalListener());
		
		RecordPanel.update();
		TurnPlayer.setText("Pat");
		Wedge.adjustPrevWedge(prev, Player.getCurrentPlayer());
		setSpinChoices(prev.getLocation());
	}
	
	public void startBonusRound() {
		spin.setEnabled(true);
		spin.setText("spin" + (roundMinimum == 1000 ? 's' : ""));	//alternate way to determine isCouples()
		spin.setSelected(true);
		buy.setEnabled(false);
		solve.setEnabled(false);
		missolve.setEnabled(false);
		buzzed.setEnabled(false);
		wedgeBox.setEnabled(true);
		wedgeLeft.setEnabled(false);	//for now
		wedgeCenter.setEnabled(false);
		wedgeRight.setEnabled(false);	//for now
		overRotation.setEnabled(false);
		letterBox.setEnabled(false);
		freqBox.setEnabled(false);
		freqBox.setSelectedIndex(0);
		submit.setEnabled(true);
		puzzleBoard.setEnabled(false);
		puzzleBoard.setText("Taken care of at the end of the round!");
		solveAttempt.setEnabled(true);
		SpecialRowPanel.setVisible(false);
		
		calledLetters.clear();
		boardCount = 0;
		
		boolean b = Player.getCurrentPlayer().hasMDW();
		RecordPanel.GAME_TEXT.append("<span" + (b ? " class = \"epic\"" : "") + "><b>$1" + (b ? ",0" : "") + "00,000 BONUS ROUND</b></span><br><br>");
		
		wedgeBox.setVisible(false);
		bonusWedgeBox.setVisible(true);
		bonusWedgeBox.setEnabled(true);
		bonusWedgeBox.setSelectedItem(null);
		for(Letter l : BonusPuzzle.GIVEN)
			letterBox.removeItem(l);
		
		resetListener(submit, new BonusListener());
		
		RecordPanel.update();
		setPlayerText();
		isBonusRound = true;
	}
	
	public void startTossUp() {
		spin.setEnabled(false);
		buy.setEnabled(false);
		solve.setEnabled(true);
		missolve.setEnabled(true);
		buzzed.setEnabled(true);
		wedgeBox.setEnabled(false);
		wedgeLeft.setEnabled(false);
		wedgeCenter.setEnabled(false);
		wedgeRight.setEnabled(false);
		overRotation.setEnabled(false);
		letterBox.setEnabled(false);
		freqBox.setEnabled(false);
		submit.setEnabled(true);
		puzzleBoard.setEnabled(true);
		puzzleBoard.setText(PUZZLEBOARD_DEFAULT);
		solveAttempt.setEnabled(true);
		solveAttempt.setText(SOLVEATTEMPT_DEFAULT);
		noMoreVowels.setEnabled(false);
		noMoreConsonants.setEnabled(false);
		useWildCard.setEnabled(false);
		prizePuzzle.setEnabled(false);
		riskMystery.setVisible(false);
		defaultRound.setVisible(false);
		tripleStumper.setVisible(true);
		tripleStumper.setEnabled(true);
		
		if(bonusWedgeBox.isVisible() || bonusAmountBox.isVisible()) {	//resetting the game if this is the case.
			wedgeBox.setVisible(true);
			bonusWedgeBox.setVisible(false);
			bonusAmountBox.setVisible(false);
		}
		
		resetListener(submit, new TossUpListener());
		
		RecordPanel.GAME_TEXT.append("<span><b>" + CAT_ID + " " + (!RecordUtilityPanel.isTied() ?  (++RecordPanel.tossUpNumber) + "k" : "TIEBREAKER") + "</b></span><br><br>");
		RecordPanel.update();
		resetPlayerText();
	}
	
	private static void resetListener(AbstractButton aB, ActionListener ... aLs) {	//this utility class is very useful
		for(ActionListener listener : aB.getActionListeners())
			aB.removeActionListener(listener);
		for(ActionListener aL : aLs)
			aB.addActionListener(aL);
	}
	
	private static boolean isTripleStumper() {
		return (Player.RED.didFailTossUp() && Player.YELLOW.didFailTossUp() && Player.BLUE.didFailTossUp());
	}
	
	public static boolean isFinalSpin() {
		return isFinalSpin;
	}
	
	public static boolean isCarWon() {
		return isCarWon;
	}
	
	public void resetPlayerText() {
		TurnPlayer.setText("-");
	}
	
	public void setCouples() {
		spin.setText("spin");
		buy.setText("buy");
		solve.setText("solve");
		missolve.setText("missolve");
		roundMinimum = 2000;
	}
	
	public static void setPrev(Wedge w) {
		prev = w;
	}
	
	public static Wedge getPrev() {
		return prev;
	}
	
	public static int getPrevPosition() {
		return prevPosition;
	}
	
	public static int getRoundMinimum() {
		return roundMinimum;
	}
	
	public void setPlayerText() {
		//if this is part of a editing reconstruct
		if(Player.getCurrentPlayer() == null) {
			String s = TurnPlayer.getText();
			for(Player p : Player.values())
				if(s.equals(p.getAbbrName())) {
					Player.setCurrentPlayer(p);
					break;
				}
		}
		TurnPlayer.setText(Player.getCurrentPlayer().getAbbrName());
	}
	
	public String getPlayerText() {
		return TurnPlayer.getText();
	}
	
	public static EnumSet<Letter> getCalledLetters() {
		return calledLetters;
	}
	
	public static void setPrizeOnWheel(String description, int amount) {
		prizeOnWheelDescription = description;
		prizeOnWheelAmount = amount;
	}
	
	public static void resetGame() {
		jackpot = 5000;
		roundMinimum = 1000;
	}
	
	public Insets getInsets() {
		return new Insets(10,10,10,10);
	}
}