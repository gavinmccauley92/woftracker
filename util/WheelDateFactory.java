package woftracker.util;

import java.time.*;
import java.time.temporal.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.*;
import java.util.function.*;

public final class WheelDateFactory {
	//private static final Map<LocalDate, Integer> SEASON_START_BY_DATE = new LinkedHashMap<>(10), SEASON_END_BY_DATE = new LinkedHashMap<>(10);
	private static final Map<Integer, LocalDate> SEASON_START_BY_NUMBER = new LinkedHashMap<>(10), SEASON_END_BY_NUMBER = new LinkedHashMap<>(10);
	private static final int MIN_S = 1, MAX_S = 33;
	
	private static LocalDate MIN_DATE, MAX_DATE;
	private static final Map<Integer, Set<LocalDate>> REPEAT_MONDAYS;
	
	static {
		int[][] seasonStartArray = {
			{ 1983, 9, 19 },
			{ 1984, 9, 10 },
			{ 1985, 9,  9 },
			{ 1986, 9,  8 },
			{ 1987, 9, 14 },
			{ 1988, 9,  5 },
			{ 1989, 9,  4 },
			{ 1990, 9,  3 },
			{ 1991, 9,  2 },
			{ 1992, 9,  7 },
			{ 1993, 9,  6 },
			{ 1994, 9,  5 },
			{ 1995, 9,  4 },
			{ 1996, 9,  2 },
			{ 1997, 9,  1 },
			{ 1998, 9,  7 },
			{ 1999, 9,  6 },
			{ 2000, 9,  4 },
			{ 2001, 9,  3 },
			{ 2002, 9,  2 },
			{ 2003, 9,  8 },
			{ 2004, 9,  6 },
			{ 2005, 9, 12 },
			{ 2006, 9, 11 },
			{ 2007, 9, 10 },
			{ 2008, 9,  8 },
			{ 2009, 9, 14 },
			{ 2010, 9, 13 },
			{ 2011, 9, 19 },
			{ 2012, 9, 17 },
			{ 2013, 9, 16 },
			{ 2014, 9, 15 },
			{ 2015, 9, 14 }
		},
		repeatMondays = {
			{ 1983, 12, 26 },
			{ 1984,  4, 23 },
			{ 1984, 12, 31 },
			{ 1985,  4,  8 },
			{ 1985, 12, 30 },
			{ 1986,  3, 31 },
			{ 1986, 12, 29 },
			{ 1987,  4, 20 },
			{ 1987, 12, 28 },
			{ 1988,  4,  4 },
			{ 1988, 12, 26 },
			{ 1989,  3, 27 },
			{ 1990,  1,  1 },
			{ 1990,  4, 16 },
			{ 1990, 12, 31 },
			{ 1991,  4,  8 },
			{ 1991, 12, 30 },
			{ 1992,  4, 20 },
			{ 1993,  1,  4 },
			{ 1993,  4, 12 },
			{ 1993, 12, 27 },
			{ 1994,  3, 28 },
			{ 1994, 12, 26 },
			{ 1995,  1,  2 },
			{ 1995,  4, 17 },
			{ 1995, 12, 25 },
			{ 1996,  4,  8 },
			{ 1996,  6, 10 }, { 1996, 6, 17 }, { 1996, 6, 24 }, { 1996, 7, 1 }, { 1996, 7, 8 },
			{ 1996, 12, 30 },
			{ 1997,  3, 24 },
			{ 1997, 12, 29 },
			{ 1998,  3, 23 },
			{ 2009,  6,  1 }, { 2009, 6, 8 } , { 2009, 6, 15 }, { 2009, 6, 22 }, { 2009, 6, 29 }, { 2009, 7, 6 }
		};
		
		REPEAT_MONDAYS = Arrays.stream(repeatMondays).map(a -> LocalDate.of(a[0], a[1], a[2]))
			.collect(Collectors.groupingBy(d -> LocalDate.of(1982, 8, 31).until(d).getYears(), Collectors.toSet()));
		
		LocalDate d2 = null;
		for(int i = MIN_S; i <= MAX_S; i++) {
			LocalDate d = LocalDate.of(seasonStartArray[i-MIN_S][0], seasonStartArray[i-MIN_S][1], seasonStartArray[i-MIN_S][2]);
			if(d.getDayOfWeek() != DayOfWeek.MONDAY)
				throw new IllegalArgumentException();
			
			if(MIN_DATE == null)
				MIN_DATE = d;
			d2 = d.plusWeeks(39 + Optional.ofNullable(REPEAT_MONDAYS.get(i)).orElse(Collections.emptySet()).size()).minusDays(3);
			//SEASON_START_BY_DATE.put(d, i);
			//SEASON_END_BY_DATE.put(d2, i);
			SEASON_START_BY_NUMBER.put(i, d);
			SEASON_END_BY_NUMBER.put(i, d2);
		}
		MAX_DATE = d2;
	}
	
	private WheelDateFactory() {}
	
	public static int showNumberFromDate(LocalDate date) {
		if(date.getDayOfWeek().getValue() >= 6 || date.isBefore(MIN_DATE))
			return -1;
		
		int showNumber = -1, seasonNumber = -1;
		for(int i = MIN_S; i <= MAX_S; i++)
			//isBefore/isAfter are strict inequalities, so expand to include endpoints
			if(date.isAfter(SEASON_START_BY_NUMBER.get(i).minusDays(1)) && date.isBefore(SEASON_END_BY_NUMBER.get(i).plusDays(1))) {
				showNumber = 195*((seasonNumber = i)-1);	//ends up corresponding to previous season finale
				break;
			}
		
		if(showNumber != -1) {
			Set<LocalDate> repeatMondays = REPEAT_MONDAYS.get(seasonNumber);
			if(repeatMondays != null && repeatMondays.stream().anyMatch(mon -> Math.abs(mon.until(date, ChronoUnit.DAYS)-2) <= 2))	// 0-4 --> -2 to 2
				return -1;
			
			showNumber += 5*(SEASON_START_BY_NUMBER.get(seasonNumber).until(date, ChronoUnit.WEEKS)
				- (repeatMondays != null ? repeatMondays.stream().filter(mon -> mon.isBefore(date.plusDays(1))).count() : 0))	//mid-season repeat adjustment
				+ date.getDayOfWeek().getValue();	//adjust week, then weekday
		}
		
		return showNumber;
	}
	
	public static LocalDate dateFromShowNumber(int showNumber) {
		int seasonNumber = ((showNumber-1)/195) + 1;
		
		if(seasonNumber < MIN_S || seasonNumber > MAX_S)
			return null;
		
		int showOfSeason = (showNumber-1) % 195;	// 0-194 index here
		
		LocalDate date = SEASON_START_BY_NUMBER.get(seasonNumber).plusWeeks(showOfSeason / 5).plusDays(showOfSeason % 5);
		
		Set<LocalDate> repeatMondays = REPEAT_MONDAYS.get(seasonNumber);
		int adjust = repeatMondays != null ? (int) repeatMondays.stream().filter(mon -> mon.isBefore(date.plusDays(1))).count() : 0;
		
		//initial adjustment might lead to a new repeat week - keep doing it until it doesn't change
		while(repeatMondays != null) {
			final LocalDate date2 = date.plusWeeks(adjust);
			int thisAdjust = (int) repeatMondays.stream().filter(mon -> mon.isBefore(date2.plusDays(1))).count();
			
			if(thisAdjust > adjust)
				adjust = thisAdjust;
			else
				break;
		}
		
		return date.plusWeeks(adjust);
	}
	
	public static void main(String[] args) {
		if(args.length == 3) {
			LocalDate d = LocalDate.of(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
			System.out.println(showNumberFromDate(d));
		} else if(args.length == 1) {
			LocalDate d = dateFromShowNumber(Integer.parseInt(args[0]));
			System.out.println(d);
		} else {
			int start = 1, end = MAX_S*195;
			IntPredicate ip = i -> i != showNumberFromDate(dateFromShowNumber(i));
			
			if(IntStream.rangeClosed(start, end).noneMatch(ip))
				System.out.println("Shows " + start + " to " + end + " all good.");
			else
				IntStream.rangeClosed(start, end).filter(ip).forEachOrdered(i -> System.out.println(i + " not working"));
		}
		
		//System.out.println(SEASON_START_BY_NUMBER);
	}
}