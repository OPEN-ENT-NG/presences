package fr.openent.presences.common.helper;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DateHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DateHelper.class);
    public static final Integer TOLERANCE = 3000;
    public static final String SQL_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String SQL_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSSSSZ";
    public static final String MONGO_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final String YEAR_MONTH_DAY = "yyyy-MM-dd";
    public static final String DAY_MONTH_YEAR = "dd/MM/yyyy";
    public static final String HOUR_MINUTES = "HH:mm";
    public static final String HOUR_MINUTES_SECONDS = "HH:mm:ss";

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

    /**
     * Get Simple format date as PostgreSQL date format
     *
     * @return Simple date format
     */
    public static SimpleDateFormat getPsqlDateSimpleDateFormat() {
        return new SimpleDateFormat(SQL_DATE_FORMAT);
    }

    public static SimpleDateFormat getMongoSimpleDateFormat() {
        return new SimpleDateFormat(MONGO_FORMAT);
    }

    public static Date parse(String date) throws ParseException {
        SimpleDateFormat ssdf = DateHelper.getPsqlSimpleDateFormat();
        SimpleDateFormat msdf = DateHelper.getMongoSimpleDateFormat();
        return date.contains("T") ? ssdf.parse(date) : msdf.parse(date);
    }

    public static Date parse(String date, String format) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.parse(date);
    }

    /**
     * Check if the first date is after the second date
     *
     * @param date1 First date
     * @param date2 Second date
     * @return Boolean that match if the first date is after the second date
     */
    public static boolean isAfter(String date1, String date2) throws ParseException {
        Date firstDate = parse(date1);
        Date secondDate = parse(date2);

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
        Date firstDate = parse(date1);
        Date secondDate = parse(date2);

        return firstDate.before(secondDate);
    }

    /**
     * Check if the first date is after or equals the second date
     *
     * @param date1 First date
     * @param date2 Second date
     * @return Boolean that match if the first date is after the second date
     */
    public static boolean isAfterOrEquals(String date1, String date2) throws ParseException {
        Date firstDate = parse(date1);
        Date secondDate = parse(date2);

        return firstDate.after(secondDate) || firstDate.equals(secondDate);

    }

    /**
     * Check if the date to compare is between start and end date
     *
     * @param startDateEventToCompareParam startDateEvent chosen to compare
     * @param endDateEventToCompareParam   endDateEvent chosen to compare
     * @param startDateParam               start date compared
     * @param endDateParam                 end date compared
     * @return Boolean that match if the date to compare is between start and end date
     */
    public static boolean isBetween(String startDateEventToCompareParam, String endDateEventToCompareParam,
                                    String startDateParam, String endDateParam) throws ParseException {
        SimpleDateFormat sdf = getPsqlSimpleDateFormat();
        Date startDateEventToCompare = sdf.parse(startDateEventToCompareParam);
        Date endDateEventToCompare = sdf.parse(endDateEventToCompareParam);
        Date slotStartDate = sdf.parse(startDateParam);
        Date slotEndDate = sdf.parse(endDateParam);

        return ((slotStartDate.after(startDateEventToCompare) || slotStartDate.equals(startDateEventToCompare)) || (startDateEventToCompare.before(slotEndDate)))
                && ((slotEndDate.before(endDateEventToCompare) || slotEndDate.equals(endDateEventToCompare)) || (endDateEventToCompare.after(slotStartDate)));
    }

    /**
     * Check if the first date is before or equals the second date
     *
     * @param date1 First date
     * @param date2 Second date
     * @return Boolean that match if the first date is before the second date
     */
    public static boolean isBeforeOrEquals(String date1, String date2) throws ParseException {
        Date firstDate = parse(date1);
        Date secondDate = parse(date2);

        return firstDate.before(secondDate) || firstDate.equals(secondDate);
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

    /**
     * Get Simple Time as string
     *
     * @param date   date to format into time
     * @param format the format wished
     * @return Simple Time format as string
     * (e.g "2019-11-05 11:00:00" (do not forget to mention the GOOD FORMAT in parameter)
     * would be "11:00:00"
     */
    public static String getTimeString(String date, String format) throws ParseException {
        Calendar cal = Calendar.getInstance();

        SimpleDateFormat sdf = new SimpleDateFormat(format);
        SimpleDateFormat sdft = new SimpleDateFormat(HOUR_MINUTES_SECONDS);

        cal.setTime(sdf.parse(date));
        return sdft.format(cal.getTime());
    }

    /**
     * Get list of dates between two dates
     *
     * @param startDate startDate
     * @param endDate   endDate
     * @return list of dates
     */
    public static List<LocalDate> getDatesBetweenTwoDates(String startDate, String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        List<LocalDate> totalDates = new ArrayList<>();

        while (!start.isAfter(end)) {
            totalDates.add(start);
            start = start.plusDays(1);
        }

        return totalDates;
    }

    /**
     * Return given date month number
     *
     * @param date date
     * @return Month number
     */
    public static int getMonthNumber(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getMonthValue();
    }

    /**
     * Return given date year value
     *
     * @param date date
     * @return Year value
     */
    public static int getYear(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getYear();
    }

    /**
     * Return date given value
     *
     * @param date  date
     * @param value Value you want. Use Calendar types to get value
     * @return value
     */
    public static int getValue(Date date, int value) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(value);
    }

    /**
     * Add to the date the number of specified value
     *
     * @param date   date you want to update
     * @param value  value. Use Calendar types
     * @param number number you want to "add"
     * @return new date updated with the new value
     */
    public static Date add(Date date, int value, int number) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(value, number);
        return cal.getTime();
    }

    public static int getDayOfMonthNumber(String date) throws ParseException {
        Calendar cal = Calendar.getInstance();
        cal.setTime(getPsqlSimpleDateFormat().parse(date));
        return cal.get(Calendar.DAY_OF_MONTH);
    }

    private static String getDayOfMonth(Date date, int dayOfMonth) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        return new SimpleDateFormat("yyyy-MM-dd'T'").format(cal.getTime());
    }

    /**
     * Get last day of month as a PostgreSQL format string
     *
     * @param date Month date
     * @return Last day of month as a PostgreSQL format string
     */
    public static String getLastDayOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return getDayOfMonth(date, cal.getActualMaximum(Calendar.DAY_OF_MONTH)) + "23:59:59";
    }

    /**
     * Get first day of month as a PostgreSQL format string
     *
     * @param date Month date
     * @return First day of month as a PostgreSQL format string
     */
    public static String getFirstDayOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return getDayOfMonth(date, cal.getActualMinimum(Calendar.DAY_OF_MONTH)) + "00:00:00";
    }

    /**
     * Get Day of Week
     *
     * @param date date chosen to get the day of week
     * @return day of week (e.g 2019-10-26 would return 6)
     */
    public static int getDayOfWeek(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        if ((calendar.get(Calendar.DAY_OF_WEEK) - 1) == 0) {
            return 7;
        }
        return calendar.get(Calendar.DAY_OF_WEEK) - 1;
    }

    public static String getCurrentDayWithHours() {
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(date);
    }

    public static String getYesterday() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        Date date = cal.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }

}
