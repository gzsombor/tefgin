package hu.qualysoft.projtime.ui;

import hu.qualysoft.tefgin.base.HourMinute;
import hu.qualysoft.tefgin.base.json.Booking;

import java.time.LocalDate;
import java.time.LocalTime;

public class SimpleDayBookingModel extends DayBookingModel {

    private Booking booking;

    public SimpleDayBookingModel(LocalDate askedTime, Booking booking) {
        super(askedTime);
        this.booking = booking;
    }

    public void setWrapped(Booking booking) {
        this.booking = booking;
    }

    @Override
    public int getNumberOfTimeRanges() {
        return booking != null ? booking.getTimeSize() : 0;
    }

    @Override
    protected LocalTime getGoingTimeImpl(int idx) {
        return booking != null ? convert(booking.getGoingTime(idx)) : null;
    }

    @Override
    protected LocalTime getComingTimeImpl(int idx) {
        return booking != null ? convert(booking.getComingTime(idx)) : null;
    }

    @Override
    protected LocalTime getPauseTimeImpl() {
        return booking != null ? convert(booking.getPauseTime()) : null;
    }

    private LocalTime convert(HourMinute hourMinute) {
        if (hourMinute != null) {
            return LocalTime.of((int) hourMinute.getHour(), (int) hourMinute.getMinute());
        }
        return null;
    }

}
