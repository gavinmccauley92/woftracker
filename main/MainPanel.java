package woftracker.main;

import woftracker.MainWindow;
import woftracker.util.GameAnalyses;
import java.math.BigDecimal;
import java.awt.*;
import java.awt.event.*;
import java.math.*;
import java.io.*;
import javax.swing.*;

public class MainPanel extends JPanel implements ActionListener {
	
	private static final String[] CS_TEXT = { "Show color chooser next time", "Use color I chose next time" };
	private JToggleButton doColorScheme;
	private JProgressBar gALoadingBar;
	private int goal;
	
	public MainPanel(boolean b) {
		setLayout(new BorderLayout());
		setOpaque(false);
		
		doColorScheme = new JToggleButton(CS_TEXT[b ? 0 : 1], b);
		doColorScheme.addActionListener(this);
		add(doColorScheme, BorderLayout.NORTH);
		
		add(new JLabel(new ImageIcon("pix/WoFTracker4.png"), SwingConstants.CENTER), BorderLayout.CENTER);
	}
	
	public void setBar(int goal) {
		gALoadingBar = new JProgressBar(0, (this.goal = goal));
		gALoadingBar.setStringPainted(true);
		gALoadingBar.setToolTipText("Double click to load all available analyses.");
		gALoadingBar.setString("0/" + goal);
		//gALoadingBar.setFont(new Font("Source Code Pro", 24, Font.BOLD));
		gALoadingBar.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() >= 2) {
					// leaving this on the event thread is bad, start fresh thread
					new Thread(() -> {
						if(GameAnalyses.initAnalyses())
							gALoadingBar.removeMouseListener(this);
						else {
							MainWindow.setGAGoal(0);
							GameAnalyses.initGameCount();
						}
					}).start();
				}
			}
		});
		add(gALoadingBar, BorderLayout.SOUTH);
	}
	
	public void updateBar(int i) {
		gALoadingBar.setValue(i);
		if(i == goal) gALoadingBar.setToolTipText("Analyses done loading.");
		gALoadingBar.setString(i + "/" + goal + " (" + new BigDecimal(100*Double.valueOf(i)/goal).setScale(0, RoundingMode.FLOOR) + "%)");
	}
	
	public void actionPerformed(ActionEvent aE) {
		if(doColorScheme == aE.getSource()) {
			try {
				MainWindow.ColorScheme cSC;
				
				try(FileInputStream colorReader = new FileInputStream("woftracker\\log\\colorscheme.ser"); ObjectInputStream persistanceIn = new ObjectInputStream(colorReader)) {
					cSC = (MainWindow.ColorScheme) persistanceIn.readObject();
				}
				
				cSC.setShow(doColorScheme.isSelected());
				
				try(FileOutputStream colorWriter = new FileOutputStream("woftracker\\log\\colorscheme.ser"); ObjectOutputStream persistanceOut = new ObjectOutputStream(colorWriter)) {
					persistanceOut.writeObject(cSC);
				}
				
				doColorScheme.setText(CS_TEXT[doColorScheme.isSelected() ? 0 : 1]);
			} catch (Exception e) {
				e.printStackTrace();	//probably do a JDialog with this at osme point so errors can be reported to me
			}
		}
	}
	
	public Insets getInsets() {
		return new Insets(20, 20, 20, 20);
	}
}