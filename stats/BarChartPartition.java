package woftracker.stats;

import java.util.*;
import java.util.stream.*;
import java.math.*;
import woftracker.record.Player;
import woftracker.record.Letter;
import java.io.Serializable;

//encapsulates a set of similar BarCharts, ordered by a particular other enum. although method names are identical & similar, this is not a sub-class.
public class BarChartPartition<X extends Enum<X>, Y extends Enum<Y>> implements Iterable<DataPoint<X, BarChart<Y>>>, Serializable {
	private static final long serialVersionUID = 1L;	//version number.
	private static final String DEFAULT_INDENT = "  ";
	
	private Map<X, BarChart<Y>> partition;
	private String description;
	private Class<X> xClass;	//well worth saving
	
	//initializes the partition with its X-values, Y-values fresh BarCharts (all same values here). The efficient EnumSet is suggested for parameters, which also excludes null keys.
	public BarChartPartition(Set<X> keys, Set<Y> values) {
		if(keys.isEmpty() || values.isEmpty())
			throw new IllegalArgumentException("Can't provide any empty set to BarChartPartition! " +
			"Use an already empty map and Class<X> constructor to start with a partially or totally empty BarChartPartition.");
		
		partition = new EnumMap<>(xClass = keys.iterator().next().getDeclaringClass());	//grab first element to get class
		for(X x : keys) {
			partition.put(x, new BarChart<>(values));
		}
	}
	
	BarChartPartition(Map<X, BarChart<Y>> partition, Class<X> xClass) {
		this.partition = new EnumMap<>(partition);	//force enum ordering
		this.xClass = xClass;
	}
	
	//only used in combine
	private BarChartPartition() {
		partition = new HashMap<>();
		xClass = null;
	}
	
	/* Partitions are less flexible than BarCharts, so no addX methods OR removeX methods widespread. Can be done indirectly in a couple rare cases. */
	
	//remove all 0s from all BarCharts; if this leaves a BarChart empty remove entire entry from partition.
	//this should be used (if wanted) after all recording of data is done, of course.
	public void remove0s() {
		Set<X> removeSet = EnumSet.noneOf(xClass);
		
		for(X x : partition.keySet()) {
			BarChart<Y> barChart = partition.get(x);
			barChart.remove0s();
			if(barChart.getXs().isEmpty())
				removeSet.add(x);
		}
		
		for(X x : removeSet)
			partition.remove(x);
	}
	
	//if x is not in the EnumMap, throw an exception. inline copy code for add(x, y, 1)
	public void add(X x, Y y) {
		try {
			partition.get(x).addY(y);
		} catch (NullPointerException e) {
			throw new IllegalArgumentException("Enum not in BarChartPartition: " + x);
		}
	}
	
	public void add(X x, Y y, Number n) {
		try {
			partition.get(x).addY(y, n);
		} catch (NullPointerException e) {
			throw new IllegalArgumentException("Enum not in BarChartPartition: " + x);
		}
	}
	
	public boolean insert(X x, BarChart<Y> y, boolean override) {
		if(override || !partition.containsKey(x)) {
			partition.put(x, y);
			return true;
		}
		
		return false;
	}
	
	public BarChart<Y> getY(X x) {
		return partition.get(x);
	}
	
	public BarChart<Y> getY(Set<X> xs) {
		return BarChart.combine(xs.stream().map(x -> partition.get(x)).collect(Collectors.toList()));
	}
	
	public int getMaxY() {
		return partition.values().stream().mapToInt(bC -> bC.getMaxY()).max().orElse(0);
	}
	
	public int getMaxTotalY() {
		return partition.values().stream().mapToInt(bC -> bC.getTotalY()).max().orElse(0);
	}
	
	public BarChart<Y> synthesizeY(Set<X> xs) {
		//let BarChart.combine take care of nulls
		return BarChart.combine(xs.stream().map(x -> partition.get(x)).collect(Collectors.toList()));
	}
	
	public BarChart<Y> synthesizeTotalY() {
		return BarChart.combine(partition.values());
	}
	
	public boolean contains(X x) {
		return partition.containsKey(x);
	}
	
	public boolean contains(X x, Y y) {
		return contains(x) && partition.get(x).contains(y);
	}
	
	/* the below can't compile since both X and Y are enums, will conflict in actual code.
	public boolean contains(Y y) {
		for(BarChart<Y> ys : partition.values())
			if(ys.contains(y)) return true;
		
		return false;
	}*/
	
	/**
	 * creates a new BarChartPartition by adding all BarChartPartitions in the collection (set or list!).
	 * the union of all the BarChartPartitions' Xs is used for the new one.
	 */
	public static <U extends Enum<U>, V extends Enum<V>> BarChartPartition<U, V> combine(Collection<BarChartPartition<U, V>> bCPs) {
		if(bCPs == null || (bCPs = bCPs.stream().filter(bCP -> bCP.xClass != null).collect(Collectors.toList())).isEmpty())
			return new BarChartPartition<>();
		if(bCPs.size() == 1)
			return bCPs.iterator().next();	//get only element as the combination
		
		Class<U> uClass = bCPs.iterator().next().getXType();
		Set<U> combinedUs = EnumSet.noneOf(uClass), maxUs = EnumSet.allOf(uClass);
		
		for(BarChartPartition<U, V> bCP : bCPs) {
			combinedUs.addAll(bCP.getXs());	//union
			if(combinedUs.equals(maxUs)) break;	//no need to continue adding
		}
		
		Map<U, BarChart<V>> combinedPartition = new EnumMap<>(uClass);
		
		for(U u : combinedUs) {
			Collection<BarChart<V>> uBarCharts = new LinkedList<>();
			
			for(BarChartPartition<U, V> bCP : bCPs) {
				BarChart<V> bC;
				if((bC = bCP.getY(u)) != null)
					uBarCharts.add(bC);
			}
			
			combinedPartition.put(u, BarChart.combine(uBarCharts));
		}
		
		return new BarChartPartition<U, V>(combinedPartition, uClass);
	}
	
	/**
	 * returns a new BarChartPartition by reversing a parameter BarChartPartition's partitions, but displaying the same data for every (y, x).
	 * therefore, two reverses returns an identical BarChartPartition (equals returns true).
	 */
	public static <U extends Enum<U>, V extends Enum<V>> BarChartPartition<V, U> reverse(BarChartPartition<U, V> bCP) {
		if(bCP == null || bCP.getXs().isEmpty())
			return new BarChartPartition<>();	//failure
		
		Class<V> vClass = bCP.iterator().next().y.getXType();
		Set<V> combinedVs = EnumSet.noneOf(vClass), maxVs = EnumSet.allOf(vClass);
		
		for(BarChart<V> bC : bCP.getYs()) {
			combinedVs.addAll(bC.getXs());	//union
			if(combinedVs.equals(maxVs)) break;	//no need to continue adding
		}
		
		BarChartPartition<V, U> reversedPartition = new BarChartPartition<>(combinedVs, bCP.getXs());
		
		for(DataPoint<U, BarChart<V>> thisU : bCP) {
			U u = thisU.x;
			for(DataPoint<V, Integer> thisV : thisU.y)
				reversedPartition.add(thisV.x, u, thisV.y);
		}
		
		reversedPartition.remove0s();	//cleanup.
		
		return reversedPartition;
	}
	
	//the BarChart values are double indented. key difference here: part of the partition can be chosen to not be included.
	public String average(int n, int d, String indent, Set<X> whatToInclude, Map<String, Set<Y>> groupings) {
		Set<X> keySet = new HashSet<>(partition.keySet());
		if(keySet.isEmpty())
			return indent + "TOTAL | 0";
		else if(!keySet.removeAll(whatToInclude))
			throw new IllegalArgumentException("Nothing to include!");
		
		if(indent == null) indent = DEFAULT_INDENT;
		String doubleIndent = indent + indent;
		StringBuilder s = new StringBuilder(500);
		
		for(Map.Entry<X, BarChart<Y>> entry : partition.entrySet())
			if(whatToInclude.contains(entry.getKey()))
				s.append(indent + entry.getKey() + ":\n" + entry.getValue().average(n, d, doubleIndent, groupings));
		
		return s.toString();
	}
	
	public String average(int n, int d) {
		return average(n, d, DEFAULT_INDENT, partition.keySet(), null);
	}
	
	public String average(int n, int d, Map<String, Set<Y>> groupings) {
		return average(n, d, DEFAULT_INDENT, partition.keySet(), groupings);
	}
	
	//special method: use a BarChart to decide which numbers are used, e.g. roundCountTable.
	public String average(BarChart<X> n, int d, String indent, Set<X> whatToInclude, Map<String, Set<Y>> groupings) {
		Set<X> keySet = new HashSet<>(partition.keySet());
		if(keySet.isEmpty())
			return indent + "TOTAL | 0";
		else if(!keySet.removeAll(whatToInclude))
			throw new IllegalArgumentException("Nothing to include!");
		
		if(indent == null) indent = DEFAULT_INDENT;
		String doubleIndent = indent + indent;
		StringBuilder s = new StringBuilder(500);
		
		for(X x : partition.keySet())
			if(whatToInclude.contains(x))
				s.append(indent + x + ":\n" + partition.get(x).average(n.getY(x), d, doubleIndent, groupings));
		
		return s.toString();
	}
	
	public String average(BarChart<X> n, int d) {
		return average(n, d, DEFAULT_INDENT, partition.keySet(), null);
	}
	
	public String average(BarChart<X> n, int d, Map<String, Set<Y>> groupings) {
		return average(n, d, DEFAULT_INDENT, partition.keySet(), groupings);
	}
	
	/**
	 * returns a representation of two BarChartPartitions as a quotient. All Xs of the numerator should exist in the denominator, else information will be lost from the numerator.
	 * % splits are optional.
	 * whatToInclude is supported as well, but with less rigorious checking than average above (spoiler: I'm lazy).
	 */
	
	public static <U extends Enum<U>, V extends Enum<V>> String quotient(BarChartPartition<U, V> num, BarChartPartition<U, V> den, String indent, boolean percentage, Set<U> whatToInclude) {
		if(indent == null) indent = DEFAULT_INDENT;
		String doubleIndent = indent + indent;
		StringBuilder s = new StringBuilder(500);
		
		Set<U> includeSet = EnumSet.copyOf(den.getXs());
		includeSet.retainAll(whatToInclude);
		
		for(U u : includeSet) {
			BarChart<V> bNum = num.getY(u);	//if this is null below, replace with a dummy chart with 0s at the appropriate spots
			s.append(indent + u + ":\n" + BarChart.quotient(Optional.ofNullable(bNum).orElse(new BarChart<V>(den.getY(u).getXs())), den.getY(u), doubleIndent, percentage, null));
		}
		
		return s.toString();
	}
	
	public static <U extends Enum<U>, V extends Enum<V>> String quotient(BarChartPartition<U, V> num, BarChartPartition<U, V> den) {
		return quotient(num, den, DEFAULT_INDENT, false, den.getXs());
	}
	
	public static <U extends Enum<U>, V extends Enum<V>> String quotient(BarChartPartition<U, V> num, BarChartPartition<U, V> den, boolean percentage) {
		return quotient(num, den, DEFAULT_INDENT, percentage, den.getXs());
	}
	
	public static <U extends Enum<U>, V extends Enum<V>> String quotient(BarChartPartition<U, V> num, BarChartPartition<U, V> den, boolean percentage, Set<U> whatToInclude) {
		return quotient(num, den, DEFAULT_INDENT, percentage, whatToInclude);
	}
	
	public Set<X> getXs() {
		return xClass != null && !partition.isEmpty() ? EnumSet.copyOf(partition.keySet()) : Collections.emptySet();
	}
	
	public Class<X> getXType() {
		return xClass;
	}
	
	public Collection<BarChart<Y>> getYs() {
		return new LinkedList<>(partition.values());
	}
	
	@SuppressWarnings("unchecked")
	public boolean equals(Object o) {
		return o == null || getClass() != o.getClass() ? false : partition.equals(((BarChartPartition<X, Y>) o).partition);
	}
	
	public int hashCode() {
		int hc = 0, maxY = new LinkedList<>(getYs()).stream().mapToInt(bC -> bC.hashCode()).max().orElse(0);
		for(DataPoint<X, BarChart<Y>> dp: this)
			hc += dp.x.hashCode()*maxY + dp.y.hashCode();
		return hc;
	}
	
	/* more shortcuts for regular integer average. */
	
	public String toIndentedString(String indent) {
		return average(1, 0, indent, partition.keySet(), null);
	}
	
	public String toPartialString(Set<X> whatToInclude) {
		return average(1, 0, DEFAULT_INDENT, whatToInclude, null);
	}
	
	public String toIndentedPartialString(String indent, Set<X> whatToInclude) {
		return average(1, 0, indent, whatToInclude, null);
	}
	
	public String toGroupedString(Map<String, Set<Y>> groupings) {
		return average(1, 0, DEFAULT_INDENT, partition.keySet(), groupings);
	}
	
	/* end shortcuts. */
	
	public String toString() {
		return average(1, 0);
	}
	
	public Iterator<DataPoint<X, BarChart<Y>>> iterator() {
		return new DataPointIterator<>(partition);
	}
	
	public BarChartPartition<X,Y> clone() {
		Map<X, BarChart<Y>> clonedPartition = new EnumMap<>(xClass);
		for(X x : getXs())
			clonedPartition.put(x, partition.get(x).clone());	//the real cloning
		return new BarChartPartition<X,Y>(clonedPartition, xClass);
	}
	
	//testing this is VERY important
	public static void main(String[] args) {
		BarChartPartition<Player, Letter>
			testPartition1 = new BarChartPartition<>(EnumSet.of(Player.RED, Player.YELLOW), EnumSet.of(Letter.A, Letter.E)),
			testPartition2 = new BarChartPartition<>(EnumSet.of(Player.YELLOW), EnumSet.of(Letter.B, Letter.E)),
			testPartition3 = new BarChartPartition<>(EnumSet.of(Player.RED, Player.YELLOW, Player.BLUE), EnumSet.of(Letter.C, Letter.I, Letter.O)),
			testPartition4 = new BarChartPartition<>(EnumSet.of(Player.RED, Player.BLUE), EnumSet.range(Letter.J, Letter.Q));
		
		Collection<BarChartPartition<Player, Letter>> partitionSet = new HashSet<>();
		partitionSet.add(testPartition1);
		partitionSet.add(testPartition2);
		partitionSet.add(testPartition3);
		partitionSet.add(testPartition4);
			
		for(int i = 0; i < 22; i++) {
			testPartition1.add(i % 5 < 4 ? Player.RED : Player.YELLOW, i % 2 == 0 ? Letter.A : Letter.E);
			testPartition2.add(Player.YELLOW, i % 3 == 0 ? Letter.B : Letter.E);
			testPartition3.add(i % 2 == 0 ? i % 3 == 0 ? Player.RED : Player.YELLOW : Player.BLUE, i % 4 == 0 ? Letter.C : Letter.O);	//2, 4, 8, 10, 14, 16, 20
			testPartition4.add(Player.BLUE, Letter.Q);
		}
		
		BarChartPartition<Player, Letter> finalPartition = BarChartPartition.combine(partitionSet);
		System.out.print(finalPartition.toPartialString(EnumSet.complementOf(EnumSet.of(Player.RED))) + "\n");
		finalPartition.remove0s();
		System.out.print("Remove0s:\n\n" + finalPartition + "TotalMaxY of finalPartition: " + finalPartition.getMaxTotalY() + "\n\n" +
			finalPartition.synthesizeY(EnumSet.of(Player.RED, Player.BLUE)));
		
		System.out.println("Empty object test: " + new BarChartPartition<Letter, Player>());
	}
}