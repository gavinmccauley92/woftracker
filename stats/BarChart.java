package woftracker.stats;

import woftracker.record.*;
import java.util.*;
import java.util.stream.*;
import java.math.*;
import java.io.Serializable;

//this is the best way to represent counts.
public class BarChart<X extends Enum<X>> implements Iterable<DataPoint<X, Integer>>, Serializable {
	private static final long serialVersionUID = 1L;	//version number.
	private static final String DEFAULT_INDENT = "  ";
	
	private Map<X, Integer> chart;
	private int totalY;
	private Class<X> xClass;	//well worth saving
	
	//initializes the chart with its X-values, Y-values 0. It is best to require the efficient EnumMap, which also excludes null keys.
	public BarChart(Set<X> keys) {
		if(keys.isEmpty())
			throw new IllegalArgumentException("Can't provide an empty set to BarChart! Use the Class<X> constructor to start with an empty BarChart.");
		
		chart = new EnumMap<>(xClass = keys.iterator().next().getDeclaringClass());	//grab first element to get class
		for(X x : keys)
			chart.put(x, 0);
		totalY = 0;
	}
	
	public BarChart(Class<X> xClass) {
		this.xClass = xClass;
		chart = new EnumMap<>(xClass);
		totalY = 0;
	}
	
	// not well-defined, for combine only
	private BarChart() {
		chart = new HashMap<>();
		xClass = null;	//error value
		totalY = 0;
	}
	
	//takes the union of the current EnumSet and the set provided.
	public void addX(Set<X> xs) {
		for(X x : xs)
			if(!chart.containsKey(x))
				chart.put(x, 0);
	}
	
	//if a certain x is not in this BarChart, there are no consequences to using remove, so no check there
	public void removeX(Set<X> xs) {
		for(X x : xs) {
			Integer i;
			if((i = chart.remove(x)) != null)
				totalY -= i;
		}
	}
	
	//all entries with Y-values 0 are removed from the BarChart. totalY thus does not need to be updated.
	public void remove0s() {
		Set<X> xs = EnumSet.noneOf(xClass);
		
		for(X x : chart.keySet())
			if(chart.get(x) == 0) xs.add(x);
		for(X x : xs)
			chart.remove(x);
	}
	
	//if x is not in the EnumMap, throw an exception. inline copy code for addY(x, 1)
	public void addY(X x) {
		if(chart.containsKey(x)) {
			chart.put(x, chart.get(x) + 1);
			totalY++;
		} else
			throw new IllegalArgumentException("Enum not in BarChart: " + x);
	}
	
	public void addY(X x, Number n) {
		if(chart.containsKey(x)) {
			int i = n.intValue();
			chart.put(x, chart.get(x) + i);
			totalY += i;
		} else
			throw new IllegalArgumentException("Enum not in BarChart: " + x);
	}
	
	//here null can be used as an error value instead of an exception, because null keys are impossible.
	public Integer getY(X x) {
		return chart.get(x);
	}
	
	public Integer getY(Set<X> xs) {
		return xs.equals(chart.keySet()) ? totalY : xs.stream().mapToInt(x -> chart.getOrDefault(x, 0)).sum();
	}
	
	public int getMaxY() {
		return totalY == 0 ? 0 : Collections.max(chart.values());
	}
	
	public int getMinY() {
		return totalY == 0 ? 0 : Collections.min(chart.values());
	}
	
	public int getTotalY() {
		return totalY;
	}
	
	public boolean contains(X x) {
		return chart.containsKey(x);
	}
	
	public BarChart<X> subChart(Set<X> subXs) {
		Set<X> xs = new HashSet<>(subXs);
		xs.retainAll(chart.keySet());
		if(xs.isEmpty())
			return new BarChart<X>(xClass);	//empty
		BarChart<X> bC = new BarChart<>(xs);
		
		for(X x : xs)
			bC.addY(x, chart.get(x));
			
		return bC;
	}
	
	public BarChart<X> difference(BarChart<X> other) {
		BarChart<X> bC = this.clone();
		bC.addX(EnumSet.allOf(xClass));
		for(DataPoint<X, Integer> dp : other)
			bC.addY(dp.x, -dp.y);
		bC.remove0s();
		return bC;
	}
	
	/**
	 * creates a new BarChart by addYing all BarCharts in the collection (set or list!).
	 * the union of all the BarCharts' Xs is used for the new one.
	 */
	public static <U extends Enum<U>> BarChart<U> combine(Collection<BarChart<U>> bCs) {
		if(bCs == null || (bCs = bCs.stream().filter(bC -> bC != null && bC.xClass != null).collect(Collectors.toList())).isEmpty())
			return new BarChart<>();	//shouldn't be combining anything null or empty
		if(bCs.size() == 1)
			return bCs.iterator().next();	//get only element as the combination
		
		Class<U> uClass = bCs.iterator().next().getXType();
		Set<U> combinedUs = EnumSet.noneOf(uClass), maxUs = EnumSet.allOf(uClass);
		
		for(BarChart<U> bC : bCs) {
			combinedUs.addAll(bC.getXs());	//union
			if(combinedUs.equals(maxUs)) break;	//no need to continue adding
		}
		
		BarChart<U> combinedChart;	//can't do this section all on one line with two different parameter types in constructor
		if(!combinedUs.isEmpty())
			combinedChart = new BarChart<>(combinedUs);
		else
			combinedChart = new BarChart<>(uClass);
		
		bCs.stream().map(bC -> bC.chart).forEach(c -> c.forEach((u, i) -> combinedChart.addY(u, i)));
		
		combinedChart.remove0s();
		return combinedChart;
	}
	
	/**
	 * returns a representation of this BarChart as averaged over n other frequency charts, to d places after decimal point.
	 * % splits included with set rounding of 1 place after decimal point.
	 */
	public String average(int n, int d, String indent, Map<String, Set<X>> groupings) {
		if(indent == null) indent = DEFAULT_INDENT;
		StringBuilder s = new StringBuilder(200);
		
		Set<X> individualXs = getXs();	//need a copy since set changes are reflected in map
		Map<String, Set<X>> rowMap = new HashMap<>(Optional.ofNullable(groupings).orElse(Collections.emptyMap()));
		
		//just in case... need no empty sets or non-sets here (and for that matter, no null/empty strings). xClass null check for private empty charts.
		Set<String> badNames = new HashSet<>();
		rowMap.forEach((sn, xs) -> {
			if(sn == null || xs == null || xs.isEmpty() || (getMinY() != 0 && Collections.disjoint(xs, individualXs)))	//if disjoint and 0s removed
				badNames.add(sn);
			else
				rowMap.replace(sn, EnumSet.copyOf(xs));	//force order
		});
		badNames.stream().forEach(bn -> rowMap.remove(bn));
		rowMap.remove("");
		
		for(Set<X> xs : rowMap.values())	//currently just groupings
			individualXs.removeAll(xs);
		for(X x : individualXs)
			rowMap.put(x.toString(), EnumSet.of(x));
		//once again, the hack of getting the first [only] element of a set is big.
		Set<String> names = new TreeSet<>((s1, s2) -> rowMap.get(s1).iterator().next().ordinal() - rowMap.get(s2).iterator().next().ordinal());
		names.addAll(rowMap.keySet());
		
		boolean more = n > 1 & totalY > 0;
		int widthX = Stream.concat(Stream.of("TOTAL"), rowMap.keySet().stream()).mapToInt(x -> String.valueOf(x).length()).max().getAsInt();	//get is guaranteed with TOTAL there
		int widthY = String.valueOf(totalY).length();	//totalY is by definition the max number and thus the potential max length.
		BigDecimal bD = new BigDecimal((double)(totalY)/n).setScale(d, RoundingMode.HALF_UP);
		int widthAverage = String.valueOf(bD).length();	//same as above
		int widthPercent = "100.0".length();	//same as above
		
		String formatX = "%-" + widthX + "s", formatY = "%" + widthY + "d", formatAverage = "%" + widthAverage + "s", formatPercent = "%" + widthPercent + "s";
		
		for(String name : names) {
			int i = getY(rowMap.get(name));
			s.append(indent + String.format(formatX, name) + " | " + String.format(formatY, i)
				+ (more ? " | " + String.format(formatAverage, new BigDecimal((double)(i)/n).setScale(d, RoundingMode.HALF_UP)) +
				" | " + String.format(formatPercent, new BigDecimal((double)(i)*100/totalY).setScale(1, RoundingMode.HALF_UP)) + "%" : "") + "\n");
		}
		
		String totalLine = String.format(formatX, "TOTAL") + " | " + String.format(formatY, totalY) + (more ? " | " + String.format(formatAverage, bD)
			+ " | " + String.format(formatPercent, new BigDecimal(100).setScale(1, RoundingMode.HALF_UP)) + "%" : "");	//of course, should be 100.0%
		if(!chart.isEmpty()) {
			s.append(indent);
			for(int i = 0; i < totalLine.length(); i++)
				s.append(totalLine.charAt(i) == '|' ? '|' : '-');
			s.append('\n');
		}
		s.append(indent + totalLine + "\n\n");
		
		return s.toString();
	}
	
	public String average(int n, int d) {
		return average(n, d, DEFAULT_INDENT, null);
	}
	
	public String average(int n, int d, String indent) {
		return average(n, d, indent, null);
	}
	
	public String average(int n, int d, Map<String, Set<X>> groupings) {
		return average(n, d, DEFAULT_INDENT, groupings);
	}
	
	/**
	 * returns a representation of two BarCharts as a quotient. All Xs of the numerator should exist in the denominator, else information will be lost from the numerator.
	 * % splits are not included for the time being.
	 */
	
	public static <U extends Enum<U>> String quotient(BarChart<U> num, BarChart<U> den, String indent, boolean percentage, Map<String, Set<U>> groupings) {
		if(indent == null) indent = DEFAULT_INDENT;
		StringBuilder s = new StringBuilder(200);
		
		Set<U> individualUs = den.getXs();	//need a copy since set changes are reflected in map
		Map<String, Set<U>> rowMap = new HashMap<>(Optional.ofNullable(groupings).orElse(Collections.emptyMap()));
		
		//just in case... need no empty sets or non-sets here (and for that matter, no null/empty strings). xClass null check for private empty charts.
		Set<String> badNames = new HashSet<>();
		rowMap.forEach((sn, us) -> {
			if(sn == null || us == null || us.isEmpty() || (den.getMinY() != 0 && Collections.disjoint(us, individualUs)))	//if disjoint and 0s removed
				badNames.add(sn);
			else
				rowMap.replace(sn, (EnumSet.copyOf(us)));	//force order
		});
		badNames.stream().forEach(bn -> rowMap.remove(bn));
		rowMap.remove("");
		
		for(Set<U> us : rowMap.values())	//currently just groupings
			individualUs.removeAll(us);
		for(U u : individualUs)
			rowMap.put(u.toString(), EnumSet.of(u));
		//once again, the hack of getting the first [only] element of a set is big.
		Set<String> names = new TreeSet<>((s1, s2) -> rowMap.get(s1).iterator().next().ordinal() - rowMap.get(s2).iterator().next().ordinal());
		names.addAll(rowMap.keySet());
		
		int widthU = Stream.concat(Stream.of("TOTAL"), rowMap.keySet().stream()).mapToInt(x -> String.valueOf(x).length()).max().getAsInt();	//get is guaranteed with TOTAL there
		int widthNum = String.valueOf(num.getTotalY()).length(), widthDen = String.valueOf(den.getTotalY()).length();	//same totalY trick as above
		int widthPercent = "100.0".length();	//same as above
		
		String formatU = "%-" + widthU + "s", formatNum = "%" + widthNum + "d", formatDen = "%" + widthDen + "d", formatPercent = "%" + widthPercent + "s";
		
		for(String name : names) {
			int iDen = den.getY(rowMap.get(name)), iNum = num.getY(rowMap.get(name));
			s.append(indent + String.format(formatU, name) + " | " + String.format(formatNum, iNum) + " / " + String.format(formatDen, iDen) + 
				(percentage ? " | " + (iDen != 0 ? String.format(formatPercent, new BigDecimal(100.0*iNum/iDen).setScale(1, RoundingMode.HALF_UP)) + "%" : "") : "") + "\n");
		}
		
		String totalLine = String.format(formatU, "TOTAL") + " | " + String.format(formatNum, num.getTotalY()) + " / " + String.format(formatDen, den.getTotalY()) +
			(percentage ? " | " + String.format(formatPercent, new BigDecimal(100.0*num.getTotalY()/den.getTotalY()).setScale(1, RoundingMode.HALF_UP)) + "%" : "");
		if(!den.getXs().isEmpty()) {	//non-empty Quotient
			s.append(indent);
			for(int i = 0; i < totalLine.length(); i++)
				s.append(totalLine.charAt(i) == '|' ? '|' : '-');
			s.append('\n');
		}
		s.append(indent + totalLine + "\n\n");
		
		return s.toString();
	}
	
	public static <U extends Enum<U>> String quotient(BarChart<U> num, BarChart<U> den) {
		return quotient(num, den, DEFAULT_INDENT, false, null);
	}
	
	public static <U extends Enum<U>> String quotient(BarChart<U> num, BarChart<U> den, Map<String, Set<U>> groupings) {
		return quotient(num, den, DEFAULT_INDENT, false, groupings);
	}
	
	public static <U extends Enum<U>> String quotient(BarChart<U> num, BarChart<U> den, boolean percentage) {
		return quotient(num, den, DEFAULT_INDENT, percentage, null);
	}
	
	public static <U extends Enum<U>> String quotient(BarChart<U> num, BarChart<U> den, boolean percentage, Map<String, Set<U>> groupings) {
		return quotient(num, den, DEFAULT_INDENT, percentage, groupings);
	}
	
	public Set<X> getXs() {
		return xClass != null && !chart.isEmpty() ? EnumSet.copyOf(chart.keySet()) : Collections.emptySet();
	}
	
	public Collection<Integer> getYs() {
		return new LinkedList<>(chart.values());
	}
	
	public Class<X> getXType() {
		return xClass;
	}
	
	@SuppressWarnings("unchecked")
	public boolean equals(Object o) {
		return o == null || getClass() != o.getClass() ? false : chart.equals(((BarChart<X>) o).chart);
	}
	
	public int hashCode() {
		int hc = 0, maxY = Collections.max(getYs());
		for(DataPoint<X, Integer> dp: this)
			hc += dp.x.hashCode()*maxY + dp.y.hashCode();
		return hc;
	}
	
	public String toIndentedString(String indent) {
		return average(1, 0, indent, null);
	}
	
	public String toGroupedString(Map<String, Set<X>> groupings) {
		return average(1, 0, DEFAULT_INDENT, groupings);
	}
	
	public String toString() {
		return average(1, 0, DEFAULT_INDENT, null);
	}
	
	public Iterator<DataPoint<X, Integer>> iterator() {
		return new DataPointIterator<>(chart);
	}
	
	//this is technically not overriding Object's clone() since Object needs to be returned, oddly. but that's a hassle.
	//(also, subChart(getXs()) works just as well)
	public BarChart<X> clone() {
		BarChart<X> clone = new BarChart<X>(xClass);
		clone.chart = new EnumMap<>(chart);	//this will create a fresh copy of the map.
		clone.totalY = totalY;	//primitive copies are fine, two separate variables.
		return clone;
	}
	
	//test, GO!
	public static void main(String[] args) {
		BarChart<Letter>
			testChart1 = new BarChart<>(EnumSet.of(Letter.A, Letter.E)),
			testChart2 = new BarChart<>(EnumSet.of(Letter.B, Letter.E)),
			testChart3 = new BarChart<>(EnumSet.of(Letter.C, Letter.I, Letter.O)),
			testChart4 = new BarChart<>(EnumSet.range(Letter.J, Letter.Q));
			
		Collection<BarChart<Letter>> chartSet = new HashSet<>();
		chartSet.add(testChart1);
		chartSet.add(testChart2);
		chartSet.add(testChart3);
		chartSet.add(testChart4);
			
		for(int i = 0; i < 11; i++) {
			testChart1.addY(i % 2 == 0 ? Letter.A : Letter.E);
			testChart2.addY(i % 3 == 0 ? Letter.B : Letter.E);
			testChart3.addY(i % 4 == 0 ? Letter.C : Letter.O);
			testChart4.addY(Letter.Q);
		}
		
		testChart4.removeX(EnumSet.of(Letter.L, Letter.N, Letter.O, Letter.A));
		testChart2.addX(EnumSet.of(Letter.Z, Letter.C, Letter.E));
		testChart2.addY(Letter.C, 3);
		testChart2.addY(Letter.Z);
		
		BarChart<Letter> finalChart = BarChart.combine(chartSet);
		System.out.print(finalChart + "\n\ntestChart2 total: " + testChart2.getTotalY() + "\nfinalChart sub-total of testChart2 values: " + finalChart.getY(testChart2.getXs())
			+ "\n\n" + finalChart.average(4, 2) + "\n\n\n"
		);
		
		BarChart<Letter> finalChartClone = finalChart.clone();
		//finalChartClone.addY(Letter.Z, 10);
		System.out.println("Clone test:\n" + finalChart + "\n" + finalChartClone + "\nEquals test: " + finalChart.equals(finalChartClone));
		
		//groupings
		Map<String, Set<Letter>> m = new HashMap<>();
		m.put("E/Q", new LinkedHashSet<>(Arrays.asList(Letter.E, Letter.Q)));
		m.put("B/J", EnumSet.of(Letter.B, Letter.J));
		m.put("A/K/O/M/Z", new LinkedHashSet<>(Arrays.asList(Letter.O, Letter.Z, Letter.K, Letter.M, Letter.A)));
		m.put("Hey", null);
		m.put("HEllo hello", Collections.emptySet());
		m.put(null, EnumSet.of(Letter.A));
		m.put("", EnumSet.of(Letter.C));
		System.out.println("Grouping test:\n" + finalChart.toGroupedString(m));
		System.out.println("Empty object test: " + new BarChart<Letter>());
	}
}