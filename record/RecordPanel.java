package woftracker.record;

import woftracker.*;
import woftracker.stats.GameAnalysis;
import java.util.Scanner;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.Border;

public class RecordPanel extends JPanel implements Recordable {
	
	static int roundNumber, tossUpNumber = 0;
	static GameAnalysis gameAnalysis = new GameAnalysis();
	private static JPanel rPP;
	static RecordGamePanel GAME;
	static RecordUtilityPanel UTILITY;
	static RecordTossInfoPanel TOSS_INFO;
	
	public RecordPanel() {
		setLayout(new BorderLayout());
		setOpaque(false);
		
		TOSS_INFO = new RecordTossInfoPanel();
		GAME = new RecordGamePanel();
		rPP = new RecordPreviewPanel();
		UTILITY = new RecordUtilityPanel();
		
		reset();
	}
	
	public void reset() {	
		removeAll();
		add(TOSS_INFO, BorderLayout.NORTH);
		add(GAME, BorderLayout.CENTER);
		add(rPP, BorderLayout.EAST);
		add(UTILITY, BorderLayout.SOUTH);
		revalidate();
	}
	
	public static void update() {
		RECAP_BOX.setText(GAME_TEXT.toString() + CLOSING_TAGS);
		TOSS_INFO.updateBanks();
		//System.out.println(GAME_TEXT.toString() + CLOSING_TAGS);	//debug
	}
	
	public void setAllBorders(Border b) {
		TOSS_INFO.setBorder(b);
		GAME.setBorder(b);
		rPP.setBorder(b);
		UTILITY.setBorder(b);
	}
	
	public Insets getInsets() {
		return new Insets(10,10,10,10);
	}
	
	/* a project I will not likely implement, it would require unprivating everything
	public static boolean createAnalysisAndRecapFromGameLog(File f) {
		//RecordPreviewPanel.resetGame();
		
		try {
			Scanner scan = new Scanner(f);
			
			Category c;
			
			for(int i = 0; scan.hasNext(); i++) {
				Scanner subScan = new Scanner(scan.nextLine());
				
				if(true) {
				
				} else if(i <= 1) {
					if(i == 0)
						subScan.useDelimiter(", ");
						Integer.parseInt(subScan.next().substring(1));
				}
			}
		} catch(Exception e) {
			System.err.println(e);
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public static void main(String[] args) {
		RecordPanel.createAnalysisAndRecapFromGameLog(new File("C:\\Java\\woftracker s29\\sources\\4.24.12.txt"));
	}*/
}

abstract class EditablePanel extends JPanel implements Serializable {
	private static final long serialVersionUID = 1L;
	
	protected JButton submit;
	static int count = 0;
	
	void softSubmit() {
		submit.doClick();	//for GameLog.
	}
	
	void forceSubmit() {
		submit.doClick();
		
		if(this instanceof RecordGamePanel)
			RecordPanel.GAME = (RecordGamePanel) this;
		else if(this instanceof RecordUtilityPanel)
			RecordPanel.UTILITY = (RecordUtilityPanel)this;
		else if(this instanceof RecordTossInfoPanel)
			RecordPanel.TOSS_INFO = (RecordTossInfoPanel) this;
	}
	
	//the order is important to be maintained in the filenames. Definitely the number of submits in a game will never go over 100, so a byte is fine and no further leading 0s need to be checked.
	void write() {
		submit.setEnabled(true);
		try(FileOutputStream panelWriter = new FileOutputStream("woftracker\\record\\editor\\" + (++count < 10 ? "0" : "") + count + ".ser");
			ObjectOutputStream persistance = new ObjectOutputStream(panelWriter)) {
			persistance.writeObject(this);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(MainWindow.getWindow(), "This part of the game won't be editable due to an error. Report this to Wayo:\n\n" + e, "Future error in editing", JOptionPane.ERROR_MESSAGE);
		}
		submit.setEnabled(true);
	}
}

//have a component be editable

class HiddenEdit {
	private HiddenEdit() {}
	
	static void addHiddenEdit(JComponent c, AbstractAction a) {
		c.setFocusable(true);
		c.addMouseListener(new HiddenEditFocusable());
		c.getInputMap().put(KeyStroke.getKeyStroke("control E"), "edit E");
		c.getActionMap().put("edit E", a);
	}
	
	private static class HiddenEditFocusable extends MouseAdapter implements Serializable {
		private static final long serialVersionUID = 1L;
			
		public void mouseClicked(MouseEvent e) {
			((JComponent)e.getSource()).requestFocusInWindow();
		}
	}
}