package fr.openent.presences.db;

import fr.wseduc.mongodb.MongoDb;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.sql.Sql;

public class DB {
    private Neo4j neo4j;
    private Sql sql;
    private MongoDb mongoDb;

    private DB() {
    }

    public static DB getInstance() {
        return DBHolder.instance;
    }

    public void init(Neo4j neo4j, Sql sql, MongoDb mongoDb) {
        this.neo4j = neo4j;
        this.sql = sql;
        this.mongoDb = mongoDb;
    }

    public Neo4j neo4j() {
        return this.neo4j;
    }

    public Sql sql() {
        return this.sql;
    }

    public MongoDb mongoDb() {
        return this.mongoDb;
    }

    private static class DBHolder {
        private static final DB instance = new DB();

        private DBHolder() {
        }
    }
}
