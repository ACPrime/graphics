package se.kth.livetech.contest.model;

/** Attrs update of a contest. */
public interface ContestUpdateEvent {
	public Contest getOldContest();

	public Attrs getUpdate();

	public Contest getNewContest();
}
