package woftracker.stats;

import woftracker.util.*;
import java.util.*;
import java.util.stream.*;
import java.math.*;
import java.io.Serializable;

import static woftracker.util.FormatFactory.*;

/* Number does not implement Comparable, and sorting the data is very important.
 * Shouldn't need anything other than Integer anyways */

 //creates immutable data from a collection of integers, calculates stats on them, and stores them for later retrieval.
public class OneVarStats implements Serializable {
	private static final long serialVersionUID = 1L;	//verison number.
	
	public static final int MIN = 0, Q1 = 1, MEDIAN = 2, Q3 = 3, MAX = 4, MODES = 5, MODE_COUNT = 6, MEAN = 7, STDEV = 8, RANGE = 9;
	
	private LinkedList<Number> data, fiveNumberSummary, modes;	//size of 5: min, q1, median, q3, max
	private int modeCount;
	private Number mean, stdev, range;
	private String dataString;
	
	private static final String SEPARATOR = "  |  ";
	
	public OneVarStats(Collection<? extends Number> c) {
		this(c, OutlierTechnique.NONE);
	}
	
	public OneVarStats(Collection<? extends Number> c, OutlierTechnique ot) {
		if(c == null || c.contains(null))
			throw new NullPointerException("A OneVarStats must have an non-null collection to work on with no null elements");
		if(ot == null)
			throw new NullPointerException("A OneVarStats must specify an outlier technique: NONE, MILD or EXTREME");
		if(c.size() == 0) {
			data = new LinkedList<>();
			fiveNumberSummary = null; modes = null; modeCount = 0; mean = 0; stdev = 0; range = 0;
			dataString = "n = 0";
			return;
		}
			
		data = new LinkedList<>(c);
		int size = data.size();
		data.sort((n1, n2) -> Double.compare(n1.doubleValue(), n2.doubleValue()));
		
		calculateFNS();
		
		if(ot != OutlierTechnique.NONE && size >= 5) {
			double q1 = get(Q1).doubleValue(), q3 = get(Q3).doubleValue();	//have these numbers on-hand
			double fence = (ot == OutlierTechnique.MILD ? 1.5 : 3) * (q3 - q1);
			int lowerIndex = (int) (data.size()*0.25), upperIndex = (int) (data.size()*0.75);
			
			while(data.get(lowerIndex).doubleValue() > q1 - fence && --lowerIndex != -1)	//if finding low outliers, lowerIndex+1 corresponds to the first new data
				;
			while(data.get(upperIndex).doubleValue() < q3 + fence && ++upperIndex != size)	//if finding high outliers, upperIndex-1 corresponds to the last new data
				;
			
			if(lowerIndex != -1 || upperIndex != size) {	//if either index has changed, trim data down and recalculate FNS.
				//<=lowerIndex and >=upperIndex are to be removed.
				this.data = new LinkedList<>(data.subList(lowerIndex+1, upperIndex));	//inclusive to exclusive.
				size = data.size();
				calculateFNS();
			}
		}
		
		double rawMean = data.stream().mapToDouble(n -> n.doubleValue()).average().getAsDouble();
		mean = new BigDecimal(rawMean).setScale(Math.abs(rawMean) >= 10.0 ? 1 : 2, RoundingMode.HALF_UP);
		
		if(size != 1) {
			double tempMean = mean.doubleValue(), squareSum = 0;
			for(Number n : data) squareSum += (tempMean-n.doubleValue())*(tempMean-n.doubleValue());
			stdev = formatNumber(Math.pow(squareSum / (size-1), 0.5));
		} else
			stdev = formatNumber(0);
		
		modes = new LinkedList<>();
		int count = 1;
		modeCount = 1;
		for(int i = 1; i < data.size(); i++) {
			if(data.get(i).equals(data.get(i-1))) {
				count++;
				if(count >= modeCount) {
					if(count > modeCount) {
						modes.clear();
						modeCount = count;
					}
					modes.add(formatNumber(data.get(i)));
				}
			} else
				count = 1;
		}
		
		range = formatNumber(data.getLast().doubleValue() - data.getFirst().doubleValue());
		
		dataString = "n = " + size + SEPARATOR + formatCollection(fiveNumberSummary) + SEPARATOR + mean + " +/- " + stdev + SEPARATOR +
			(modeCount != 1 ? modeCount + ": " + formatCollection(modes) : "none")
		;
	}
	
	private void calculateFNS() {
		int size = data.size(), halfIndex = (int) Math.floor(size*0.5d);
		boolean isEven = size % 2 == 0;
		int firstHalfIndex = size != 1 ? halfIndex : 1, secondHalfIndex = size != 1 ? halfIndex + (isEven ? 0 : 1) : 0;
		
		fiveNumberSummary = new LinkedList<>();
		fiveNumberSummary.add(formatNumber(data.getFirst()));
		fiveNumberSummary.add(formatNumber(median(data.subList(0, firstHalfIndex))));
		fiveNumberSummary.add(formatNumber(median(data)));
		fiveNumberSummary.add(formatNumber(median(data.subList(secondHalfIndex, size))));
		fiveNumberSummary.add(formatNumber(data.getLast()));
	}
	
	//no need to sort here, that is already done above
	private static Number median(List<Number> l) {
		int size = l.size();
		
		if(size == 1)
			return l.get(0);
		
		if(size % 2 == 1)
			return l.get(((size+1)/2) - 1);	//some annoying 0-index adjusting here
		else {
			int halfIndex = size/2;
			return (l.get(halfIndex-1).doubleValue()+l.get(halfIndex).doubleValue())/2;
		}
	}
	
	public static OneVarStats combine(Collection<OneVarStats> c, OutlierTechnique ot) {
		List<Number> allData = new LinkedList<>();
		
		for(OneVarStats oVS : c)
			allData.addAll(oVS.getData());
		
		return new OneVarStats(allData, ot);
	}
	
	//note the int: obviously truncates and meant for int only
	public BarChart<IntegerEnum> intBarChart() {
		if(data.size() == 0)
			return new BarChart<>(IntegerEnum.class);
		
		int from = data.getFirst().intValue(), to = data.getLast().intValue();
		
		if(from >= 0 && to < IntegerEnum.values().length) {	//every element must be within IntegerEnum's bounds. length is also last value for IntegerEnum
			BarChart<IntegerEnum> distribution = new BarChart<>(IntegerEnum.createRange(from, to));
			int count = 0; Number cur = data.getFirst();
			for(Number n : data) {
				if(!n.equals(cur)) {
					distribution.addY(IntegerEnum.getEnum(cur), count);
					cur = n;
					count = 1;
				} else
					count++;
			}
			distribution.addY(IntegerEnum.getEnum(cur), count);
			distribution.remove0s();
			return distribution;
		} else
			return new BarChart<>(IntegerEnum.class);	//failed
	}
	
	public boolean equals(Object o) {
		return o instanceof OneVarStats ? this.data.equals(((OneVarStats) o).data) : false;
	}
	
	//supports all but modes, which won't really be gotten here
	public Number get(int i) {
		switch(i) {
			case MIN: case Q1: case MEDIAN: case Q3: case MAX:
				return fiveNumberSummary.get(i);
			case MEAN:
				return mean;
			case MODE_COUNT:
				return modeCount;
			case STDEV:
				return stdev;
			case RANGE:
				return range;
			default:
				return null;
		}
	}
	
	public int getN() {
		return data.size();
	}
	
	public List<Number> getData() {
		return Collections.unmodifiableList(data);
	}
	
	public OneVarStats clone() {
		return new OneVarStats(getData());
	}
	
	public OneVarStats withOT(OutlierTechnique ot) {
		return new OneVarStats(getData(), ot);
	}
	
	public String toString() {
		return dataString;
	}
	
	public static void main(String[] args) {
		//http://www.itl.nist.gov/div898/handbook/prc/section1/prc16.htm
		List<Integer> testSet = Arrays.asList(-30, 171, 184, 201, 212, 250, 265, 270, 272, 289, 305, 306, 322, 322, 336, 346, 351, 370, 390, 404, 409, 411, 436, 437, 439, 441, 444, 448, 451, 453, 470, 480, 482, 487, 494, 495, 499, 503, 514, 521, 522, 527, 548, 550, 559, 560, 570, 572, 574, 578, 585, 592, 592, 607, 616, 618, 621, 629, 637, 638, 640, 656, 668, 707, 709, 719, 737, 739, 752, 758, 766, 792, 792, 794, 802, 818, 830, 832, 843, 858, 860, 869, 918, 925, 953, 991, 1000, 1005, 1068, 1441);
		OneVarStats outlierTest = new OneVarStats(testSet, OutlierTechnique.MILD), normalTest = new OneVarStats(testSet);
		System.out.println("Outliers removed:\n" + outlierTest + "\n\n" + outlierTest.intBarChart() + "\nNormal:\n" + normalTest + "\n\n" + normalTest.intBarChart());
		
		System.out.println(new OneVarStats(Arrays.asList(47, 52, 56, 54, 14)));
		//Pocket Change! See Pocket.java for results
		/*
		List<Float> distribution = Arrays.asList(.00f, .00f, .05f, .05f, .05f, .05f, .10f, .10f, .10f, .10f, .10f,
												.25f, .25f, .25f, .25f, .50f, .50f, .50f, .75f, 2.00f),
		results = new LinkedList<>();
		
		final int PLAYINGS = 100000;
		
		for(int i = 0; i < PLAYINGS; i++) {
			List<Float> thisDistribution = new LinkedList<>(distribution);
			Collections.shuffle(thisDistribution);
			
			float total = 0.25f;
			
			for(int j = 0; j < 4; j++)
				total += thisDistribution.get(j);
				
			results.add(total);
		}
		
		System.out.println(new OneVarStats(results));
		*/
	}
}

//might make public later
enum OutlierTechnique {
	NONE, MILD, EXTREME;
}