package de.unileipzig.irpsim.server.connectionLeakUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;

public class H2IdleConnectionCounter implements IdleConnectionCounter {

   public static final IdleConnectionCounter INSTANCE = new H2IdleConnectionCounter();

   @Override
   public boolean appliesTo(Class<? extends Dialect> dialect) {
      return H2Dialect.class.isAssignableFrom(dialect);
   }

   @Override
   public int count(final Connection connection) {
      try (final Statement statement = connection.createStatement()) {
         try (final ResultSet resultSet = statement.executeQuery(
               "SELECT COUNT(*) " +
                     "FROM information_schema.sessions " +
                     "WHERE statement IS NULL")) {
            while (resultSet.next()) {
               return resultSet.getInt(1);
            }
            return 0;
         }
      } catch (SQLException e) {
         throw new IllegalStateException(e);
      }
   }

}
