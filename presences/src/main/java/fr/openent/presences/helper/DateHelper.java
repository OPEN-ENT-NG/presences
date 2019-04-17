package fr.openent.presences.helper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateHelper {

    //TODO Add unit test

    public static final Integer TOLERANCE = 3000;
    public static final String SQL_FORMAT = "yyyy-MM-d'T'HH:mm:ss";
    public static final String MONGO_FORMAT = "yyyy-MM-d HH:mm:ss";

    /**
     * Return time difference between 2 dates.
     *
     * @param date1 First date
     * @param date2 Second date
     * @return Difference time
     * @throws ParseException
     */
    public static long getTimeDiff(String date1, String date2) throws ParseException {
        SimpleDateFormat ssdf = new SimpleDateFormat(SQL_FORMAT);
        SimpleDateFormat msdf = new SimpleDateFormat(MONGO_FORMAT);
        Date firstDate = date1.contains("T") ? ssdf.parse(date1) : msdf.parse(date1);
        Date secondDate = date2.contains("T") ? ssdf.parse(date2) : msdf.parse(date2);

        return secondDate.getTime() - firstDate.getTime();
    }

    /**
     * Return time difference between 2 dates as an absolute value
     *
     * @param date1 First date
     * @param date2 Second Date
     * @return Difference time
     * @throws ParseException
     */
    public static long getAbsTimeDiff(String date1, String date2) throws ParseException {
        return Math.abs(getTimeDiff(date1, date2));
    }

    /**
     * Get Simple format date as PostgreSQL timestamp without timezone format
     *
     * @return Simple date format
     */
    public static SimpleDateFormat getPsqlSimpleDateFormat() {
        return new SimpleDateFormat(SQL_FORMAT);
    }

    /**
     * Check if the first date is after the second date
     *
     * @param date1 First date
     * @param date2 Second date
     * @return Boolean that match if the first date is after the second date
     */
    public static boolean isAfter(String date1, String date2) throws ParseException {
        SimpleDateFormat sdf = getPsqlSimpleDateFormat();
        Date firstDate = sdf.parse(date1);
        Date secondDate = sdf.parse(date2);

        return firstDate.after(secondDate);
    }

    /**
     * Check if the first date is before the second date
     *
     * @param date1 First date
     * @param date2 Second date
     * @return Boolean that match if the first date is before the second date
     */
    public static boolean isBefore(String date1, String date2) throws ParseException {
        SimpleDateFormat sdf = getPsqlSimpleDateFormat();
        Date firstDate = sdf.parse(date1);
        Date secondDate = sdf.parse(date2);

        return firstDate.before(secondDate);
    }
}
