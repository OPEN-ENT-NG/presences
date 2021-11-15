package fr.openent.presences.common.helper;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

public class DateHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DateHelper.class);
    public static final Integer TOLERANCE = 3000;
    public static final String SQL_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String SQL_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSSSSZ";
    public static final String MONGO_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String MONGO_FORMAT_TO_STRING_YMD_HMINS = "%Y-%m-%d %H:%M:%S";
    public static final String YEAR_MONTH_DAY_HOUR_MINUTES_SECONDS = "yyyy/MM/dd HH:mm:ss";

    public static final String YEAR_MONTH_DAY = "yyyy-MM-dd";
    public static final String DAY_MONTH_YEAR = "dd/MM/yyyy";
    public static final String DAY_MONTH_YEAR_DASH = "dd-MM-yyyy";
    public static final String YEAR_MONTH = "yyyy-MM";
    public static final String SHORT_MONTH = "MMM"; // e.g "Jan"
    public static final String HOUR_MINUTES = "HH:mm";
    public static final String HOUR_MINUTES_SECONDS = "HH:mm:ss";
    public static final String SAFE_HOUR_MINUTES = "kk'h'mm";

    public static final String DEFAULT_START_TIME = "00:00:00";
    public static final String DEFAULT_END_TIME = "23:59:59";

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
     * Assign time (hh, mm, dd => from chosen format) to a given date
     *
     * @param dateString   Date to get assigned by time
     * @param timeString   Time to assign to date
     * @param timeFormat   Initial time format
     * @param wishedFormat Format string expected to be return
     * @return String corresponding to date with new time
     */
    public static String setTimeToDate(String dateString, String timeString, String timeFormat, String wishedFormat) {
        Calendar date = Calendar.getInstance();
        Calendar time = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(wishedFormat);

        try {
            if (dateString != null) date.setTime(parse(dateString));
            if (timeString != null) time.setTime(parse(timeString, timeFormat));

            time.set(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DATE));

        } catch (ParseException e) {
            LOGGER.error("[Presence@DateHelper::setTimeToDate] Error when casting date: ", e);
        }

        return sdf.format(time.getTime());
    }

    public static String setTimeToNow(String timeString, String timeFormat, String wishedFormat) {
        return setTimeToDate(null, timeString, timeFormat, wishedFormat);
    }

    /**
     * Return the number of days between 2 dates.
     *
     * @param date1 First date
     * @param date2 Second date
     * @return Difference time
     * @throws ParseException
     */
    public static long getDayDiff(String date1, String date2) throws ParseException {
        long diffInMilis = getTimeDiff(date1, date2);
        return (diffInMilis / 1000 / (60 * 60) / 24) + 1;
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

    public static Date parseDate(String dateString) {
        Date date = new Date();
        SimpleDateFormat ssdf = DateHelper.getPsqlSimpleDateFormat();
        SimpleDateFormat msdf = DateHelper.getMongoSimpleDateFormat();
        try {
            date = dateString.contains("T") ? ssdf.parse(dateString) : msdf.parse(dateString);
        } catch (ParseException e) {
            LOGGER.error("[Presence@DateHelper::parseDate] Error when casting date: ", e);
        }

        return date;
    }

    public static Date parse(String date, String format) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.parse(date);
    }

    public static Date parseDate(String dateString, String format) {
        Date date = new Date();

        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            date = sdf.parse(dateString);
        } catch (ParseException e) {
            LOGGER.error("[Presence@DateHelper::parseDate] Error when casting date: ", e);
        }

        return date;
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
     * Same that isAfter, but from a given format and without try / catch
     *
     * @param date1 First hour
     * @param date2 Second hour
     * @param format base dates format
     * @return Boolean that match if the first date is before the second date
     */
    public static boolean isDateFormatAfter(String date1, String date2, String format) {
        Date firstHour = new Date();
        Date secondHour = new Date();
        SimpleDateFormat sdft = new SimpleDateFormat(format);
        try {
            firstHour = sdft.parse(date1);
            secondHour = sdft.parse(date2);
        } catch (ParseException e) {
            LOGGER.error("[Presence@DateHelper::isHourAfter] Error when casting hour: ", e);
        }

        return firstHour.after(secondHour);
    }

    /**
     * Same that isAfter, but from a given format and without try / catch
     *
     * @param date1 First hour
     * @param date2 Second hour
     * @param format base dates format
     * @return Boolean that match if the first date is before the second date
     */
    public static boolean isHourAfterOrEqual(String date1, String date2, String format) {
        return isDateFormatAfter(date1, date2, format) || isDateFormatEqual(date1, date2, format);
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
     * Test if 2 dates are equals, but without try / catch
     *
     * @param date1 First date
     * @param date2 Second date
     * @return Boolean that match if the first date is before the second date
     */
    public static boolean isDateEqual(String date1, String date2) {
        Date firstDate = new Date();
        Date secondDate = new Date();
        try {
            firstDate = parse(date1);
            secondDate = parse(date2);
        } catch (ParseException e) {
            LOGGER.error("[Presence@DateHelper::isDateEqual] Error when casting date: ", e);
        }

        return firstDate.equals(secondDate);
    }

    /**
     * Test if 2 hours are equals from a given format, but without try / catch
     *
     * @param date1 First hour
     * @param date2 Second hour
     * @param format base dates format
     * @return Boolean that match if the first hour is before the second hour
     */
    public static boolean isDateFormatEqual(String date1, String date2, String format) {
        Date firstDate = new Date();
        Date secondDate = new Date();
        SimpleDateFormat sdft = new SimpleDateFormat(format);

        try {
            firstDate = sdft.parse(date1);
            secondDate = sdft.parse(date2);
        } catch (ParseException e) {
            LOGGER.error("[Presence@DateHelper::isHourEqual] Error when casting hour: ", e);
        }

        return firstDate.equals(secondDate);
    }

    /**
     * Same that isBefore, but without try / catch
     *
     * @param date1 First date
     * @param date2 Second date
     * @return Boolean that match if the first date is before the second date
     */
    public static boolean isDateBefore(String date1, String date2) {
        Date firstDate = new Date();
        Date secondDate = new Date();
        try {
            firstDate = parse(date1);
            secondDate = parse(date2);
        } catch (ParseException e) {
            LOGGER.error("[Presence@DateHelper::isDateBefore] Error when casting date: ", e);
        }

        return firstDate.before(secondDate);
    }

    /**
     * Same that isDateBefore, but from a given format
     *
     * @param date1 First date
     * @param date2 Second date
     * @param format base dates format
     * @return Boolean that match if the first date is before the second date
     */
    public static boolean isDateFormatBefore(String date1, String date2, String format) {
        Date firstHour = new Date();
        Date secondHour = new Date();
        SimpleDateFormat sdft = new SimpleDateFormat(format);
        try {
            firstHour = sdft.parse(date1);
            secondHour = sdft.parse(date2);
        } catch (ParseException e) {
            LOGGER.error("[Presence@DateHelper::isHourBefore] Error when casting hour: ", e);
        }

        return firstHour.before(secondHour);
    }

    public static boolean isDateBetween(String date, String startDate, String endDate) {
        boolean isBetween = false;
        try {
            isBetween = isBeforeOrEquals(date, endDate) && isAfterOrEquals(date, startDate);
        } catch (ParseException e) {
            LOGGER.error("[Presence@DateHelper::isDateBetween] Error when paring dates: ", e);
        }
        return isBetween;
    }

    /**
     * Same that isBeforeOrEquals, but without try / catch
     *
     * @param date1 First date
     * @param date2 Second date
     * @return Boolean that match if the first date is before the second date
     */
    public static boolean isDateBeforeOrEqual(String date1, String date2) {
        Date firstDate = new Date();
        Date secondDate = new Date();
        try {
            firstDate = parse(date1);
            secondDate = parse(date2);
        } catch (ParseException e) {
            LOGGER.error("[Presence@DateHelper::isDateBeforeOrEqual] Error when casting date: ", e);
        }

        return firstDate.before(secondDate) || firstDate.equals(secondDate);
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
     * Check if the date to compare is between start and end date (add format)
     *
     * @param startDateEventToCompareParam startDateEvent chosen to compare
     * @param endDateEventToCompareParam   endDateEvent chosen to compare
     * @param startDateParam               start date compared
     * @param endDateParam                 end date compared
     * @param format                       your date format (SQL_FORMAT, SQL_DATE_FORMAT, MONGO_FORMAT etc...)
     * @param secondFormat                 your second date format for your another date (SQL_FORMAT, SQL_DATE_FORMAT, MONGO_FORMAT etc...)
     * @return Boolean that match if the date to compare is between start and end date
     */
    public static boolean isBetween(String startDateEventToCompareParam, String endDateEventToCompareParam,
                                    String startDateParam, String endDateParam, String format, String secondFormat) throws ParseException {

        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date startDateEventToCompare = sdf.parse(startDateEventToCompareParam);
        Date endDateEventToCompare = sdf.parse(endDateEventToCompareParam);
        SimpleDateFormat secondSdf = new SimpleDateFormat(secondFormat);
        Date anotherStartDate = secondSdf.parse(startDateParam);
        Date anotherEndDate = secondSdf.parse(endDateParam);

        return ((anotherStartDate.after(startDateEventToCompare) || anotherStartDate.equals(startDateEventToCompare))
                || (startDateEventToCompare.before(anotherEndDate)))
                && ((anotherEndDate.before(endDateEventToCompare) || anotherEndDate.equals(endDateEventToCompare))
                || (endDateEventToCompare.after(anotherStartDate)));
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
            LOGGER.error("[Common@DateHelper::getDateString] Failed to parse date " + date, err);
            return date;
        }
    }

    /**
     * Get Simple date as string, use in case your date format is not standard
     *
     * @param date         date to format
     * @param format       the source format
     * @param wishedFormat the format wished
     * @return Simple date format as string
     */
    public static String getDateString(String date, String format, String wishedFormat) {
        try {
            Date parsedDate = parse(date, format);
            return new SimpleDateFormat(wishedFormat).format(parsedDate);
        } catch (ParseException err) {
            LOGGER.error("[Common@DateHelper::getDateString] Failed to parse date " + date, err);
            return date;
        }
    }

    public static String getDateString(String date, String format, String wishedFormat, Locale locale) {
        try {
            Date parsedDate = parse(date, format);
            return new SimpleDateFormat(wishedFormat, locale).format(parsedDate);
        } catch (ParseException err) {
            LOGGER.error("[Common@DateHelper::getDateString] Failed to parse date " + date, err);
            return date;
        }
    }

    public static String getDateString(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat();
        return sdf.format(date);
    }

    public static String getDateString(Date date, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }

    /**
     * Get Simple Time as string || fetchTimeString to avoid using Try Catch in ur own function
     *
     * @param date   date to format into time
     * @param format the format of your date
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

    public static String fetchTimeString(String date, String format) {
        try {
            Calendar cal = Calendar.getInstance();

            SimpleDateFormat sdf = new SimpleDateFormat(format);
            SimpleDateFormat sdft = new SimpleDateFormat(HOUR_MINUTES_SECONDS);

            cal.setTime(sdf.parse(date));

            return sdft.format(cal.getTime());
        } catch (ParseException err) {
            LOGGER.error("[Common@DataHelper::fetchTimeString] Failed to parse date " + date, err);
            return date;
        }
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
        return DateHelper.getDateString(date, DateHelper.YEAR_MONTH_DAY);
    }

    public static String getCurrentDay() {
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        return DateHelper.getDateString(date, DateHelper.YEAR_MONTH_DAY);
    }

    public static String getCurrentTime() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
        return cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE);
    }

    /**
     * Fetching current date (now())
     *
     * @param format format date to format your type of start and end date
     * @return return current date with the wished format
     */
    public static String getCurrentDate(String format) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
        return sdf.format(calendar.getTime());
    }

    /**
     * Fetching current date (now())
     * <p>
     * Possibility to modify value in this calendar method
     *
     * @param format        format date to format your type of start and end date
     * @param calendarValue Calendar.HOUR, Calendar.MINUTE etc...
     * @param value         value amount on calendarValue
     * @return return       current date with the wished format
     */
    public static String getCurrentDate(String format, int calendarValue, int value) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));

        calendar.add(calendarValue, value);
        return sdf.format(calendar.getTime());
    }


    /**
     * Fetching a list of date based on two dates
     *
     * @param start_date    start_date defined (Since its localDate, it will only take 'YYYY-MM-DD' in account)
     * @param end_date      end_date defined (Since its localDate, it will only take 'YYYY-MM-DD' in account)
     * @param format        format date to format your type of start and end date
     * @param day           day chosen to fetch kind of day of week, must be "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"
     * @param isHalfDayMode if true = fetch data every 2 weeks (only if day is specified)
     * @return list of LocalDate between two dates (start and end)
     */
    public static List<LocalDate> getListOfDateBasedOnDates(String start_date, String end_date, String format, String day,
                                                            Boolean isHalfDayMode) {
        LocalDate start = LocalDate.parse(DateHelper.getDateString(start_date, format));
        LocalDate end = LocalDate.parse(DateHelper.getDateString(end_date, format));

        List<LocalDate> dateFromDayOfWeekFetched = new ArrayList<>();

        if (start.equals(end)) {
            dateFromDayOfWeekFetched.add(start);
            return dateFromDayOfWeekFetched;
        }

        // if day is specified, we add day else none
        LocalDate dayOfWeek = day.isEmpty() ? start : start.with(TemporalAdjusters.nextOrSame(DayOfWeek.valueOf(day)));
        while (dayOfWeek.isBefore(end) || dayOfWeek.isEqual(end)) {
            dateFromDayOfWeekFetched.add(dayOfWeek);
            // increment by week if we are based to fetch specified day
            if (!day.isEmpty()) {
                if (!isHalfDayMode) {
                    dayOfWeek = dayOfWeek.plusWeeks(1);
                } else {
                    dayOfWeek = dayOfWeek.plusWeeks(2);
                }
            } else {
                // increment by day if none is specified and that we want to fetch all date based on start and end
                dayOfWeek = dayOfWeek.plusDays(1);
            }
        }

        return dateFromDayOfWeekFetched;
    }

    /**
     * Calculate number of distinct months separating 2 dates (no matter the day)
     * <p>
     * (if using same month), will count as ONE
     *
     * @param startAtString start date
     * @param endAtString   end date
     * @return number of distinct months separating 2 dates
     */
    public static long distinctMonthsNumberSeparating(String startAtString, String endAtString) {
        YearMonth startAt = YearMonth.parse(DateHelper.getDateString(startAtString, DateHelper.YEAR_MONTH));
        YearMonth endAt = YearMonth.parse(DateHelper.getDateString(endAtString, DateHelper.YEAR_MONTH));

        return ChronoUnit.MONTHS.between(startAt, endAt);
    }

    /**
     * get days between two dates
     *
     * @param date1     begin date
     * @param date2     date that should be over date1
     *
     * @return number of days diff
     */
    public static long getDaysBetweenTwoDates(String date1, String date2) {
        LocalDate start = LocalDate.parse(date1);
        LocalDate end = LocalDate.parse(date2);

        return ChronoUnit.DAYS.between(start, end);
    }

}
