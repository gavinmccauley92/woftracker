package woftracker.util;

import java.util.*;
import java.math.*;
import javax.swing.*;
import java.util.function.BiConsumer;
import java.awt.Color;
import java.awt.Component;
import javax.swing.text.*;

public class FormatFactory {
	
	private FormatFactory() {}
	
	public static BigDecimal formatNumber(Number n) {
		double d = n.doubleValue();
		return new BigDecimal(d).setScale(Math.floor(d) == d ? 0 : 1, RoundingMode.HALF_UP);	//equation of doubles for whole integer values is OK
	}
	
	public static String formatCollection(Collection<?> c) {
		return c == null || c.isEmpty() ? "none" : c.toString().replaceAll("\\x5B|\\x5D", "").replaceAll("null", "none");	//ASCII: [ = 5B, ] = 5D
	}
	
	public static String toTitleCase(String input) {
		StringBuilder titleCase = new StringBuilder();
		boolean nextTitleCase = true;

		for (char c : input.toCharArray()) {
			if (Character.isSpaceChar(c)) {
				nextTitleCase = true;
			} else if (nextTitleCase) {
				c = Character.toTitleCase(c);
				nextTitleCase = false;
			}

			titleCase.append(c);
		}

		return titleCase.toString();
	}
	
	public static <X> String formatMap(Map<X, ?> m) {
		return formatMap(m, m.keySet(), null);
	}
	
	public static <X,Y> String formatMap(Map<X, ?> m, BiConsumer<X,Object> c) {
		return formatMap(m, m.keySet(), c);
	}
	
	public static <X> String formatMap(Map<X, ?> m, Set<X> whatToInclude) {
		return formatMap(m, whatToInclude, null);
	}
	
	//gives same formatting style as BarChart[Partition]
	public static <X> String formatMap(Map<X, ?> m, Set<X> whatToInclude, BiConsumer<X,Object> c) {
		Set<X> keySet = new HashSet<X>(m.keySet());
		if(!keySet.removeAll(whatToInclude))
			return "  None.";
		
		StringBuilder s = new StringBuilder();
		for(Map.Entry<X, ?> entry : m.entrySet())
			if(whatToInclude.contains(entry.getKey())) {
				X x = entry.getKey();
				Object o = entry.getValue();
				if(c != null)
					c.accept(x, o);
				s.append("  " + entry.getKey() + ": " + entry.getValue() + "\n");
			}
		
		return s.deleteCharAt(s.length()-1).toString().replaceAll("\\x5B|\\x5D", "").replaceAll("null", "none");	//if key/value somehow contains collection
	}
	
	public static <Y extends Collection<?>> void removeEmptyValuesFromMap(Map<?, Y> m) {
		Collection<Y> emptyYs = new LinkedList<>();
		for(Y y : m.values())
			if(y.isEmpty())
				emptyYs.add(y);
		
		for(Y y : emptyYs)
			m.values().remove(y);
	}
	
	public static void fixStringBuilder(StringBuilder s, String s1, String s2) {
		int i, j = s1.length();
		while((i = s.indexOf(s1)) != -1)
			s.replace(i, i + j, s2);
	}
	
	public static final SimpleAttributeSet SKIPPED_LINE_STYLE = new SimpleAttributeSet(), ERROR_LINE_STYLE = new SimpleAttributeSet(), DEFAULT_LINE_STYLE = new SimpleAttributeSet();
	static {
		StyleConstants.setForeground(SKIPPED_LINE_STYLE, Color.GRAY);
		StyleConstants.setItalic(SKIPPED_LINE_STYLE, true);
		StyleConstants.setForeground(ERROR_LINE_STYLE, Color.RED);
		StyleConstants.setBold(ERROR_LINE_STYLE, true);
		StyleConstants.setForeground(DEFAULT_LINE_STYLE, Color.BLACK);
		StyleConstants.setBold(DEFAULT_LINE_STYLE, false);
		StyleConstants.setItalic(DEFAULT_LINE_STYLE, false);
	}
	
	public static void highlightLine(JTextPane jtp, String line, MutableAttributeSet a) {
		jtp.getStyledDocument().setCharacterAttributes(jtp.getText().indexOf(line), line.length(), a, true);
	}
	
	public static void resetHighlights(JTextPane jtp) {
		jtp.getStyledDocument().setCharacterAttributes(0, jtp.getText().length(), DEFAULT_LINE_STYLE, true);
	}
	
	public static JPanel wrapComponentIntoPanel(Component c, int axis) {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, axis));
		p.add(axis == BoxLayout.LINE_AXIS ? Box.createHorizontalGlue() : Box.createVerticalGlue());
		p.add(c);
		p.add(axis == BoxLayout.LINE_AXIS ? Box.createHorizontalGlue() : Box.createVerticalGlue());
		return p;
	}
	
	public static void main(String[] args) {
		Map<String, List<Integer>> m = new HashMap<String, List<Integer>>();
		
		m.put("Test", Arrays.asList(2));
		m.put("test", Arrays.asList(66, 33, 999));
		m.put("TEST", Arrays.asList(6823, 62398, null));
		
		System.out.println(formatMap(m));
		System.out.println(formatMap(m, new HashSet<String>(Arrays.asList("teSt"))));	//should throw exception :P
		Enumeration<?> styles = new StyleContext().getStyleNames();
		while(styles.hasMoreElements())
			System.out.println(styles.nextElement());
	}
}

/* GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font[] fonts = ge.getAllFonts();
        char ch = '\u2192';
        for(Font font : fonts) {
            if (font.canDisplay(ch)) {
                System.out.println(font.getName());
            }
        }
*/