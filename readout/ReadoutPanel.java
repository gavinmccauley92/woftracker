package woftracker.readout;

import woftracker.record.*;
import woftracker.stats.*;
import woftracker.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.border.*;
import javax.swing.undo.*;
import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.*;
import java.util.function.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;
import java.util.stream.*;
import java.time.*;
import java.time.format.*;

import static woftracker.util.FormatFactory.*;

public class ReadoutPanel extends JPanel {
	private GameAnalysis gA;
	private String line;
	private int lineNumber;
	private LocalDate gaDate;
	private Wedge prizeReplacement = null;
	
	private static final File TEMP_READOUT = new File("woftracker\\log\\tempreadout.txt");
	//should be able to split appropriate info (with some leeway - see when line is split)
	private static final String LINE_REGEX = "([\\.,:\\(\\)]| spin(s)? | buy(s)? | use(s)? |, and all bought| after a triple dud)",
		BANK_REGEX = "(\\.|( )?\\+( )?| = )",
		//Only overlap between missolve line and puzzleboard is if A is on its own line, which will never happen
		BOARD_REGEX = "^([A-Z_'&-?!/\\.] )*[A-Z_'&-?!\\.]$";	//update 9/1/13: added potential punctuation in puzzle
	private JTextField imReadouter;
	private JTextPane readout;
	private JScrollPane jSP;
	private AnalysisPanel check;
	private JComboBox<Integer> females;
	private JCheckBox doubles, lowerGas, debug;
	private JLabel gaDateLabel;
	private JList<File> sources;
	private JButton appendix, convert, finalize;
	
	private ButtonGroup seasonGroup;
	private JToggleButton s30, s31, s32, s33;
	
	private final UndoManager undoManager = new UndoManager();

	public ReadoutPanel() {
		setLayout(new BorderLayout());
		setOpaque(false);
		setBorder(new EmptyBorder(10, 10, 10, 10));
		
		females = new JComboBox<>(IntStream.rangeClosed(0, 6).boxed().toArray(Integer[]::new));
		females.setSelectedItem(2);
		females.setToolTipText("Number of females in the game");
		females.setMaximumSize(new Dimension(25, 25));
		doubles = new JCheckBox("Doubles");
		doubles.setToolTipText("3 or 6 players in the game");
		lowerGas = new JCheckBox("LGM");
		lowerGas.setToolTipText("Lower Gas Money: lower BR car bonus cash from 5k to 3k.");
		debug = new JCheckBox("Debug");
		debug.setToolTipText("If checked, more detailed line-by-line parsing is available in the debug panel when converting.");
		
		gaDateLabel = new JLabel("N/A");
		gaDateLabel.setToolTipText("The date & number of the show currently set to be analyzed.");
		gaDateLabel.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				//wheel scroller on my desktop, probably platform dependent
				if(e.getButton() == MouseEvent.BUTTON2 && e.getClickCount() == 2) {
					try {
						loadReadout(JOptionPane.showInputDialog(gaDateLabel, "!"), null);
					} catch(Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		});
		
		JPanel genderPanel = new JPanel();
		genderPanel.setLayout(new BoxLayout(genderPanel, BoxLayout.LINE_AXIS));
		genderPanel.setMaximumSize(new Dimension(450, 25));
		genderPanel.add(Box.createHorizontalGlue());
		genderPanel.add(females);
		genderPanel.add(Box.createHorizontalStrut(10));
		genderPanel.add(doubles);
		genderPanel.add(Box.createHorizontalStrut(10));
		genderPanel.add(lowerGas);
		genderPanel.add(Box.createHorizontalStrut(10));
		genderPanel.add(debug);
		genderPanel.add(Box.createHorizontalStrut(10));
		genderPanel.add(gaDateLabel);
		genderPanel.add(Box.createHorizontalGlue());
		
		readout = new JTextPane();
		//readout.setToolTipText("End any line with a ^ or $ to ignore it.");
		readout.setPreferredSize(new Dimension(350, 500));
		readout.setMaximumSize(new Dimension(350, 800));
		readout.setEnabled(false);
		readout.setFont(new Font("KaiTi", Font.PLAIN, 14));
		
		//http://docs.oracle.com/javase/tutorial/uiswing/components/generaltext.html#undo | see ReadoutFormatException.java
		UndoAction uA = new UndoAction(undoManager); RedoAction rA = new RedoAction(undoManager); uA.setRedoAction(rA); rA.setUndoAction(uA);
		readout.getStyledDocument().addUndoableEditListener(e -> { undoManager.addEdit(e.getEdit()); uA.updateUndoState(); rA.updateRedoState(); });
		InputMap iM = readout.getInputMap();
		iM.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK), uA);
		iM.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Event.CTRL_MASK), rA);
		
		jSP = new JScrollPane(readout, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		jSP.setRowHeaderView(new TextLineNumber(readout));
		
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.add(genderPanel);
		panel.add(Box.createVerticalStrut(10));
		panel.add(jSP);
		
		Function<String, JScrollPane> createSourceView = 
			season -> {
				File[] sou = new File("sources\\" + season).listFiles(f -> f.isFile());	//Java 8 - lambda for FileFilter, accept/filter method
				Arrays.sort(sou, (f1, f2) -> {
					String[] s1 = f1.getName().split("\\."), s2 = f2.getName().split("\\.");
					return String.format("%02d.%02d.%02d", Integer.parseInt(s1[0]), Integer.parseInt(s1[1]), Integer.parseInt(s1[2]))
						.compareTo(String.format("%02d.%02d.%02d", Integer.parseInt(s2[0]), Integer.parseInt(s2[1]), Integer.parseInt(s2[2])));
				});	//Java 8 - lambda for Comparator
				sources = new JList<>(sou);
				sources.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				sources.setToolTipText("Double click on any file to load the readout box.");	//see ReadoutFormatException.java
				sources.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent e) {
						if(e.getClickCount() >= 2) {
							loadReadout(sources.getSelectedValue());
							check.showAnalyses(null);	//disable
							finalize.setEnabled(false);
						}
					}
				});
				sources.setLayoutOrientation(JList.VERTICAL_WRAP);
				sources.setVisibleRowCount(5);	//one week at a time
				sources.setCellRenderer(new WheelReadoutCellRenderer());	//see ReadoutFormatException.java
				
				JScrollPane jSP = new JScrollPane(sources, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
				//jSP.getHorizontalScrollBar().setVisible(false);
				jSP.setMaximumSize(new Dimension(600, 300));
				return jSP;
			}
		;
		JPanel panel3 = new JPanel();
		seasonGroup = new ButtonGroup();
		s30 = new JToggleButton("30", false);	s31 = new JToggleButton("31", false);	s32 = new JToggleButton("32", false);	s33 = new JToggleButton("33", true);
		ItemListener toggleSeasons =
			e -> {
				if(e.getStateChange() == ItemEvent.SELECTED) {
					panel3.remove(0);
					panel3.add(createSourceView.apply(((JToggleButton)e.getItem()).getText()), 0);
					panel3.repaint();
				}
			}
		;
		JPanel seasonPanel = new JPanel();
		seasonPanel.setLayout(new BoxLayout(seasonPanel, BoxLayout.LINE_AXIS));
		Stream.of(s30, s31, s32, s33).forEach(s -> { s.setFont(new Font("Jokerman", Font.BOLD, 24)); seasonGroup.add(s); s.addItemListener(toggleSeasons); seasonPanel.add(s); });
		
		imReadouter = new JTextField("WayoshiM");
		imReadouter.setToolTipText("What user to copy IMs over to readout.");
		imReadouter.setMaximumSize(new Dimension(200, 25));
		imReadouter.setHorizontalAlignment(SwingConstants.CENTER);
		
		convert = new JButton("\u2192");
		convert.setEnabled(false);
		convert.setFont(new Font("KaiTi", Font.BOLD, 108));
		convert.addActionListener(
			e -> {
				try {
					convertReadoutToAnalysis();
					check.showAnalyses(Stream.of(gA).collect(TreeSet::new, TreeSet::add, TreeSet::addAll));
					check.showSummary();
					finalize.setEnabled(true);
				} catch (ReadoutFormatException ex) {
					JOptionPane.showMessageDialog(convert, ex, "Readout Format Mismatch", JOptionPane.ERROR_MESSAGE);
					highlightLine(readout, ex.line, ERROR_LINE_STYLE);
				} catch (Exception ex2) {
					System.err.println(line + " at " + lineNumber);
					ex2.printStackTrace();
				}
			}
		);
		
		finalize = new JButton("Finalize");
		finalize.setEnabled(false);
		finalize.setFont(new Font("Jokerman", Font.BOLD, 54));
		finalize.addActionListener(
			e -> {
				if(gA != null && gaDate != null) {
					if(REDO_OVERRIDE || JOptionPane.showOptionDialog(finalize, "This will confirm the analysis of " + gaDate + " as done and in the multi-analysis set.",
						"Finish Analysis", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, Arrays.asList("OK", "Cancel").toArray(), "Cancel") == JOptionPane.YES_OPTION) {
						try(FileWriter fW = new FileWriter("analysis\\backup\\readouts\\" + gA.getFileName().replace("wga", "txt"))) {
							gA.writeAnalysis(true);
							gA.writeReport();
							GameAnalyses.updateGA(gA);
							fW.write(readout.getText());
							sources.repaint();
						} catch (Exception ex) {
							//System.out.println(ex);
							ex.printStackTrace();
						}
					}
				}
			}
		);
		
		appendix = new JButton("Options & Notes");
		appendix.setFont(new Font("Jokerman", Font.PLAIN, 24));
		appendix.addActionListener(
			e -> {
				Wedge w;
				if((w = (Wedge) JOptionPane.showInputDialog(appendix, "A.1. End any line with a ^ or $ to explicitly not process it.\n" +
				"A.2. Any lines that  ignored are shown in gray italics.\n\n" +
				"B.1. Spin strengths between 34 and 97 are assumed to be correct.\n" +
				"B.2. Between 26-33 and 100-105, you must clarify when converting.\n" +
				"B.3. To force <26 (usually 73-97) and >106 (usually 34-71), end the spinning line with an asterisk (*).\n\n" +
				"C.1. For toss-ups and BRs with a dash (-) at the end of a line on the puzzleboard, add a space after the dash in the answer, to properly count lines.\n\n" +
				"D.1. Insert an asterisk (*) to the immediate right of the cash part of a solved bank to force that manual amount.\n\n" +
				"Set the wedge underneath the Prize on the Wheel:", "Additional Info & Settings", JOptionPane.INFORMATION_MESSAGE, null,
				Arrays.asList(Wedge.Purple500_1, Wedge.Purple650, Wedge.Red800).toArray(), Wedge.getPrizeReplacement())) != null) {
					prizeReplacement = w;
					Wedge.setPrizeReplacement(prizeReplacement);
				}
			}
		);
		
		
		JPanel panel2 = new JPanel(new BorderLayout());
		//panel3 = new JPanel();
		panel3.setLayout(new BoxLayout(panel3, BoxLayout.PAGE_AXIS));
		panel3.add(createSourceView.apply("33"));
		//panel3.add(Box.createVerticalStrut(5));
		panel3.add(seasonPanel);
		//panel3.add(Box.createVerticalStrut(5));
		panel3.add(imReadouter);
		panel3.add(Box.createVerticalStrut(10));
		panel2.add(panel3, BorderLayout.NORTH);
		panel2.add(convert, BorderLayout.CENTER);
		JPanel panel4 = new JPanel(new BorderLayout());
		panel4.add(finalize, BorderLayout.NORTH);
		panel4.add(appendix, BorderLayout.SOUTH);
		panel2.add(panel4, BorderLayout.SOUTH);
		
		check = new AnalysisPanel(false);
		
		//add(new JLabel("Convert a raw readout into the corresponding analysis here.", SwingConstants.CENTER), BorderLayout.NORTH);
		add(panel, BorderLayout.WEST);
		add(panel2, BorderLayout.CENTER);
		add(check, BorderLayout.EAST);
	}
	
	private void enableReadout(String[] s2) {
		readout.setEnabled(true);
		convert.setEnabled(true);
		gaDateLabel.setText((gaDate = LocalDate.parse(s2[0] + "/" + s2[1] + "/" + s2[2], DateTimeFormatter.ofPattern("uu/M/d"))) + " | #" + WheelDateFactory.showNumberFromDate(gaDate));
		readout.setCaretPosition(0);
	}
	
	private void loadReadout(File f) {
		try {
			loadReadout(f.getName(), Files.readAllLines(f.toPath(), Charset.defaultCharset()));
		} catch (IOException e) {
			readout.setText("Could not load readout from file " + f + ":\n\n" + e);
			readout.setEnabled(false);
			convert.setEnabled(false);
		}
	}
	
	private void loadReadout(String fName, java.util.List<String> lineList) {
		resetHighlights(readout);
		
		String[] s2 = fName.split("\\.");
		
		try {
			readout.setText(new String(Files.readAllBytes(Paths.get("analysis\\backup\\readouts\\" +
				String.format("%02d.%02d.%02d.txt", Integer.parseInt(s2[0]), Integer.parseInt(s2[1]), Integer.parseInt(s2[2]))))));
			enableReadout(s2);
			return;
		} catch (NoSuchFileException ex) {
			//do nothing - done readout file is not here, so it's a new readout starting
		} catch(IOException ex) {	//some other IO error - investigate
			readout.setText("Could not load readout from file " + fName + ":\n\n" + ex);
			readout.setEnabled(false);
			convert.setEnabled(false);
			ex.printStackTrace();
			return;
		}
		
		if(lineList != null) {
			/* now usual procedure */
			boolean hasIMStarted = false;
			//String s = "^" + imReadouter.getText() + " \\([12]?\\d:[0-5]\\d:[0-5]\\d\\): .*$";
			String s = imReadouter.getText();
			
			java.util.List<String> lines = new LinkedList<>(lineList);
			StringBuilder str = new StringBuilder(2000);
			for(int i = 0; i < lines.size(); i++) {
				String line = lines.get(i);
				if(line.startsWith(s)) {
					String more = "\n";
					while(i < lines.size()-1 && !lines.get(i+1).contains("): ") && !lines.get(i+1).contains("):\t"))
						if(!lines.get(++i).matches("\\s*"))
							more += lines.get(i) + "\n";
					str.append(line.substring(Math.max(line.indexOf("): "), line.indexOf("):\t")) + 3) + more);
					hasIMStarted = true;
				} else if(!hasIMStarted)
					str.append(line + "\n");
			}
			
			readout.setText(optimizeReadout(str.toString()));
		}
		
		enableReadout(s2);
	}
	
	/* the super method of conversion. Called in try of event method above, so throw all exceptions there. */
	
	private void convertReadoutToAnalysis() throws IOException, ReadoutFormatException {
		gA = new GameAnalysis();
		gA.setPrizeReplacement(Optional.ofNullable(prizeReplacement).orElse(Wedge.Purple500_1));
		
		LinkedList<String> lines;
		resetHighlights(readout);
		
		try(FileWriter fW = new FileWriter(TEMP_READOUT)) {
			TEMP_READOUT.delete();
			fW.write(readout.getText());
		}
		
		int showNumber = WheelDateFactory.showNumberFromDate(gaDate);
		Wedge.CURRENT_SEASON = ((showNumber - 1) / 195) + 1;
		gA.setTitle(gaDate);
		
		int roundMinimum = 1000, fem = (int) females.getSelectedItem(); boolean six = doubles.isSelected();
		if(six)
			roundMinimum *= 2;	//2000 minimum if necessary
		else if(fem > 3)
			throw new ReadoutFormatException("Gender info", 0, "More than 3 females in a singles game");
		
		gA.analyzeGender(fem, six);
		
		Wedge.resetWedges();	//when doing multiple readouts at once
		Wedge.LOOKUP.remove("$1000");
		for(Player pl : Player.values())
			pl.reset();	//same
		
		// some appropriate intermediate variables
		//String line;
		Category c = null;	//save for duration of a round
		Category.setPlural(false);
		
		boolean expectingPuzzleBoard = false, ppTime = false, brTime = false, brCatPlural = false, madeBRCalls = false, jackpotTime = false, canGetJackpot = false, resetPattern = false, skipLine = false;
		String puzzleBoard = "", prizeOnWheel = null;
		int prizeOnWheelAmount = 0, ppAmount = 0, prevPosition = 0, jackpot = 5000,
			halfCarAmount = 0, brCarAmount = 0, roundNumber = 0, tossUpNumber = 0;
		TreeSet<Integer> puStringRegions = new TreeSet<>();
		Wedge finalSpinWedge = null;	//note that prev is outsourced to RecordGamePanel's static variable, since I kinda am locked into it in Wedge & Player
		BonusWedge bonusWedge = null;
		BonusAmount bA = null;	//a condition to end
		//end intermediate variables
		
		//for Mystery
		Wedge.Purple1000_1.setActive(true);
		Wedge.Purple1000_2.setActive(true);
		
		lines = new LinkedList<>(Files.readAllLines(TEMP_READOUT.toPath(), Charset.defaultCharset()));	//didn't work in try block (time?)
		
		for(lineNumber = 1; bA == null && (line = lines.poll()) != null; lineNumber++) {
			if(line.matches("\\s*") || skipLine || line.matches(".*[\\^\\$]$")) {
				skipLine = false;
				if(debug.isSelected())
					System.out.println("Skipping " + line + " at " + lineNumber + " by force");
				highlightLine(readout, line, SKIPPED_LINE_STYLE);
				continue;
			} else if(line.matches(".*([1-3]k|R[1-7]|TT)")) {	//category declaration, save for later. round is handled in GameAnalysis.
				//replace (always only one) substring of any whitespace followed by 1-3k / R1-6 with nothing, the category name.
				String cat = line.replaceFirst("\\s+([1-3]k|R\\d|TT)", "");
				c = lookupCategory(cat, line, lineNumber, false);
				
				expectingPuzzleBoard = line.matches(".*([1-3]k|TT)");
				resetPattern = line.matches(".*[2-3]k");
				jackpotTime = line.endsWith("R1");
				ppTime = line.endsWith("R3");
				puStringRegions.clear();	//reset
				
				if(line.matches(".*R\\d")) {
					roundNumber++;
					Player.setRoundStarter(roundNumber);
					Wedge.setRoundConfiguration(roundNumber);
					
					//resetting prev for R1-4.
					Wedge prev;
					if(roundNumber < 5) {
						if(Player.getCurrentPlayer() == Player.RED) {
							RecordGamePanel.setPrev(prev = Wedge.Orange300);
							prev.setPosition(Wedge.LEFT);
						} else if(Player.getCurrentPlayer() == Player.YELLOW) {
							RecordGamePanel.setPrev(prev = Wedge.Turquoise2500);		//this is only for determining spin strength, so we can substitute in later rounds
							prev.setPosition(Wedge.CENTER);
						} else {
							RecordGamePanel.setPrev(prev = Wedge.Yellow900);
							prev.setPosition(Wedge.RIGHT);
						}
					}
				}
				
				continue;
			}
			
			//leeway: get info with extraneous ""; get info with extra spaces and fix now
			LinkedList<String> action = Arrays.stream(line.split(isSolve(line) ? (!brTime ? " - " : "( - |\\.)") : LINE_REGEX))
				.filter(s -> !s.matches("\\s*")).map(s -> s.trim()).collect(Collectors.toCollection(LinkedList::new));
			if(debug.isSelected())
				System.out.println("Action for " + lineNumber + ": " + action);
			
			if(!isSolve(line))
				canGetJackpot = false;
			
			Player p = Player.getCurrentPlayer();
			Wedge prev = RecordGamePanel.getPrev();
			if(debug.isSelected())
				System.out.println(brTime ? "BR at " + lineNumber + ", bW: " + bonusWedge + ", cat: " + c + ", bA: " + bA :
					p + " at " + lineNumber + (p != null ? ", " + p.getRoundScore() + " pure cash bank" : ""));	//debug
			
			//now big block of ifs.
			if(expectingPuzzleBoard) {	//impossible really to write an explicit regex when puzzle itself can have conflicting punctuation
				line = line.trim().replaceAll("[ ]{2,}", " ");
				puzzleBoard += (!puzzleBoard.isEmpty() ? "<br>" : "") + line;
				
				if(puStringRegions.isEmpty())
					puStringRegions.add(0);
				int regionPoint = puStringRegions.last() + (int) line.chars().filter(ch -> ch != ' ').count();
				puStringRegions.add(regionPoint);
				//skip over newline (which will be a space in puString, separating words). there's an exception for dashes taken care of in insertNewLines
				if(expectingPuzzleBoard = lines.peek().trim().replaceAll("[ ]{2,}", " ").matches(BOARD_REGEX))
					puStringRegions.add(regionPoint + 1);
				if(debug.isSelected())
					System.out.println("puStringRegions update at " + lineNumber + ": " + puStringRegions);
			} else if(line.startsWith("PRIZE:")) {	//Prize on Wheel info (starts R1). put early in this block to avoid some character errors
				String s = action.get(1);
				prizeOnWheel = s.substring(0, s.lastIndexOf(' '));
				prizeOnWheelAmount = Integer.parseInt(s.substring(s.lastIndexOf(' ')+1));
				
				Wedge.LOOKUP.put(prizeOnWheel, Wedge.PrizeOnWheel);
				gA.analyzePrizeOnWheel(prizeOnWheelAmount);
			} else if(line.matches("[\\w&]+ spin(s)? .+(, (buzz[ ]?out|[A-Z], ([0-9]{1,2}|called))[\\.!]?)?")) {	//action should have: abbr, wedge, third (if not MDW), letter, freq
				Wedge w = Wedge.LOOKUP.get(action.get(1));
				Letter l = null;
				Byte freq = null;
				
				if(w == null) {
					if(JOptionPane.showConfirmDialog(null, "Confirm \"" + action.get(1) + "\" as Prize on Wheel for this show?", "Prize on Wheel",
						JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
						w = Wedge.PrizeOnWheel;
						String[] a = action.get(1).split("[\\{\\}]");
						Wedge.LOOKUP.put(a[0], Wedge.PrizeOnWheel);
						if(a.length == 2) {
							try {
								prizeOnWheelAmount = Integer.parseInt(a[1]);
								gA.analyzePrizeOnWheel(prizeOnWheelAmount);
								if(debug.isSelected())
									System.out.println("Amount detected in curly brackets at " + lineNumber + ": " + prizeOnWheelAmount);
							} catch(NumberFormatException e) {
								System.err.println("Unable to parse \"" + a[1] + "\" as prize amount");
							}
						}
					} else
						throw new ReadoutFormatException(line, lineNumber, "Wedge \"" + action.get(1) + "\" not found");
				}
				
				/* copied over from RecordGamePanel with some changes */
				
				if(prev == w)
					prevPosition = prev.getPosition();
				
				boolean isMDW;
				
				if(isMDW = w == Wedge.MillionDollarWedge)
					switch(action.get(1).toUpperCase()) {
						case "MDW":
							w.setPosition(Wedge.CENTER);
							break;
						case "LEFT MDW BANKRUPT":
							w.setPosition(Wedge.LEFT);
							break;
						case "RIGHT MDW BANKRUPT":
							w.setPosition(Wedge.RIGHT);
							break;
					}
				else
					switch(action.get(2)) {
						case "C":
						case "?":
							w.setPosition(Wedge.CENTER);
							break;
						case "L":
							w.setPosition(Wedge.LEFT);
							break;
						case "R":
							w.setPosition(Wedge.RIGHT);
							break;
						default:
							throw new ReadoutFormatException(line, lineNumber, "Third \"" + action.get(2) + "\" not L, C, R or ?");
					}
				
				int spinStrength = Wedge.determineStrength(prev, w, false, prevPosition);	//assume no OR, then fix
				boolean spinStrengthException = line.endsWith("*"), pat = action.get(0).equals("Pat");	//force <28 or >110
				
				if(!REDO_OVERRIDE &&	//Pat is not a triple-digit spinner anymore, lol
					((spinStrength < 26 && !spinStrengthException) || (spinStrength > 33 && spinStrengthException) ||
					(26 <= spinStrength && spinStrength <= 33 && JOptionPane.showOptionDialog(check,
					"R" + roundNumber + "\nPlayer: " + (pat ? "Pat (FINAL SPIN)" : (p + " (" + p.getName() + ")")) +
					"\nnow: " + w + ", " + w.getPosition() + "\nprev: " + prev + ", " + prevPosition,
					"Spin Strength Check", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
					null, Arrays.asList(spinStrength+72, spinStrength).toArray(), spinStrength) == JOptionPane.YES_OPTION)))
					spinStrength += 72; // check gray area of 26-33 / 98-105
				
				if(spinStrengthException && line.matches(".*\\*-?\\d+\\*")) {
					Scanner specialScan = new Scanner(line.substring(line.indexOf("*")));
					specialScan.useDelimiter("\\*");
					spinStrength += specialScan.nextInt();
				}
				
				RecordGamePanel.setPrev(w);
				
				if(pat) {
					finalSpinWedge = w;
					gA.analyzeSpin(null, w, null, null, (short) spinStrength, false);
					if(REDO_OVERRIDE)
						gA.setSpinStrengths(OLD_SPIN_STRENGTHS);
					continue;
				}
				
				boolean isBlack = Wedge.isBlack(w), isBlackAndWhite = isBlack || w == Wedge.LOSE_A_TURN;
				
				if(action.size() >= 4 && action.get(3).contains("buzz"))	//spin & buzzout combo, buzz is where letter would be
					gA.analyzeBuzz(null);
				else if(!isBlackAndWhite) {
					//similar code to convertCall
					String letterString = action.get(isMDW ? 2 : 3), freqString = action.get(isMDW ? 3 : 4);
					parseLetterAndFreq(letterString, freqString, line, lineNumber);
					l = CALLED_LETTER.get();
					freq = CALLED_FREQ.get();
					checkSameLetter(c, action, p);
				}
				
				boolean isExempt = (w == Wedge.FreePlay),
					didFail = freq == null || freq == 0,	//can properly evaluate this now that freq has been read
					wasMysteryBankrupt = false, wasFPSolveAttempt = false, wasExpressRideSuccessful = true, didExpressRide = false;
				
				if(didFail && !isBlackAndWhite && !isExempt)
					Player.loseTurn();
				else
					switch(w) {
						case HalfCar_1:
						case HalfCar_2:
						case HalfCar_3:
						case HalfCar_4:
						case HalfCar_5:
						case HalfCar_6:
							if((Integer.parseInt(w.name().substring(w.name().length()-1))-1) / 2 != roundNumber-1)	//1 and 2 --> 1-1, 3 and 4 --> 2-1, 5 and 6 --> 3-2
								throw new ReadoutFormatException(line, lineNumber, w + " impossible in R" + roundNumber);
						case WildCard:	//as of S30
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
							if(w.getPosition() == Wedge.CENTER) 
								p.addRoundStatus(w);
							else
								Player.loseTurn();
							Wedge.replaceWedge(w);
							break;
						case Purple1000_1:
						case Purple1000_2:
							String next1 = lines.peek();	//why lines needs to explicitly be a LinkedList
							if(next1.startsWith("\tand")) {
								lines.poll();	//eat line
								w.setActive(false);	//GameAnalysis uses isActive here to analyze
								if(wasMysteryBankrupt = next1.contains("BANKRUPT"))
									Player.loseTurn();
								else
									p.addRoundStatus(w);
								Wedge.replaceWedge(w);
								Wedge.LOOKUP.put("$1000", w == Wedge.Purple1000_1 ? Wedge.Purple1000_2 : Wedge.Purple1000_1);
							} else
								p.addMoney(w, freq);
							break;
						case Express:
							p.addMoney(w, freq);
							int riskedMoney = p.getRoundScore();	//if going for it, this is the risked dough to save now
							boolean isM = false;	//derailed ride due to missolve or not
							Byte lastFreq = 1;	//to see if derailed ride due to dud/repeat
							
							if(didExpressRide = lines.peek().startsWith("\t")) {
								LinkedHashMap<Letter, Byte> expressRide = new LinkedHashMap<>();	//INSERTION ORDER!
								String next;
								
								/** process the entire chain creating the express "run". I could have an expressFlag I suppose, but
									it's a new situation where continuous letter CALLS (not SPINS) are made by the same player, and
									I will want lists when analyzing it all. This is ultimately more compact.
									Note that analyzing each letter call in the run is taken care of in order in GameAnalysis. */
								while((next = lines.peek()).startsWith("\t") && !isSolve(next) && !isTooLong(next) && !(isM = isMissolve(next))) {
									lineNumber++;
									lines.remove();
									
									if(!next.contains("NO MORE") && !next.contains("FULLY FILLED IN")) {
										try {
											//syntax: \tletter, freq. so trim() is important.
											//redoing fetching letter & freq
											LinkedList<String> nextAction = new LinkedList<>();
											for(String s : next.split(LINE_REGEX))
												if(!s.isEmpty())
													nextAction.add(s.trim());
											
											//redoing fetching letter & freq
											String letterString2 = nextAction.get(0), freqString2 = nextAction.get(1);
											
											parseLetterAndFreq(letterString2, freqString2, next, lineNumber);
											Letter l2 = CALLED_LETTER.get();
											Byte freq2 = CALLED_FREQ.get();
											checkSameLetter(c, nextAction, p);
											lastFreq = freq2;
											
											expressRide.put(l2, freq2);
											if(Letter.CONSONANTS.contains(l2))
												Optional.ofNullable(freq2).ifPresent(f2 -> p.addMoney(Wedge.Express, f2));
											else
												p.buyVowel();
											
											if(debug.isSelected())
												System.out.println("Express call at " + lineNumber + ": " + letterString2 + ", " + freqString2 + "; " + p + " now at " + p.getRoundScore());
										} catch(IndexOutOfBoundsException e) {	//spl is only length 1, probably a commentary line
											if(debug.isSelected())
												System.out.println("Skipping \"" + next + "\" at " + lineNumber);
											highlightLine(readout, next, SKIPPED_LINE_STYLE);
										}
									}
								}
								
								wasExpressRideSuccessful = next.startsWith("\t") && isSolve(next);	//some immediate future processing here
								gA.analyzeExpressRide(p, expressRide, riskedMoney, wasExpressRideSuccessful, isM);	//see ExpressRide object
							}
							
							if(wasExpressRideSuccessful || (lastFreq != null && lastFreq > 0))	//not a dud/repeat derail - let missolve or buzzout area take care of loseTurn
								break;
						case BANKRUPT_1:
						case BANKRUPT_2:
						case LOSE_A_TURN:
							Player.loseTurn();
							break;
						case FreePlay:
							if(Letter.CONSONANTS.contains(l)) {
								if(freq != null)	//in the very rare case a repeat is called on Free Play...
									p.addMoney(w, freq);
							}
							break;
						default:
							p.addMoney(w, freq);
					}
				
				gA.analyzeSpin(p, w, l, freq, (short) spinStrength, wasMysteryBankrupt || didExpressRide);
				
				if(jackpotTime)
					jackpot += w.getAmount();
				
				if(isBlack || wasMysteryBankrupt || !wasExpressRideSuccessful)
					p.bankrupt();
				
				canGetJackpot = w == Wedge.Jackpot;				
			} else if(line.matches("[\\w&]+ buy(s)? [AEIOU], ([0-9]{1,2}|called)(, and all bought)?\\.?.*$")) {	//action should have: abbr, letter, freq (or just "called")
				p.buyVowel();
				if(!convertCall(p, action, null, lineNumber, line))
					Player.loseTurn();
			} else if(isMissolve(line)) {	// non-BR missolve area
				gA.analyzeSolveAttempt(null, null);	//see this method, as long as puzzle is null player doesn't matter. this covers all possible missolves in the maingame.
				if(!puzzleBoard.isEmpty()) {	//toss-up missolve
					String l = lines.peek();
					if(!isSolve(l) && !isMissolve(l) && !isTooLong(l)) {
						puzzleBoard = "";
						puStringRegions.clear();
						expectingPuzzleBoard = true;
					}
				}
				else if(finalSpinWedge == null || !stillHasTurn(lines))	//if not speed-up time OR missolving is over in a speed-up turn (short-circuit || critical here)
					Player.loseTurn();
			} else if(isTooLong(line)) {	//buzzout area
				if(debug.isSelected())
					System.out.println("Buzzout at " + lineNumber);
				gA.analyzeBuzz(null);	//again, tracking buzzes by color was removed and is a "leftover" of the code
				if(!puzzleBoard.isEmpty()) {	//toss-up "blank"
					String l = lines.peek();
					if(!isSolve(l) && !isMissolve(l) && !isTooLong(l)) {
						puzzleBoard = "";
						puStringRegions.clear();
						expectingPuzzleBoard = true;
					}
				} else
					Player.loseTurn();
			} else if(!puzzleBoard.isEmpty() && !brTime) {	//tossup solve; should not be from a puzzleboard. should have puzzle, scoreline
				if(debug.isSelected())
					System.out.println(puzzleBoard);
				if(action.size() == 2) {	//toss-up solve
					tossUpNumber++;
					boolean isTiebreaker = finalSpinWedge != null;
					
					TossUpPuzzle pu = new TossUpPuzzle(insertNewLinesIntoPuzzleBoard(action.get(0), puStringRegions));
					if(debug.isSelected())
						System.out.println(pu);
					try {
						pu.setTossUpState(puzzleBoard);
					} catch(IllegalArgumentException e) {
						throw new ReadoutFormatException(line, lineNumber, e.getMessage());
					}
					
					Player pl = tossUpWinner(action.get(1), tossUpNumber, isTiebreaker);	//ignore p in this case
					
					gA.analyzeSolveAttempt(pl, pu);
					puzzleBoard = "";
					
					if(pl != null) {
						if(!isTiebreaker)
							pl.winTossUp(tossUpNumber);
						else {
							brTime = true;
							puStringRegions.clear();
							Player.setCurrentPlayer(pl);	//tiebreaker toss-up winner gets to BR, but no more maingame winnings
						}
					} else if(tossUpNumber == 2)
						pl = Player.RED;	//for 2k. RED starts R1. For 3k, R4 starter defaults back to R1's, so keep null to not overwrite pattern.
						//throw new ReadoutFormatException(action.get(1), lineNumber, "Toss-up winner could not be determined");
					
					if(resetPattern)
						Player.setRoundPattern(pl);
					gA.analyzeRound(c, 0, 0);
				} else {	//score update 
					
				}
			} else if(isSolve(line)) {	//non-tossup solve area. puzzles should not have slashes in them fortunately. puzzle, scoreline/BR amount
				if(bonusWedge == null) {
					Puzzle puzzle = new MainPuzzle(action.get(0).replaceAll(" \\(n\\) ", "<br>"));
					String bank = action.getLast();
					boolean customRoundScore = false;
					
					if(customRoundScore = action.getLast().matches("^\\d{4,5}\\*.*$")) {	//custom exception: set pure cash of round to indicated amount, for use in bad null cycles
						int newRoundScore = Integer.parseInt(bank.substring(0, bank.indexOf('*')));
						if(debug.isSelected())
							System.out.println("Custom bank change for " + p + " at " + lineNumber + ": from " + p.getRoundScore() + " to " + newRoundScore);
						p.setMoney(newRoundScore);
					}
					
					int rawBank = p.getRoundScore();
					
					if(p.hasGiftTag())
						p.addMoney(1000);
					if(p.hasPrize())
						p.addMoney(prizeOnWheelAmount);
					if(p.hasMystery())
						p.addMoney(10000);
					if(canGetJackpot)
						p.addMoney(jackpot);
					
					boolean wonHalfCar = false, isRoundMinimum = false, tripWon = ppTime || p.hasPrize();	//wonHalfCar used in analyzeRound below
					
					//single | because, on PP & Car rounds, I have to process numHalfCars! (or I could put wonHalfCar first)
					if((ppTime | (wonHalfCar = p.numHalfCars() >= 2)) || (p.hasPrize() && prizeOnWheelAmount == 0)) {
						String s = bank;
						lineNumber++;
						while(!(line = lines.poll()).matches("(\\d|\\+(MDW|WC|1/2Car))+(/(\\d|\\+(MDW|WC|1/2Car))+){2}")) {
							s += line + "\n";
							lineNumber++;
						}
						
						Scanner customAmountScan = new Scanner(s).useDelimiter("\\D+");
						
						boolean seenRaw = false;	//in the rare case the custom amount is equal to the raw bank
						
						while(customAmountScan.hasNextInt()) {
							int i = customAmountScan.nextInt();
							
							if(i == rawBank && !seenRaw) {
								seenRaw = true;
								continue;
							}
							
							if(tripWon && wonHalfCar) {
								//choose based on an arbitrary limit. I would hope I never get KO'd for this. :P
								if(i >= 13920)	//Chevy Spark base price
									halfCarAmount = i;
								else if(i >= 5000)
									ppAmount = i;
								else
									continue;
								
								p.winCustomAmount(i);
								
								if(ppAmount != 0 && halfCarAmount != 0)
									break;
							} else if(tripWon && i >= 5000) {
								ppAmount = i;
								p.winCustomAmount(i);
								break;
							} else if(i >= 13290) {	//wonHalfCar
								halfCarAmount = i;
								p.winCustomAmount(i);
								break;
							}
						}
						
						if(debug.isSelected())
							System.out.println("Custom amounts | PP/PW: " + ppAmount + "\t1/2 Car: " + halfCarAmount);
					} else
						isRoundMinimum = p.getRoundScore() < roundMinimum;  //&& !ppTime && !wonHalfCar implied here
					
					if(isRoundMinimum && !customRoundScore)
						p.setMoney(roundMinimum);
					
					try {
						gA.analyzeSolveAttempt(p, puzzle);
					} catch(IllegalArgumentException iae) {
						throw new ReadoutFormatException(line, lineNumber, iae.getMessage());
					}
					
					if(jackpotTime)
						gA.analyzeJackpot(jackpot, canGetJackpot);
					
					/* the order of the rest of this branch is critical for analyzeRound to properly function. */
					
					for(Player pl : Player.values())
						pl.winRound(pl == p);
					if(finalSpinWedge != null) {
						for(Player pl : Player.values())
							pl.checkTotalMinimum(roundMinimum);
						brTime = true;	//lets &= work below
						puStringRegions.clear();
						Player.setCurrentPlayer(Arrays.stream(Player.values()).max((p1, p2) -> p1.getTotalScore() - p2.getTotalScore()).get());	//basically calculating winner on the fly here
					}
					
					brTime &= !gA.analyzeRound(c, tripWon ? ppAmount : 0, wonHalfCar ? halfCarAmount : 0);
					
					if(roundNumber == 3 || wonHalfCar) {
						for(Player pl : Player.values())
							pl.resetHalfCars();	//no harm in letting this run twice-thrice if won in R1-2
						halfCarAmount = 0;	//critical, or else can be counted in subsequent rounds in winnings
					}
				} else {
					String brString = insertNewLinesIntoPuzzleBoard(action.get(0), puStringRegions);
					
					gA.analyzeSolveAttempt(p, new BonusPuzzle(brString));
					
					bA = BonusAmount.LOOKUP.get(action.get(1));
					
					if(bA == null) {
						bA = BonusAmount.VEHICLE;
						try {
							IntStream.Builder isb = IntStream.builder();
							String s = readout.getText();
							Scanner vehicleScan = new Scanner(s.substring(s.lastIndexOf(action.get(1)))).useDelimiter("\\D+");
							while(vehicleScan.hasNextInt())
								isb.accept(vehicleScan.nextInt());
							brCarAmount = isb.build().filter(i -> i >= 22500).min().getAsInt();	//usually do both car and overall total. cheapest brCar has been 23/24k
						} catch(Exception e) {
							throw new ReadoutFormatException(line, lineNumber, "Unable to find a possible brCarAmount (at least 22500)");
						}
					} else if(bA == (p.hasMDW() ? BonusAmount.$100000 : BonusAmount.ONE_MILLION))
						throw new ReadoutFormatException(line, lineNumber, bA + " is not possible with MDW status");
					
					gA.analyzeBonus(bonusWedge, bA, lowerGas.isSelected());
					Category.setPlural(brCatPlural);
					gA.analyzeRound(c, brCarAmount, 0);	//should be the last processing done at all
					
					if(debug.isSelected())
						System.out.println(brString + "\t" + bA);
				}
			} else if(line.contains("/") /*&& !brTime*/ && !line.matches(BOARD_REGEX)) {
				if(line.contains(" / ")) {	//names
					String[] names = line.split(" / ");	//ignore action here
					for(Player pl : Player.values()) {
						//System.out.println(names[pl.ordinal()]);
						pl.setName(names[pl.ordinal()]);
					}
					Player.checkAbbrNames();
				} else {
					//score update
				}
			} else if(line.matches("[\\w&]+: [A-Z], ([0-9]{1,2}|called)\\.?.*$")) {	//speed-up call. should contain abbr, letter, freq
				//note how I have curly brackets even with just one simple if. The compiler would have interpreted the rest below as a chain of else ifs inside of this else if!
				if(!convertCall(p, action, finalSpinWedge, lineNumber, line) || !stillHasTurn(lines))
					Player.loseTurn();
			} else if(line.contains("BZZ")) {	//going to use " - " vs ", " for BR win/loss I think
				//ended up not needing anything here
			} else if(brTime) {
				if(bonusWedge == null) {	//wait to do anything with brTime until formal start of BR, allowing comments
					if((bonusWedge = BonusWedge.LOOKUP.get(action.get(0))) == null) {
						if(debug.isSelected())
							System.out.println("Skipping \"" + line + "\" at " + lineNumber);
						highlightLine(readout, line, SKIPPED_LINE_STYLE);
					}
				} else
					switch(action.size()) {
						case 1:
							String unknown = action.get(0);
							Category c2 = lookupCategory(unknown, line, lineNumber, true);
							
							if(c2 != null) {
								c = c2;
								brCatPlural = Category.isCurrentlyPlural();
								if(debug.isSelected())
									System.out.println("brCatPlural: " + brCatPlural);
								expectingPuzzleBoard = true;
							} else if(madeBRCalls && !line.matches(BOARD_REGEX) && line.matches("^[A-Z'&-?!/ ]+$")) {
								if(debug.isSelected())
									System.out.println(line + " at " + lineNumber + " counted as BR missolve");
								gA.analyzeSolveAttempt(p, null);
							} else if(!lines.peek().isEmpty() && !madeBRCalls) {
								//nothing needed here due to refactoring
							}
							break;
						case 4: case 5: //letter calls
							madeBRCalls = true;
							for(String sl : action) {
								Letter l = Letter.getLetter(sl);
								if(l != null)
									gA.analyzeCall(p, l, null);	//currentRound should be BR
								else
									throw new ReadoutFormatException(line, lineNumber, "Letter \"" + sl + "\": capital A-Z only");
							}
							break;
						case 2:	//BR answer after BZZ failure
							//System.out.println(action.get(0) + puStringRegions);
							String pu = action.get(0);
							if(pu.equals(pu.toUpperCase())) {	//this should take care of a lot of garbage lines
								String brString = insertNewLinesIntoPuzzleBoard(pu, puStringRegions);
								gA.analyzeSolveAttempt(null, new BonusPuzzle(brString));
								bA = BonusAmount.LOOKUP.get(action.get(1));
								
								if(bA == null)
									bA = BonusAmount.VEHICLE;
								else if(bA == (p.hasMDW() ? BonusAmount.$100000 : BonusAmount.ONE_MILLION))
									throw new ReadoutFormatException(line, lineNumber, bA + " is not possible with MDW status");
								
								gA.analyzeBonus(bonusWedge, bA);
								Category.setPlural(brCatPlural);
								gA.analyzeRound(c, 0, 0);
								if(debug.isSelected())
									System.out.println(brString + "\t" + bA);
							}
					}	//else ignore line. BR solves are taken care of in the solve area above.
			} else if(line.contains("*DING DING*")) {	//final spin indicator
				Wedge.adjustPrevWedge(prev, p, true);
			} else if(line.startsWith("\tuse")) {	//WC maingame use. "use[s] WC", letter, freq (let's not worry about buzzouts now)
				String letterString = action.get(1), freqString = action.get(2);
				
				parseLetterAndFreq(letterString, freqString, line, lineNumber);
				Letter l = CALLED_LETTER.get();
				Byte freq = CALLED_FREQ.get();
				checkSameLetter(c, action, p);
				
				p.setTotalStatus(Wedge.WildCard, false);
				if(freq == null || freq == 0)
					Player.loseTurn();
				else
					p.addMoney(prev, freq);
				
				gA.analyzeSpin(p, prev, l, freq, (short) 0, false);
			} else if(!line.contains("NO MORE") && !line.contains("FULLY FILLED IN")) {
				if(debug.isSelected())
					System.out.println("Skipping \"" + line + "\" at " + lineNumber);
				highlightLine(readout, line, SKIPPED_LINE_STYLE);
				//throw new ReadoutFormatException(line, lineNumber, "Not a recognized action / header");
			}
		}
	}
	
	/* begin utility methods and fields */
	
	private static Category lookupCategory(String cat, String line, int lineNumber, boolean nullAllowed) {
		Category c = Category.LOOKUP.get(cat);
		if(c == null) {
			c = Category.PLURAL_LOOKUP.get(cat);
			if(c != null)
				Category.setPlural(true);
			else if(!nullAllowed)
				throw new ReadoutFormatException(line, lineNumber, "Category \"" + line.replaceFirst("\\s*([1-3]k|R\\d|TT)", "") + "\" not found");
			else
				Category.setPlural(false);
		} else
			Category.setPlural(false);
			
		return c;
	}
	
	private static boolean isSolve(String l) {
		return l.contains(" - ") && !l.matches(BOARD_REGEX);	//BOARD_REGEX check is needed for dashes in BRs
	}
	
	private static boolean isMissolve(String l) {
		return l.contains("is wrong");
	}
	
	private static boolean isTooLong(String l) {
		return l.matches(".*(blank|buzz(es)?[ ]?out).*");
	}
	
	private boolean convertCall(Player p, LinkedList<String> action, Wedge w, int lineNumber, String line) throws ReadoutFormatException {
		String letterString = action.get(1), freqString = action.get(2);
		
		parseLetterAndFreq(letterString, freqString, line, lineNumber);
		Letter l = CALLED_LETTER.get();
		Byte freq = CALLED_FREQ.get();
		
		gA.analyzeCall(p, l, freq);
		if(w != null && Letter.CONSONANTS.contains(l) && freq != null)	//speedup consonant call
			p.addMoney(w.getAmount() + 1000, freq);
		
		return !(freq == null || freq == 0);
	}
	
	//encapsulating one variable as an object, not really taking advantage of volatile effects
	private static final AtomicReference<Letter> CALLED_LETTER = new AtomicReference<>();
	private static final AtomicReference<Byte> CALLED_FREQ = new AtomicReference<>();
	
	private static void parseLetterAndFreq(String letterString, String freqString, String line, int lineNumber) throws ReadoutFormatException {
		try {
			CALLED_LETTER.set(Letter.getLetter(letterString));
			CALLED_FREQ.set(CALLED_LETTER.get() != null ? Byte.parseByte(freqString) : null);
		} catch(NumberFormatException e) {
			if(!freqString.equalsIgnoreCase("called"))
				throw new ReadoutFormatException(line, lineNumber, "Frequency \"" + freqString + "\": Non-negative number or \"called\" only");
			CALLED_FREQ.set(null);
		}
		if(CALLED_LETTER.get() == null)
			throw new ReadoutFormatException(line, lineNumber, "Letter \"" + letterString + "\": capital A-Z only");
	}
	
	//should be done immediately after parsing freq (to catch the error of "spins whatever, B, SL." or something)
	private static void checkSameLetter(Category c, LinkedList<String> action, Player p) {
		if(c == Category.SAME_LETTER && action.getLast().equalsIgnoreCase("SL"))
			p.addMoney(1000);
	}
	
	private static boolean stillHasTurn(LinkedList<String> lines) {
		String l = lines.peek();
		if(l.contains("NO MORE")) l = lines.get(1);	//second line. kinda breaks implementation of queue, but I kinda need it!
		return l.contains("is wrong") || l.contains(" - ");
	}
	
	private static Player tossUpWinner(String scoreLine, int tossUpNumber, boolean isTieBreaker) {
		if(isTieBreaker) {
			//let's do podium color or name or abbrName for this once-in-a-blue-moon thing, when we can't rely on scoreline.
			try {
				return Enum.valueOf(Player.class, scoreLine.toUpperCase());
			} catch(IllegalArgumentException e) {
				return Arrays.stream(Player.values()).filter(p -> p.getName().equals(scoreLine) || p.getAbbrName().equals(scoreLine)).findAny().orElse(null);
			}
		} else if(!scoreLine.contains("/")) {	//shortcut should be for 3k only. 1k you must define, 2k you probably need to define and should
			int score = new Scanner(scoreLine).useDelimiter("\\+").nextInt();	//ignore +WC or +MDW
			//System.out.println(score);
			for(Player p2 : Player.values())
				if(p2.getTotalScore() + 1000*tossUpNumber == score)
					return p2;
			return null;	//triple stumper
		} else {
			String[] scores = scoreLineSplit(scoreLine);
			for(Player p : Player.values())
				if(p.getTotalScore() != Integer.parseInt(scores[p.ordinal()].split(BANK_REGEX)[0]))	//only numerical part of score
					return p;
			return null;	//triple stumper
		}
	}
	
	private static String[] scoreLineSplit(String scoreLine) {
		String[] split = scoreLine.split("\\b/\\b");	//includes 1/2Car, hence this separate method to deal with it.
		if(split.length != 3) {
			String[] sLS = new String[3];
			for(int i = 0, j = 0; i < 3; i++, j++) {
				sLS[i] = split[j];
				if(split[j].endsWith("+1"))
					sLS[i] += '/' + split[++j];	//separation of i & j here
			}
			return sLS;
		} else
			return split;
	}
	
	private String insertNewLinesIntoPuzzleBoard(String puString, TreeSet<Integer> puStringRegions) {
		if(debug.isSelected())
			System.out.println("insertNewLine parameters: " + puString + "\t" + puStringRegions);
		if(puStringRegions == null || puStringRegions.size() == 0 || puStringRegions.size() % 2 != 0)
			throw new IllegalArgumentException("String region set must have an even non-zero number of elements");
		
		if(puString.contains("-")) {
			StringBuilder puSB = new StringBuilder(puString);
			//search all ends of regions, but not the very end
			puStringRegions.stream().filter(i -> new Integer(i+1).equals(puStringRegions.higher(i)))
			.forEachOrdered(i -> {
				if(puSB.charAt(i-1) == '-')
					puSB.insert(i, " ");
			});
			puString = puSB.toString();
		}
		
		Integer start = puStringRegions.first(), end = puStringRegions.higher(start);
		String puNLString = puString.substring(start, end);
		
		for(start = puStringRegions.higher(end); start != null; start = puStringRegions.higher(end)) {
			end = puStringRegions.higher(start);
			puNLString += "<br>" + puString.substring(start, end);
		}
		
		return puNLString;
	}
	
	private static int frequencyOfChar(String s, char c) {
		int freq = 0, pos = 0;
		while((pos = s.indexOf(c, pos) + 1) != 0)
			freq++;
		
		return freq;
	}
	
	/* for basic typo corrections, for now a single swap of any two consecutive letters */
	
	private static final Map<String, String> TYPO_REGEX_MAP;
	static {
		TYPO_REGEX_MAP = Arrays.stream
			("BANKRUPT/LOSE A TURN/orange/yellow/blue/red/green/purple/spins/buys/NO MORE VOWELS/NO MORE CONSONANTS/FULLY FILLED IN/wrong/blanks/and all bought/and go".split("/"))
			.collect(Collectors.toMap(k -> k, v -> allBasicTyposOf(v).toString().replace('[', '(').replace(']', ')').replace(", ", "|")));
	}
	private static Set<String> allBasicTyposOf(String s) {
		Set<String> anagrams = new HashSet<>();
		
		for(int i = 0; i < s.length() - 1; i++)
			anagrams.add(s.substring(0, i) + s.charAt(i+1) + s.charAt(i) + s.substring(i+2));
		
		/* if (s == null) {
            return null;
        } else if (s.length() == 0) {
            anagrams.add("");
            return anagrams;
        }
		
		char first = s.charAt(0);
		String rest = s.substring(1);
		for(String subAna : allAnagramsOf(rest))
			for(int i = 0; i <= subAna.length(); i++) 
				anagrams.add(subAna.substring(0, i) + first + subAna.substring(i));
		*/
		
		return anagrams;
	}
	
	/* most important utility method - autonomize common changes from IM to proper readout */
	private static String optimizeReadout(String r) {
		/* TO DO: 
			
		*/
		
		StringBuilder s = new StringBuilder(r);
		
		// trim everything up to 1k. bA finishing for loop effectively trims the variable end
		int _1k_index = s.indexOf(" 1k");
		while(_1k_index > 0 && s.charAt(_1k_index-1) != '\n')
			_1k_index--;
		if(_1k_index > 0)
			s.delete(0, _1k_index);
		
		//add tab space to appropriate lines
		fixStringBuilder(s, "\nuse", "\n\tuse");
		fixStringBuilder(s, "\nand go", "\n\tand go");
		//combine two-line Mystery risk
		fixStringBuilder(s, "and go\nand", "and go, and");
		//SAME LETTER cuts
		s = new StringBuilder(s.toString().replaceAll("\\([2-5] words\\)", ""));
		fixStringBuilder(s, "(same letter)", "");
		//some more cuts
		fixStringBuilder(s, " after a triple dud", "");
		fixStringBuilder(s, "*BZZ*", "");	//for close speed-ups, obviously this ruins analysis
		//some common typos
		fixStringBuilder(s, "< ", ", ");
		fixStringBuilder(s, "(N)", "(n)");
		
		//shift third in mystery spins
		Matcher mysteryMatcher = Pattern.compile("\\$1000\\([LCR?]\\) (next to LAT|next to MDW|next to where MDW was)").matcher(s);
		while(mysteryMatcher.find()) {	//sets start, end, and group
			String mysterySpin = mysteryMatcher.group();
			// $1000 (C)
			// 01234 5678
			s.replace(mysteryMatcher.start(), mysteryMatcher.end(), mysterySpin.substring(0, 5) + mysterySpin.substring(8) + mysterySpin.substring(5, 8));
		}
		
		//combine two-line critical call
		Matcher bigMoneyMatcher = Pattern.compile("spin(s)? ((\\$(25|35|50)00|1/2 Car #[1-6]|Express)\\([LCR?]\\)|MDW)\n[A-Z]").matcher(s);
		while(bigMoneyMatcher.find())	//sets start, end, and group
			s.replace(bigMoneyMatcher.start(), bigMoneyMatcher.end(), bigMoneyMatcher.group().replace("\n", ", "));
		
		/** fix names. this matcher should only match once. e.g.
			... "0/1000/0
			Hi, Me, Test
			T/H/M"
			The abbrs are required because sometimes I will properly align the names in a non-IM setting.
		*/
		Matcher nameMatcher = Pattern.compile("(1000/0/0|0/1000/0|0/0/1000)\n+[A-Za-z&\\(\\)\\. ]+((, | / )[A-Za-z&\\(\\)\\. ]+){2}\n+[A-Za-z&]+(/[A-Za-z&]+){2}").matcher(s);
		if(nameMatcher.find()) {
			String n = nameMatcher.group().replace(", ", " / ");
			switch(n.indexOf("1000")) {
				case 2:	//yellow wins 1k, shift red to beginning
					s.replace(nameMatcher.start(), nameMatcher.end(),
						n.substring(0, n.indexOf("\n")+1) + n.substring(n.lastIndexOf(" / ")+3, n.lastIndexOf("\n")) + " / " + n.substring(n.indexOf("\n")+1, n.lastIndexOf(" / "))
					);
					break;
				case 4:	//blue wins 1k, shift blue to end
					s.replace(nameMatcher.start(), nameMatcher.end(),
						n.substring(0, n.indexOf("\n")+1) + n.substring(n.indexOf(" / ")+3, n.lastIndexOf("\n")) + " / " + n.substring(n.indexOf("\n")+1, n.indexOf(" / "))
					);
					break;
				case 0:	//red wins 1k, just cut off last line
					s.replace(nameMatcher.start(), nameMatcher.end(), n.substring(0, n.lastIndexOf("\n")));
					break;
				default:
					System.err.println("Index of 1000 in unexpected location: " + n.indexOf("1000"));
			}
		}
		
		// fix prizeOnWheel. this matcher should only match once. e.g. "Athens is 9370\nFOOD & DRINK R1"
		Matcher prizeMatcher = Pattern.compile("[A-Za-z& ]+ (is|are) \\d{4,5}\n+[\\w\\p{Punct} ]+ R1").matcher(s);
		if(prizeMatcher.find()) {
			String p = prizeMatcher.group();
			int i = Math.max(p.indexOf("is"), p.indexOf("are"));
			s.replace(prizeMatcher.start(), prizeMatcher.end(), "PRIZE: " + p.substring(0, i) + p.substring(p.indexOf(" ", i) + 1));
		}
		
		// add tabs for Express. ending matching pattern with a \n is safe since this will never be the end of input
		Matcher expressMatcher = Pattern.compile("Express\\([LCR?]\\), [A-Z], [1-9][0-9]?\\.[\\w\\p{Punct}\\p{Space}]*?(\n([A-Z], ([0-9]|called)(, and all bought)?|NO MORE (VOWELS|CONSONANTS)|FULLY FILLED IN)\\..*$)+(\n(.*is wrong.*|.* - .*))?\n", Pattern.MULTILINE).matcher(s);
		while(expressMatcher.find()) {
			String e = expressMatcher.group();
			s.replace(expressMatcher.start(), expressMatcher.end(), e.substring(0, e.length() - 1).replace("\n", "\n\t") + "\n");
		}
		
		// get rid of "hanging..."
		Matcher hangingMatcher = Pattern.compile(" hanging on [\\w\\$ ]+(\\.|\n|, )").matcher(s);
		while(hangingMatcher.find()) {
			s.replace(hangingMatcher.start(), hangingMatcher.end(), hangingMatcher.group().endsWith(".") ? "." : ", ");
			//http://stackoverflow.com/revisions/16161503/5 - not sure why I needed this here and not anywhere else, maybe because the replacement is << the original's length
			hangingMatcher = hangingMatcher.region(hangingMatcher.start(), s.length());
		}
		
		//combine two-line BR WC call. this matcher should only match once. e.g. "C, D, M, A\nand H"
		Matcher wcMatcher = Pattern.compile("[A-Z](, [A-Z]){3}\nand [A-Z]").matcher(s);
		if(wcMatcher.find())
			s.replace(wcMatcher.start(), wcMatcher.end(), wcMatcher.group().replace("\nand", ","));
			
		//combine two-line BR loss. this matcher should only match once.
		Matcher brLossMatcher = Pattern.compile(" was the answer[\\w\\p{Punct}\\p{Space}]*?no").matcher(s);
		if(brLossMatcher.find())
			s.replace(brLossMatcher.start(), brLossMatcher.end(), ",");
			
		//last thing: fix typos
		String s2 = s.toString();
		for(String fixedTypo : TYPO_REGEX_MAP.keySet())
			s2 = s2.replaceAll(TYPO_REGEX_MAP.get(fixedTypo), fixedTypo);
		
		return s2;
	}
	
	/* end utility methods */
	
	/** CAREFUL USING THIS!!!! */
	private static boolean REDO_OVERRIDE = false;
	private static Map<Player, java.util.List<Short>> OLD_SPIN_STRENGTHS;
	private static void redoAnalyses(File[] readouts, int season) {
		REDO_OVERRIDE = true;
		ReadoutPanel p = new ReadoutPanel();
		for(File f : readouts) {
			try {
				String s[] = f.getName().split("\\.");
				OLD_SPIN_STRENGTHS = GameAnalysis.readAnalysis(new File(
					String.format("analysis\\s" + season + "\\%02d.%02d.%02d.ser", Integer.parseInt(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2])))).getSpinStrengths();
				p.loadReadout(f);
				p.convertReadoutToAnalysis();
				p.gA.writeAnalysis(true);
			} catch(Exception e) {
				System.err.println(f + " failed:\n");
				e.printStackTrace();
				return;
			}
		}
		REDO_OVERRIDE = false;
	}
	
	public static void main(String[] args) throws IOException {
		if(args.length == 1) {
			int season = Integer.parseInt(args[0]);
			new File("sources\\" + season).mkdir();
			for(int i = 195*(season-1)+1; i <= season*195; i++)
				//safe: "if and only if a file with this name does not yet exist"
				new File("sources\\" + season + "\\" + WheelDateFactory.dateFromShowNumber(i).format(DateTimeFormatter.ofPattern("uu.M.d")) + ".txt").createNewFile();
			return;
		}
		/* String pattern = SOME PATTERN TO TEST,
			tests[] = { "W spins yellow $900(R), F, 2.", "B buys E, 0.", "An spins $3500(C), L, 1.", "L spins LEFT MDW BANKRUPT.", "B spins LOSE A TURN(L)." };
			
		for(String s : tests)
			//System.out.println(s + "\t" + s.matches(pattern));
			System.out.println(s.split(LINE_REGEX));
		
		//System.out.println(Arrays.asList("THIS IS NOT (n) A DRILL - 1000/0/0".split("([\\.,:]| - )")));
		//System.out.println("THIS (n) IS NOT (n) A (n) DRILL".replace("(n)", "<br>"));
		
		*/
		
		//IM --> rough readout conversion
		
		/*
		String s2 = "HIGH VOLUME - Audi A4 34870 + $5000 cash = 39870.";
		String[] s = s2.replaceAll("\\D", " ").split(" ");	//remove everything but numbers.
		java.util.List<String> l = new LinkedList<>(Arrays.asList(s));
		l.removeAll(Arrays.asList(""));
		System.out.println(l.get(l.indexOf("5000")-1));
		
		System.out.println(insertNewLinesIntoPuzzleBoard("WIFE AND KIDS", 2));
		
		Predicate<String> dP = String::isEmpty;
		
		String[] s3 = "DANCING IN (n) THE STREET - 2900 + Car Ford Fiesta 15090 + MDW + Belize 5845 = 23835 + MDW.".split(BANK_REGEX);
		System.out.println(Arrays.toString(s3));
		
		System.out.println(String.format("%1$02d.%2$02d.%3$02d.txt", 13, 2, 6));
		System.out.println("T _ _ _ _ - _ _ _ _".matches(BOARD_REGEX));
		
		System.out.println(Arrays.toString("0/8000+1/2Car/2500+1/2Car+WC".split("/[^(2Car)]")));
		
		System.out.println(allValidAnagramsOf("ornage"));
		*/
		
		Arrays.asList("1000/0/0\nTest, Hi, Me\nT/H/M", "0/1000/0\nHi, Me, Test\nT/H/M", "0/0/1000\nMe, Test, Hi\nT/H/M", "0/1000/0\nJason, Kim, Lindsay\nL/J/K",
			"Athens is 9370\nFOOD & DRINK R1", "G spins Express(R), T, 3.\nand I'm hopping on\nS, 2.\nNO MORE CONSONANTS.\nE, 1.\nI, 0.\nA spins $3500(C), R, 1.",
			"G spins Express(R), T, 3.\nS, 2.\nE, 1.\nCORNO CURRO CABINET is wrong.\nA spins $3500(C), R, 1.", "G spins Express(R), T, 3.\nS, 2.\nE, 1.\nCORNER CURIO CABINET - lol.\n",
			"EXHIBITION was the answer.\nno 30")
			.stream().forEachOrdered(s -> System.out.println(optimizeReadout(s) + "\n"));
		System.out.println(new Scanner("5550+WC").useDelimiter("\\+").nextInt());
		
		Scanner ppScan = new Scanner("3600.\nyour place is 6514\n10114").useDelimiter("\\D+");
		while(ppScan.hasNextInt())
			System.out.print(ppScan.nextInt() + "\t");
			
		System.out.println("NO MORE VOWELSS, 0NO MORE CONSONANTSI, 0.".matches("(([A-Z], ([0-9]|called)|NO MORE (VOWELS|CONSONANTS)|FULLY FILLED IN)(\\.)?)+"));
		System.out.println(String.format("%03d", 1) + "\t" + String.format("%03d", 2));
		System.out.println("	uses WC, L, 3.".startsWith("\tuse"));
		System.out.println(isTooLong("\tR buzzes out."));
		
		System.out.println("_ A C _ _ _ - _ A C _ E D".matches(BOARD_REGEX));
	}
}

