package de.unileipzig.irpsim.core.testutils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

import javax.persistence.EntityTransaction;

import de.unileipzig.irpsim.core.data.simulationparameters.GdxConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.unileipzig.irpsim.core.data.simulationparameters.GdxConfiguration;
import de.unileipzig.irpsim.core.data.simulationparameters.OptimisationScenario;
import de.unileipzig.irpsim.core.data.timeseries.DatabaseConnectionHandler;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationJobPersistent;
import de.unileipzig.irpsim.core.standingdata.SzenarioSet;
import de.unileipzig.irpsim.core.standingdata.SzenarioSetElement;
import de.unileipzig.irpsim.core.standingdata.data.Datensatz;
import de.unileipzig.irpsim.core.standingdata.data.Person;
import de.unileipzig.irpsim.core.standingdata.data.Stammdatum;

/**
 * Verwaltet die Initialisierung der Testdatenbank.
 *
 * @author reichelt
 */
public final class DatabaseTestUtils {

   private static final Logger LOG = LogManager.getLogger(DatabaseTestUtils.class);

   public static final String TESTBUSINESS = "TestBusiness";
   public static final String TESTCREATOR = "TestCreator";
   public static final String TESTINVESTMENT = "TestInvestment";
   public static final String TESTAUGENMERK = "TestAugenmerk";

   /**
    * Privater Konstruktor.
    */
   private DatabaseTestUtils() {

   }

   /**
    * Konfiguriert den DatabaseConnectionHandler für die Grundverbindung zur Datenbank
    */
   public static void setupDbConnectionHandler() {
      try {
         final String user = System.getenv().get("IRPSIM_MYSQL_USER_TEST") != null ? System.getenv().get("IRPSIM_MYSQL_USER_TEST") : "root";
         final String password = System.getenv().get("IRPSIM_MYSQL_PASSWORD_TEST") != null ? System.getenv().get("IRPSIM_MYSQL_PASSWORD_TEST") : "test123";
         final String url;
         if (System.getenv().get("IRPSIM_MYSQL_URL_TEST") != null) {
            url = System.getenv().get("IRPSIM_MYSQL_URL_TEST");
         } else {
            File file = new File("url.txt");
            if (file.exists()) {
               url = getFileContent(file);
            } else {
               File candidate = new File("src/test/scripts/url.txt");
               url = getFileContent(candidate);
            }

         }
         LOG.debug("Datenbankbenutzer: {} Datenbank-URL: {}", user, url);

         DatabaseConnectionHandler.getInstance().setUrl(url);
         DatabaseConnectionHandler.getInstance().setUser(user);
         DatabaseConnectionHandler.getInstance().setPassword(password);
         DatabaseConnectionHandler.getInstance().setPersistenceUnitName("irpsimpersistence");
      } catch (final FileNotFoundException e) {
         e.printStackTrace();
      }
   }

   private static String getFileContent(File file) throws FileNotFoundException {
      try (final Scanner scanner = new Scanner(file)){
         return scanner.useDelimiter("\\Z").next();
      }
   }

   /**
    * Räumt die Datenbank auf, indem die relevanten Tabellen Stammdatum und Person geleert werden.
    */
   public static void cleanUp() {
      LOG.debug("Aufräumen");
      try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         final EntityTransaction transaction = em.getTransaction();
         transaction.begin();
         // verhindert truncate foreign key Schutz, Nicht im Produktionscode verwenden
         em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0;").executeUpdate();
         em.createNativeQuery("TRUNCATE TABLE " + OptimisationScenario.class.getSimpleName()).executeUpdate();
         em.createNativeQuery("TRUNCATE TABLE " + OptimisationJobPersistent.class.getSimpleName()).executeUpdate();
         em.createNativeQuery("TRUNCATE TABLE " + Stammdatum.class.getSimpleName()).executeUpdate();
         em.createNativeQuery("TRUNCATE TABLE " + Datensatz.class.getSimpleName()).executeUpdate();
         em.createNativeQuery("TRUNCATE TABLE " + Person.class.getSimpleName()).executeUpdate();
         em.createNativeQuery("TRUNCATE TABLE " + SzenarioSet.class.getSimpleName()).executeUpdate();
         em.createNativeQuery("TRUNCATE TABLE " + SzenarioSetElement.class.getSimpleName()).executeUpdate();
         em.createNativeQuery("TRUNCATE TABLE " + GdxConfiguration.class.getSimpleName()).executeUpdate();
         em.createNativeQuery("TRUNCATE TABLE series_data_in").executeUpdate();
         em.createNativeQuery("TRUNCATE TABLE series_data_out").executeUpdate();
         em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1;").executeUpdate();
         transaction.commit();
      }
   }

   /**
    * Setzt Parametersets für das Basismodel.
    *
    * @param source Quelldatei mit den zu speichernden Daten.
    * @return ID des Parametersatzes
    * @throws FileNotFoundException 
    */
   public static int initializeParametersetsDatabase(final File source) throws FileNotFoundException {

      try (final Connection connection = DatabaseConnectionHandler.getInstance().getConnection(); final Statement statement = connection.createStatement()) {
         statement.executeUpdate("TRUNCATE TABLE " + OptimisationScenario.class.getSimpleName());
      } catch (final SQLException e) {
         e.printStackTrace();
      }

      final OptimisationScenario spm = getScenario(source);

      try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         final EntityTransaction et = em.getTransaction();
         et.begin();
         em.persist(spm);
         et.commit();
      }
      return spm.getId();
   }

   private static OptimisationScenario getScenario(final File source) throws FileNotFoundException {
      String text = getParameterText(source);

      final String name = source.getName().split("\\.")[0];
      final OptimisationScenario spm = new OptimisationScenario();
      // spm.setId(1);
      spm.setName(name + 1);
      spm.setDeletable(true);
      spm.setData(text);
      spm.setModeldefinition(1);
      LOG.trace("Data: " + text);
      return spm;
   }

   /**
    * Liest den Text aus der Datei in einen String.
    *
    * @param source Die zu lesende Datei.
    * @return Inhalt der Datei.
    * @throws FileNotFoundException Wird geworfen falls die Datei nicht gefunden wird.
    */
   public static String getParameterText(final File source) throws FileNotFoundException {
      final Scanner scanner = new Scanner(source);
      final String content = scanner.useDelimiter("\\Z").next();
      scanner.close();
      return content;
   }

   public static JSONParametersMultimodel getParameterObject(final File source) throws FileNotFoundException {
      final String content = DatabaseTestUtils.getParameterText(source);
      final ObjectMapper om = new ObjectMapper();
      try {
         final JSONParametersMultimodel gpj = om.readValue(content, JSONParametersMultimodel.class);
         gpj.getDescription().setBusinessModelDescription(TESTBUSINESS);
         gpj.getDescription().setCreator(TESTCREATOR);
         gpj.getDescription().setInvestmentCustomerSide(TESTINVESTMENT);
         gpj.getDescription().setParameterAttention(TESTAUGENMERK);
         return gpj;
      } catch (final IOException e) {
         e.printStackTrace();
         return null;
      }
   }
}
