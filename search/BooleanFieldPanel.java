package woftracker.search;

import woftracker.stats.GameAnalysis;
import java.util.function.Predicate;
import java.awt.Dimension;
import javax.swing.*;
import java.lang.reflect.Field;
import java.util.stream.Stream;

class BooleanFieldPanel extends FieldPanel {
	
	public BooleanFieldPanel(Field gaField) {
		super(gaField, false);
		setOptions(100, "is true", "is false");
		add(options);
	}
	
	Predicate<GameAnalysis> fieldPredicate() {
		switch(options.getSelectedIndex()) {
			case 1:
				return gA -> ((Boolean) getField(gA)).booleanValue();
			case 2:
				return gA -> !((Boolean) getField(gA)).booleanValue();
			default:
				return null;
		}
	}
	
	String fieldPredicateString() {
		return gaField.getName() + " " + options.getSelectedItem();
	}
}