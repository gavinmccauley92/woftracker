package woftracker.search;

import woftracker.stats.GameAnalysis;
import java.util.function.Predicate;
import java.util.Collection;
import java.util.Arrays;
import java.util.stream.Stream;
import java.awt.Dimension;
import javax.swing.*;
import java.lang.reflect.Field;

abstract class FieldPanel extends JPanel {
	protected Field gaField;
	protected boolean isWithinAFP;
	protected JComboBox<StringBuilder> options;
	
	public FieldPanel(Field gaField, boolean isWithinAFP) {
		setOpaque(false);
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		this.gaField = gaField;
		this.isWithinAFP = isWithinAFP;
		
		options = new JComboBox<>();
		options.setRenderer(new FieldListCellRenderer());
	}
	
	protected void setOptions(int width, String ... ss) {
		options.setMinimumSize(new Dimension(width, 25));
		options.setPreferredSize(new Dimension(width, 25));
		options.setMaximumSize(new Dimension(width, 25));
		Stream.concat(Stream.of(" "), Arrays.stream(ss)).filter(s -> s != null && !s.isEmpty()).forEachOrdered(s -> options.addItem(new StringBuilder(s)));
	}
	
	abstract Predicate<GameAnalysis> fieldPredicate();
	abstract String fieldPredicateString();
	
	protected Object getField(GameAnalysis gA) {
		return gA.getStat(gaField);
	}
	
	protected static String formatCollection(Collection<?> c) {
		return c == null ? "not present" : (c.size() == 0 ? "empty" : (c.size() == 1 ? c.iterator().next().toString() : c.toString()));
	}
	
	public Field gaField() {
		return gaField;
	}
}