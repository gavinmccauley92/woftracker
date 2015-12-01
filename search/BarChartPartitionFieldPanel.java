package woftracker.search;

import woftracker.stats.GameAnalysis;
import woftracker.stats.BarChartPartition;
import woftracker.stats.BarChart;
import woftracker.util.FormatFactory;
import java.util.function.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import java.lang.reflect.*;
import java.util.stream.Stream;

class BarChartPartitionFieldPanel extends FieldPanel {
	private JList<Enum> allEnumsX;
	private BarChartFieldPanel bcfp;
	
	private static final BiFunction<BarChartPartition<? extends Enum, ? extends Enum>, Set<? extends Enum>, BarChart<? extends Enum>> GETY = BarChartPartition::synthesizeY;
	
	@SuppressWarnings("unchecked")
	public BarChartPartitionFieldPanel(Field gaField, boolean isWithinAFP, Class<?> enumXType, Class<?> enumYType) {
		super(gaField, isWithinAFP);
		//setPreferredSize(new Dimension(340, 150));
		//setMaximumSize(new Dimension(340, 150));
		
		Enum[] es = (Enum[]) enumXType.getEnumConstants();
		
		setOptions(125, "'s subchart for", "'s total chart");
		options.addItemListener(e -> {
			int i = options.getSelectedIndex();
			allEnumsX.setEnabled(i == 1);
			if(i != 2) allEnumsX.clearSelection(); else allEnumsX.setSelectionInterval(0, es.length);
		});
		
		allEnumsX = new JList<>(es);
		allEnumsX.setEnabled(false);
		allEnumsX.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		allEnumsX.setCellRenderer(new FieldListCellRenderer());
		allEnumsX.setAlignmentX(Component.CENTER_ALIGNMENT);
		allEnumsX.setVisibleRowCount(Math.min(es.length, 6));	//this sets the size for me nicely enough
		allEnumsX.setPrototypeCellValue(Arrays.stream(es).max((e1, e2) -> e1.toString().length() - e2.toString().length()).get());
		
		JScrollPane jSP = new JScrollPane(allEnumsX, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		//adjustment for jSP
		Dimension d = allEnumsX.getPreferredScrollableViewportSize();
		jSP.setPreferredSize(new Dimension(d.width + 30, d.height + 7));
		jSP.setMaximumSize(new Dimension(d.width + 30, d.height + 7));
		
		bcfp = new BarChartFieldPanel(gaField, true, enumYType) {
			protected Object getField(GameAnalysis gA) {
				BarChartPartition<? extends Enum, ? extends Enum> bCP = (BarChartPartition<? extends Enum, ? extends Enum>) BarChartPartitionFieldPanel.this.getField(gA);
				return BarChartPartitionFieldPanel.this.options.getSelectedIndex() == 1 ? GETY.apply(bCP, new HashSet<Enum>(allEnumsX.getSelectedValuesList())) : bCP.synthesizeTotalY();
			}
		};
		
		add(options);
		add(Box.createHorizontalStrut(10));
		add(jSP);
		add(Box.createHorizontalStrut(10));
		add(bcfp);
	}
	
	public BarChartPartitionFieldPanel(Field gaField, Class<?> enumXType, Class<?> enumYType) {
		this(gaField, false, enumXType, enumYType);
	}
	
	Predicate<GameAnalysis> fieldPredicate() {
		return options.getSelectedIndex() == 0 ? null : bcfp.fieldPredicate();
	}
	
	String fieldPredicateString() {
		return gaField.getName() + options.getSelectedItem() + (options.getSelectedIndex() == 1 ? " " + formatCollection(allEnumsX.getSelectedValuesList()) : "")
			+ bcfp.fieldPredicateString();
	}
}