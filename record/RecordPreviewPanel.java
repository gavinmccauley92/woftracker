package woftracker.record;

import woftracker.*;
import woftracker.stats.GameAnalysis;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import java.io.*;
import java.util.List;
import java.util.LinkedList;

public class RecordPreviewPanel extends JPanel implements ActionListener, Recordable {
	private static JPanel OptionPanel;
	private static JScrollPane jSP;
	private static JButton recordGame, editGame, saveGame;
	private static String fileName = "";
	
	RecordPreviewPanel() {
		RECAP_BOX.setPreferredSize(new Dimension(350, 250));
		RECAP_BOX.setMaximumSize(new Dimension(350, 250));
		RECAP_BOX.setContentType("text/html");
		//hack, thanks http://stackoverflow.com/questions/1527021/html-jtextpane-newline-support
		RECAP_BOX.getDocument().putProperty(DefaultEditorKit.EndOfLineStringProperty, "<br>");
		RECAP_BOX.setEditable(false);
		RECAP_BOX.setEnabled(false);
		RECAP_BOX.setOpaque(false);
		jSP = new JScrollPane(RECAP_BOX, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		jSP.setPreferredSize(new Dimension(350, 250));
		jSP.setMaximumSize(new Dimension(350, 250));
		jSP.setOpaque(false);
		
		OptionPanel = new JPanel();
		OptionPanel.setLayout(new BoxLayout(OptionPanel, BoxLayout.LINE_AXIS));
		OptionPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		OptionPanel.setOpaque(false);
		OptionPanel.add(Box.createHorizontalGlue());
		recordGame = new JButton("Record Game");
		recordGame.addActionListener(this);
		OptionPanel.add(recordGame);
		OptionPanel.add(Box.createHorizontalStrut(10));
		editGame = new JButton("Edit Game");
		editGame.addActionListener(this);
		//editGame.setEnabled(false);
		editGame.setVisible(true);
		OptionPanel.add(editGame);
		OptionPanel.add(Box.createHorizontalStrut(10));
		saveGame = new JButton("Save Game");
		saveGame.addActionListener(this);
		//saveGame.setVisible(false);
		OptionPanel.add(saveGame);
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		
		setOpaque(false);
		setupPanel();
	}
	
	public void actionPerformed(ActionEvent aE) {
		Object gameButton = aE.getSource();
		
		if(gameButton == recordGame) {
			Object[] options = { "OK", "Cancel" };
			if(recordGame.getText().equals("New Game")) {
				if(JOptionPane.showOptionDialog(MainWindow.getWindow(), "You will lose everything! Consider editing the game instead.",
					"New Game", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]) == JOptionPane.YES_OPTION
				)
					resetGame();
			} else {
				/* if(JOptionPane.showOptionDialog(MainWindow.getWindow(), "Set mystery round to R2?",
					"New Game", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]) == JOptionPane.YES_OPTION
				) 	no need to check as of S29
				Wedge.MYSTERY_ROUND = 2; */
				startGame();
			}
		 } else if(gameButton == editGame) {
			new GameEditor();
		} else {
			try {
				writeGame();	//saveGame button
			} catch(IOException e) {

				RECAP_BOX.setText("Error recording recap and/or analysis, try again. Here is the error:\n\n" + e);
			}
		}
	}
	
	private static void startGame() {
		editGame.setEnabled(true);
		for(File f : new File("woftracker\\record\\editor").listFiles())
			if(!f.isDirectory())
				f.delete();
		
		RECAP_BOX.setEnabled(true);
		RecordPanel.TOSS_INFO.startTossUp();
		RecordPanel.GAME.startTossUp();
		recordGame.setText("New Game");
	}
	
	private static void resetGame() {
		Player.RED.reset();
		Player.YELLOW.reset();
		Player.BLUE.reset();
		Wedge.resetWedges();
		RecordPanel.roundNumber = 0;
		RecordPanel.tossUpNumber = 0;
		RecordPanel.GAME_TEXT.delete(0, RecordPanel.GAME_TEXT.length()).append(INITIAL_TEXT);
		RecordPanel.TOSS_INFO.resetGame();
		RecordTossInfoPanel.resetEditRedo();
		RecordGamePanel.resetGame();
		RecordPanel.gameAnalysis = new GameAnalysis();
		startGame();
	}
	
	private static class GameEditor extends JDialog implements ActionListener, ItemListener {
		private JPanel totalPanel, actionPanel;
		private JButton submit;
		private JComboBox<ActionEditor> actions;
		
		GameEditor() {
			super(MainWindow.getWindow(), "Edit Game", true);	//force modal for now, as well as only one frame
			
			totalPanel = new JPanel();
			totalPanel.setOpaque(false);
			totalPanel.setLayout(new BoxLayout(totalPanel, BoxLayout.PAGE_AXIS));
			
			submit = new JButton("Submit");
			submit.addActionListener(this);
			actions = new JComboBox<ActionEditor>();
			totalPanel.add(Box.createVerticalGlue());
			totalPanel.add(submit);
			totalPanel.add(Box.createVerticalStrut(20));
			totalPanel.add(actions);
			totalPanel.add(Box.createVerticalStrut(20));
			totalPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
			totalPanel.add(Box.createVerticalStrut(20));
			
			int count = 1;
			actionPanel = new JPanel();
			actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.PAGE_AXIS));
			
			for(File f : new File("woftracker\\record\\editor").listFiles()) {
				EditablePanel eP;
				
				try(FileInputStream panelReader = new FileInputStream(f); ObjectInputStream persistance = new ObjectInputStream(panelReader)) {
					eP = (EditablePanel) persistance.readObject();
				} catch (Exception e) {
					continue;
				}
				
				ActionEditor aE = new ActionEditor(eP, count++);
				actions.addItem(aE);
				actionPanel.add(aE);
			}
			
			actions.setSelectedIndex(-1);
			actions.addItemListener(this);
			
			totalPanel.add(actionPanel);
			totalPanel.add(Box.createVerticalGlue());
			add(totalPanel);
			pack();
			setLocationRelativeTo(MainWindow.getWindow());
			setVisible(true);
		}
		
		public void itemStateChanged(ItemEvent iE) {
			boolean added;
			((ActionEditor) iE.getItem()).setVisible((added = iE.getStateChange() == ItemEvent.SELECTED));
			
			if(added)
				pack();
		}
		
		private static class ActionEditor extends JPanel {
			private JPanel panel;
			private EditablePanel eP;
			private JCheckBox include;
			private String indexString;
			
			ActionEditor(EditablePanel theEP, int index) {
				panel = new JPanel();
				
				eP = theEP;
				indexString = "Action #" + index;
				
				include = new JCheckBox("Include in edited game", true);
				//include.setEnabled(eP instanceof RecordGamePanel);
				include.setToolTipText("If unchecked, this part will be skipped over. Be very careful unchecking this, unexpected results can easily occur.");
				
				setVisible(false);
				
				panel.setOpaque(false);
				panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
				panel.add(include);
				panel.add(Box.createVerticalStrut(20));
				panel.add(eP);
				
				add(panel);
			}
			
			public void resubmit() {			
				if(include.isSelected())
					eP.forceSubmit();
			}
			
			//for now
			public String toString() {
				return indexString;
			}
		}
		
		public void actionPerformed(ActionEvent event) {
			resetGame();
			
			ActionEditor aE;
			int index = 0;
			
			while((aE = actions.getItemAt(index++)) != null)
				aE.resubmit();
			
			MainWindow.resetRP();
			dispose();
		}
		
		public Insets getInsets() {
			return new Insets(10, 10, 10, 10);
		}
	}
	
	private void setupPanel() {
		removeAll();
		add(Box.createVerticalGlue());
		add(jSP);
		RECAP_BOX.setEditable(false);
		add(Box.createVerticalStrut(10));
		add(OptionPanel);
		add(Box.createVerticalGlue());
	}
	
	private static void writeGame() throws IOException {
		try(FileWriter recapWriter = new FileWriter(fileName)) {
			recapWriter.write((RecordPanel.GAME_TEXT.toString() + END_TEXT + CLOSING_TAGS).replace("10pt", "8pt").replace("12pt", "10pt"));
			RecordPanel.gameAnalysis.writeAnalysis();
		}
		
		RECAP_BOX.setText("All set! Check that the file is in \"" + fileName + "\" before closing or starting a new game.");
		//recordGame.setText("Record Game");
		//saveGame.setEnabled(false);
	}
	
	public static void setFileName(String date) {
		fileName = "recaps\\" + (date.length() != 0 ? date : "default") + ".html";
	}
	
	public Insets getInsets() {
		return new Insets(10,10,10,10);
	}
}