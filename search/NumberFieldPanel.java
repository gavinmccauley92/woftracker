package woftracker.search;

import woftracker.stats.GameAnalysis;
import java.util.function.Predicate;
import java.awt.Dimension;
import javax.swing.*;
import java.lang.reflect.Field;
import java.util.stream.Stream;

class NumberFieldPanel extends FieldPanel {
	private JSpinner numberSpinner, numberSpinner2;
	private SpinnerNumberModel nsm, nsm2;
	
	public NumberFieldPanel(Field gaField, boolean isWithinAFP) {
		super(gaField, isWithinAFP);
		/*setMinimumSize(new Dimension(325, 25));
		setPreferredSize(new Dimension(325, 25));
		setMaximumSize(new Dimension(325, 25));*/
		
		setOptions(120, "is equal to", "is not", "is greater than", "is less than", "is at least", "is at most", "is between", "is not between");
		
		options.addItemListener(e -> { int index = options.getSelectedIndex(); numberSpinner.setEnabled(index != 0); numberSpinner2.setEnabled(index >= 7); });
		
		Class<?> numberType = gaField.getType();
		int max = numberType == byte.class ? (int) Byte.MAX_VALUE : (numberType == short.class ? (int) Short.MAX_VALUE : Integer.MAX_VALUE);
		numberSpinner = new JSpinner(nsm = new SpinnerNumberModel(0, 0, max, 1));
		numberSpinner.setPreferredSize(new Dimension(75, 25));
		numberSpinner.setEnabled(false);
		numberSpinner2 = new JSpinner(nsm2 = new SpinnerNumberModel(0, 0, max, 1));
		numberSpinner2.setPreferredSize(new Dimension(75, 25));
		numberSpinner2.setEnabled(false);
		
		JPanel extraPanel = new JPanel();
		extraPanel.setLayout(new BoxLayout(extraPanel, BoxLayout.LINE_AXIS));
		extraPanel.add(Box.createHorizontalStrut(5));
		extraPanel.add(new JLabel("and"));
		extraPanel.add(Box.createHorizontalStrut(5));
		extraPanel.add(numberSpinner2);
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		JPanel p1 = new JPanel(), p2 = new JPanel();
		p1.add(options);
		p2.add(numberSpinner);
		p2.add(extraPanel);
		add(Box.createVerticalGlue());
		add(p1);
		add(p2);
		add(Box.createVerticalGlue());
	}
	
	public NumberFieldPanel(Field gaField) {
		this(gaField, false);
	}
	
	public boolean isActive() {
		return options.getSelectedIndex() != 0;
	}
	
	Predicate<GameAnalysis> fieldPredicate() {
		int n = nsm.getNumber().intValue(), n2 = nsm2.getNumber().intValue();
		switch(options.getSelectedIndex()) {
			case 1:
				return gA -> ((Number) getField(gA)).intValue() == n;
			case 2:
				return gA -> ((Number) getField(gA)).intValue() != n;
			case 3:
				return gA -> ((Number) getField(gA)).intValue() > n;
			case 4:
				return gA -> ((Number) getField(gA)).intValue() < n;
			case 5:
				return gA -> ((Number) getField(gA)).intValue() >= n;
			case 6:
				return gA -> ((Number) getField(gA)).intValue() <= n;
			case 7:
				return gA -> { int gan = ((Number) getField(gA)).intValue(); return gan >= n && gan <= n2; };
			case 8:
				return gA -> { int gan = ((Number) getField(gA)).intValue(); return gan < n || gan > n2; };
			default:
				return null;
		}
	}
	
	String fieldPredicateString() {
		return (!isWithinAFP ? gaField.getName() + " " : "") + options.getSelectedItem() + " " + nsm.getNumber() +
			(options.getSelectedIndex() >= 7 ? " and " + nsm2.getNumber() : "");
	}
}