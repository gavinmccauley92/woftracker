package woftracker.puzzle;

import woftracker.MainWindow;
import woftracker.stats.PuzzleBoard;
import java.util.*;
import java.math.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

class PuzzleTableCellRenderer extends DefaultTableCellRenderer {
	/* PuzzleBoardCellRenderer: column 2 */
	private static final int[] LINE_LENGTH = { 12, 14, 14, 12 };
	private static final Color EMPTY_COLOR = new Color(0x339955), LETTER_BG = new Color(0xFFEEFF), LETTER_FG = new Color(0x111122);
	
	private static final JPanel PANEL = new JPanel(new GridLayout(4, 14, 1, 1));
	private static final JLabel[][] LABELS = new JLabel[4][14];
	static {
		Font PUZZLE_FONT;
		try {
			PUZZLE_FONT = Font.createFont(Font.TRUETYPE_FONT, new File("woftracker\\puzzle\\HelveticaNeue-CondensedBold.otf"))
				.deriveFont(Font.BOLD).deriveFont(28f);
		} catch(Exception e) {
			System.err.println(e);
			PUZZLE_FONT = new Font("Helvetica Neue, Rockwell Condensed", Font.PLAIN, 24);
		}
		
		for(int i = 0; i < 4; i++)
			for(int j = 0; j < 14; j++) {
				JLabel l = new JLabel("", SwingConstants.CENTER);
				l.setOpaque(true);
				l.setFont(PUZZLE_FONT);
				
				if((j == 0 || j == 13) && (i == 0 || i == 3))
					l.setBackground(new Color(0, true));
				else
					l.setBorder(BorderFactory.createLineBorder(new Color(0x332222), 2));
				
				PANEL.add(l);
				LABELS[i][j] = l;
			}
	}
	
	private void updatePanel(PuzzleBoard pb) {
		String[] lines = pb.getFinalState().split("\n");

		String[][] letters = new String[lines.length][];
		for(int i = 0; i < lines.length; i++)
			letters[i] = lines[i].split(" ");
		
		for(int i = 0; i < 4; i++) {
			int j1 = i == 0 || i == 3 ? 1 : 0, j2 = i == 0 || i == 3 ? 13 : 14;
			for(int j = j1; j < j2; j++) {
				JLabel l = LABELS[i][j];
				if(true) {
					l.setForeground(new Color(0, true));
					l.setBackground(EMPTY_COLOR);
				}
			}
		}
		
		final int line_start = lines.length > 2 && letters[0].length <= 12 ? 0 : 1;
		
		java.util.List<Integer> li = new LinkedList<>();
		for(int i = 0, j = line_start; i < lines.length; i++, j++) {
			int k = new BigDecimal((LINE_LENGTH[j] - letters[i].length)/2f).setScale(0, RoundingMode.HALF_UP).intValue();
			if(j == 0 || j == 3)	//12 letter lines - convert from 12 to 14 by adding 1 (since 1 on each side)
				k++;
			li.add(k);
		}
		
		final int startpos = Collections.min(li);
		
		for(int i = 0, j = line_start; i < lines.length; i++, j++) {
			int pos = Math.max(startpos, j == 0 || j == 3 ? 1 : 0);
			
			for(String c : letters[i]) {
				if(!c.equals("/")) {
					LABELS[j][pos].setBackground(LETTER_BG);
					if(!c.equals("_")) {
						LABELS[j][pos].setForeground(LETTER_FG);
						LABELS[j][pos].setText(c.equals("|") ? "/" : c);
					}
				}
				pos++;
			}
		}
	}
	
	/* end PuzzleBoardCellRenderer */
	
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		boolean emptyPB = false;
		pbif: if(value instanceof PuzzleBoard) {
			PuzzleBoard pb = (PuzzleBoard) value;
			if(pb == null || pb.getFinalState() == null) {
				emptyPB = true;
				break pbif;
			}
			
			updatePanel(pb);
			return PANEL;
		}
		
		JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		l.setHorizontalAlignment(SwingConstants.CENTER);
		l.setForeground(new Color(0, 0, 0, emptyPB ? 0x80 : 0xFF));
		if(emptyPB)
			l.setText("N/A");
		
		return l;
	}
}