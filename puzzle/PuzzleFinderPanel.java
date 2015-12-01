package woftracker.puzzle;

import woftracker.*;
import woftracker.record.Letter;
import woftracker.record.Category;
import woftracker.record.MainPuzzle;
import woftracker.util.FormatFactory;
import woftracker.stats.*;
import javax.swing.filechooser.*;
import java.util.*;
import java.time.*;
import java.time.format.*;
import java.io.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

import static woftracker.stats.GameAnalysis.*;
import static woftracker.util.FormatFactory.*;
import static woftracker.util.GameAnalyses.*;

// Helvetica-Condensed-Black-Se

public class PuzzleFinderPanel extends JPanel {
	
	private final Font PUZZLE_FINDER_FONT = new Font("Rockwell", Font.PLAIN, 24);
	
	private GameAnalysis gA = null;
	private JTextField gameSelector;
	private LocalDate showDate;
	private Map<Round, JLabel> roundLabels;
	private JLabel currentGameLabel;
	private JTable puzzleTable;
	
	private Round[] currentRounds = new Round[0];
	private Category[] currentCats = new Category[0];
	private PuzzleBoard[] currentPuzzles = new PuzzleBoard[0];
	
	public static final PuzzleBoard DUMMY_BOARD;
	static {
		MainPuzzle p = new MainPuzzle("N/A");
		p.updatePuzzle(EnumSet.of(Letter.A, Letter.N));
		DUMMY_BOARD = new PuzzleBoard(p);
	}
	
	public PuzzleFinderPanel() {
		roundLabels = new EnumMap<>(Round.class);
		
		gameSelector = generateTextField(this::updateRoundLabels, false);
		gameSelector.setFont(PUZZLE_FINDER_FONT);
		gameSelector.setMaximumSize(new Dimension(200, 200));
		
		currentGameLabel = new JLabel("", SwingConstants.CENTER);
		Font f;
		try {
			f = Font.createFont(Font.TRUETYPE_FONT, new File("woftracker\\puzzle\\HelveticaNeue-CondensedBlack.otf")).deriveFont(30f);
		} catch(Exception e) {
			f = new Font("Helvetica Neue, 3 Strikes, Rockwell", Font.BOLD, 30);
		}
		currentGameLabel.setFont(f);
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setOpaque(false);
		
		add(Box.createVerticalGlue());
		add(FormatFactory.wrapComponentIntoPanel(gameSelector, BoxLayout.LINE_AXIS));
		add(Box.createVerticalStrut(15));
		add(FormatFactory.wrapComponentIntoPanel(currentGameLabel, BoxLayout.LINE_AXIS));
		add(Box.createVerticalStrut(15));
		
		puzzleTable = new JTable(new AbstractTableModel() {
			public int getRowCount() {
				return currentRounds.length;
			}
			
			public int getColumnCount() {
				return 4;
			}
			
			private final String[] ROW_HEADERS = { "R", "PUZZLE", "CATEGORY", "FINAL STATE" };
			public String getColumnName(int column) {
				return ROW_HEADERS[column];
			}
			
			public Class<?> getColumnClass(int column) {
				return column == 3 ? PuzzleBoard.class : String.class;
			}
			
			public Object getValueAt(int row, int column) {
				switch(column) {
					case 0: return currentRounds[row];
					case 1: return Optional.ofNullable(currentPuzzles[row]).orElse(DUMMY_BOARD).getFullPuzzle();
					case 2: Category.setPlural(gA.wasCatPlural(currentRounds[row])); return currentCats[row].toString();
					default: return currentPuzzles[row];
				}
			}
		}) {
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
				JComponent c = (JComponent) super.prepareRenderer(renderer, row, column);
				c.setBackground(row % 2 == 0 ? new Color(0x669977) : new Color(0x77AA88));
				c.setFont(PUZZLE_FINDER_FONT.deriveFont(17f));
				if(column > 0)
					c.setBorder(new CompoundBorder(new EmptyBorder(-1, 0, -1, -1), BorderFactory.createDashedBorder(new Color(0xCC336655, true), 2, 2)));
				c.setBorder(new CompoundBorder(c.getBorder(), new EmptyBorder(6,6,6,6)));
				return c;
			}
		};
		
		puzzleTable.setRowHeight(PUZZLE_FINDER_FONT.getSize()*7);
		//puzzleTable.setBackground(new Color(0x669977));
		
		puzzleTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		
		int twenty_fifth = (int) ((MainWindow.WINDOW_WIDTH-50)/25f);
		puzzleTable.getColumnModel().getColumn(0).setPreferredWidth(twenty_fifth);
		puzzleTable.getColumnModel().getColumn(1).setPreferredWidth(9*twenty_fifth);
		puzzleTable.getColumnModel().getColumn(2).setPreferredWidth(5*twenty_fifth);
		puzzleTable.getColumnModel().getColumn(3).setPreferredWidth(10*twenty_fifth);
		
		//puzzleTable.setRowMargin(12);
		puzzleTable.setRowSelectionAllowed(false);
		puzzleTable.getTableHeader().setResizingAllowed(false);
		
		Color mc = MainWindow.getMainColor();
		puzzleTable.setDefaultRenderer(Object.class, new PuzzleTableCellRenderer());
		
		puzzleTable.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				l.setHorizontalAlignment(SwingConstants.CENTER);
				l.setFont(new Font("SF Fortune Wheel Condensed", Font.PLAIN, 24));
				l.setBackground(new Color(0x558866));
				return l;
			}
		});
		
		JScrollPane jSP = new JScrollPane(puzzleTable);
		jSP.getVerticalScrollBar().setUnitIncrement(10);
		
		add(jSP);
		add(Box.createVerticalGlue());
	}
	
	private void updateRoundLabels(TreeSet<GameAnalysis> gAs) {
		try {
			gA = gAs.first();	//only single for now
			currentGameLabel.setText(gA.getShowInfo());
		} catch(NoSuchElementException e) {
			javax.swing.Timer t = new javax.swing.Timer(250, null);
			t.addActionListener(new ActionListener() {
				int count = 0;
				public void actionPerformed(ActionEvent e) {
					if(count++ == 10)
						t.stop();
					else
						gameSelector.setForeground(count % 2 == 0 ? Color.BLACK : new Color(0xBB0000));
				}
			});
			t.start();
		}
		
		EnumMap<Round, PuzzleBoard> m = gA.getPuzzles();
		Map<Round, Category> m2 = gA.getCategoryMap();
		currentRounds = m.keySet().toArray(currentRounds);
		currentCats = m2.values().toArray(currentCats);	//note EnumMap implementation makes this work, a bit cheap honestly
		currentPuzzles = m.values().toArray(currentPuzzles);
		
		puzzleTable.repaint();
		for(Round r : EnumSet.of(Round.T1, Round.T2, Round.T3))
			puzzleTable.setRowHeight(r.ordinal(), PUZZLE_FINDER_FONT.getSize()*(currentPuzzles[r.ordinal()].getFinalState() == null ? 2 : 7));
	}
	
	public Insets getInsets() {
		return new Insets(10,10,10,10);
	}
}