package woftracker.stats;

import java.io.Serializable;
import java.util.Set;
import java.util.Map;

//wraps two BarCharts into one object, meant to be represented as a quotient.
class BarChartQuotient<X extends Enum<X>> implements Serializable {
	private static final long serialVersionUID = 1L;	//version number.
	
	private BarChart<X> num, den;
	private boolean usePercent;
	
	public BarChartQuotient(Set<X> xs, boolean usePercent) {
		num = new BarChart<>(xs);
		den = new BarChart<>(xs);
		this.usePercent = usePercent;
	}
	
	public BarChartQuotient(BarChart<X> num, BarChart<X> den, boolean usePercent) {
		this.num = num;
		this.den = den;
		this.usePercent = usePercent;
	}
	
	public void addY(X x, boolean includeNum) {
		if(includeNum)
			num.addY(x);
		den.addY(x);
	}
	
	public void addY(X x, Number n, boolean includeNum) {
		if(includeNum)
			num.addY(x, n);
		den.addY(x, n);
	}
	
	public void remove0s() {
		num.remove0s();
		den.remove0s();
	}
	
	public BarChart<X> getNum() {
		return num;
	}
	
	public BarChart<X> getDen() {
		return den;
	}
	
	@SuppressWarnings("unchecked")
	public boolean equals(Object o) {
		if(o == null)
			return false;
			
		if(getClass() != o.getClass())
			return false;
			
		BarChartQuotient<X> rhs = (BarChartQuotient<X>) o;
		
		return num.equals(rhs.getNum()) && den.equals(rhs.getDen());
	}
	
	public String toString() {
		return BarChart.quotient(num, den, usePercent);
	}
	
	public String toGroupedString(Map<String, Set<X>> groupings) {
		return BarChart.quotient(num, den, usePercent, groupings);
	}
}