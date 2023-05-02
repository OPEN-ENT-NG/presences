package fr.openent.presences.core.constants;

public class MongoField {
    //Mongo
    public static final String $_ID = "$_id";
    public static final String $PROJECT = "$project";
    public static final String $IN = "$in";
    public static final String $GROUP = "$group";
    public static final String $GT = "$gt";
    public static final String $GTE = "$gte";
    public static final String $LTE = "$lte";
    public static final String $COND = "$cond";
    public static final String $ADDFIELDS = "$addFields";
    public static final String $OR = "$or";
    public static final String $MATCH = "$match";
    public static final String $SUM = "$sum";
    public static final String $SIZE = "$size";
    public static final String $MAX = "$max";
    public static final String $IFNULL = "$ifNull";
    public static final String $ = "$";
    public static final String $SET = "$set";
    public static final String $SORT = "$sort";
    public static final String $LT = "$lt";
    public static final String $DAYOFMONTH = "$dayOfMonth";
    public static final String $MONTH = "$month";
    public static final String $YEAR = "$year";
    public static final String $LIMIT = "$limit";
    public static final String $SKIP = "$skip";
    public static final String $FIRST = "$first";
    public static final String $DATETOSTRING = "$dateToString";
    public static final String $DATEFROMSTRING = "$dateFromString";


    private MongoField() {
        throw new IllegalStateException("Utility class");
    }
}
