package se.kth.livetech.control;

import java.util.Timer;
import java.util.TimerTask;

import se.kth.livetech.contest.graphics.ICPCColors;
import se.kth.livetech.contest.model.Contest;
import se.kth.livetech.contest.model.ContestUpdateEvent;
import se.kth.livetech.contest.model.ContestUpdateListener;
import se.kth.livetech.contest.model.Run;
import se.kth.livetech.contest.model.Team;
import se.kth.livetech.contest.replay.ContestReplayer;
import se.kth.livetech.presentation.layout.ScoreboardPresentation;
import se.kth.livetech.properties.IProperty;
import se.kth.livetech.properties.PropertyListener;

public class ContestReplayControl implements PropertyListener, ContestUpdateListener {
	
	private ContestReplayer replayer;
	private ScoreboardPresentation sp;
	private IProperty propertyReplay, propertyBase;
	private int bronzeMedals, silverMedals, goldMedals, medals;
	private int resolveRow = -1;
	private int stepCounter = 0;
	private boolean showingPresentation = false;
	private boolean hasFinishedReplayer = false;
	private String state = "";
	private int replayDelay = 0;
	private int resolveProblemDelay = 0;
	private int resolveTeamDelay = 0;
	private Timer timer;
	
	public ContestReplayControl(ContestReplayer replayer, IProperty propertyBase, ScoreboardPresentation sp) {
		this.replayer = replayer;
		this.sp = sp;
		this.propertyReplay = propertyBase.get("replay");
		this.propertyBase = propertyBase;
		propertyBase.addPropertyListener(this);
	}
	
	@Override
	public void propertyChanged(IProperty changed) {		
		state = propertyReplay.get("state").getValue();
		bronzeMedals = propertyReplay.get("bronzeMedals").getIntValue();
		silverMedals = propertyReplay.get("silverMedals").getIntValue();
		goldMedals = propertyReplay.get("goldMedals").getIntValue();
		medals = bronzeMedals + silverMedals + goldMedals;
		replayDelay = propertyReplay.get("replayDelay").getIntValue();
		resolveProblemDelay = propertyReplay.get("resolveProblemDelay").getIntValue();
		resolveTeamDelay = propertyReplay.get("resolveTeamDelay").getIntValue();
		
		int freezeTime = propertyReplay.get("freezeTime").getIntValue();
		if(freezeTime>0) {
			replayer.setFreezeTime(freezeTime);
			System.out.println("Set freeze time : "+freezeTime);
		}
		int untilTime = propertyReplay.get("untilTime").getIntValue();
		if(untilTime>0) {
			replayer.setUntilTime(untilTime);
			System.out.println("Set until time: "+untilTime);
		}

		if(state.equals("pause"))
			replayer.setState(ContestReplayer.State.PAUSED);
		else if(state.equals("live"))
			replayer.setState(ContestReplayer.State.LIVE);
		else if(state.equals("replay")) {
			replayer.setIntervals(replayDelay, 0);
			replayer.setState(ContestReplayer.State.UNTIL_INTERVAL);
		}
		else if(state.equals("resolver")) {
			// Assuming no new runs will be added from now and onwards.
			
			// Init resolver and ensure all earlier runs has been processed.
			if(!hasFinishedReplayer) {
				replayer.setState(ContestReplayer.State.UNTIL_INTERVAL);
				while(replayer.processPendingState());
				while(replayer.processEarliestRun());
				hasFinishedReplayer = true;
			}
			replayer.setState(ContestReplayer.State.PAUSED);
			initResolveRank();
			// Ensure task is running.
			if(timer == null) {
				timer = new Timer();
				timer.schedule(new ResolverTask(), 0);
			}
		}
		
		int stepUntil = propertyReplay.get("presentationStep").getIntValue();
		while(stepCounter < stepUntil) {
			initResolveRank();
			step(true);
		}
	}
	
	private void highlightNext() {
		System.out.println("Highlighting row " + resolveRow);
		if(sp!=null) {
			sp.highlightRow(resolveRow);
			int runId = replayer.getHighestRankedRun();
			if(runId>=0 && resolveRow>0) {
				Run run = replayer.getContest().getRun(runId);
				Team team = replayer.getContest().getRankedTeam(resolveRow);
				if(run!=null && team!=null && team.getId()==run.getTeam()) {
					sp.highlightProblem(run.getProblem());
				}
			}
		}
	}
	
	private void showWinnerPresentation(int teamId, String award) {
		IProperty awardProperty = propertyBase.get("awards");
		awardProperty.get("team").setIntValue(teamId);
		awardProperty.set("award", award);
		propertyBase.set("mode", "award");
	}
	
	private void showScoreboard() {
		propertyBase.set("mode", "score");
	}
	
	private void showBronzeMedal(int row) {
		if(sp!=null)
			sp.setRowColor(row, ICPCColors.BRONZE);
	}
	
	private void showSilverMedal(int row) {
		if(sp!=null)
			sp.setRowColor(row, ICPCColors.SILVER);
	}
	
	private void showGoldMedal(int row) {
		if(sp!=null)
			sp.setRowColor(row, ICPCColors.GOLD);
	}

	private void initResolveRank() {
		if(resolveRow==-1) {
			Contest contest = replayer.getContest();
			resolveRow = contest.getTeams().size();
			highlightNext();
		}
	}
	
	private class ResolverTask extends TimerTask {
		public void run() {
			if(resolveRow<=medals || !state.equals("resolver")) {
				timer.cancel();
				timer = null;
				return;
			}
			System.out.println("ResolveRank " + resolveRow);
			int runId = replayer.getHighestRankedRun();
			if(runId<0) {
				timer.cancel();
				timer = null;
				return;
			}
			int stepValue = step(false);
			switch(stepValue) {
			case -1: // No processed run.
				return;
			case 0: // Team changed row.
			case 2: // Highlight moved to next row.
			case 3: // Bronze medal. Should not happen.
			case 4: // Silver medal. Should not happen.
			case 5: // Gold medal. Should not happen.
				//System.out.println("resolveTeamDelay "+resolveTeamDelay);
				timer.schedule(new ResolverTask(), resolveTeamDelay);
				break;
			case 1: // Processed run for this row.
				//System.out.println("resolveProblemDelay "+resolveProblemDelay);
				timer.schedule(new ResolverTask(), resolveProblemDelay);
				break;
			default:
				System.out.println("Unknown return code: "+stepValue);
			}
		}
	}

	private int step(boolean updateStepCounter) {
		Contest contest = replayer.getContest();
		if(updateStepCounter)
			++stepCounter;
		if(resolveRow<=0) return -1;
		int runId = replayer.getHighestRankedRun();
		Run run = null;
		if(runId>=0) run = contest.getRun(runId);
		Team team = contest.getRankedTeam(resolveRow);
		if(run !=null && run.getTeam() == team.getId()) {
			showingPresentation = false;
			// Current row has more runs.
			System.out.println("Next run on row "+resolveRow + ", run id "+run.getId());
			replayer.processProblem(run.getTeam(), run.getProblem());
			highlightNext();
			Team team2 = replayer.getContest().getRankedTeam(resolveRow);
			if(team.getId()==team2.getId()) return 1;
			return 0;
		} else if(resolveRow>medals || showingPresentation) {
			if(showingPresentation)
				showScoreboard();
			showingPresentation = false;
			// Highlight next row
			--resolveRow;
			highlightNext();
			return 2;
		} else if(resolveRow>silverMedals+goldMedals) {
			showingPresentation = true;
			System.out.println("Bronze medal to team " + team.getId() + " on row " + resolveRow);
			showWinnerPresentation(team.getId(), "Bronze");
			showBronzeMedal(resolveRow);
			return 3;
		} else if(resolveRow>goldMedals) {
			showingPresentation = true;
			System.out.println("Silver medal to team " + team.getId() + " on row " + resolveRow);
			showWinnerPresentation(team.getId(), "Silver");
			showSilverMedal(resolveRow);
			return 4;
		} else {
			showingPresentation = true;
			System.out.println("Gold medal to team " + team.getId() + " on row " + resolveRow);
			showWinnerPresentation(team.getId(), "Gold");
			showGoldMedal(resolveRow);
			return 5;
		}	
	}

	@Override
	public void contestUpdated(ContestUpdateEvent e) {
		// TODO Auto-generated method stub
		//System.out.println(e);
	}		
}