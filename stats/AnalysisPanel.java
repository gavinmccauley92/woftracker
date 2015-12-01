package woftracker.stats;

import woftracker.*;
import woftracker.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import java.io.*;
import java.util.*;
import java.time.*;
import java.time.temporal.*;
import java.time.format.*;
import java.lang.reflect.Field;

import static woftracker.util.GameAnalyses.*;

/* http://www.theserverside.com/discussions/thread.tss?thread_id=19221 */
//(JScrollPane object).getViewport().setView(Component); - sets the "child" dynamically

public class AnalysisPanel extends JPanel {
	private GameAnalysis gA;
	private MultiGameAnalysis mGA;
	private Thread mGAThread = new Thread();
	
	private JList<String> singleList, multiList;
	private JComboBox<String> multiSectionHeaders;
	private JTextPane statBox;
	private JScrollPane lSP, rSP;
	private JSplitPane jSP;
	
	private JTextField analysisChooser;
	private static final Font FONT = new Font("KaiTi", Font.PLAIN, 13);
	
	@SuppressWarnings("unchecked")
	public AnalysisPanel(boolean allowFileSelect) {
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setOpaque(false);
		
		statBox = new JTextPane();
		statBox.setFont(FONT);
		
		statBox.setEditable(false);
		statBox.setEnabled(false);
		statBox.setOpaque(false);
		statBox.setMinimumSize(new Dimension(300, 400));
		statBox.setPreferredSize(new Dimension(300, 500));
		statBox.setMaximumSize(new Dimension(450, 900));
		statBox.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK), "find-what");
		statBox.getActionMap().put("find-what", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if(statBox.isEnabled()) {
					String s = statBox.getText(), s2 = JOptionPane.showInputDialog(MainWindow.getWindow(), "Find what:");
					int i = s.indexOf(s2, statBox.getCaretPosition());
					if(i != -1) {
						statBox.setCaretPosition(i);
						statBox.select(i, i + s2.length());
						RXTextUtilities.centerLineInScrollPane(statBox);
					}
				}
			}
		});
		
		//avoid more than one call per change.
		ListSelectionListener lsl = e -> { if(!e.getValueIsAdjusting())	updateStatBox((JList<String>) e.getSource()); };
		/* only one of these should be visible at a time */
		singleList = new JList<>(GameAnalysis.STAT_NAMES);
		singleList.setEnabled(false);
		singleList.setFont(FONT);
		singleList.addListSelectionListener(lsl);
		multiList = new JList<>(MultiGameAnalysis.STAT_NAMES);
		multiList.setEnabled(false);
		multiList.setVisible(false);
		multiList.setFont(FONT);
		multiList.addListSelectionListener(lsl);
		
		JPanel j = new JPanel();
		j.add(singleList);
		j.add(multiList);
		
		lSP = new JScrollPane(j, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		lSP.getVerticalScrollBar().setUnitIncrement(16);
		lSP.setPreferredSize(new Dimension(300, 75));
		lSP.setMinimumSize(new Dimension(300, 75));
		lSP.setMaximumSize(new Dimension(300, 200));
		//add(lSP);
		
		rSP = new JScrollPane(statBox, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		rSP.setPreferredSize(new Dimension(300, 400));
		rSP.setMaximumSize(new Dimension(450, 800));
		//rSP.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		jSP = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, lSP, rSP);
		jSP.setAlignmentX(Component.CENTER_ALIGNMENT);
		jSP.setPreferredSize(new Dimension(300, 425));
		jSP.resetToPreferredSizes();
		add(jSP);
		
		multiSectionHeaders = new JComboBox<>();
		multiSectionHeaders.setVisible(false);
		multiSectionHeaders.addItemListener(e -> {
			if(e.getStateChange() == ItemEvent.SELECTED) {
				String s = '-' + multiSectionHeaders.getSelectedItem().toString() + '-';
				int i = statBox.getText().indexOf(s);
				if(i != -1) {
					statBox.setCaretPosition(i);
					statBox.select(i, i + s.length());
					RXTextUtilities.centerLineInScrollPane(statBox);
				}
			}
		});
		add(multiSectionHeaders);
		
		if(allowFileSelect) {	//panels making this false should have external code calling showAnalyses (thus its public nature)
			analysisChooser = generateTextField(this::showAnalyses, true);
			analysisChooser.setFont(FONT.deriveFont(18f));
			//add(Box.createVerticalStrut(10));
			add(analysisChooser);
		}
	}
	
	public AnalysisPanel() {
		this(true);
	}
	
	public void showAnalyses(TreeSet<GameAnalysis> gAs) {
		StringBuilder s = new StringBuilder();	//for errors only
		JList<String> l = null, l2 = null;
		
		if(gAs != null && gAs.size() > 0) {
			try {
				if(gAs.size() == 1) {
					l = singleList;
					l2 = multiList;
					gA = gAs.first();
					statBox.setText(gA.toString());
				} else {
					l = multiList;
					l2 = singleList;
					mGAThread = new Thread(() -> {
						try {
							mGA = new MultiGameAnalysis(gAs);
						} catch(Exception e) {
							throw new RuntimeException(e);	//throwing as unchecked exception
						}
						multiSectionHeaders.removeAllItems();
						for(String sh : mGA.getSectionHeaders())
							multiSectionHeaders.addItem(sh);
						statBox.setText(mGA.toString());
					});
					mGAThread.start();
				}
			} catch(Exception e) {
				s.append("Unknown error relating to MultiGameAnalysis. Report to Wayoshi:\n" + e + "\n\n");
				for(StackTraceElement sTE : e.getStackTrace())
					s.append(sTE + "\n");
			} finally {	//a return statement in try does NOT stop from getting here!
				statBox.setEnabled(true);	//allow copypasta on errors.
				if(s.length() == 0) {
					l.setEnabled(true);
					l2.setEnabled(false);
					l.setVisible(true);
					l2.setVisible(false);
					l.setSelectedIndex(0);
					statBox.setCaretPosition(0);
				} else
					statBox.setText(s.toString());	//error text
				
				MutableAttributeSet mas = new SimpleAttributeSet(statBox.getParagraphAttributes());
				StyleConstants.setLineSpacing(mas, -0.2f);	//oddly enough, it's an offset from 1.0f
				statBox.getStyledDocument().setParagraphAttributes(0, statBox.getText().length(), mas, true);
			}
		} else {
			statBox.setEnabled(false);
			statBox.setText("No game(s) to analyze at this time.");
			singleList.setEnabled(false);
			multiList.setEnabled(false);
		}
	}
	
	@SuppressWarnings("unchecked")
	public void showSummary() {
		if(singleList.isVisible()) {
			//java.util.List<Integer> l = new LinkedList<>();
			String s = "finalMainWinnings/winnings/prizeResults/pWheelAmount/ppAmount/bonusAmount/missolves/puzzleDetails/" +
				"puzzleDissections/spinStrengths/gooseEggs/roundWinners/expressRides/roundCat/catPlurality";
			
			String[] s2 = s.split("/");
			int[] i = new int[s2.length];
			
			for(int j = 0; j < i.length; j++)
				i[j] = GameAnalysis.STAT_NAMES.indexOf(s2[j]);	//ugly extraneous work but toArray() is difficult
			
			singleList.setSelectedIndices(i);
		} else {
			System.err.println("Single list not visible when called");
		}
	}
	
	@SuppressWarnings("unchecked")
	private void updateStatBox(JList<String> source) {
		if(mGAThread.isAlive())
			try { mGAThread.join(); } catch(InterruptedException e) {}
		
		Object o = source == singleList ? gA : mGA;
		java.util.List<String> selected = source.getSelectedValuesList();
		Map<String, Field> statMap = source == singleList ? GameAnalysis.STAT_MAP : MultiGameAnalysis.STAT_MAP;
		multiSectionHeaders.setVisible(false);
		
		if(selected.isEmpty())
			return;
		
		StringBuilder str = new StringBuilder(7500);
		
		if(selected.get(0).equals("report")) {
			str.append(o.toString());
			if(source == multiList || gA.getShowNumber() >= 5469)
				str.append(GameAnalysis.DISCLAIMER);
			multiSectionHeaders.setVisible(o == mGA);
		} else {
			String header = "";
			for(String s : selected) {
				Field f = statMap.get(s);
				try {
					Object fo = f.get(o);
					if(fo instanceof Map<?, ?>)
						fo = FormatFactory.formatMap((Map<?, ?>) fo);
					else if(fo instanceof Collection<?>)
						fo = FormatFactory.formatCollection((Collection<?>) fo);
					str.append(header + s + "\n\n" + fo);
				} catch(IllegalAccessException ex) {
					str.append(header + "Error accessing " + f + " :" + ex);
				} finally {
					header = GameAnalysis.LINE;
				}
			}
		}
		
		statBox.setText(str.toString());
		statBox.setCaretPosition(0);
	}
	
	public static void main(String[] args) {
		java.util.List<Integer> l = new LinkedList<>();
			GameAnalysis.STAT_NAMES.stream()
				.filter(s -> s.matches("(finalMainWinnings|winnings|prizeResults|ppAmount|bonusAmount)"))
				.forEach(s -> l.add(GameAnalysis.STAT_NAMES.indexOf(s)));
		
		System.out.println(l);
	}
}