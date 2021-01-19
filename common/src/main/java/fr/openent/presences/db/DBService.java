package fr.openent.presences.db;

import fr.wseduc.mongodb.MongoDb;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.sql.Sql;

public class DBService {
    protected Sql sql;
    protected Neo4j neo4j;
    protected MongoDb mongoDb;

    public DBService() {
        this.sql = DB.getInstance().sql();
        this.neo4j = DB.getInstance().neo4j();
        this.mongoDb = DB.getInstance().mongoDb();
    }
}
