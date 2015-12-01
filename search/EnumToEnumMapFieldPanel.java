package woftracker.search;

import woftracker.stats.StatEnums;
import woftracker.stats.GameAnalysis;
import woftracker.util.FormatFactory;
import java.util.function.Predicate;
import java.util.Arrays;
import java.util.Map;
import java.util.Collections;
import java.awt.*;
import javax.swing.*;
import java.lang.reflect.Field;
import java.util.stream.Stream;

class EnumToEnumMapFieldPanel extends FieldPanel implements StatEnums {
	private JList<Enum> allKeyEnums, allValueEnums;
	private NumberFieldPanel nfp;
	
	public EnumToEnumMapFieldPanel(Field gaField, Class<?> keyEnumType, Class<?> valueEnumType) {
		super(gaField, true);
		//setPreferredSize(new Dimension(340, 150));
		//setMaximumSize(new Dimension(340, 150));
		
		Enum[] es = (Enum[]) keyEnumType.getEnumConstants();
		allKeyEnums = new JList<>(gaField.getName().equals("prizeResults") ? Arrays.stream(es).filter(e -> CARDBOARD.contains(e)).toArray(Enum[]::new) : es);
		allKeyEnums.setEnabled(true);
		allKeyEnums.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		allKeyEnums.setCellRenderer(new FieldListCellRenderer());
		allKeyEnums.setAlignmentX(Component.CENTER_ALIGNMENT);
		allKeyEnums.setVisibleRowCount(Math.min(es.length, 6));	//this sets the size for me nicely enough
		allKeyEnums.setPrototypeCellValue(Arrays.stream(es).max((e1, e2) -> e1.toString().length() - e2.toString().length()).get());
		
		nfp = new NumberFieldPanel(gaField, true) {
			protected Object getField(GameAnalysis gA) {
				return allValueEnums.getSelectedValuesList().stream().mapToInt(e -> Collections.frequency(Map.class.cast(super.getField(gA)).values(), e)).sum();
			}
		};
		/*nfp.options.addItemListener(e -> {
			allKeyEnums.setEnabled(nfp.options.getSelectedIndex() == 0);
			allValueEnums.setEnabled(nfp.options.getSelectedIndex() != 0);
			options.setEnabled(nfp.options.getSelectedIndex() == 0);
		});*/
		
		
		Enum[] es2 = (Enum[]) valueEnumType.getEnumConstants();
		allValueEnums = new JList<>(es2);
		allValueEnums.setEnabled(false);
		allValueEnums.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		allValueEnums.setCellRenderer(new FieldListCellRenderer());
		allValueEnums.setAlignmentX(Component.CENTER_ALIGNMENT);
		allValueEnums.setVisibleRowCount(Math.min(es2.length, 6));	//this sets the size for me nicely enough
		allValueEnums.setPrototypeCellValue(Arrays.stream(es2).max((e1, e2) -> e1.toString().length() - e2.toString().length()).get());
		allValueEnums.addListSelectionListener(e -> {
			if(!e.getValueIsAdjusting()) {
				StringBuilder s = (StringBuilder) options.getModel().getSelectedItem();
				int i = s.indexOf(" one of");
				if(allValueEnums.getSelectedIndices().length <= 1 && i != 1)
					s.replace(i, i + " one of".length(), "");
				else if(i == -1)
					s.append(" one of");
				allValueEnums.requestFocusInWindow();
			}
		});
		
		setOptions(125, "is", "is not", "is present", "is not present", "'s subtotal for", "'s overall total");
		options.addItemListener(e -> {
			int option = options.getSelectedIndex();
			allKeyEnums.setEnabled(option < 4);
			allValueEnums.setEnabled(option < 5);
			nfp.setEnabled(option >= 4);
			allKeyEnums.clearSelection();
			if(option != 6) 
				allValueEnums.clearSelection();
			else
				allValueEnums.setSelectionInterval(0, es2.length-1);
		});
		
		JScrollPane jSP = new JScrollPane(allKeyEnums, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		//adjustment for jSP
		Dimension d = allKeyEnums.getPreferredScrollableViewportSize();
		jSP.setPreferredSize(new Dimension(d.width + 30, d.height + 7));
		jSP.setMaximumSize(new Dimension(d.width + 30, d.height + 7));
		
		JScrollPane jSP2 = new JScrollPane(allValueEnums, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		//adjustments for jSP
		Dimension d2 = allValueEnums.getPreferredScrollableViewportSize();
		jSP2.setPreferredSize(new Dimension(d2.width + 30, d2.height + 7));
		jSP2.setMaximumSize(new Dimension(d2.width + 30, d2.height + 7));
		
		add(options);
		add(Box.createHorizontalStrut(10));
		add(new JLabel("for"));
		add(Box.createHorizontalStrut(10));
		add(jSP);
		add(Box.createHorizontalStrut(10));
		add(options);
		add(Box.createHorizontalStrut(10));
		add(jSP2);
		add(Box.createHorizontalStrut(10));
		add(nfp);
	}
	
	Predicate<GameAnalysis> fieldPredicate() {
		Enum k = allKeyEnums.getSelectedValue();
		java.util.List<Enum> selectedEnums = allValueEnums.getSelectedValuesList();
		
		if(nfp.isActive())
			return nfp.fieldPredicate();
		else
			switch(options.getSelectedIndex()) {
				case 1:
					return gA -> selectedEnums.contains(Map.class.cast(getField(gA)).get(k));
				case 2:
					return gA -> !selectedEnums.contains(Map.class.cast(getField(gA)).get(k));
				case 3:
					return gA -> Map.class.cast(getField(gA)).get(k) != null;
				case 4:
					return gA -> Map.class.cast(getField(gA)).get(k) == null;
				default:
					return null;
			}
	}
	
	String fieldPredicateString() {
		return nfp.isActive() ? gaField.getName() + " " + options.getSelectedItem() + " " + formatCollection(allValueEnums.getSelectedValuesList()) + " " + nfp.fieldPredicateString()
		: gaField.getName() + " for " + allKeyEnums.getSelectedValue() + " " + options.getSelectedItem() + " " + (options.getSelectedIndex() < 3 ? formatCollection(allValueEnums.getSelectedValuesList()) : "");
	}
}