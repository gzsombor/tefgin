package hu.qualysoft.tefgin.daxclient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeConverter {

    public static LocalDateTime unmarshalDateTime(String dateTime) {
        return LocalDateTime.parse(dateTime);
    }

    public static String marshalDateTime(LocalDateTime datetime) {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(datetime);
    }

    public static LocalDate unmarshalDate(String dateTime) {
        return LocalDate.parse(dateTime);
    }

    public static String marshalDate(LocalDate datetime) {
        return DateTimeFormatter.ISO_DATE.format(datetime);
    }

}
