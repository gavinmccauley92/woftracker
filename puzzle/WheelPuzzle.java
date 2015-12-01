package woftracker.puzzle;

import woftracker.record.Letter;
import woftracker.record.Category;
import woftracker.stats.BarChart;
import woftracker.util.*;
import java.util.*;
import java.util.regex.Pattern;
import java.time.*;
import java.time.format.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.stream.*;
import java.util.function.BiFunction;

import static woftracker.util.FormatFactory.*;
import static woftracker.util.WheelDateFactory.*;

/** a WheelPuzzle has the actual puzzle, a category, a date, and a round (potentially "PP", a "past puzzle" - retro BR). */

public class WheelPuzzle {
	private static final BiFunction<String, Character, Integer> FREQ_OF_CHAR = 
		(s, c) -> {
			int freq = 0, pos = 0;
			while((pos = s.indexOf(c, pos) + 1) != 0)
				freq++;
			return freq;
		}
	;

	private static final Map<BarChart<Letter>, Set<WheelPuzzle>> PUZZLE_KEY = new HashMap<>(195*9*7);
	private static final Map<LocalDate, Set<WheelPuzzle>> PUZZLE_FINDER = new HashMap<>(195*9*7);
	static {
		try {
			Arrays.stream(new File("woftracker\\log").listFiles())
				.filter(f -> f.getName().matches("s\\d+_compendium.html"))
				.map(f -> { try { return new Scanner(f); } catch(FileNotFoundException e) { return null; } })
				.forEach(scan -> {
					int i = 0;
					for(String puzLine; (puzLine = scan.findWithinHorizon(
						Pattern.compile("<td align=\"center\">[A-Z'&-?!/\\. &&[\\D]]{5,}(<br />)?</td>\n?" +
										"<td align=\"center\">[\\w\\p{Punct} ]+(<br />)?</td>\n?" +
										"<td align=\"center\">\\d{1,2}/\\d{1,2}/\\d{2,4} \\(#\\d{1,4}\\)(<br />)?</td>\n?" +
										"<td align=\"center\">[\\w*'\\^]{2,3}(<br />)?</td>"), 1000000)) != null; i++) {
						puzLine = puzLine.replace("&amp;", "&");
						int startIndex = puzLine.indexOf('>') + 1;
						String puzString = puzLine.substring(startIndex, puzLine.indexOf('<', startIndex));
						BarChart<Letter> bC = new BarChart<>(EnumSet.allOf(Letter.class));
						for(Letter l : Letter.values())
							bC.addY(l, FREQ_OF_CHAR.apply(puzString, l.getChar()));
						bC.remove0s();
						
						WheelPuzzle wp = new WheelPuzzle(puzLine, puzString, bC);
						
						if(PUZZLE_KEY.containsKey(bC))
							PUZZLE_KEY.get(bC).add(wp);
						else
							PUZZLE_KEY.put(bC, new HashSet<>(Collections.singleton(wp)));
						
						if(PUZZLE_FINDER.containsKey(wp.showDate))
							PUZZLE_FINDER.get(wp.showDate).add(wp);
						else
							PUZZLE_FINDER.put(wp.showDate, new HashSet<>(Collections.singleton(wp)));
					}
					//System.out.println(i);
				});
		} catch(Exception e) {
			System.err.println("PUZZLE_KEY not correctly initialized:\n");
			e.printStackTrace();
		}
	}
	
	public static enum Round { T1, T2, R1, R2, PP, R3, T3, R4, R5, R6, TB, BR };
	
	private String puzzle;
	private BarChart<Letter> dissection;
	private Category category;
	private LocalDate showDate;
	private int showNumber;
	private Round round;
	
	protected WheelPuzzle(String htmlString, String puzzle, BarChart<Letter> dissection) {
		this.puzzle = puzzle;
		this.dissection = dissection;
		
		String[] htmlSplit = htmlString.split("/td>");	//exact characters of this split very important
		
		//0th element basically taken care of in static block above, now take care of the other three
		IntStream.rangeClosed(1, 3).forEach(i -> {
			int startIndex = htmlSplit[i].indexOf('>') + 1;
			String infoString = htmlSplit[i].substring(startIndex, htmlSplit[i].indexOf('<', startIndex));
			
			if(i == 1) {	//category
				this.category = Category.LOOKUP.get(infoString.toUpperCase());
				if(this.category == null)
					System.err.println(infoString + " did not parse into a Category (even with all uppercase)");
			} else if(i == 2) {	//date / showNumber
				String dateString = infoString.substring(0, infoString.indexOf(' '));
				this.showDate = LocalDate.parse(dateString, DateTimeFormatter.ofPattern(dateString.matches("\\d{1,2}/\\d{1,2}/\\d{2}") ? "M/d/uu" : "M/d/uuuu"));
				this.showNumber = WheelDateFactory.showNumberFromDate(showDate);
			} else {	//round
				try {
					this.round = Enum.valueOf(Round.class, infoString.replaceAll("\\p{Punct}", ""));
				} catch(IllegalArgumentException e) {
					this.round = null;
					System.err.println(infoString + " did not parse into a WheelPuzzle.Round");
				}
			}
		});
	}
	
	public static Set<WheelPuzzle> lookupBarChart(BarChart<Letter> bC) {
		return PUZZLE_KEY.get(bC);
	}
	
	public static Set<WheelPuzzle> lookupDate(LocalDate d) {
		return Optional.ofNullable(PUZZLE_FINDER.get(d)).orElse(new HashSet<WheelPuzzle>());
	}
	
	public boolean equals(Object o) {
		if(o == null || getClass() != o.getClass()) return false;
		WheelPuzzle wp = (WheelPuzzle) o;
		return puzzle.equals(wp.puzzle) && category.equals(wp.category) && showDate.equals(wp.showDate) && round.equals(wp.round);
	}
	
	public int hashCode() {
		return showNumber*100 + puzzle.hashCode();	//let's see how this goes...
	}
	
	public String getPuzzle() {
		return puzzle;
	}
	
	public Round getRound() {
		return round;
	}
	
	public BarChart<Letter> getDissection() {
		return dissection;
	}
	
	public String toSimpleString() {
		return puzzle + " (" + category + ")";
	}
	
	public String toString() {
		return puzzle + " (" + category + "), " + showDate + " (#" + showNumber + "), " + round;
	}
	
	private static void downloadFile(String url, String toFile) throws Exception {
        //this could be all done on one line, but on errors I want to see exactly what failed
		ReadableByteChannel rbc = Channels.newChannel(new URL(url).openStream());
		FileChannel fc = new FileOutputStream(toFile).getChannel();
		fc.transferFrom(rbc, 0, Long.MAX_VALUE);
    }
	
	public static void main(String[] args) throws Exception {
		/*downloadFile("https://sites.google.com/site/wheeloffortunepuzzlecompendium/home/compendium/season-26-compendium", "woftracker\\log\\s26_compendium.html");
		downloadFile("https://sites.google.com/site/wheeloffortunepuzzlecompendium/home/compendium/season-27-compendium", "woftracker\\log\\s27_compendium.html");
		downloadFile("https://sites.google.com/site/wheeloffortunepuzzlecompendium/home/compendium/season-28-compendium", "woftracker\\log\\s28_compendium.html");*/
		
		downloadFile("view-source:http://www.wheeloffortunesolutions.com/201209.asp", "woftracker\\log\\s30_sep.html");
		byte[] b = Files.readAllBytes(Paths.get("woftracker\\log\\s30_sep.html"));
		try(FileWriter fW = new FileWriter(new File("woftracker\\log\\s30_sep.html"))) {
			fW.write(new String(b, Charset.defaultCharset()).replaceAll("<.*>", ""));
		}
		/*System.out.println(PUZZLE_KEY.size());
		for(WheelPuzzle wp : PUZZLE_FINDER.get(LocalDate.of(2013, 9, 16))) {
			BarChart<Letter> bC = new BarChart<>(EnumSet.allOf(Letter.class));
			for(Letter l : Letter.values())
				bC.addY(l, FREQ_OF_CHAR.apply(wp.puzzle, l.getChar()));
			bC.remove0s();
			System.out.println(PUZZLE_KEY.get(bC) + "\n" + bC);
		}
		File f = new File("puzzle_repeats.txt");
		f.delete();
		System.setOut(new PrintStream(f));
		new LinkedList<>(PUZZLE_KEY.values()).stream().filter(l -> l.size() > 1).forEach(l -> System.out.println(formatCollection(l)));
		//System.out.println(PUZZLE_KEY.values());
		*/
	}
}