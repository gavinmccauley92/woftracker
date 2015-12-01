package woftracker.record;

import woftracker.*;
import java.awt.*;;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.io.*;
import java.io.Serializable;

public class RecordTossInfoPanel extends EditablePanel implements Recordable, ItemListener {
	private JPanel RowOnePanel, RowTwoPanel, RedPanel, YellowPanel, BluePanel, RowThreePanel;
	private GenderPanel redGender, yellowGender, blueGender;
	private JTextField showNumber, seasonNumber, location, theme, date, redName, yellowName, blueName;
	private JButton redTossUp, yellowTossUp, blueTossUp;
	private JLabel redBank, redScore, yellowBank, yellowScore, blueBank, blueScore;
	private JCheckBox couples;
	
	private final Font PLAYER_FONT = new Font("Verdana", Font.BOLD, 14);
	private static boolean tossUpButtonPressed = false;
	
	RecordTossInfoPanel() {
	
		RowOnePanel = new JPanel();
		showNumber = new JTextField("####");
		showNumber.setMaximumSize(new Dimension(50, 30));
		seasonNumber = new JTextField("###");
		seasonNumber.setMaximumSize(new Dimension(40, 30));
		location = new JTextField("Culver City");
		location.setMaximumSize(new Dimension(150, 30));
		theme = new JTextField();
		theme.setPreferredSize(new Dimension(300, 30));
		theme.setMaximumSize(new Dimension(300, 30));
		date = new JTextField();
		date.setMaximumSize(new Dimension(60, 30));
		
		
		try {
			Scanner recentShow = new Scanner(new File("woftracker\\record\\recentshow.txt")).useDelimiter("\n");
			showNumber.setText(recentShow.nextLine());
			int sn = Integer.parseInt(recentShow.nextLine());
			seasonNumber.setText((sn < 100 ? "0" + (sn < 10 ? "0" : "") : "") + sn);
			location.setText(recentShow.nextLine());
			theme.setText(recentShow.nextLine());
		} catch (FileNotFoundException e) {}	//not the end of the world if this extra feature fails
		
		
		GregorianCalendar gC = new GregorianCalendar(Locale.US);
		date.setText((gC.get(Calendar.MONTH) + 1) + "/" + gC.get(Calendar.DATE) + "/" + (gC.get(Calendar.YEAR) % 100));
		
		RowOnePanel.setLayout(new BoxLayout(RowOnePanel, BoxLayout.LINE_AXIS));
		RowOnePanel.setOpaque(false);
		RowOnePanel.add(new JLabel("Show #:"));
		RowOnePanel.add(Box.createHorizontalStrut(5));
		RowOnePanel.add(showNumber);
		RowOnePanel.add(Box.createHorizontalStrut(15));
		RowOnePanel.add(new JLabel("Season #:"));
		RowOnePanel.add(Box.createHorizontalStrut(5));
		RowOnePanel.add(seasonNumber);
		RowOnePanel.add(Box.createHorizontalStrut(15));
		RowOnePanel.add(new JLabel("Location:"));
		RowOnePanel.add(Box.createHorizontalStrut(5));
		RowOnePanel.add(location);
		RowOnePanel.add(Box.createHorizontalStrut(15));
		RowOnePanel.add(new JLabel("Theme:"));
		RowOnePanel.add(Box.createHorizontalStrut(5));
		RowOnePanel.add(theme);
		RowOnePanel.add(Box.createHorizontalStrut(15));
		RowOnePanel.add(new JLabel("Date:"));
		RowOnePanel.add(Box.createHorizontalStrut(5));
		RowOnePanel.add(date);
		
		redName = new JTextField("Red");
		redName.setPreferredSize(new Dimension(300, 30));
		redName.setMaximumSize(new Dimension(300, 30));
		yellowName = new JTextField("Yellow");
		yellowName.setPreferredSize(new Dimension(300, 30));
		yellowName.setMaximumSize(new Dimension(300, 30));
		blueName = new JTextField("Blue");
		blueName.setPreferredSize(new Dimension(300, 30));
		blueName.setMaximumSize(new Dimension(300, 30));
		
		RedPanel = new JPanel();
		RedPanel.setLayout(new BoxLayout(RedPanel, BoxLayout.PAGE_AXIS));
		RedPanel.setBackground(RED_COLOR);
		RedPanel.add(redName);
		RedPanel.add(Box.createVerticalStrut(10));
		redBank = new JLabel("", JLabel.CENTER);
		redBank.setFont(PLAYER_FONT);
		RedPanel.add(redBank);
		RedPanel.add(Box.createVerticalStrut(10));
		redTossUp = new JButton("Buzzed In");
		redTossUp.setAlignmentX(Component.CENTER_ALIGNMENT);
		RedPanel.add(redTossUp);
		redGender = new GenderPanel(RED_COLOR);
		redGender.setVisible(false);
		RedPanel.add(Box.createVerticalStrut(10));
		RedPanel.add(redGender);
		
		YellowPanel = new JPanel();
		YellowPanel.setLayout(new BoxLayout(YellowPanel, BoxLayout.PAGE_AXIS));
		YellowPanel.setBackground(YELLOW_COLOR);
		YellowPanel.add(yellowName);
		YellowPanel.add(Box.createVerticalStrut(10));
		yellowBank = new JLabel("", JLabel.CENTER);
		yellowBank.setFont(PLAYER_FONT);
		YellowPanel.add(yellowBank);
		YellowPanel.add(Box.createVerticalStrut(10));
		yellowTossUp = new JButton("Buzzed In");
		yellowTossUp.setAlignmentX(Component.CENTER_ALIGNMENT);
		YellowPanel.add(yellowTossUp);
		yellowGender = new GenderPanel(YELLOW_COLOR);
		yellowGender.setVisible(false);
		YellowPanel.add(Box.createVerticalStrut(10));
		YellowPanel.add(yellowGender);
		
		BluePanel = new JPanel();
		BluePanel.setLayout(new BoxLayout(BluePanel, BoxLayout.PAGE_AXIS));
		BluePanel.setBackground(BLUE_COLOR);
		BluePanel.add(blueName);
		BluePanel.add(Box.createVerticalStrut(10));
		blueBank = new JLabel("", JLabel.CENTER);
		blueBank.setFont(PLAYER_FONT);
		BluePanel.add(blueBank);
		BluePanel.add(Box.createVerticalStrut(10));
		blueTossUp = new JButton("Buzzed In");
		blueTossUp.setAlignmentX(Component.CENTER_ALIGNMENT);
		BluePanel.add(blueTossUp);
		blueGender = new GenderPanel(BLUE_COLOR);
		blueGender.setVisible(false);
		BluePanel.add(Box.createVerticalStrut(10));
		BluePanel.add(blueGender);
		
		HiddenEdit.addHiddenEdit(redBank, new HiddenRoundScoreEdit(redBank, Player.RED));
		HiddenEdit.addHiddenEdit(yellowBank, new HiddenRoundScoreEdit(yellowBank, Player.YELLOW));
		HiddenEdit.addHiddenEdit(blueBank, new HiddenRoundScoreEdit(blueBank, Player.BLUE));
		
		RowTwoPanel = new JPanel();
		RowTwoPanel.setLayout(new GridLayout(1, 3, 10, 0));
		RowTwoPanel.setOpaque(false);
		RowTwoPanel.add(RedPanel);
		RowTwoPanel.add(YellowPanel);
		RowTwoPanel.add(BluePanel);
		
		submit = new JButton("Submit Show Info");
		submit.addActionListener(new InfoListener());
		RowThreePanel = new JPanel();
		RowThreePanel.setLayout(new BoxLayout(RowThreePanel, BoxLayout.LINE_AXIS));
		RowThreePanel.setOpaque(false);
		RowThreePanel.setVisible(false);
		RowThreePanel.add(Box.createHorizontalGlue());
		couples = new JCheckBox("Couples");
		couples.addItemListener(this);
		couples.setToolTipText("Check to enable couples (another M/F option).");
		couples.setOpaque(false);
		couples.setEnabled(false);
		RowThreePanel.add(couples);
		RowThreePanel.add(Box.createHorizontalStrut(15));
		RowThreePanel.add(submit);
		
		showNumber.setEnabled(false);
		seasonNumber.setEnabled(false);
		location.setEnabled(false);
		theme.setEnabled(false);
		date.setEnabled(false);
		redName.setEnabled(false);
		yellowName.setEnabled(false);
		blueName.setEnabled(false);
		redTossUp.setEnabled(false);
		yellowTossUp.setEnabled(false);
		blueTossUp.setEnabled(false);
		
		TossUpListener tUL = new TossUpListener();
		redTossUp.addActionListener(tUL);
		yellowTossUp.addActionListener(tUL);
		blueTossUp.addActionListener(tUL);
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setOpaque(false);
		
		add(RowOnePanel);
		add(Box.createVerticalStrut(15));
		add(RowTwoPanel);
		add(Box.createVerticalStrut(10));
		add(RowThreePanel);
	}
	
	private class HiddenRoundScoreEdit extends AbstractAction implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private JComponent c;
		private Player p;
		
		HiddenRoundScoreEdit(JComponent c, Player p) {
			super();
			this.c = c;
			this.p = p;
		}
		
		public void actionPerformed(ActionEvent e) {
			try {
				p.setMoney(Integer.parseInt(JOptionPane.showInputDialog(c, "ONLY USE IN SCOREBOARD\nINCONSISTENCY CASES!", "Hidden: Custom Round Score",
					JOptionPane.QUESTION_MESSAGE, null, null, String.valueOf(p.getRoundScore())).toString())
				);
				updateBanks();
			} catch(Exception ex) {}
		}
	}
	
	private static class GenderPanel extends JPanel implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private ButtonGroup buttons1, buttons2;
		private JRadioButton m1, f1, m2, f2;
		
		public GenderPanel(Color c) {
			m1 = new JRadioButton("M");
			m1.setBackground(c);
			f1 = new JRadioButton("F");
			f1.setBackground(c);
			m2 = new JRadioButton("M");
			m2.setBackground(c);
			m2.setVisible(false);
			f2 = new JRadioButton("F");
			f2.setBackground(c);
			f2.setVisible(false);
			
			buttons1 = new ButtonGroup();
			buttons1.add(m1);
			buttons1.add(f1);
			buttons2 = new ButtonGroup();
			buttons2.add(m2);
			buttons2.add(f2);
			
			setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
			setBackground(c);
			
			add(Box.createHorizontalGlue());
			add(m1);
			add(Box.createHorizontalStrut(5));
			add(f1);
			add(Box.createHorizontalStrut(15));
			add(Box.createHorizontalGlue());
			add(m2);
			add(Box.createHorizontalStrut(5));
			add(f2);
			add(Box.createHorizontalStrut(15));
			add(Box.createHorizontalGlue());
			add(Box.createHorizontalGlue());
		}
		
		public void setCouples(boolean isSelected) {
			m2.setVisible(isSelected);
			f2.setVisible(isSelected);
			
			if(!isSelected)
				buttons2.clearSelection();
		}
		
		public boolean isOK() {
			return (m1.isSelected() || f1.isSelected()) && (!m2.isVisible() || (m2.isSelected() || f2.isSelected()));
		}
		
		public int getFemales() {
			return (f1.isSelected() ? 1 : 0) + (f2.isSelected() ? 1 : 0);
		}
	}
	
	public void itemStateChanged(ItemEvent iE) {
		redGender.setCouples(couples.isSelected());
		yellowGender.setCouples(couples.isSelected());
		blueGender.setCouples(couples.isSelected());
	}
	
	public void resetGame() {
		RowOnePanel.setVisible(true);
		redName.setVisible(true);
		redName.setText("Red");
		yellowName.setVisible(true);
		yellowName.setText("Yellow");
		blueName.setVisible(true);
		blueName.setText("Blue");
		redBank.setVisible(true);
		yellowBank.setVisible(true);
		blueBank.setVisible(true);
	}
	
	public void startTossUp() {
		redTossUp.setVisible(true);
		yellowTossUp.setVisible(true);
		blueTossUp.setVisible(true);
		redTossUp.setEnabled(true);
		yellowTossUp.setEnabled(true);
		blueTossUp.setEnabled(true);
		
		if(RecordUtilityPanel.isTied()) {
			EnumSet<Player> winners = RecordUtilityPanel.getWinners();
			if(winners.size() != 3) {	//if three-way tie, ignore below (will this EVER happen?)
				if(!winners.contains(Player.RED)) {
					redTossUp.setVisible(false);
					Player.RED.setFailTossUp(true);
				} else if(!winners.contains(Player.YELLOW)) {
					yellowTossUp.setVisible(false);
					Player.YELLOW.setFailTossUp(true);
				} else {
					blueTossUp.setVisible(false);
					Player.BLUE.setFailTossUp(true);
				}
			}
		}
	}
	
	public void resetTossUp() {
		redTossUp.setVisible(false);
		yellowTossUp.setVisible(false);
		blueTossUp.setVisible(false);
	}
	
	private class TossUpListener implements ActionListener, Serializable {
		private static final long serialVersionUID = 1L;
		
		public void actionPerformed(ActionEvent aE) {
			Object player = aE.getSource();
			
			if(player == redTossUp) {
				Player.setCurrentPlayer(Player.RED);
				redTossUp.setEnabled(false);
			} else if(player == yellowTossUp) {
				Player.setCurrentPlayer(Player.YELLOW);
				yellowTossUp.setEnabled(false);
			} else {
				Player.setCurrentPlayer(Player.BLUE);
				blueTossUp.setEnabled(false);
			}
			
			RecordPanel.GAME.setPlayerText();
			tossUpButtonPressed = true;
		}
	}

	private class InfoListener implements ActionListener, Serializable {
		private static final long serialVersionUID = 1L;
		
		public void actionPerformed(ActionEvent aE) {
			if(!redGender.isOK() || !yellowGender.isOK() || !blueGender.isOK()) {
				JOptionPane.showMessageDialog(MainWindow.getWindow(), "Genders not all filled in. Check couples.", "Error in gender row", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			try {
				RecordPanel.gameAnalysis.setTitle(date.getText(), showNumber.getText(), seasonNumber.getText());
			} catch(NumberFormatException e) {
				JOptionPane.showMessageDialog(MainWindow.getWindow(), "Show or season # is not a number, or the date is not formatted properly: mm/dd/yy", "Error in top row", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			Player.RED.setName(redName.getText());
			Player.YELLOW.setName(yellowName.getText());
			Player.BLUE.setName(blueName.getText());
			if(couples.isSelected() && (!Player.RED.isCouples() || !Player.YELLOW.isCouples() || !Player.BLUE.isCouples())) {
				JOptionPane.showMessageDialog(MainWindow.getWindow(), "Couples' names must be separated by \" & \" exactly.", "Error in name area", JOptionPane.ERROR_MESSAGE);
				return;
			}
			Player.checkAbbrNames();
			write();
			
			showNumber.setEnabled(false);
			seasonNumber.setEnabled(false);
			location.setEnabled(false);
			theme.setEnabled(false);
			date.setEnabled(false);
			redName.setEnabled(false);
			yellowName.setEnabled(false);
			blueName.setEnabled(false);
			redName.setVisible(false);
			yellowName.setVisible(false);
			blueName.setVisible(false);
			redGender.setVisible(false);
			yellowGender.setVisible(false);
			blueGender.setVisible(false);
			submit.setEnabled(false);
			couples.setEnabled(false);
			
			
			try(FileWriter showWriter = new FileWriter("woftracker\\record\\recentshow.txt")) {
				showWriter.write(showNumber.getText() + "\n" + seasonNumber.getText() + "\n" + location.getText() + "\n" + theme.getText());
			} catch(IOException e) {}
			
			
			RecordPanel.GAME_TEXT.append("~~~~~~~~~~<br><span>" + date.getText() + " @ " + location.getText() +
				"<br>Show #: " + showNumber.getText() + "<br>Season #: " + seasonNumber.getText() +
				"<br>Theme: " + theme.getText() + "<br>" + "<br>Red: " + Player.RED.getName() + "<br>Yellow: " +
				Player.YELLOW.getName() + "<br>Blue: " + Player.BLUE.getName() + "</span><br>~~~~~~~~~~<br><br>"
			);
			
			//replace oldAbbrs with new on all 1k action.
			String oldAbbrs[] = { "R* ", "Y* ", "B* " };
			for(int i = 0, index = 0; i < 3; i++)
				if((index = RecordPanel.GAME_TEXT.indexOf(oldAbbrs[i])) != -1)
					RecordPanel.GAME_TEXT.replace(index, index + 3, Player.values()[i].getAbbrName() + " ");
			
			if(couples.isSelected())
				RecordPanel.GAME.setCouples();
			
			RecordPanel.gameAnalysis.analyzeGender(redGender.getFemales() + yellowGender.getFemales() + blueGender.getFemales(), couples.isSelected());
			
			RecordPanel.update();
			startTossUp();
			RecordPanel.GAME.startTossUp();
			RowOnePanel.setVisible(false);
			RowThreePanel.setVisible(false);
			RecordPreviewPanel.setFileName(RecordPanel.gameAnalysis.getFileName().replace(".ser", ""));
			
			int index = RecordPanel.GAME_TEXT.indexOf(DATE_ID);
			RecordPanel.GAME_TEXT.replace(index, index + DATE_ID.length(), date.getText());
		}
	}
	
	public void allowInfo() {
		showNumber.setEnabled(true);
		seasonNumber.setEnabled(true);
		location.setEnabled(true);
		theme.setEnabled(true);
		date.setEnabled(true);
		redName.setEnabled(true);
		yellowName.setEnabled(true);
		blueName.setEnabled(true);
		redTossUp.setEnabled(false);
		yellowTossUp.setEnabled(false);
		blueTossUp.setEnabled(false);
		RowThreePanel.setVisible(true);
		redGender.setVisible(true);
		yellowGender.setVisible(true);
		blueGender.setVisible(true);
		couples.setEnabled(true);
		resetTossUp();
	}
	
	public void updateBanks() {
		redBank.setText(Player.RED.toLabelString());
		yellowBank.setText(Player.YELLOW.toLabelString());
		blueBank.setText(Player.BLUE.toLabelString());
	}
	
	public void setBonusBanks() {
		Player.setRoundActive(false);
		
		Player p = Player.getCurrentPlayer();
		
		redBank.setVisible(p == Player.RED);
		yellowBank.setVisible(p == Player.YELLOW);
		blueBank.setVisible(p == Player.BLUE);
	}
	
	public static void resetEditRedo() {
		tossUpButtonPressed = false;
	}
	
	public static boolean isEditRedo() {
		return !tossUpButtonPressed;
	}
	
	public Insets getInsets() {
		return new Insets(10,10,10,10);
	}
}