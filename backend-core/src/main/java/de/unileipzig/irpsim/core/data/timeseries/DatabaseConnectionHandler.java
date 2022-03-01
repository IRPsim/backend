package de.unileipzig.irpsim.core.data.timeseries;

import java.io.File;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.engine.spi.SessionImplementor;

import de.unileipzig.irpsim.core.Constants;

/**
 * Bietet Funktionalitäten zur Datenbankverbindung an, d.h. zum Verwalten der Zugangsdaten, der MySQL-Java und MySQL-SQL-Pfade sowie dem Zugang über JDBC-Connections und Hibernate-Entitymanager.
 *
 * @author reichelt
 */
public class DatabaseConnectionHandler {
	private static DatabaseConnectionHandler instance;

	private static final Logger LOG = LogManager.getLogger(DatabaseConnectionHandler.class);
	private static final String MYSQL_JAVA_PATH, MYSQL_SQL_PATH;

	private String url, user, password;
	private EntityManagerFactory createEntityManagerFactory;

	static {
		try {
			final String mysqlSavePathTemp = System.getenv(Constants.IRPSIM_MYSQL_JAVAPATH) != null ? System.getenv(Constants.IRPSIM_MYSQL_JAVAPATH) : "target/import/";
			final String mysqlLoadPathTemp = System.getenv(Constants.IRPSIM_MYSQL_PATH) != null ? System.getenv(Constants.IRPSIM_MYSQL_PATH) : "/var/lib/import";
			if (!mysqlSavePathTemp.endsWith(File.separator)) {
				MYSQL_JAVA_PATH = mysqlSavePathTemp + File.separator;
			} else {
				MYSQL_JAVA_PATH = mysqlSavePathTemp;
			}
			if (!new File(MYSQL_JAVA_PATH).exists()) {
				new File(MYSQL_JAVA_PATH).mkdirs();
			}
			if (!mysqlLoadPathTemp.endsWith(File.separator)) {
				MYSQL_SQL_PATH = mysqlLoadPathTemp + File.separator;
			} else {
				MYSQL_SQL_PATH = mysqlLoadPathTemp;
			}
		} catch (final Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	private String persistenceUnitName = "irpsimpersistence";

	private DatabaseConnectionHandler() {
	   
	}
	
	/**
	 * @param persistenceUnitName
	 *            the persistenceUnitName to set
	 */
	public void setPersistenceUnitName(final String persistenceUnitName) {
		this.persistenceUnitName = persistenceUnitName;
	}

	// TODO: Warum kein EntityManagerContainer? Erzeugt neuen {@link EntityManager} für die Hibernate
	// Verbindungskonfiguration.
	/**
	 * @return Der erzeugte {@link EntityManager}
	 */
	public EntityManager getEntityManager() {
		if (createEntityManagerFactory == null) {
			LOG.info("URL: {}, User: {}", getUrl(), getUser());
			final Map<String, Object> configOverrides = new HashMap<>();
			configOverrides.put("hibernate.connection.url", getUrl());
			configOverrides.put("hibernate.connection.username", getUser());
			configOverrides.put("hibernate.connection.password", getPassword());
			// configOverrides.put("hibernate.show_sql", false);

			createEntityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnitName, configOverrides);
		}
		return createEntityManagerFactory.createEntityManager();
	}

	/**
	 * Gibt eine Datenbankverbindung zurück. Falls kein Verbindungspool vorhanden ist, wird dieser erst erstellt.
	 *
	 * @return Die Datenbankverbindung als Connection
	 */
   public Connection getConnection() {
      Connection connection = getEntityManager().unwrap(SessionImplementor.class).connection();
      if (connection == null) {
         throw new RuntimeException("Datenbankverbindung konnte nicht hergestellt werden");
      }
      return connection;
   }

	/**
	 * Liefert den Datenbankbenutzer.
	 *
	 * @return Name des Datenbankbenutzers als String
	 */
	public String getUser() {
		return user;
	}

	/**
	 * Setzt Datenbankbenutzer.
	 *
	 * @param user
	 *            Der zu setzende Nutzername
	 */
	public void setUser(final String user) {
		this.user = user;
	}

	/**
	 * Liefert das Datenbankpasswort.
	 *
	 * @return Das Datenbankpasswort als String
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Setzt Datenbankpasswort.
	 *
	 * @param password
	 *            Das zu setzende Datenbankpasswort
	 */
	public void setPassword(final String password) {
		this.password = password;
	}

	/**
	 * Liefert die Datenbank-URL.
	 *
	 * @return Die Datenbank-URL als String
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Setzt die Datenbank-URL.
	 *
	 * @param url
	 *            Die zu setzende Datenbank-URL
	 */
	public void setUrl(final String url) {
		this.url = url;
	}

	/**
	 * Schließt alle offenen Verbindungen.
	 */
//	public void closeConnections() {
//		if (createEntityManagerFactory != null) {
//			LOG.info("Schließe EntityManagerFactory");
//			createEntityManagerFactory.close();
//			createEntityManagerFactory = null;
//		}
//	}

	/**
	 * GetInstance-Methode der Singleton-Klasse.
	 *
	 * @return Die erzeugte LoadDataHandler-Instanz
	 */
	public static DatabaseConnectionHandler getInstance() {
		if (null == instance) {
			instance = new DatabaseConnectionHandler();
		}
		return instance;
	}

	/**
	 * Liefert den MySQL-Java-Pfad (früher: MySQL-Save-Path), d.h. der Pfad, unter dem Java auf CSV-Dateien für MySQL zugreift.
	 *
	 * @return Der MySQL-Java-Pfad
	 */
	public String getMysqlJavaPath() {
		return MYSQL_JAVA_PATH;
	}

	/**
	 * Liefert den MySQL-SQL-Pfad (früher: MySQL-Savepath), d.h. den Pfad, unter dem der MySQL-Server auf CSV-Dateien zugreift.
	 *
	 * @return Der MySQL-SQL-Pfad
	 */
	public String getMysqlSQLPath() {
		return MYSQL_SQL_PATH;
	}

}
