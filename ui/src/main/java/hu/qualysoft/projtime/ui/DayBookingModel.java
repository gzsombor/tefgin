package hu.qualysoft.projtime.ui;

import hu.qualysoft.tefgin.base.HourMinute;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoField;

public abstract class DayBookingModel implements Comparable<DayBookingModel> {

    protected final LocalDate askedTime;
    protected LocalTime comingTime;
    protected LocalTime goingTime;
    protected LocalTime pauseTime;

    public DayBookingModel(LocalDate askedTime) {
        this.askedTime = askedTime;
    }

    public abstract int getNumberOfTimeRanges();

    protected abstract LocalTime getGoingTimeImpl(int idx);

    protected abstract LocalTime getComingTimeImpl(int idx);

    protected abstract LocalTime getPauseTimeImpl();

    public LocalDate getAskedTime() {
        return askedTime;
    }

    public void setComingTime(LocalTime comingTime) {
        this.comingTime = comingTime;
    }

    public void setGoingTime(LocalTime goingTime) {
        this.goingTime = goingTime;
    }

    public void setPauseTime(LocalTime pauseTime) {
        this.pauseTime = pauseTime;
    }

    public String getComingTimeAsString() {
        return pickTimeAsString(comingTime, getComingTime(0));
    }

    public String getGoingTimeAsString() {
        return pickTimeAsString(goingTime, getGoingTime(0));
    }

    public String getPauseTimeAsString() {
        return pickTimeAsString(pauseTime, getPauseTime());
    }

    public LocalTime getGoingTime() {
        return getGoingTime(0);
    }

    public LocalTime getGoingTime(int idx) {
        if (idx == 0 && goingTime != null) {
            return goingTime;
        }
        return getGoingTimeImpl(idx);
    }

    public LocalTime getComingTime() {
        return getComingTime(0);
    }

    public LocalTime getComingTime(int idx) {
        if (idx == 0 && comingTime != null) {
            return comingTime;
        }
        return getComingTimeImpl(idx);
    }

    public LocalTime getPauseTime() {
        if (pauseTime != null) {
            return pauseTime;
        }
        return getPauseTimeImpl();
    }

    public boolean isDatePossibleSelected() {
        return getComingTime(0) != null && getGoingTime(0) != null;
    }

    private String pickTimeAsString(LocalTime first, LocalTime second) {
        final LocalTime t = first != null ? first : second;
        return t != null ? t.toString() : "-";
    }

    @Override
    public int compareTo(DayBookingModel o) {
        if (askedTime == null) {
            return -1;
        } else {
            final LocalDate otherAskedTime = o.askedTime;
            if (otherAskedTime == null) {
                return 1;
            } else {
                return askedTime.compareTo(otherAskedTime);
            }
        }
    }

    public HourMinute getWorkingTime() {
        long minutes = 0;
        for (int i = 0; i < getNumberOfTimeRanges(); i++) {
            final LocalTime coming = getComingTime(i);
            final LocalTime going = getGoingTime(i);
            if (going != null && coming != null) {
                final long goingMinute = going.getLong(ChronoField.MINUTE_OF_DAY);
                final long comingMinute = coming.getLong(ChronoField.MINUTE_OF_DAY);
                minutes += goingMinute - comingMinute;
            }
        }
        final LocalTime pauseTime = getPauseTime();
        if (pauseTime != null) {
            minutes -= pauseTime.getLong(ChronoField.MINUTE_OF_DAY);
        }
        return new HourMinute(minutes);
    }

}