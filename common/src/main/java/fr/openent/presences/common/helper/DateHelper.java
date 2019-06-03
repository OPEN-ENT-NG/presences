package fr.openent.presences.common.helper;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DateHelper.class);
    public static final Integer TOLERANCE = 3000;
    public static final String SQL_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String MONGO_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private DateHelper() {
    }

    /**
     * Return time difference between 2 dates.
     *
     * @param date1 First date
     * @param date2 Second date
     * @return Difference time
     * @throws ParseException
     */
    public static long getTimeDiff(String date1, String date2) throws ParseException {
        Date firstDate = parse(date1);
        Date secondDate = parse(date2);

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

    public static SimpleDateFormat getMongoSimpleDateFormat() {
        return new SimpleDateFormat(MONGO_FORMAT);
    }

    public static Date parse(String date) throws ParseException {
        SimpleDateFormat ssdf = DateHelper.getPsqlSimpleDateFormat();
        SimpleDateFormat msdf = DateHelper.getMongoSimpleDateFormat();
        return date.contains("T") ? ssdf.parse(date) : msdf.parse(date);
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


    /**
     * Get Simple date as string
     *
     * @param date   date to format
     * @param format the format wished
     * @return Simple date format as string
     */
    public static String getDateString(String date, String format) {
        try {
            Date parsedDate = parse(date);
            return new SimpleDateFormat(format).format(parsedDate);
        } catch (ParseException err) {
            LOGGER.error("[Common@DataHelper] Failed to parse date " + date, err);
            return date;
        }
    }

}
