package woftracker.search;

import woftracker.stats.GameAnalysis;
import woftracker.stats.BarChart;
import woftracker.util.FormatFactory;
import java.util.function.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import java.lang.reflect.*;
import java.util.stream.Stream;

class BarChartFieldPanel extends FieldPanel {
	private JList<Enum> allEnums;
	private NumberFieldPanel nfp;
	
	private static final BiFunction<BarChart<? extends Enum>, Set<? extends Enum>, Integer> GETY = BarChart::getY;
	
	@SuppressWarnings("unchecked")
	public BarChartFieldPanel(Field gaField, boolean isWithinAFP, Class<?> enumType) {
		super(gaField, isWithinAFP);
		//setPreferredSize(new Dimension(340, 150));
		//setMaximumSize(new Dimension(340, 150));
		
		Enum[] es = (Enum[]) enumType.getEnumConstants();
		
		setOptions(125, "'s subtotal for", "'s overall total");
		options.addItemListener(e -> {
			int i = options.getSelectedIndex();
			allEnums.setEnabled(i == 1);
			//found https://bugs.openjdk.java.net/browse/JDK-7108280 before fixing this
			if(i != 2) allEnums.clearSelection(); else allEnums.setSelectionInterval(0, es.length-1);
		});
		
		allEnums = new JList<>(es);
		allEnums.setEnabled(false);
		allEnums.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		allEnums.setCellRenderer(new FieldListCellRenderer());
		allEnums.setAlignmentX(Component.CENTER_ALIGNMENT);
		allEnums.setVisibleRowCount(Math.min(es.length, 6));	//this sets the size for me nicely enough
		allEnums.setPrototypeCellValue(Arrays.stream(es).max((e1, e2) -> e1.toString().length() - e2.toString().length()).get());
		
		JScrollPane jSP = new JScrollPane(allEnums, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		//adjustment for jSP
		Dimension d = allEnums.getPreferredScrollableViewportSize();
		jSP.setPreferredSize(new Dimension(d.width + 30, d.height + 7));
		jSP.setMaximumSize(new Dimension(d.width + 30, d.height + 7));
		
		nfp = new NumberFieldPanel(gaField, true) {
			protected Object getField(GameAnalysis gA) {
				BarChart<? extends Enum> bC = (BarChart<? extends Enum>) BarChartFieldPanel.this.getField(gA);
				return BarChartFieldPanel.this.options.getSelectedIndex() == 1 ? GETY.apply(bC, new HashSet<Enum>(allEnums.getSelectedValuesList())) : bC.getTotalY();
			}
		};
		
		add(options);
		add(Box.createHorizontalStrut(10));
		add(jSP);
		add(Box.createHorizontalStrut(10));
		add(nfp);
	}
	
	public BarChartFieldPanel(Field gaField, Class<?> enumType) {
		this(gaField, false, enumType);
	}
	
	Predicate<GameAnalysis> fieldPredicate() {
		return options.getSelectedIndex() == 0 ? null : nfp.fieldPredicate();
	}
	
	String fieldPredicateString() {
		return (!isWithinAFP ? gaField.getName() : "") + options.getSelectedItem() + (options.getSelectedIndex() == 1 ? " " + formatCollection(allEnums.getSelectedValuesList()) : "")
			+ " " + nfp.fieldPredicateString();
	}
}