package woftracker.stats;

import java.io.Serializable;
import java.util.Set;
import java.util.Map;

//wraps two BarChartPartitions of X & Y into one object, meant to be represented as a quotient.
class BarChartPartitionQuotient<X extends Enum<X>, Y extends Enum<Y>> implements Serializable {
	private static final long serialVersionUID = 1L;	//version number.
	
	private BarChartPartition<X, Y> num, den;
	private boolean usePercent;
	
	public BarChartPartitionQuotient(Set<X> xs, Set<Y> ys, boolean usePercent) {
		num = new BarChartPartition<>(xs, ys);
		den = new BarChartPartition<>(xs, ys);
		this.usePercent = usePercent;
	}
	
	public BarChartPartitionQuotient(BarChartPartition<X, Y> num, BarChartPartition<X, Y> den, boolean usePercent) {
		this.num = num;
		this.den = den;
		this.usePercent = usePercent;
	}
	
	public void add(X x, Y y, boolean includeNum) {
		if(includeNum)
			num.add(x, y);
		den.add(x, y);
	}
	
	public void add(X x, Y y, Number n, boolean includeNum) {
		if(includeNum)
			num.add(x, y, n);
		den.add(x, y, n);
	}
	
	public void remove0s() {
		num.remove0s();
		den.remove0s();
	}
	
	public BarChartPartition<X, Y> getNum() {
		return num;
	}
	
	public BarChartPartition<X, Y> getDen() {
		return den;
	}
	
	@SuppressWarnings("unchecked")
	public boolean equals(Object o) {
		if(o == null)
			return false;
			
		if(getClass() != o.getClass())
			return false;
			
		BarChartPartitionQuotient<X, Y> rhs = (BarChartPartitionQuotient<X, Y>) o;
		
		return num.equals(rhs.getNum()) && den.equals(rhs.getDen());
	}
	
	public String toPartialString(Set<X> whatToInclude) {
		return BarChartPartition.quotient(num, den, usePercent, whatToInclude);
	}
	
	public BarChartQuotient<Y> synthesizedQuotient() {
		return new BarChartQuotient<>(num.synthesizeTotalY(), den.synthesizeTotalY(), usePercent);
	}
	
	public String toString() {
		return BarChartPartition.quotient(num, den, usePercent, den.getXs());
	}
}