package woftracker.stats;

import java.util.Iterator;
import java.util.Map;
import java.util.Arrays;

public class DataPoint<X,Y> {
	public final X x;
	public final Y y;
	
	public DataPoint(X x, Y y) {
		this.x = x;
		this.y = y;
	}
	
	public String toString() {
		return x + ": " + y;
	}
}

class DataPointIterator<X,Y> implements Iterator<DataPoint<X,Y>> {
	private X[] xs;
	private Map<X, Y> m;
	private int cursor;
	
	@SuppressWarnings("unchecked")
	DataPointIterator(Map<X, Y> m) {
		this.m = m;
		xs = (X[]) m.keySet().toArray();
		cursor = 0;
	}
	
	public boolean hasNext() {
		return cursor < xs.length;
	}
	
	public DataPoint<X,Y> next() {
		X x = xs[cursor++];
		return new DataPoint<>(x, m.get(x));
	}
	
	public void remove() {
		throw new UnsupportedOperationException();
	}
}