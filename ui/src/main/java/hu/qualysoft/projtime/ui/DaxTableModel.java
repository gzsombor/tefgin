package hu.qualysoft.projtime.ui;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public class DaxTableModel implements TableModel {

    private final static int START = 1;
    private final static int END = 2;
    private final static int PAUSE = 3;
    private final static int HASBOOKING = 4;
    private final static int CREATE = 5;

    private final static String[] COLUMN_NAMES = new String[] { "Dátum", "Kezdés", "Befejezés", "Szünet", "Kitöltött", "Másolás" };

    boolean block;
    LocalDate start;
    LocalDate end;

    List<TableModelListener> listeners = new ArrayList<>();
    Set<LocalDate> notFilledDates = new HashSet<>();
    Map<LocalDate, DayBookingModel> bookings = new HashMap<>();
    Set<LocalDate> selectedDates = new HashSet<>();

    private static final DateTimeFormatter LENIENT_ISO_LOCAL_TIME;
    static {
        // similar to the ISO STYLE, just accepts hours with 1 width, eg: '1:23'
        LENIENT_ISO_LOCAL_TIME = new DateTimeFormatterBuilder().appendValue(HOUR_OF_DAY, 1, 2, SignStyle.EXCEEDS_PAD).appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2).optionalStart().appendLiteral(':').appendValue(SECOND_OF_MINUTE, 2).optionalStart()
                .appendFraction(NANO_OF_SECOND, 0, 9, true).toFormatter();
    }

    @Override
    public int getRowCount() {
        return start != null && end != null ? (int) ChronoUnit.DAYS.between(start, end) + 1 : 0;
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return COLUMN_NAMES[columnIndex];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return LocalDate.class;
            case HASBOOKING:
            case CREATE:
                return Boolean.class;
            default:
                return String.class;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (block) {
            return false;
        }
        if (columnIndex == CREATE || columnIndex == START || columnIndex == END || columnIndex == PAUSE) {
            return true;
        }
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        final LocalDate row = start.plusDays(rowIndex);
        final DayBookingModel b = bookings.get(row);
        switch (columnIndex) {
            case 0:
                return row;
            case START:
                return b != null ? b.getComingTimeAsString() : "";
            case END:
                return b != null ? b.getGoingTimeAsString() : "";
            case PAUSE:
                return b != null ? b.getPauseTimeAsString() : "";
            case HASBOOKING:
                // TODO: not filled dates nem mukodik !
                return !notFilledDates.contains(row);
            case CREATE:
                return selectedDates.contains(row) && isDatePossibleSelected(row);

            default:
                return "x";
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        final LocalDate row = start.plusDays(rowIndex);
        if (columnIndex == CREATE) {
            if (Boolean.TRUE.equals(aValue)) {
                selectedDates.add(row);
            }
            if (Boolean.FALSE.equals(aValue)) {
                selectedDates.remove(row);
            }
        }
        if (aValue instanceof String) {
            if (columnIndex == START || columnIndex == END || columnIndex == PAUSE) {
                final DayBookingModel wr = getBookingWrapper(row);
                final LocalTime t = parseTime((String) aValue);
                switch (columnIndex) {
                    case START:
                        wr.setComingTime(t);
                        break;
                    case END:
                        wr.setGoingTime(t);
                        break;
                    case PAUSE:
                        wr.setPauseTime(t);
                        break;
                }
                // if we filled everything, check the upload mark
                if (wr.isDatePossibleSelected()) {
                    selectedDates.add(row);
                }
            }
        }
        fireTableChanged(rowIndex);
    }

    private LocalTime parseTime(String aValue) {
        try {
            return LocalTime.parse(aValue);
        } catch (final DateTimeParseException e) {
            // System.err.println("Error parsing :" + aValue);
            // it's ugly, but the default parser couldnt parse the 1:23 style
            // time strings.
            try {
                return LocalTime.parse("0" + aValue);
            } catch (final DateTimeParseException t) {
                return null;
            }
        }
    }

    boolean isDatePossibleSelected(LocalDate row) {
        final DayBookingModel b = bookings.get(row);
        return b != null && b.isDatePossibleSelected();
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    public void setNotFilledDates(Collection<LocalDate> dates) {
        notFilledDates.clear();
        notFilledDates.addAll(dates);
        for (final LocalDate d : dates) {
            if (isDatePossibleSelected(d)) {
                selectedDates.add(d);
            }
        }
        fireTableChanged();
    }

    public void setRange(LocalDate start, LocalDate end) {
        this.start = start;
        this.end = end;
        fireTableChanged();
    }

    public void addBooking(LocalDate date, DayBookingModel booking) {
        selectedDates.remove(date);
        bookings.put(date, booking);
        if (notFilledDates.contains(date) && booking.getComingTime() != null) {
            selectedDates.add(date);
        }
        fireTableChanged();
    }

    private synchronized DayBookingModel getBookingWrapper(LocalDate date) {
        DayBookingModel wrapper = bookings.get(date);
        if (wrapper == null) {
            wrapper = new SimpleDayBookingModel(date, null);
            bookings.put(date, wrapper);
        }
        return wrapper;
    }

    private void fireTableChanged() {
        fireTableEvent(new TableModelEvent(this));
    }

    private void fireTableChanged(int row) {
        fireTableEvent(new TableModelEvent(this, row));
    }

    private void fireTableEvent(TableModelEvent e) {
        for (final TableModelListener l : listeners) {
            l.tableChanged(e);
        }
    }

    public void setBlocking(boolean b) {
        this.block = b;
    }

    public Stream<DayBookingModel> getSelectedBookings() {
        return selectedDates.stream().map(dt -> bookings.get(dt)).filter(booking -> booking != null && booking.getComingTime() != null)
        		.filter(booking -> booking.askedTime.isAfter(start) || booking.askedTime.isEqual(start))
        		.filter(booking -> booking.askedTime.isBefore(end) || booking.askedTime.isEqual(end));
    }

}
