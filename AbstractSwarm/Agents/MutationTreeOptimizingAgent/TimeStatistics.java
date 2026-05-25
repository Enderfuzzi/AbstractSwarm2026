
public class TimeStatistics {

	
	public boolean newRun = true;
	public boolean newBestRun = false;
	public boolean lastRunCompleted = false;
	
	public int numberOfRuns = 0;
	public int numberOfCompletedRuns = 0;
	public int runsSinceCurrentBest = 0;

	public long time = 0L;
	
	public long roundTimeUnit = 0L;
	public long lowestTimeUnit = Long.MAX_VALUE;
	
	public long lastValue = 0L;
	
	public long lowestTwT = Long.MAX_VALUE;
	public long currentTwT = Long.MAX_VALUE;

	@Override
	public String toString() {
		return String.format("[TimeStatistic] Last Run time Unit %d Last run completed %b Current best time unit: %d"
				+ " Number of runs: %d Number of completed runs: %d Runs since best: %d New Run: %b", 
				roundTimeUnit, lastRunCompleted, lowestTimeUnit, numberOfRuns, numberOfCompletedRuns, runsSinceCurrentBest, newRun);
	}


}
