package woftracker.search;

import woftracker.stats.GameAnalysis;
import woftracker.util.FormatFactory;
import java.util.function.Predicate;
import java.util.Arrays;
import java.awt.*;
import javax.swing.*;
import java.lang.reflect.Field;
import java.util.stream.Stream;

class EnumFieldPanel extends FieldPanel {
	private JList<Enum> allEnums;
	
	public EnumFieldPanel(Field gaField) {
		super(gaField, false);
		//setPreferredSize(new Dimension(340, 150));
		//setMaximumSize(new Dimension(340, 150));
		
		setOptions(125, "is", "is not", "is present", "is not present");
		options.addItemListener(e -> {
			boolean enable = options.getSelectedIndex() != 0 && options.getSelectedIndex() != 3;
			allEnums.setEnabled(enable);
			if(!enable)
				allEnums.clearSelection();
		});
		
		Enum[] es = (Enum[]) gaField.getType().getEnumConstants();
		allEnums = new JList<>(es);
		allEnums.setEnabled(false);
		allEnums.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		allEnums.setCellRenderer(new FieldListCellRenderer());
		allEnums.setAlignmentX(Component.CENTER_ALIGNMENT);
		allEnums.setVisibleRowCount(Math.min(es.length, 6));	//this sets the size for me nicely enough
		allEnums.setPrototypeCellValue(Arrays.stream(es).max((e1, e2) -> e1.toString().length() - e2.toString().length()).get());
		allEnums.addListSelectionListener(e -> {
			if(!e.getValueIsAdjusting()) {
				StringBuilder s = (StringBuilder) options.getModel().getSelectedItem();
				int i = s.indexOf(" one of");
				if(allEnums.getSelectedIndices().length <= 1 && i != 1)
					s.replace(i, i + " one of".length(), "");
				else if(i == -1)
					s.append(" one of");
				allEnums.requestFocusInWindow();
			}
		});
		
		JScrollPane jSP = new JScrollPane(allEnums, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		//adjustment for jSP
		Dimension d = allEnums.getPreferredScrollableViewportSize();
		jSP.setPreferredSize(new Dimension(d.width + 30, d.height + 7));
		jSP.setMaximumSize(new Dimension(d.width + 30, d.height + 7));
		
		add(options);
		add(Box.createHorizontalStrut(10));
		add(jSP);
	}
	
	Predicate<GameAnalysis> fieldPredicate() {
		java.util.List<Enum> selectedEnums = allEnums.getSelectedValuesList();
		switch(options.getSelectedIndex()) {
			case 1:
				return gA -> selectedEnums.contains(getField(gA));
			case 2:
				return gA -> !selectedEnums.contains(getField(gA));
			case 3:
				return gA -> getField(gA) != null;
			case 4:
				return gA -> getField(gA) == null;
			default:
				return null;
		}
	}
	
	String fieldPredicateString() {
		return gaField.getName() + " " + options.getSelectedItem() + " " + (options.getSelectedIndex() < 3 ? formatCollection(allEnums.getSelectedValuesList()) : "");
	}
}