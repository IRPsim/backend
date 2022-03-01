package de.unileipzig.irpsim.server.connectionLeakUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.testing.jdbc.leak.ConnectionLeakException;

import de.unileipzig.irpsim.core.data.timeseries.DatabaseConnectionHandler;

/*
 * Tool zum Aufspüren von connection-leaks.
 * @author Vlad Mihalcea
 * https://vladmihalcea.com/the-best-way-to-detect-database-connection-leaks/
 */
public class ConnectionLeakUtil {

   private static final Logger LOG = LogManager.getLogger(ConnectionLeakUtil.class);
   private final IdleConnectionCounter connectionCounter;
   private int connectionLeakCount;

   public ConnectionLeakUtil() {
      this.connectionCounter = new MySQLIdleConnectionCounter();
      if (connectionCounter != null) {
         connectionLeakCount = countConnectionLeaks();
      }
   }

   public void assertNoLeaks() {
      if (connectionCounter != null) {
         int currentConnectionLeakCount = countConnectionLeaks();
         int diff = currentConnectionLeakCount - connectionLeakCount;
         if (diff > 0) {
            throw new ConnectionLeakException(
                  String.format(
                        "%d connection(s) have been leaked! Previous leak count: %d, Current leak count: %d",
                        diff,
                        connectionLeakCount,
                        currentConnectionLeakCount));
         }
      }
   }

   protected int countConnectionLeaks() {
      try (Connection connection = DatabaseConnectionHandler.getInstance().getConnection()) {
         return connectionCounter.count(connection);
      } catch (SQLException e) {
         throw new IllegalStateException(e);
      }
   }

   /*
    * Zeigt, welche Threads aktuell auf die DB zugreifen.
    */
   public static void showDbProcessList() {
      try (final Connection connection = DatabaseConnectionHandler.getInstance().getConnection();
            final Statement statement = connection.createStatement();
            final ResultSet resultSet = statement.executeQuery("SHOW PROCESSLIST")) {
         LOG.debug("DBs ProcessList: ");
         LOG.debug("---------------------------------------------------------------------------------------------------------");
         while (resultSet.next()) {
            LOG.debug("id:{}, user:{}, host:{}, db:{}, command:{}, state:{}, info:{}",
                  resultSet.getInt("id"),
                  resultSet.getString("user"),
                  resultSet.getString("host"),
                  resultSet.getString("db"),
                  resultSet.getString("command"),
                  resultSet.getString("state"),
                  resultSet.getString("info"));
         }
         LOG.debug("---------------------------------------------------------------------------------------------------------");

      } catch (SQLException e) {
         // TODO: handle exception
      }
   }

   /*
    * Zeigt Verbindungsinformationen der DB an.
    */
   public static void showConnectionInfos() {
      try (final Connection connection = DatabaseConnectionHandler.getInstance().getConnection();
            final Statement statement = connection.createStatement()) {
         LOG.debug("DB connection-info: ");
         LOG.debug("---------------------------------------------------------------------------------------------------------");
         showResult(statement, "show status like 'Con%'");
         showResult(statement, "show status like 'Threads_connected%'");
         showResult(statement, "show status like 'Max_used_connections'");
         LOG.debug("---------------------------------------------------------------------------------------------------------");

      } catch (SQLException e) {
         // TODO: handle exception
      }
   }

   private static void showResult(final Statement statement, String query) throws SQLException {
      try (final ResultSet resultSet = statement.executeQuery(query)) {
         while (resultSet.next()) {
            LOG.debug("{}: {}, ", resultSet.getString("Variable_name"), resultSet.getInt("Value"));
         }
      }
   }

}
