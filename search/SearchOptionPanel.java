package woftracker.search;

import woftracker.*;
import woftracker.stats.*;
import woftracker.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.text.*;
import java.io.*;
import java.util.*;
import java.util.stream.*;
import java.util.function.*;
import java.time.*;
import java.time.temporal.*;
import java.time.format.*;
import java.lang.reflect.*;
import java.util.concurrent.atomic.AtomicReference;

public class SearchOptionPanel extends JPanel {
	private JScrollPane jSP, jSP2;
	private JTextPane filteredShows;
	
	private JButton find;
	private JComboBox<String> overallCombineOptions;
	private Map<Field, JPanel> fieldConditionPanels;
	private Map<Field, AtomicReference<String>> multiConditionLabels;
	
	@SuppressWarnings("unchecked")
	public SearchOptionPanel() {
		fieldConditionPanels = new LinkedHashMap<>();
		multiConditionLabels = new LinkedHashMap<>();
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setOpaque(false);
		setMinimumSize(new Dimension(1500, 500));
		setPreferredSize(new Dimension(1500, 500));
		
		JPanel fieldPanel = new JPanel();
		fieldPanel.setLayout(new BoxLayout(fieldPanel, BoxLayout.PAGE_AXIS));
		GameAnalysis.STAT_MAP.forEach((s, f) -> {
			if(!s.equals("report")) {
				JPanel fPanel = new JPanel();
				fPanel.setLayout(new BoxLayout(fPanel, BoxLayout.LINE_AXIS));
				fPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY, 1), BorderFactory.createEmptyBorder(15, 0, 15, 0)));
				fPanel.add(Box.createHorizontalGlue());
				JLabel fieldLabel = new JLabel(s);
				fPanel.add(fieldLabel);
				fPanel.add(Box.createHorizontalStrut(10));
				
				JPanel afPanel = new JPanel();
				afPanel.setLayout(new BoxLayout(afPanel, BoxLayout.PAGE_AXIS));
				
				final AtomicReference<String> mcgAR = new AtomicReference<>();
				
				//it is up to each call to this lambda to supply parameters that match an accessible constructor.
				BiConsumer<Class<? extends FieldPanel>, java.util.List<?>> setupMultiLabel = (afpClass, params) -> {
					fieldLabel.setForeground(Color.BLUE);
					fieldLabel.setToolTipText("<html>Double click to change condition grouping.<br>Currently: (1)");
					mcgAR.set("(| 1)");
					
					fieldLabel.addMouseListener(new MouseAdapter() {
						public void mouseClicked(MouseEvent e) {
							if(e.getClickCount() == 2) {
								String mcg = mcgAR.get();
								mcg = Optional.ofNullable(JOptionPane.showInputDialog(fieldLabel,
									"<html>Follow LISP format: (b i/f i/f ...)<br>Where b can be a boolean function, one of & | ^ !, taking any number of i's or f's" +
									"<br>i can be 1-n, the ith condition<br>f is another function: a nested (b i/f i/f ...)" +
									"<br>Note that ^ must have at least two arguments, and ! must have exactly one argument.",
									mcg)).orElse(mcg);
								mcg = mcg.isEmpty() ? "(| 1)" : mcg.replaceAll("(-| 0)", "").replaceAll("\\s{2,}", " ");
								
								Set<Integer> is = new HashSet<>();
								Scanner sc = new Scanner(mcg);
								sc.useDelimiter("\\D+");
								while(sc.hasNextInt())
									is.add(sc.nextInt());
								int cs = afPanel.getComponentCount(), maxC = is.stream().mapToInt(i -> i).max().getAsInt();
								
								if(cs < maxC)
									for(int i = cs; i < maxC; i++) {
										FieldPanel afNew;
										try {
											//generic array constructor reference allowed... and works. OK then
											afNew = afpClass.getConstructor(params.stream().map(p -> p.getClass()).toArray(Class<?>[]::new)).newInstance(params.toArray());
										} catch(Exception ex) {
											System.err.println("Unable to setup add/remove functionality for field " + f.getName());
											continue;
										}
										afPanel.add(afNew);
									}
								else if(cs > maxC)
									for(int i = cs; i > maxC; i--) {
										Component afOld = afPanel.getComponent(i-1);
										afPanel.remove(afOld);
									}
								
								afPanel.repaint();
								fieldLabel.setToolTipText("<html>Double click to change condition grouping.<br>Currently: " + transformLISP(mcg));
								mcgAR.set(mcg);
							}
						}
						
						public void mouseEntered(MouseEvent e) {
							ToolTipManager.sharedInstance().setDismissDelay(1000*60);
						}
						
						public void mouseExited(MouseEvent e) {
							ToolTipManager.sharedInstance().setDismissDelay(4000);	//I know default is 4 sec
						}
					});
				};
				
				FieldPanel af = null;
				Class<?> afType = f.getType(), afRawType = null, afGenericType1 = null, afGenericType2 = null;
				ParameterizedType subGenericType = null;
				
				if(afType == boolean.class)
					af = new BooleanFieldPanel(f);
				else if(afType == int.class || afType == byte.class || afType == short.class) {
					af = new NumberFieldPanel(f);
					setupMultiLabel.accept(NumberFieldPanel.class, Arrays.asList(f));
				} else if(afType.isEnum()) {
					af = new EnumFieldPanel(f);
				} else {
					try {
						ParameterizedType pt = (ParameterizedType) f.getGenericType();
						afRawType = (Class<?>) pt.getRawType();
						Type[] genericTypes = pt.getActualTypeArguments();
						
						if(genericTypes.length >= 1) {
							if(genericTypes.length == 2) {
								try {
									afGenericType2 = (Class<?>) genericTypes[1];
								} catch(ClassCastException e) {
									subGenericType = (ParameterizedType) genericTypes[1];
								}
							}
							afGenericType1 = (Class<?>) genericTypes[0];
						}
						
						//System.out.println(afRawType + " " + afGenericType1 + " " + afGenericType2);
						//Map<Enum, [OneVarStats/List<Enum/Number>/Number]>
						
						if(Collection.class.isAssignableFrom(afRawType) && afGenericType1.isEnum()) {
							af = new EnumCollectionFieldPanel(f, afGenericType1);
							setupMultiLabel.accept(EnumCollectionFieldPanel.class, Arrays.asList(f, afGenericType1));
						} else if(Map.class.isAssignableFrom(afRawType) && afGenericType1.isEnum()) {
							if(afGenericType2 != null) {
								if(afGenericType2.isEnum()) {
									af = new EnumToEnumMapFieldPanel(f, afGenericType1, afGenericType2);
									setupMultiLabel.accept(EnumToEnumMapFieldPanel.class, Arrays.asList(f, afGenericType1, afGenericType2));
								} else if(OneVarStats.class.isAssignableFrom(afGenericType2)) {
									
								} else if(Number.class.isAssignableFrom(afGenericType2)) {
									
								}
							} else if(subGenericType != null) {
								Type[] subTypes = subGenericType.getActualTypeArguments();
								if(java.util.List.class.isAssignableFrom((Class<?>) subGenericType.getRawType())) {
									Class<?> subGenericGenericType = (Class<?>) subTypes[0];
									if(subGenericGenericType.isEnum()) {
										
									} else if(Number.class.isAssignableFrom(subGenericGenericType)) {
									
									}
								}
							}
						} else if(afRawType == BarChart.class) {
							af = new BarChartFieldPanel(f, afGenericType1);
							setupMultiLabel.accept(BarChartFieldPanel.class, Arrays.asList(f, afGenericType1));
						} else if(afRawType == BarChartPartition.class) {
							af = new BarChartPartitionFieldPanel(f, afGenericType1, afGenericType2);
							setupMultiLabel.accept(BarChartPartitionFieldPanel.class, Arrays.asList(f, afGenericType1, afGenericType2));
						}
					} catch(Exception e) {
						if(false /*f.getName().matches("(bigMoneyFreq|spinStrengths)")*/) {
							System.err.println("Could not initalize " + afType + " field panel:\n");
							e.printStackTrace();
						}
					}
				}
				
				if(af != null) {
					afPanel.add(af);
					fieldConditionPanels.put(f, afPanel);
					
					fPanel.add(afPanel);
					if(mcgAR.get() != null) {
						multiConditionLabels.put(f, mcgAR);
					}
				}
				
				fPanel.add(Box.createHorizontalGlue());
				fieldPanel.add(fPanel);
			}
		});
		
		jSP = new JScrollPane(fieldPanel);
		jSP.setMinimumSize(new Dimension(900, 450));
		jSP.setPreferredSize(new Dimension(900, 450));
		jSP.setMaximumSize(new Dimension(900, 450));
		jSP.getVerticalScrollBar().setUnitIncrement(20);
		
		find = new JButton("Search...");
		find.addActionListener(e -> {
			Map<Field, java.util.List<FieldPanel>> allAFPsByField = new LinkedHashMap<>();
			Map<Field, String> multiConditionGroupingsByField = new HashMap<>();
			
			fieldConditionPanels.forEach((f, jp) -> allAFPsByField.put(f,
				Arrays.stream(jp.getComponents()).filter(comp -> comp instanceof FieldPanel).map(comp -> (FieldPanel) comp)
					.collect(Collectors.toList()))
			);
			multiConditionLabels.forEach((f, ar) -> multiConditionGroupingsByField.put(f, ar.get()));
			
			java.util.List<Predicate<GameAnalysis>> predicatesByField = new LinkedList<>();
			
			allAFPsByField.forEach((f, afplist) -> {
				if(afplist.size() > 1) {
					String conditionOrderString = multiConditionGroupingsByField.get(f);
					
					try {
						//technically this is just a quick check for obvious impossible characters, as recursive regular expressions in Java are impossible
						if(!conditionOrderString.matches("\\([\\s\\d\\(\\)|&^!]+\\)"))
							throw new IllegalArgumentException("\"" + conditionOrderString + "\" improperly formatted");
						
						ConditionGroupingScanner coScan = new ConditionGroupingScanner(conditionOrderString);
						
						Predicate<GameAnalysis> levelPred = null;
						
						Stack<DataPoint<AtomicReference<Character>, Queue<Predicate<GameAnalysis>>>> opStack = new Stack<>();
						DataPoint<AtomicReference<Character>, Queue<Predicate<GameAnalysis>>> opAndPredQueuePair = null;
						
						while(coScan.hasNextOperand() || coScan.hasNextOperator()) {
							if(coScan.hasNextOperator()) {
								char operator = coScan.nextOperator();
								
								if(operator == '(') {
									opStack.push(opAndPredQueuePair);
									opAndPredQueuePair = new DataPoint<>(new AtomicReference<>(), new LinkedList<>());
								} else if (operator == ')') {
									switch(opAndPredQueuePair.x.get()) {
										case '&':
											levelPred = opAndPredQueuePair.y.stream().reduce(gA -> true, (and1, and2) -> and1.and(and2));
											break;
										case '|':
											levelPred = opAndPredQueuePair.y.stream().reduce(gA -> false, (or1, or2) -> or1.or(or2));
											break;
										case '!':
											if(opAndPredQueuePair.y.size() != 1)
												throw new IllegalArgumentException("\"!\" must have only one operand");
											levelPred = opAndPredQueuePair.y.remove().negate();
											break;
										case '^':
											if(opAndPredQueuePair.y.size() < 2)
												throw new IllegalArgumentException("\"^\" must have at least two operands");
											//xor1.and(xor2.negate()).or(xor2.and(xor1.negate())) should be equivalent
											levelPred = opAndPredQueuePair.y.stream().reduce((xor1, xor2) -> xor1.or(xor2).and(xor1.and(xor2).negate())).get();
											break;
										default:
											throw new IllegalArgumentException("Unknown boolean operator " + opAndPredQueuePair.x.get());
									}
									
									opAndPredQueuePair = opStack.pop();	//retrieve the super-level queue by popping the stack
									if(opAndPredQueuePair != null)	//if this is not the last expression (null passed first into stack)...
										opAndPredQueuePair.y.offer(levelPred);	//enqueue the result of the sub-expression to the super-level queue
								} else
									opAndPredQueuePair.x.set(operator);
							} else {
								int pNum = coScan.nextOperand();
								Predicate<GameAnalysis> p;
								try {
									p = afplist.get(pNum-1).fieldPredicate();
								} catch(IndexOutOfBoundsException ex) {
									p = null;
								}
								
								if(p != null)
									opAndPredQueuePair.y.offer(p);
								else
									throw new IllegalArgumentException("Condition #" + pNum + " either not set or does not exist");
							}
						}
						
						if(levelPred == null)
							throw new RuntimeException("Overall predicate somehow null");
						predicatesByField.add(levelPred);
					} catch(IllegalArgumentException ex) {
						JOptionPane.showMessageDialog(null, "<html>" + ex.getMessage() + "<br>for field " + f.getName() + "<br>Defaulting to simple OR of all "
							+ afplist.size() + " conditions", "Condition Grouping Failed", JOptionPane.INFORMATION_MESSAGE);
						predicatesByField.add(afplist.stream().map(afp -> afp.fieldPredicate()).reduce(gA -> false, (pred1, pred2) -> pred1.or(pred2)));
					} catch(Exception ex) {
						System.err.println("Unable to process condition grouping for " + f.getName() + ":\n" + ex);
					}
				} else
					Optional.ofNullable(afplist.get(0).fieldPredicate()).ifPresent(p -> predicatesByField.add(p));
			});
			
			boolean and = overallCombineOptions.getSelectedIndex() == 0;
			BinaryOperator<Predicate<GameAnalysis>> combiningPredicate = and ? Predicate::and : Predicate::or;
			
			Set<GameAnalysis> foundShows = GameAnalyses.filter(predicatesByField.stream().reduce(gA -> and, (pred1, pred2) -> combiningPredicate.apply(pred1, pred2)));
			
			String results = foundShows.stream().map(gA -> gA.getShowInfo() + "\n")
				.collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();
			StringBuilder parameterSB = new StringBuilder(foundShows.size() * 100);
			allAFPsByField.forEach((f, afplist) -> {
				java.util.List<FieldPanel> plist = afplist.stream().filter(p -> p.fieldPredicate() != null).collect(Collectors.toList());
				int pls = plist.size();
				
				if(pls == 1) {
					parameterSB.append("-" + plist.get(0).fieldPredicateString() + "\n\n");
				} else if(pls > 1) {
					parameterSB.append("-" + f.getName() + " matching the following conditions in this way: \"" + transformLISP(multiConditionGroupingsByField.get(f)) + "\"\n");
					int i = 1;
					for(FieldPanel fp : plist)
						parameterSB.append("-- " + i++ + ". " + fp.fieldPredicateString() + "\n");
					parameterSB.append('\n');
				}
			});
			
			filteredShows.setText(foundShows.size() + " show(s) found matching " + (and ? "ALL" : "ANY ONE OR MORE") + " of the following fields:\n\n" + parameterSB +
			"\n-----------------------\n\n" + results + "\n-----------------------\n\nMultiple conditions for the same field are evaluated by the given grouping for that field.");
			filteredShows.setCaretPosition(0);
		});
		
		overallCombineOptions = new JComboBox<String>();
		overallCombineOptions.setRenderer(new FieldListCellRenderer());
		overallCombineOptions.setMinimumSize(new Dimension(75, 25));
		overallCombineOptions.setPreferredSize(new Dimension(75, 25));
		overallCombineOptions.setMaximumSize(new Dimension(75, 25));
		Stream.of("AND", "OR").forEach(s -> overallCombineOptions.addItem(s));
		
		JPanel findPanel = new JPanel();
		findPanel.setLayout(new BoxLayout(findPanel, BoxLayout.LINE_AXIS));
		findPanel.add(Box.createHorizontalGlue());
		findPanel.add(find);
		findPanel.add(Box.createHorizontalStrut(15));
		findPanel.add(overallCombineOptions);
		findPanel.add(Box.createHorizontalGlue());
		
		filteredShows = new JTextPane();
		filteredShows.setMinimumSize(new Dimension(350, 500));
		filteredShows.setPreferredSize(new Dimension(350, 500));
		filteredShows.setMaximumSize(new Dimension(350, 500));
		filteredShows.setFont(new Font("KaiTi", Font.PLAIN, 14));
		jSP2 = new JScrollPane(filteredShows, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		jSP2.setRowHeaderView(new TextLineNumber(filteredShows));
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.LINE_AXIS));
		mainPanel.add(jSP);
		mainPanel.add(Box.createHorizontalStrut(15));
		mainPanel.add(jSP2);
		
		add(mainPanel);
		add(Box.createVerticalStrut(10));
		add(findPanel);
	}
	
	private static String transformLISP(String lisp) {
		String barString = lisp.substring(1, lisp.length()-1);	//remove outer ()
		char bo = barString.charAt(0);
		String boString = bo == '&' ? "and" : bo == '|' ? "or" : bo == '!' ? "not" : "xor";
		
		Scanner barScan = new Scanner(barString.substring(1));
		StringBuilder s = new StringBuilder();
		String s2;
		while((s2 = barScan.findInLine("(\\d|\\(.+?\\))")) != null)
			s.append((s.length() == 0 && bo != '!' ? "" : (bo != '!' ? " " : "") + boString + " ") + (s2.matches("\\d") ? s2 : transformLISP(s2)));
		
		return '(' + s.toString() + ')';
	}
	
	public Insets getInsets() {
		return new Insets(10, 10, 10, 10);
	}
	
	public static void main(String[] args) {
		Stream.of("(| 2 3 1)", "(& (! 2) 1)", "(^ 1 (& 2 3) 4 (| 6 7) (! 5))").forEach(s -> System.out.println(transformLISP(s)));
	}
}

//stealing from CS210 Project 2
class ConditionGroupingScanner {
	private String e;
	private int position;
	  
	public ConditionGroupingScanner(String e) {
		this.e = e;
		this.position = 0;
	}
	
	public boolean hasNextOperator() {
		skipWhiteSpace();
		return position < e.length() && isOperator(e.charAt(position));
	}
	
	public char nextOperator() {
		skipWhiteSpace();
		return e.charAt(position++);
	}
	
	public boolean hasNextOperand() {
		skipWhiteSpace();
		return position < e.length() && isDigit(e.charAt(position));
	}
	
	public int nextOperand() {
		skipWhiteSpace();
		int operand = 0;
		while (e.charAt(position) >= '0' && e.charAt(position) <='9')
			operand = 10 * operand + e.charAt(position++) - '0';
		return operand;
	}
	
	private void skipWhiteSpace() {
		char c;
		while (position < e.length() && ((c = e.charAt(position)) == ' ' || c == '\t' || c == '\n'))
			position++;
		return;
	}
	
	private static boolean isOperator(char c) {
		return c == '(' || c == '&' || c == '|' || c == '!' || c == '^' || c == ')';
	}
	
	private static boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}
}