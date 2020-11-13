package fr.openent.presences.common.helper;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.text.ParseException;

import static junit.framework.TestCase.assertTrue;

public class DateHelperTest {

    private String MONGO_FIRST_DATE = "2019-04-16 18:15:00";
    private String MONGO_SECOND_DATE = "2019-04-16 18:15:01";
    private String POSTGRES_FIRST_DATE = "2019-04-16T18:15:00";
    private String POSTGRES_SECOND_DATE = "2019-04-16T18:15:01";
    private String THROW_DATE = "2019-04-16T18:15";

    @Test
    @DisplayName("DateHelper.getAbsTimeDiff should returns 1000 value")
    public void getAbsTimeDiff_should_returns_1000() throws ParseException {
        Long mongoDiff = DateHelper.getAbsTimeDiff(MONGO_FIRST_DATE, MONGO_SECOND_DATE);
        Long sqlDiff = DateHelper.getAbsTimeDiff(POSTGRES_FIRST_DATE, POSTGRES_SECOND_DATE);
        Long sqlMongoDiff = DateHelper.getAbsTimeDiff(POSTGRES_FIRST_DATE, MONGO_SECOND_DATE);
        assertTrue("Mongo date diff", mongoDiff.equals(1000L));
        assertTrue("PostgreSQL date diff", sqlDiff.equals(1000L));
        assertTrue("Mix PostgreSQL et MongoDB diff", sqlMongoDiff.equals(1000L));
    }

    @Test(expected = ParseException.class)
    @DisplayName("DateHelper.getAbsTimeDiff should throws ParseException")
    public void getAbsTimeDiff_should_throw_ParseException() throws ParseException {
        DateHelper.getAbsTimeDiff(THROW_DATE, MONGO_SECOND_DATE);
    }

    @Test
    @DisplayName("DateHelper.isAfter should returns true")
    public void isAfter_should_returns_true() throws ParseException {
        assertTrue(DateHelper.isAfter(POSTGRES_SECOND_DATE, POSTGRES_FIRST_DATE));
    }

    @Test(expected = ParseException.class)
    @DisplayName("DateHelper.isAfter should throws ParseException")
    public void isAfter_should_throws_ParseException() throws ParseException {
        DateHelper.isAfter(MONGO_SECOND_DATE, THROW_DATE);
    }

    @Test
    @DisplayName("DateHelper.isBefore should returns true")
    public void isBefore_should_returns_true() throws ParseException {
        assertTrue(DateHelper.isBefore(POSTGRES_FIRST_DATE, POSTGRES_SECOND_DATE));
    }

    @Test(expected = ParseException.class)
    @DisplayName("DateHelper.isBefore should throws ParseException")
    public void isBefore_should_throws_ParseException() throws ParseException {
        DateHelper.isBefore(MONGO_SECOND_DATE, THROW_DATE);
    }
}
