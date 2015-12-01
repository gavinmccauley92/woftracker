package woftracker.record;

import woftracker.*;
import java.awt.*;;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import java.text.NumberFormat;
import java.util.*;
import java.io.Serializable;

public class RecordUtilityPanel extends EditablePanel implements Recordable {
	private JComboBox<Category> catBox;
	private JCheckBox plural, isTrivia;
	private JTextPane notes;
	private JTextField customDescription, customAmount;
	private JLabel customText;
	
	private JPanel CustomPanel;
	
	private static boolean isMainGame, isFinalSpin = false, isCar = false, isPrizePuzzle = false, stillTied = false, redBelowMinimum = false, yellowBelowMinimum = false, blueBelowMinimum = false;
	private static String grandTotal = "";
	
	private static EnumSet<Player> winner = EnumSet.noneOf(Player.class);
	private static final String[] CUSTOM_OPTIONS = { "------------------", "Prize on Wheel", "Prize Puzzle", "Car", "PP & Car" };
	
	private static NumberFormat nF = NumberFormat.getCurrencyInstance(Locale.US);
	
	RecordUtilityPanel() {
		CustomPanel = new JPanel();
		CustomPanel.setOpaque(false);
		CustomPanel.setLayout(new BoxLayout(CustomPanel, BoxLayout.PAGE_AXIS));
		customText = new JLabel(CUSTOM_OPTIONS[0]);
		customText.setAlignmentX(Component.CENTER_ALIGNMENT);
		customText.setToolTipText("This label will be set to: " +
			"(1) Prize on Wheel, After 2k tossup; " +
			"(2) Prize Puzzle Prize, After the prize puzzle (R2 or 3), use the prize puzzle button; " +
			"(3) Car, if either the 1/2 Car or BR car is won; " +
			"(4) Prize Puzzle & Car, if (2) and (3) happen at the same time with 1/2 Car. Use \" / \" between descriptions and any whitespace between amounts."
		);
		CustomPanel.add(Box.createVerticalGlue());
		CustomPanel.add(customText);
		CustomPanel.add(Box.createVerticalStrut(10));
		customDescription = new JTextField();
		customDescription.setPreferredSize(new Dimension(100, 30));
		customDescription.setMaximumSize(new Dimension(100, 30));
		customDescription.setToolTipText("Input a concise string describing the prize selected.");
		customDescription.setEnabled(false);
		CustomPanel.add(customDescription);
		CustomPanel.add(Box.createVerticalStrut(10));
		customAmount = new JTextField();
		customAmount.setPreferredSize(new Dimension(50, 30));
		customAmount.setMaximumSize(new Dimension(50, 30));
		customAmount.setToolTipText("Price of the prize. All characters in this input must be digits. For Gift Tag, disabled as it is always $1000.");
		customAmount.setEnabled(false);
		CustomPanel.add(customAmount);
		CustomPanel.add(Box.createVerticalGlue());
		
		notes = new JTextPane();
		notes.getDocument().putProperty(DefaultEditorKit.EndOfLineStringProperty, "<br>");
		notes.setPreferredSize(new Dimension(650, 90));
		notes.setMaximumSize(new Dimension(650, 90));
		notes.setToolTipText("Input any notes on the round here.");
		notes.setEnabled(false);
		
		catBox = new JComboBox<Category>(Category.values());
		catBox.setEnabled(false);
		catBox.setSelectedItem(null);
		catBox.setMaximumSize(new Dimension(140, 30));
		catBox.addActionListener(new TriviaListener());
		
		plural = new JCheckBox("P");
		plural.setOpaque(false);
		plural.setToolTipText("Check if the category is plural.");
		plural.setEnabled(false);
		plural.addItemListener(new CatListener());
		
		isTrivia = new JCheckBox("T");
		isTrivia.setOpaque(false);
		isTrivia.setToolTipText("Check if the applicable category had a isTrivia bonus.");
		isTrivia.setEnabled(false);
		isTrivia.setVisible(false);
		
		submit = new JButton("Next Round");
		submit.setEnabled(false);
		submit.addActionListener(new NoteListener());
		
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		
		setOpaque(false);
		add(Box.createHorizontalStrut(10));
		add(Box.createHorizontalGlue());
		add(CustomPanel);
		add(Box.createHorizontalStrut(20));
		add(notes);
		add(Box.createHorizontalStrut(20));
		add(catBox);
		add(Box.createHorizontalStrut(5));
		add(plural);
		add(Box.createHorizontalStrut(5));
		add(isTrivia);
		add(Box.createHorizontalStrut(20));
		add(submit);
		add(Box.createHorizontalStrut(10));
	}
	
	private class CatListener implements ItemListener, Serializable {
		private static final long serialVersionUID = 1L;
		
		public void itemStateChanged(ItemEvent e) {
			Category.setPlural(plural.isSelected());
		}
	}
	
	private class TriviaListener implements ActionListener, Serializable {
		private static final long serialVersionUID = 1L;
		
		public void actionPerformed(ActionEvent e) {
			Category c = (Category) catBox.getSelectedItem();
			
			if(c != null) {
				isTrivia.setVisible(Category.TRIVIA.contains(c));
				isTrivia.setSelected(c == Category.WHATS_THAT_SONG);
			}
		}
	}
	
	public void endRound(boolean b) {
		catBox.setEnabled(true);
		plural.setEnabled(true);
		plural.setSelected(true);	//this will generate loading the box above :)
		plural.setSelected(false);
		isTrivia.setEnabled(true);
		notes.setEnabled(true);
		submit.setEnabled(true);
		isMainGame = b;
		isFinalSpin = RecordGamePanel.isFinalSpin();
		
		isCar = RecordPanel.GAME_TEXT.indexOf(VEHICLE_ID) != -1;
		isPrizePuzzle = RecordPanel.GAME_TEXT.indexOf(PP_ID) != -1;
		
		if(isMainGame || isFinalSpin) {	//in the BR, override isTossUp. a bit messy and confusing :(
			if(isPrizePuzzle) {
				customText.setText(CUSTOM_OPTIONS[isCar ? 4 : 2]);
				customDescription.setEnabled(true);
				customAmount.setEnabled(true);
			} else if(isCar) {
				customText.setText(CUSTOM_OPTIONS[3]);
				customDescription.setEnabled(true);
				if(RecordPanel.GAME_TEXT.indexOf("*BZZ BZZ*") == -1)
					customAmount.setEnabled(true);
				else
					customAmount.setText("0");
			}
		} else {
			if(RecordPanel.tossUpNumber == 2) {
				customText.setText(CUSTOM_OPTIONS[1]);
				customDescription.setEnabled(true);
				customAmount.setEnabled(true);
			}
		}
	}
	
	private class NoteListener implements ActionListener, Serializable {
		private static final long serialVersionUID = 1L;
		
		public void actionPerformed(ActionEvent aE) {
			int amount = 0, amount2 = 0;
			boolean isPP = isPrizePuzzle, isSingleCustom = isPrizePuzzle ^ isCar, isDoubleCustom = isPP && isCar;
			
			if(customAmount.isEnabled()) {
				try {
					if(isDoubleCustom) {
						Scanner scan = new Scanner(customAmount.getText());
						amount = scan.nextInt();
						amount2 = scan.nextInt();
						if(amount2 <= 0)
							return;
					} else
						amount = Integer.parseInt(customAmount.getText());
				} catch (NumberFormatException nFE) {
					JOptionPane.showMessageDialog(MainWindow.getWindow(), "Prize amount is not a number.", "Error in custom prize area", JOptionPane.ERROR_MESSAGE);
					return;
				} catch(InputMismatchException e) {
					JOptionPane.showMessageDialog(MainWindow.getWindow(), "One of the prize amounts is not a number.", "Error in custom prize area", JOptionPane.ERROR_MESSAGE);
					return;
				} catch(NoSuchElementException e) {
					JOptionPane.showMessageDialog(MainWindow.getWindow(), "Less than two prize amounts.", "Error in custom prize area", JOptionPane.ERROR_MESSAGE);
				}
				
				if(amount <= 0)
					return;
			}
			Category c;
			String cat, description, description2 = null;
			int indexC = RecordPanel.GAME_TEXT.indexOf(CAT_ID);
			
			if(catBox.getSelectedItem() == null) {
				JOptionPane.showMessageDialog(MainWindow.getWindow(), "Category left blank.", "Error in utility area", JOptionPane.ERROR_MESSAGE);
				return;
			} else {
				c = (Category) catBox.getSelectedItem();
				cat = c.toString();
				if(isDoubleCustom) {
					try {
						Scanner scan = new Scanner(customDescription.getText());
						scan.useDelimiter(" / ");
						description = scan.next();
						description2 = scan.next();
						if(description2.length() == 0)
							return;
					} catch(InputMismatchException e) {
						JOptionPane.showMessageDialog(MainWindow.getWindow(), "Report to Wayo: " + e, "Error in custom prize area", JOptionPane.ERROR_MESSAGE);
						return;
					} catch(NoSuchElementException e) {
						JOptionPane.showMessageDialog(MainWindow.getWindow(), "Less than two descriptions. Separate with \" / \".", "Error in custom prize area", JOptionPane.ERROR_MESSAGE);
						return;
					}
				} else
					description = customDescription.getText();
				
				if(customDescription.isEnabled() && (description.length() == 0 || (isDoubleCustom && description2.length() == 0))) {
					JOptionPane.showMessageDialog(MainWindow.getWindow(), "Description(s) are empty.", "Error in custom prize area", JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
			
			write();
			
			RecordPanel.GAME_TEXT.replace(indexC, indexC + CAT_ID.length(), cat /*+ (isPrizePuzzle ? " (PRIZE)" : "")*/);	//PP is always R3 as of early S29
			
			if(Category.TRIVIA.contains(c) && isTrivia.isSelected()) {
				TriviaBonus bonus = new TriviaBonus();
				
				String guess = bonus.getGuessText(), answer = bonus.getAnswerText();
				boolean right = answer.length() == 0;
				
				RecordPanel.GAME_TEXT.append("<span><b>TRIVIA BONUS:</b> \"" + guess + "\" is <b>" + (right ? "" : "in") + "correct</b>" + (right ? " for $3000 more" : ". It was \"" + answer + '\"') +
					".</span><br><br>"
				);
				
				if(right)
					Player.getCurrentPlayer().winCustomAmount(3000);
					
				RecordPanel.gameAnalysis.analyzeTrivia(c, right);
			}
			
			if(notes.getText().length() != 0)
				RecordPanel.GAME_TEXT.append("<span><b>Notes:</b><br>" + notes.getText() + "</span><br><br>");
			
			if(isMainGame) {
				if(isSingleCustom || isDoubleCustom)
					writeMainGameCustom(description, amount, isPP ? PP_ID : VEHICLE_ID);
				if(isDoubleCustom)
					writeMainGameCustom(description2, amount2, VEHICLE_ID);
				
				
				String red = Player.RED.getName() + ": " + Player.RED.getFormattedTotalScore(true),
					yellow = Player.YELLOW.getName() + ": " + Player.YELLOW.getFormattedTotalScore(true),
					blue = Player.BLUE.getName() + ": " + Player.BLUE.getFormattedTotalScore(true);
				
				if(isFinalSpin) {
					int count = determineWinner(Player.RED.getTotalScore(), Player.YELLOW.getTotalScore(), Player.BLUE.getTotalScore());
					
					redBelowMinimum = Player.RED.checkTotalMinimum();
					yellowBelowMinimum = Player.YELLOW.checkTotalMinimum();
					blueBelowMinimum = Player.BLUE.checkTotalMinimum();
					
					//activate convolution!!
					if(redBelowMinimum)
						red = red.substring(0, red.indexOf('$') + 1) + Player.RED.getPreMinimumScore() + " --> " + Player.RED.getFormattedTotalScore(true);
					if(yellowBelowMinimum)
						yellow = yellow.substring(0, yellow.indexOf('$') + 1) + Player.YELLOW.getPreMinimumScore() + " --> " + Player.YELLOW.getFormattedTotalScore(true);
					if(blueBelowMinimum)
						blue = blue.substring(0, blue.indexOf('$') + 1) + Player.BLUE.getPreMinimumScore() + " --> " + Player.BLUE.getFormattedTotalScore(true);
					
					if(count == 1) {
						Player p;
						
						if(winner.contains(Player.RED)) {
							red = "<b>" + red + "</b>";
							p = Player.RED;
						} else if(winner.contains(Player.YELLOW)) {
							yellow = "<b>" + yellow + "</b>";
							p = Player.YELLOW;
						} else {
							blue = "<b>" + blue + "</b>";
							p = Player.BLUE;
						}
						
						Player.setCurrentPlayer(p);
					} else
						stillTied = true;	//still a lot of work to do on this
					
					grandTotal = nF.format(Player.RED.getTotalScore() + Player.YELLOW.getTotalScore() + Player.BLUE.getTotalScore());	//regardless of tie (see 2003 clip), we can do this now
				}
				
				RecordPanel.GAME_TEXT.append("<span><i>" + red + " / " + yellow + " / " + blue + "</i></span><br>" +
					(isFinalSpin ? "<span><i><b>Total:</b> " + grandTotal.substring(0, grandTotal.length() - 3) + "</i></span><br>" : "")
					+ "<br><br>"
				);
				
				if(isFinalSpin && !stillTied) {
					RecordPanel.TOSS_INFO.setBonusBanks();
					RecordPanel.GAME.startBonusRound();
				} else if(RecordPanel.roundNumber == 3 || stillTied) {
					RecordPanel.TOSS_INFO.startTossUp();
					RecordPanel.GAME.startTossUp();
				} else {
					RecordPanel.GAME.startMainRound();
				}
			} else if(isFinalSpin) {	//this will be the bonus round always, because isMainGame will take precedence above
				if(isCar) {
					int indexP = RecordPanel.GAME_TEXT.indexOf(VEHICLE_ID);
					
					if(RecordPanel.GAME_TEXT.indexOf("*BZZ BZZ*") != -1)
						RecordPanel.GAME_TEXT.replace(indexP, indexP + VEHICLE_ID.length(), description);
					else {
						RecordPanel.GAME_TEXT.replace(indexP, indexP + VEHICLE_ID.length(), description + " valued at $" + amount + ", along with $5000 in cash");
						
						String totalID = "GRAND TOTAL: $";
						int indexT = RecordPanel.GAME_TEXT.indexOf(totalID);
						int indexBracket = RecordPanel.GAME_TEXT.indexOf("<", indexT);	//</b></span>
						
						int indexOS = indexT + totalID.length();
						String oldScore = RecordPanel.GAME_TEXT.substring(indexOS, indexBracket);
						String newScore = nF.format(Integer.parseInt(oldScore.replace(",", "")) + amount + 5000);
						newScore = newScore.substring(0, newScore.length() - 3);
						
						RecordPanel.GAME_TEXT.replace(indexOS - 1, indexBracket, newScore);
						
						Player.getCurrentPlayer().winCustomAmount(amount + 5000);	//nice and easy update 9/9/10
					}
				}
			} else {
				switch(RecordPanel.tossUpNumber) {
					case 1:
						RecordPanel.TOSS_INFO.allowInfo();
						break;
					case 2:
						RecordGamePanel.setPrizeOnWheel(description, amount);
						RecordPanel.GAME_TEXT.append("<span><b>PRIZE ON WHEEL:</b> " + description + " valued at $" + amount + "</span><br><br><br>");
					case 3:
						RecordPanel.TOSS_INFO.resetTossUp();
						Player.setRoundPattern(Player.getCurrentPlayer());
						RecordPanel.GAME.startMainRound();
				}
			}
			
			Category.setPlural(false);	//if BR is plural, don't pluralize all categories in list 3/26/12
			
			if(isSingleCustom && isCar)
				RecordPanel.gameAnalysis.analyzeRound(c, 0, amount);
			else
				RecordPanel.gameAnalysis.analyzeRound(c, amount, amount2);
			
			if(isMainGame && (RecordPanel.tossUpNumber == 3 || isCar)) {
				Player.resetHalfCars();
				
				//following: correct 1/2 Car or Car on score line after analyzing.
				final String LEFTOVER = " + 1/2 Car", LEFTOVER2 = " + Car", BLANK = "";
				int index;
				final int START_INDEX = RecordPanel.GAME_TEXT.lastIndexOf("<i>"), LENGTH = LEFTOVER.length(), LENGTH2 = LEFTOVER2.length();	//italics are only used on score line
				
				while((index = RecordPanel.GAME_TEXT.indexOf(LEFTOVER, START_INDEX)) != -1)
					RecordPanel.GAME_TEXT.replace(index, index + LENGTH, BLANK);
				if((index = RecordPanel.GAME_TEXT.indexOf(LEFTOVER2, START_INDEX)) != -1)	//can only be one full Car.
					RecordPanel.GAME_TEXT.replace(index, index + LENGTH2, BLANK);
			}
			
			customText.setText(CUSTOM_OPTIONS[0]);
			customDescription.setEnabled(false);
			customDescription.setText("");
			customAmount.setEnabled(false);
			customAmount.setText("");
			catBox.setSelectedItem(null);
			catBox.setEnabled(false);
			plural.setEnabled(false);
			isTrivia.setEnabled(false);
			isTrivia.setVisible(false);
			notes.setText("");
			notes.setEnabled(false);
			submit.setEnabled(false);
			RecordPanel.TOSS_INFO.updateBanks();
			RecordPanel.update();
		}
	}
	
	private void writeMainGameCustom(String description, int amount, String id) {
		int indexP = RecordPanel.GAME_TEXT.indexOf(id);
		RecordPanel.GAME_TEXT.replace(indexP, indexP + id.length(), description + (amount != 0 ? " valued at $" + amount : ""));
		
		if(amount != 0) {
			String totalID = "a total of $";
			int indexT = RecordPanel.GAME_TEXT.indexOf(totalID, RecordPanel.GAME_TEXT.indexOf("R" + RecordPanel.roundNumber));	//only need to start looking at current round
			int indexPeriod = RecordPanel.GAME_TEXT.indexOf(".", indexT);
			
			int indexOS = indexT + totalID.length();
			String oldScore = RecordPanel.GAME_TEXT.substring(indexOS, indexPeriod);
			String newScore = nF.format(Integer.parseInt(oldScore.replace(",", "")) + amount);
			newScore = newScore.substring(0, newScore.length() - 3);
			
			RecordPanel.GAME_TEXT.replace(indexOS - 1, indexPeriod, newScore);
			
			Player.getCurrentPlayer().winCustomAmount(amount);
		}
	}
	
	private class TriviaBonus extends JDialog implements ActionListener, ItemListener {
		JPanel TBPanel;
		JTextField guess, answer;
		JToggleButton result;
		JButton submit;
		
		String guessText = "", answerText = "";
		
		TriviaBonus() {
			super(MainWindow.getWindow(), "Trivia Bonus", true);	//force modal for now, as well as only one frame
			
			guess = new JTextField();
			guess.setOpaque(false);
			answer = new JTextField();
			answer.setOpaque(false);
			result = new JToggleButton("Wrong");
			result.setToolTipText("\"Push\" this toggle button in if the contestant attempted the bonus question successfully.");
			result.addItemListener(this);
			submit = new JButton("Submit");
			submit.setToolTipText("Must press this button to continue on. All visible text fields above MUST be filled in to do so.");
			submit.addActionListener(this);
			
			TBPanel = new JPanel();
			TBPanel.setLayout(new BoxLayout(TBPanel, BoxLayout.PAGE_AXIS));
			TBPanel.setOpaque(false);
			TBPanel.add(guess);
			TBPanel.add(Box.createVerticalStrut(10));
			TBPanel.add(result);
			TBPanel.add(Box.createVerticalStrut(10));
			TBPanel.add(answer);
			TBPanel.add(Box.createVerticalStrut(20));
			TBPanel.add(submit);
			
			add(TBPanel);
			pack();
			setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			setLocationRelativeTo(MainWindow.getWindow());
			setVisible(true);
		}
		
		public void itemStateChanged(ItemEvent iE) {
			boolean right = result.isSelected();
			result.setText(right ? "Right" : "Wrong");
			answer.setVisible(!right);
			if(!right)
				answer.setText("");
		}
		
		public void actionPerformed(ActionEvent aE) {
			String guessText = guess.getText(), answerText = answer.getText();
			
			if(guessText.length() == 0 || (!result.isSelected() && answerText.length() == 0))
				return;
			else {
				this.guessText = guessText;
				this.answerText = answerText;
				
				guess.setText("");
				answer.setText("");
				this.setVisible(false);
			}
		}
		
		public String getGuessText() {
			return guessText;
		}
		
		public String getAnswerText() {
			return answerText;
		}
	}
	
	//with just 3 integers, there's no need to do anything costly with TreeSet
	private int determineWinner(int red, int yellow, int blue) {
		winner.clear();
		
		//one and only one of the following must be true.
		if(red > yellow && red > blue)
			winner.add(Player.RED);
		else if(yellow > red && yellow > blue)
			winner.add(Player.YELLOW);
		else if(blue > red && blue > yellow)
			winner.add(Player.BLUE);
		else if(red == yellow) {
			winner.add(Player.RED);
			winner.add(Player.YELLOW);
			if(yellow == blue)
				winner.add(Player.BLUE);
		} else if(red == blue) {
			winner.add(Player.RED);
			winner.add(Player.BLUE);
		} else {
			winner.add(Player.YELLOW);
			winner.add(Player.BLUE);
		}
		
		return winner.size();
	}
	
	public static void setTied(boolean b) {
		stillTied = b;
	}
	
	public static boolean isTied() {
		return stillTied;
	}
	
	public static EnumSet<Player> getWinners() {
		return winner;	//used exclusively in a tie only.
	}
	
	public Insets getInsets() {
		return new Insets(10,10,10,10);
	}
}