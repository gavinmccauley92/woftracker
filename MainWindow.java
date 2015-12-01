package woftracker;

import woftracker.record.RecordPanel;
import woftracker.stats.StatPanel;
import woftracker.main.MainPanel;
import woftracker.readout.ReadoutPanel;
import woftracker.puzzle.PuzzleFinderPanel;
import woftracker.util.DebugPanel;
import woftracker.util.GameAnalyses;
import woftracker.search.SearchOptionPanel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.UIManager.*;

import static java.awt.GraphicsDevice.WindowTranslucency.*;	//http://docs.oracle.com/javase/tutorial/uiswing/misc/trans_shaped_windows.html

public final class MainWindow {
	private MainWindow() {}
	
	public static final int WINDOW_WIDTH = 1250, WINDOW_HEIGHT = 550;
	
	private static JFrame window;
	private static RecordPanel rP;
	private static MainPanel mP;
	private static JTabbedPane tp;
	public static final String CSC_FILENAME = "woftracker\\log\\colorscheme.ser", SYSERR_FILENAME = "woftracker\\log\\syserr_log.txt", SYSOUT_FILENAME = "woftracker\\log\\sysout_log.txt";
	
	public static void openWindow(boolean b) {
		window = new JFrame("Wheel of Fortune Tracker");
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		tp = new JTabbedPane();
		tp.addTab("Welcome", (mP = new MainPanel(b)));
		//tp.addTab("Record Game", (rP = new RecordPanel()));
		tp.addTab("Readout", new ReadoutPanel());
		tp.addTab("Stats", new StatPanel());
		tp.addTab("Puzzle Finder", new PuzzleFinderPanel());
		tp.addTab("Search", new SearchOptionPanel());
		
		tp.setEnabledAt(2, false);	//stats
		tp.setEnabledAt(3, false);	//search
		tp.setEnabledAt(4, false);	//puzzle finder
		
		window.getContentPane().add(tp);
		window.pack();	//makes frame displayable, causing errors above
		window.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
		//window.setResizable(false);
		window.setLocationRelativeTo(null);
		
		GameAnalyses.initGameCount();
		window.setVisible(true);
	}
	
	private static int gameCount = 0, goal;
	public static void updateGAProgress() {
		mP.updateBar(++gameCount);
		if(gameCount == goal) {
			tp.setEnabledAt(2, true);	//stats
			tp.setEnabledAt(3, true);	//search
			tp.setEnabledAt(4, true);	//puzzle finder
			tp.addTab("Debug", new DebugPanel());	//allow errors in getting the window open to go to console before said window is available
			//window.pack();
		}
	}
	
	public static void setGAGoal(int g) {
		mP.setBar(goal = g);
	}
	
	public static void main(String[] args) {		
		//recommended way to start a Swing application (event-handling thread)
		SwingUtilities.invokeLater(new Runnable() {
            public void run() {				
				try {
					ColorScheme cSC = null;
					Color c = null;
					
					try(FileInputStream colorReader = new FileInputStream(CSC_FILENAME); ObjectInputStream persistanceIn = new ObjectInputStream(colorReader)) {
						cSC = (ColorScheme) persistanceIn.readObject();
						c = cSC.getChoice();
					} catch(Exception e) {	//more laziness, ain't it great?
						e.printStackTrace();
						cSC = new ColorScheme();
					}
					
					if(cSC.doShow()) {	
						do {
							cSC.setChoice(JColorChooser.showDialog(null, "Choose color scheme", c != null ? c : new Color(188, 143, 143)));
							c = cSC.getChoice();
						} while(c == null);
					}
					
					try(FileOutputStream colorWriter = new FileOutputStream(CSC_FILENAME); ObjectOutputStream persistanceOut = new ObjectOutputStream(colorWriter)) {
						persistanceOut.writeObject(cSC);
					}
					
					//Nimbus modification
					UIManager.put("nimbusBase", c.darker());
					UIManager.put("nimbusBlueGrey", c);
					UIManager.put("nimbusLightBackground", c.brighter());
					UIManager.put("control", c);
					
					mainColor = c;
					
					//from Java tutorial
					for(LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
						if("Nimbus".equals(info.getName())) {
							UIManager.setLookAndFeel(info.getClassName());
							break;
						}
					}
					
					openWindow(cSC.doShow());
					//rP.setAllBorders(BorderFactory.createLineBorder(c.darker(), 2));
				} catch (Exception e) { e.printStackTrace(); }	//a lot of various errors can happen here, maybe make a clean debugging later, but yes, this is lazy
            }
        });
	}
	
	private static Color mainColor;
	public static Color getMainColor() {
		return mainColor;
	}
	
	public static class ColorScheme implements Serializable {	//public for needing to be accessed in MainPanel... I need to reorganize sometime
		private static final long serialVersionUID = 1L;	//version number.
		
		private Color c;
		private boolean doShow = true;
		
		public void setChoice(Color c) {
			this.c = c;
		}
		
		public Color getChoice() {
			return c;
		}
		
		public void setShow(boolean b) {
			doShow = b;
		}
		
		public boolean doShow() {
			return doShow;
		}
	}
	
	public static void resetRP() {
		rP.reset();
	}
	
	public static JFrame getWindow() {
		return window;
	}
}