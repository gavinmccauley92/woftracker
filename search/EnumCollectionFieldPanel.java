package woftracker.search;

import woftracker.stats.GameAnalysis;
import woftracker.util.FormatFactory;
import java.util.*;
import java.util.function.*;
import java.awt.*;
import javax.swing.*;
import java.lang.reflect.Field;
import java.util.stream.Stream;

class EnumCollectionFieldPanel extends FieldPanel {
	private JComboBox<String> andOrOptions;
	private JList<Enum> allEnums;
	private NumberFieldPanel nfp;
	
	private static final Function<Collection<?>, Integer> SIZE = Collection::size;
	
	public EnumCollectionFieldPanel(Field gaField, boolean isWithinAFP, Class<?> enumType) {
		super(gaField, isWithinAFP);
		//setPreferredSize(new Dimension(340, 150));
		//setMaximumSize(new Dimension(340, 150));
		
		setOptions(165, "contains nothing", "is exactly", "contains all of", "contains any of", "does not contain any of");
		options.addItemListener(e -> allEnums.setEnabled(options.getSelectedIndex() > 1));
		
		Enum[] es = (Enum[]) enumType.getEnumConstants();
		allEnums = new JList<>(es);
		allEnums.setEnabled(false);
		allEnums.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		allEnums.setCellRenderer(new FieldListCellRenderer());
		allEnums.setAlignmentX(Component.CENTER_ALIGNMENT);
		allEnums.setVisibleRowCount(Math.min(es.length, 6));	//this sets the size for me nicely enough
		allEnums.setPrototypeCellValue(Arrays.stream(es).max((e1, e2) -> e1.toString().length() - e2.toString().length()).get());
		
		JScrollPane jSP = new JScrollPane(allEnums, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		//adjustments for jSP
		Dimension d = allEnums.getPreferredScrollableViewportSize();
		jSP.setPreferredSize(new Dimension(d.width + 30, d.height + 7));
		jSP.setMaximumSize(new Dimension(d.width + 30, d.height + 7));
		
		andOrOptions = new JComboBox<>();
		andOrOptions.setRenderer(new FieldListCellRenderer());
		andOrOptions.setPreferredSize(new Dimension(50, 25));
		andOrOptions.setMaximumSize(new Dimension(50, 25));
		Stream.of("or", "and").forEach(s -> andOrOptions.addItem(s));
		
		nfp = new NumberFieldPanel(gaField, true) {
			protected Object getField(GameAnalysis gA) {
				return Collection.class.cast(super.getField(gA)).size();
			}
		};
		
		add(options);
		add(Box.createHorizontalStrut(10));
		add(jSP);
		add(Box.createHorizontalStrut(10));
		add(andOrOptions);
		add(Box.createHorizontalStrut(10));
		add(nfp);
	}
	
	public EnumCollectionFieldPanel(Field gaField, Class<?> enumType) {
		this(gaField, false, enumType);
	}
	
	@SuppressWarnings("unchecked")
	Predicate<GameAnalysis> fieldPredicate() {
		java.util.List<Enum> selectedEnums = allEnums.getSelectedValuesList();
		Predicate<GameAnalysis> enumPredicate = null;
		
		switch(options.getSelectedIndex()) {
			case 1:
				enumPredicate = gA -> ((Collection<Enum>) getField(gA)).isEmpty();
				break;
			case 2:
				enumPredicate = gA -> new HashSet<Enum>(((Collection<Enum>) getField(gA))).equals(new HashSet<Enum>(selectedEnums));
				break;
			case 3:
				enumPredicate = gA -> ((Collection<Enum>) getField(gA)).containsAll(selectedEnums);
				break;
			case 4:
				enumPredicate = gA -> ((Collection<Enum>) getField(gA)).stream().anyMatch(e -> selectedEnums.contains(e));
				break;
			case 5:
				enumPredicate = gA -> ((Collection<Enum>) getField(gA)).stream().noneMatch(e -> selectedEnums.contains(e));
				break;
			default:
				return null;
		}
		
		return nfp.isActive() ? (andOrOptions.getSelectedItem().equals("and") ? enumPredicate.and(nfp.fieldPredicate()) : enumPredicate.or(nfp.fieldPredicate())) : enumPredicate;
	}
	
	String fieldPredicateString() {
		return gaField.getName() + " " + options.getSelectedItem() + (options.getSelectedIndex() > 1 ? " " + formatCollection(allEnums.getSelectedValuesList()) : "") +
			(nfp.isActive() ? " " + andOrOptions.getSelectedItem() + " whose size " + nfp.fieldPredicateString() : "");
	}
}