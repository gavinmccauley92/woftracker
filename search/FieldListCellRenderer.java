package woftracker.search;

import javax.swing.*;
import java.awt.Component;

/** http://stackoverflow.com/questions/4518603/is-it-possible-to-change-the-names-shown-for-the-items-in-a-java-swing-jlist */
class FieldListCellRenderer extends DefaultListCellRenderer {
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		// I know DefaultListCellRenderer always returns a JLabel
		// super setups up all the defaults
		JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		
		// "value" is whatever object you put into the list, you can use it however you want here
		
		label.setHorizontalAlignment(SwingConstants.CENTER);

		return label;
    }
}