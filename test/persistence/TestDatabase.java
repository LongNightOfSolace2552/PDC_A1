package persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import main.persistence.DatabaseCreateTables;
import main.persistence.DatabaseInsertData;

/*
a throw-away Derby database for the tests: it lives only in memory (so
no RPGDB folder is touched), has the real tables and the real game data,
and is dropped again when the test is done. every test gets its own, so
tests cannot affect each other.
*/
public final class TestDatabase {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private final String name;
    private final Connection connection;

    private TestDatabase(String name, Connection connection) {
        this.name = name;
        this.connection = connection;
    }

    /* a new in-memory database with the tables created and the game data inserted. */
    public static TestDatabase create() throws SQLException {
        String name = "rpgtest" + COUNTER.incrementAndGet() + "x" + System.nanoTime();
        Connection connection = DriverManager.getConnection("jdbc:derby:memory:" + name + ";create=true");
        new DatabaseCreateTables().createTables(connection);
        new DatabaseInsertData().insertData(connection);
        return new TestDatabase(name, connection);
    }

    /* a new in-memory database with no tables at all. */
    public static TestDatabase createEmpty() throws SQLException {
        String name = "rpgempty" + COUNTER.incrementAndGet() + "x" + System.nanoTime();
        return new TestDatabase(name, DriverManager.getConnection("jdbc:derby:memory:" + name + ";create=true"));
    }

    public Connection connection() {
        return connection;
    }

    /* closes the connection and deletes the database; safe to call more than once. */
    public void drop() {
        try {
            connection.close();
        } catch (SQLException ignored) {
            /* already closed by the test */
        }
        try {
            DriverManager.getConnection("jdbc:derby:memory:" + name + ";drop=true");
        } catch (SQLException expected) {
            /* Derby reports a successful drop as an exception (SQLState 08006). */
        }
    }
}
