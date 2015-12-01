package woftracker.readout;

import javax.swing.*;
import javax.swing.undo.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.time.*;
import java.time.format.*;
import woftracker.util.WheelDateFactory;

class ReadoutFormatException extends RuntimeException {
	String line;
	ReadoutFormatException(String line, int lineNumber, String message) {
		super("\nline " + lineNumber + ": " + line + "\n" + message);
		this.line = line;
	}
}

/** http://stackoverflow.com/questions/4518603/is-it-possible-to-change-the-names-shown-for-the-items-in-a-java-swing-jlist */
class WheelReadoutCellRenderer extends DefaultListCellRenderer {
	private static final Color COMPLETE = new Color(0xC8, 0xFB, 0xC8), INCOMPLETE = new Color(0xFB, 0xC8, 0xC8);
	
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		// I know DefaultListCellRenderer always returns a JLabel
		// super setups up all the defaults
		JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		
		// "value" is whatever object you put into the list, you can use it however you want here
		if(!(value instanceof File))
			return label;
		
		//remove parent directories such as "sources\" as well as remove extension
		String fn = ((File) value).getName();
		label.setText(fn.substring(0, fn.lastIndexOf('.')));
		label.setHorizontalAlignment(SwingConstants.CENTER);	//hehe
		
		LocalDate gaDate = LocalDate.parse(label.getText(), DateTimeFormatter.ofPattern("uu.M.d"));
		int showNumber = WheelDateFactory.showNumberFromDate(gaDate), seasonNumber = ((showNumber-1)/195)+1, showOfSeason = ((showNumber-1)%195)+1;
		
		label.setToolTipText("Double click to load the readout box with #" + showNumber + ", #" + String.format("%03d", showOfSeason));
		label.setBackground(new File(String.format("analysis\\s%02d\\%s.wga", seasonNumber, gaDate.format(DateTimeFormatter.ofPattern("uu.MM.dd")))).exists()
			? COMPLETE : INCOMPLETE);
		
		if(isSelected) {
			label.setForeground(label.getForeground().darker().darker());	//pure white a bit too much
			label.setFont(label.getFont().deriveFont(Font.BOLD));
		}
		return label;
    }
}

//http://docs.oracle.com/javase/tutorial/displayCode.html?code=http://docs.oracle.com/javase/tutorial/uiswing/examples/components/TextComponentDemoProject/src/components/TextComponentDemo.java

class UndoAction extends AbstractAction {
	private UndoManager undo;
	private RedoAction redoAction;
	
	public UndoAction(UndoManager u) {
		super("Undo");
		undo = u;
		setEnabled(false);
	}
	
	public void setRedoAction(RedoAction redoAction) {
		this.redoAction = redoAction;
	}
 
	public void actionPerformed(ActionEvent e) {
		try {
		   undo.undo();
		} catch (CannotUndoException ex) {
			System.out.println("Unable to undo: " + ex);
			ex.printStackTrace();
		}
		updateUndoState();
		redoAction.updateRedoState();
	}
 
	protected void updateUndoState() {
		if (undo.canUndo()) {
			setEnabled(true);
			putValue(Action.NAME, undo.getUndoPresentationName());
		} else {
			setEnabled(false);
			putValue(Action.NAME, "Undo");
		}
	}
}

class RedoAction extends AbstractAction {
	private UndoManager undo;
	private UndoAction undoAction;
	
	public RedoAction(UndoManager u) {
		super("Redo");
		undo = u;
		setEnabled(false);
	}
	
	public void setUndoAction(UndoAction undoAction) {
		this.undoAction = undoAction;
	}
	
	public void actionPerformed(ActionEvent e) {
		try {
			undo.redo();
		} catch (CannotRedoException ex) {
			System.out.println("Unable to redo: " + ex);
			ex.printStackTrace();
		}
		updateRedoState();
		undoAction.updateUndoState();
	}
 
   protected void updateRedoState() {
		if (undo.canRedo()) {
			setEnabled(true);
			putValue(Action.NAME, undo.getRedoPresentationName());
		} else {
			setEnabled(false);
			putValue(Action.NAME, "Redo");
		}
	}
}