package woftracker.util;

import woftracker.MainWindow;
import woftracker.stats.GameAnalysis;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

class GameAnalysisFileVisitor extends SimpleFileVisitor<Path> {
	public static final int COUNT_MODE = 1, READ_MODE = 2;
	
	private int gameCount = 0, mode;
	private Map<LocalDate, GameAnalysis> analyses = new HashMap<>();
	
	public GameAnalysisFileVisitor(int mode) {
		this.mode = mode;
	}
	
	public FileVisitResult preVisitDirectory(Path p, BasicFileAttributes bfa) {
		return p.toString().matches("analysis(\\\\s\\d{2})?") ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
	}
	
	public FileVisitResult visitFile(Path p, BasicFileAttributes bfa) {
		if(p.toString().endsWith(".wga"))
			if(mode == COUNT_MODE)
				gameCount++;
			else {
				GameAnalysis gA = GameAnalysis.readAnalysis(p.toFile());
				if(gA != null)
					analyses.put(gA.getShowDate(), gA);
				else
					System.err.println(p + " not loaded");
				
				try {
					MainWindow.updateGAProgress();
				} catch(Exception e) {}
			}
		return FileVisitResult.CONTINUE;
	}
	
	public int getGameCount() {
		return gameCount;
	}
	
	public Map<LocalDate, GameAnalysis> getAnalyses() {
		return analyses;
	}
	
	public void setMode(int mode) {
		this.mode = mode;
	}
}