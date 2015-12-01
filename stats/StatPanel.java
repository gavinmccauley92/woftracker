package woftracker.stats;

import java.awt.*;
import javax.swing.*;

public class StatPanel extends JPanel {
	
	//private static JLabel welcome = new JLabel("Welcome to the analysis tab."); static { welcome.setAlignmentX(Component.CENTER_ALIGNMENT); }
	private JPanel AnalysisRow;
	
	public StatPanel() {
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setOpaque(false);
		
		add(Box.createVerticalGlue());
		//add(welcome);
		//add(Box.createVerticalStrut(25));
		
		AnalysisRow = new JPanel();
		AnalysisRow.setLayout(new BoxLayout(AnalysisRow, BoxLayout.LINE_AXIS));		AnalysisRow.setOpaque(false);		AnalysisRow.add(Box.createHorizontalGlue());		AnalysisRow.add(new AnalysisPanel());		AnalysisRow.add(Box.createHorizontalStrut(25));		AnalysisRow.add(new AnalysisPanel());		AnalysisRow.add(Box.createHorizontalStrut(25));		AnalysisRow.add(new AnalysisPanel());		AnalysisRow.add(Box.createHorizontalGlue());
		
		add(AnalysisRow);
		add(Box.createVerticalGlue());
	}
	
	public Insets getInsets() {
		return new Insets(10,10,10,10);
	}
}