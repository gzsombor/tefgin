package hu.qualysoft.projtime.ui;

import hu.qualysoft.tefgin.base.json.PositionList;

import java.time.LocalDate;
import java.time.LocalTime;

public class PositionListBookingModel extends DayBookingModel {

    private final PositionList positionList;
    private final long sumOfAllWork;

    public PositionListBookingModel(LocalDate askedTime, PositionList positionList) {
        super(askedTime);
        this.positionList = positionList;
        long tmp = 0;
        for (int i = 0; i < positionList.size(); i++) {
            final Long duration = positionList.get(i).getDuration();
            if (duration != null) {
                tmp += duration.longValue();
            }
        }
        sumOfAllWork = tmp;
    }

    @Override
    public int getNumberOfTimeRanges() {
        return sumOfAllWork > 0 ? 1 : 0;
    }

    @Override
    protected LocalTime getGoingTimeImpl(int idx) {
        return idx == 0 && sumOfAllWork > 0 ? LocalTime.of(9, 0).plusMinutes(sumOfAllWork) : null;
    }

    @Override
    protected LocalTime getComingTimeImpl(int idx) {
        return idx == 0 && sumOfAllWork > 0 ? LocalTime.of(9, 0) : null;
    }

    @Override
    protected LocalTime getPauseTimeImpl() {
        return null;
    }

}
