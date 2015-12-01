package woftracker.util;

import woftracker.MainWindow;
import woftracker.record.Category;
import woftracker.stats.GameAnalysis;
import woftracker.stats.PuzzleBoard;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.util.stream.*;
import java.util.function.*;
import java.time.*;
import java.time.temporal.*;
import java.time.format.*;
import java.io.*;

import static woftracker.util.WheelDateFactory.*;
import static woftracker.util.GameAnalysisFileVisitor.*;

/** in the same vein as Array/Arrays, Collection/Collections, some useful static methods on GameAnalyses for the entire project. */

public final class GameAnalyses {
	private GameAnalyses() {}
	
	private static Map<LocalDate, GameAnalysis> allAnalyses = new HashMap<>();
	public static void initGameCount() {
		try {
			GameAnalysisFileVisitor gafv = new GameAnalysisFileVisitor(COUNT_MODE);
			Files.walkFileTree(Paths.get("analysis"), gafv);
			try {
				MainWindow.setGAGoal(gafv.getGameCount());
			} catch(Exception e) {}
		} catch(IOException e) {
			System.err.println("File walking failed: " + e);
		}
	}
	public static boolean initAnalyses() {
		try {
			GameAnalysisFileVisitor gafv = new GameAnalysisFileVisitor(READ_MODE);
			Files.walkFileTree(Paths.get("analysis"), gafv);
			allAnalyses = gafv.getAnalyses();
			return true;
		} catch(IOException e) {
			System.err.println("File walking failed: " + e);
			return false;
		}
	}
	
	public static Set<GameAnalysis> filter(Predicate<GameAnalysis> p) {
		return allAnalyses.values().parallelStream().filter(p).collect(Collectors.toCollection(TreeSet::new));
	}
	
	public static GameAnalysis getSingleAnalysis(int showNumber) {
		return allAnalyses.get(dateFromShowNumber(showNumber));
	}
	
	public static GameAnalysis getSingleAnalysis(LocalDate date) {
		return allAnalyses.get(date);
	}
	
	public static void updateGA(GameAnalysis gA) {
		allAnalyses.put(gA.getShowDate(), gA);	//put works for both cases: overwrite old one or write new one
	}
	
	//inclusive by default
	public static Set<GameAnalysis> getMultipleAnalysesByNumberRange(int start, boolean startInclusive, int end, boolean endInclusive) {
		return allAnalyses.values().parallelStream()
			.filter(gA -> gA.getShowNumber() > (startInclusive ? start-1 : start) && gA.getShowNumber() < (endInclusive ? end+1 : end)).collect(Collectors.toCollection(TreeSet::new));
	}
	
	public static Set<GameAnalysis> getMultipleAnalysesByNumberRange(int start, int end) {
		return getMultipleAnalysesByNumberRange(start, true, end, true);
	}
	
	//inclusive by default
	public static Set<GameAnalysis> getMultipleAnalysesByDateRange(LocalDate start, boolean startInclusive, LocalDate end, boolean endInclusive) {
		return allAnalyses.values().parallelStream()
			.filter(gA -> gA.getShowDate().isAfter(startInclusive ? start.minusDays(1) : start) && gA.getShowDate().isBefore(endInclusive ? end.plusDays(1) : end))
			.collect(Collectors.toCollection(TreeSet::new));
	}
	
	public static Set<GameAnalysis> getMultipleAnalysesByDateRange(LocalDate start, LocalDate end) {
		return getMultipleAnalysesByDateRange(start, true, end, true);
	}
	
	public static JTextField generateTextField(Consumer<TreeSet<GameAnalysis>> gAsConsumer, boolean allowMGA) {
		JTextField analysisChooser = new JTextField(30);
		analysisChooser.setHorizontalAlignment(JTextField.CENTER);
		analysisChooser.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter-show-info");
		analysisChooser.getActionMap().put("enter-show-info", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				Scanner scan = new Scanner(analysisChooser.getText());
				TreeSet<GameAnalysis> gAs = new TreeSet<>();
				
				while(scan.hasNext()) {
					String token = scan.next();
					if(token.matches("[aA][lL]{2}(\\d{2}(-\\d{2})?)?")) {
						if(token.length() > 3) {
							int dashIndex = token.indexOf('-'),
								seasonStart = Integer.parseInt(dashIndex == -1 ? token.substring(3) : token.substring(3, dashIndex)),
								seasonEnd = dashIndex == -1 ? seasonStart : Integer.parseInt(token.substring(dashIndex+1));
							
							Set<Integer> seasonSet = IntStream.rangeClosed(seasonStart, seasonEnd).boxed().collect(Collectors.toSet());
							gAs.addAll(filter(gA -> seasonSet.contains(gA.getSeason())));
						} else {
							gAs.addAll(filter(gA -> true));
							break;
						}
					} else if(token.matches("\\d{4}")) {
						Optional.ofNullable(getSingleAnalysis(Integer.parseInt(token))).ifPresent(gA -> gAs.add(gA));
					} else if(allowMGA && token.matches("\\d{4}-(\\d{2}|\\d{4})")) {
						Scanner subScan = new Scanner(token);
						subScan.useDelimiter("-");
						int start = subScan.nextInt(), end = subScan.nextInt();
						gAs.addAll(getMultipleAnalysesByNumberRange(start, end < 100 ? 100*(start/100) + end : end));
					} else if(token.matches("[Ss]\\d{1,2}[Ee]\\d{1,3}")) {
						Scanner subScan = new Scanner(token);
						subScan.useDelimiter("[SsEe]");
						Optional.ofNullable(getSingleAnalysis(195*(subScan.nextInt()-1) + subScan.nextInt())).ifPresent(gA -> gAs.add(gA));
					} else if(allowMGA && token.matches("[Ss]\\d{1,2}[Ee]\\d{1,3}-[Ss]\\d{1,2}[Ee]\\d{1,3}")) {
						Scanner subScan = new Scanner(token);
						subScan.useDelimiter("([SsEe]|-[Ss])");
						gAs.addAll(getMultipleAnalysesByNumberRange(195*(subScan.nextInt()-1) + subScan.nextInt(),
																195*(subScan.nextInt()-1) + subScan.nextInt()));
					} else if(token.matches("\\d{1,2}/\\d{1,2}/\\d{2}")) {
						Optional.ofNullable(getSingleAnalysis(LocalDate.parse(token, DateTimeFormatter.ofPattern("M/d/uu")))).ifPresent(gA -> gAs.add(gA));
					} else if(allowMGA && token.matches("\\d{1,2}/\\d{1,2}/\\d{2}-\\d{1,2}/\\d{1,2}/\\d{2}")) {
						Scanner subScan = new Scanner(token);
						subScan.useDelimiter("-");
						gAs.addAll(getMultipleAnalysesByDateRange(LocalDate.parse(subScan.next(), DateTimeFormatter.ofPattern("M/d/uu")),
																	LocalDate.parse(subScan.next(), DateTimeFormatter.ofPattern("M/d/uu"))));
					}
				}
				
				gAsConsumer.accept(gAs);
			}
		});
		return analysisChooser;
	}
	
	/**
		<tr>
		<td> DAILY FLIGHTS</td>
		<td>Things </td>
		<td><span style="font-size:13.3333330154419px;line-height:21.3333320617676px">1/15/2015 (#6134)</span></td>
		<td>BR </td>
		</tr>
	*/
	public static void main(String[] args) {
		initGameCount();
		initAnalyses();
		
		try(FileWriter fW = new FileWriter("woftracker\\log\\compendium_help.txt")) {
			for(GameAnalysis gA : getMultipleAnalysesByNumberRange(6147, 6300)) {
				String showString = gA.getShowDate().format(DateTimeFormatter.ofPattern("M/d/uuuu"));
				int showNumber = gA.getShowNumber();
				
				Map<GameAnalysis.Round, PuzzleBoard> pb = gA.getPuzzles();
				Map<GameAnalysis.Round, Category> cs = gA.getCategoryMap();
				
				for(GameAnalysis.Round r : pb.keySet()) {
					fW.write("<tr>\n<td>");
					fW.write(pb.get(r).getFullPuzzle());
					fW.write("</td>\n<td>");
					fW.write(cs.get(r).getTitleCase());
					fW.write("</td>\n<td>");
					fW.write(showString + " (#" + gA.getShowNumber());
					fW.write(")</td>\n<td>");
					fW.write(r + (r == GameAnalysis.Round.R3 ? "*" : ""));
					fW.write("</td>\n</tr>\n");
				}
			}
		} catch(IOException e) {
			System.err.println(e);
		}
	}
}

