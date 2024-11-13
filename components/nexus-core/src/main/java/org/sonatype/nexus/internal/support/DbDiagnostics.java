/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.internal.support;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.inject.Inject;

import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.DataStoreManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toMap;

/**
 * Adds Db diagnostic info to support zip
 *
 */
public class DbDiagnostics
{
  private static final Logger log = LoggerFactory.getLogger(DbDiagnostics.class);
  private final ApplicationDirectories directories;
  private final DataStoreManager dataStoreManager;
  private static final String DATABASE_NAME = "Database Name: ";
  private static final String DATABASE_VERSION = "Database Version: ";
  private static final String DATABASE_SIZE = "Database Size (Bytes): ";
  private static final String MICROSECONDS = " microseconds";



  @Inject
  public DbDiagnostics(ApplicationDirectories directories, final DataStoreManager dataStoreManager){
    this.directories = checkNotNull(directories);
    this.dataStoreManager = checkNotNull(dataStoreManager);
  }

  private Stream<Entry<String, Object>> dataStoreHelper(final DataStore<?> dataStore) {
    String databaseProductName = "";
    String databaseProductVersion = "";
    String h2DBPath = "";

    long databaseSize = 0;
    StringBuilder latencySB = new StringBuilder();
    StringBuilder dbSettingsSB = new StringBuilder();

    try (Connection connection = dataStore.getDataSource().getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();
      databaseProductName = metaData.getDatabaseProductName();
      databaseProductVersion = metaData.getDatabaseMajorVersion() + "." + metaData.getDatabaseMinorVersion();

      if((databaseProductName).equalsIgnoreCase("H2")){
        final File h2Db = new File(getH2DB().toString());
        h2DBPath = h2Db.getPath();
        databaseSize = h2Db.length();

        getH2Settings(connection).forEach((name, value) -> dbSettingsSB.append(name).append(": ").append(value).append("\n"));
      }else{
        getPostgresSettings(connection).forEach((name, value) -> dbSettingsSB.append(name).append(": ").append(value).append("\n"));
      }
      latencySB = getLatencyInformation(dataStore);
    }
    catch (SQLException e) {
      throw new RuntimeException("Failed to connect to the database", e);
    }

    return Stream.of(new SimpleEntry<>(DATABASE_NAME, databaseProductName),
        new SimpleEntry<>(DATABASE_VERSION, databaseProductVersion),
        new SimpleEntry<>(DATABASE_SIZE, databaseSize),
        new SimpleEntry<>("Latency", latencySB),
        new SimpleEntry<>("H2DB PATH: ", h2DBPath),
        new SimpleEntry<>("DB SETTINGS: ", dbSettingsSB));
  }

  public Map<String, Object> metricsByDbType() {
    return StreamSupport.stream(dataStoreManager.browse().spliterator(), false)
        .flatMap(this::dataStoreHelper)
        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public String getDbFileInfo() {
    log.trace("Getting DB file info");
    Map<String, Object> metricsByDbType = metricsByDbType();
    final StringBuilder dbInfo = new StringBuilder();

    dbInfo.append("-- Database Diagnostics --\n");
    dbInfo.append(DATABASE_NAME).append(metricsByDbType.get(DATABASE_NAME)).append("\n");
    dbInfo.append(DATABASE_VERSION).append(metricsByDbType.get(DATABASE_VERSION)).append("\n");

    if(metricsByDbType.get(DATABASE_NAME).equals("H2")){
      dbInfo.append(DATABASE_SIZE).append(metricsByDbType.get(DATABASE_SIZE)).append("\n");
      dbInfo.append("H2DB Path: ").append(metricsByDbType.get("H2DB PATH: ")).append("\n");
    }

    dbInfo.append(metricsByDbType.get("Latency"));
    dbInfo.append("-- Database Settings --").append("\n");
    dbInfo.append(metricsByDbType.get("DB SETTINGS: "));

    return dbInfo.toString();
  }

  public Path getH2DB(){
    Path dbPath = directories.getWorkDirectory("db").toPath();
    Path h2Db = dbPath.resolve("nexus.mv.db");

    if (Files.exists(h2Db)){
      return h2Db;
    } else{
      return null;
    }
  }

  public StringBuilder getLatencyInformation(final DataStore<?> dataStore) throws SQLException {
    StringBuilder sb = new StringBuilder();

    try (Connection connection = dataStore.getDataSource().getConnection()){
      long latencyMinimum = Long.MAX_VALUE;
      long latencyMaximum = Long.MIN_VALUE;
      long latencyCumulative = 0;

      // Ping database 5 times, find out the minimum, maximum and average latency
      int tryCount = 5;
      for (int i = 0; i < tryCount; i++) {
        long start = System.nanoTime();
        connection.isValid(/* timeout in seconds */ 3);
        long latency = (System.nanoTime() - start) / 1000;

        latencyMinimum = Math.min(latency, latencyMinimum);
        latencyMaximum = Math.max(latency, latencyMaximum);
        latencyCumulative = latencyCumulative + latency;
      }
      long averageLatency = latencyCumulative / tryCount;

      // Add the information
      sb.append("-- Latency Information --\n");
      sb.append("Minimum: ").append(latencyMinimum).append(MICROSECONDS).append("\n");
      sb.append("Average: ").append(averageLatency).append(MICROSECONDS).append("\n");
      sb.append("Maximum: ").append(latencyMaximum).append(MICROSECONDS).append("\n");
    }
    catch (SQLException e) {
      throw new SQLException("Failed to get database latency info.", e);
    }

    return sb;
  }

  private static SortedMap<String, String> getPostgresSettings(Connection connection) throws SQLException {
    SortedMap<String, String> postgresSettingsMap = new TreeMap<>();

      String query = "SHOW ALL";
      try (Statement stmt = connection.createStatement()){
        ResultSet rs = stmt.executeQuery(query);
        while (rs.next()){
          String name = rs.getString(1);
          String value = rs.getString(2);
          postgresSettingsMap.put(name, value);
        }
      }
      catch (SQLException e) {
        throw new SQLException("Failed execute SHOW ALL query", e);
      }

    return postgresSettingsMap;
  }

  private static SortedMap<String, String> getH2Settings(Connection connection) throws SQLException {
    SortedMap<String, String> h2SettingsMap = new TreeMap<>();

    try (
        PreparedStatement stmt =
             connection.prepareStatement("SELECT SETTING_NAME, SETTING_VALUE FROM INFORMATION_SCHEMA.SETTINGS");
         ResultSet rs = stmt.executeQuery()){
      while (rs.next()){
        String name = rs.getString(1);
        String value = rs.getString(2);
        h2SettingsMap.put(name, value);
      }
    }
    catch (SQLException e) {
      throw new SQLException("Failed to execute query, SELECT SETTING_NAME, SETTING_VALUE FROM INFORMATION_SCHEMA.SETTINGS", e);
    }

    return h2SettingsMap;
  }
  
}