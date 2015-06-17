package hu.qualysoft.projtime.ui;

import hu.qualysoft.tefgin.base.HourMinute;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

public class TefBookingTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private final static String[] COLUMN_NAMES = new String[] { "Dátum", "Kezdés", "Befejezés", "Szünet", "Egyéb" };

    private final DateTimeFormatter format = DateTimeFormatter.ISO_DATE;

    private final List<DayBookingModel> bookings = new ArrayList<>();

    public TefBookingTableModel() {
    }

    @Override
    public int getRowCount() {
        return bookings.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < bookings.size()) {
            final DayBookingModel b = bookings.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    final LocalDate time = b.getAskedTime();
                    return time != null ? format.format(time) : "";
                case 1:
                    final LocalTime comingTime = b.getComingTime();
                    return comingTime != null ? comingTime.toString() : "";
                case 2:
                    final LocalTime goingTime = b.getGoingTime();
                    return goingTime != null ? goingTime.toString() : "";
                case 3:
                    final LocalTime pauseTime = b.getPauseTime();
                    return pauseTime != null ? pauseTime.toString() : "";
                case 4:
                    final StringBuilder str = new StringBuilder();
                    if (b.getNumberOfTimeRanges() > 1) {
                        str.append("Extra:");
                        for (int i = 1; i < b.getNumberOfTimeRanges(); i++) {
                            str.append('(').append(b.getComingTime(i)).append('-').append(b.getGoingTime(i)).append(") ");
                        }
                    }
                    return str.toString();
            }
        }
        return null;
    }

    public void clear() {
        bookings.clear();
        fireTableDataChanged();
    }

    public void add(DayBookingModel b) {
        final int idx = Collections.binarySearch(bookings, b);
        if (idx < 0) {
            bookings.add(-idx - 1, b);
            fireTableRowsInserted(-idx, -idx + 1);
        } else {
            bookings.set(idx, b);
            fireTableRowsUpdated(idx, idx);
        }
    }

    public HourMinute getSummary() {
        HourMinute result = new HourMinute(0);
        for (final DayBookingModel b : bookings) {
            result = result.operator_plus(b.getWorkingTime());
        }
        return result;
    }

}
