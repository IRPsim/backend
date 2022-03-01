package de.unileipzig.irpsim.server.connectionLeakUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.persistence.EntityTransaction;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.testing.jdbc.leak.ConnectionLeakException;
import org.junit.Test;

import de.unileipzig.irpsim.core.data.timeseries.DatabaseConnectionHandler;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.server.utils.ServerTests;

public class ConnectionLeakUtilTest extends ServerTests{

   private static final ConnectionLeakUtil connectionLeakUtil = new ConnectionLeakUtil();

   /*
    * Prüft, ob bei unnötig offenen Verbindungen eine ConnectionLeakException geworfen wird.
    */
   @Test(expected = ConnectionLeakException.class)
   public void testLeakingConnections() {

      final int alreadySleepingConnections = connectionLeakUtil.countConnectionLeaks();
      for (int i = 0; i < alreadySleepingConnections+1; i++) {
         DatabaseConnectionHandler.getInstance().getConnection();
      }
      connectionLeakUtil.assertNoLeaks();
   }

   /*
    * Prüft, ob bei Lambda-Ausdrücken in try-with-resources die connections geschlossen werden.
    */
   @Test
   public void testTryWithLambda() throws HibernateException, SQLException {
      try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {

         final int alreadySleepingConnections = connectionLeakUtil.countConnectionLeaks();
         for (int i = 0; i < alreadySleepingConnections+1; i++) {
            final Session session = em.unwrap(Session.class);
            final EntityTransaction transaction = session.beginTransaction();

            try (final Statement statement = session.doReturningWork(connection -> connection.createStatement())) {
               statement.executeUpdate("UPDATE Stammdatum sd SET vollstaendig = "
                     + "(SELECT COUNT(*) FROM Datensatz WHERE stammdatum_id = sd.id AND aktiv=true) * 100 / "
                     + "((sd.prognoseHorizont+1) * (SELECT COUNT(*) FROM SzenarioSetElement WHERE set_jahr=sd.bezugsjahr))");
            }

            try (final Statement statement = session.doReturningWork(connection -> connection.createStatement())) {

               final String updateQuery = "UPDATE Datensatz LEFT JOIN Stammdatum ON Datensatz.stammdatum_id =Stammdatum.id SET aktiv=false WHERE bezugsjahr="
                     + 1999 + " AND szenario=" + 5;

               statement.executeUpdate(updateQuery);

               final String selectQuery = "SELECT COUNT(*) FROM Datensatz LEFT JOIN Stammdatum ON Datensatz.stammdatum_id =Stammdatum.id  WHERE Stammdatum.bezugsjahr="
                     + 1999 + " AND szenario =" + 5;
               try (final ResultSet result = session.doReturningWork(connection -> connection.createStatement().executeQuery(selectQuery))) {
                  result.next();
               }
            }
            transaction.commit();
         }
      }
      connectionLeakUtil.assertNoLeaks();
   }
}
