package com.geridea.trentastico.utils.time;


/*
 * Created with ♥ by Slava on 16/03/2017.
 */

public class WeekIntervalCutResult {
    private final WeekInterval firstRemaining;
    private final WeekInterval secondRemaining;
    private final WeekInterval cutInterval;

    public WeekIntervalCutResult(WeekInterval cutInterval, WeekInterval firstRemaining, WeekInterval secondRemaining) {
        this.cutInterval = cutInterval;
        this.firstRemaining = firstRemaining;
        this.secondRemaining = secondRemaining;
    }

    public WeekIntervalCutResult(WeekInterval cutInterval, WeekInterval firstRemaining) {
        this(cutInterval, firstRemaining, null);
    }

    public WeekIntervalCutResult(WeekInterval cutInterval) {
        this(cutInterval, null, null);
    }

    public WeekInterval getFirstRemaining() {
        return firstRemaining;
    }

    public WeekInterval getSecondRemaining() {
        return secondRemaining;
    }

    /**
     * @return the interval that has been cut; technically the intersection between the original
     * interval and the cut one.
     */
    public WeekInterval getCutInterval() {
        return cutInterval;
    }

    /**
     * @return false if the the entire interval has been cut (the weeks of the cut interval were a
     * subset of the weeks of the cutter), true otherwise.
     */
    public boolean hasAnyRemainingResult() {
        return firstRemaining != null || secondRemaining != null;
    }

    public boolean hasOnlyOneResult() {
        return firstRemaining != null && secondRemaining == null;
    }
}
