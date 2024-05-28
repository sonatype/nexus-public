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
package org.sonatype.nexus.bootstrap.osgi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

import org.sonatype.nexus.bootstrap.internal.DirectoryHelper;

import org.osgi.framework.Version;

import com.google.common.annotations.VisibleForTesting;

import static java.lang.Boolean.parseBoolean;
import static org.sonatype.nexus.common.app.FeatureFlags.*;

public class NexusEditionPropertiesConfigurer
{
  static final String NEXUS_EDITION = "nexus-edition";

  static final String NEXUS_FULL_EDITION = "nexus-full-edition";

  static final String NEXUS_DB_FEATURE = "nexus-db-feature";

  private static final String NEXUS_EXCLUDE_FEATURES = "nexus-exclude-features";

  private static final Version ORIENT_MAX_JAVA_VERSION = new Version(11, 0, 0);

  public Properties getPropertiesFromConfiguration() throws IOException {

    Properties properties = System.getProperties();
    if (properties == null) {
      throw new IllegalStateException("Missing bootstrap configuration properties");
    }

    // Ensure required properties exist
    requireProperty(properties, "karaf.base");
    requireProperty(properties, "karaf.data");

    Path workDirPath = new File(properties.getProperty("karaf.data")).getCanonicalFile().toPath();
    DirectoryHelper.mkdir(workDirPath);

    NexusEditionFactory.selectActiveEdition(properties,workDirPath);

    selectDatastoreFeature(properties);
    selectAuthenticationFeature(properties);
    readEnvironmentVariables(properties);

    requireProperty(properties, NEXUS_EDITION);
    requireProperty(properties, NEXUS_DB_FEATURE);
    ensureHACIsDisabled();
    return properties;
  }

  private void readEnvironmentVariables(final Properties properties) {

    if (properties.getProperty(CHANGE_REPO_BLOBSTORE_TASK_ENABLED) == null) {
      properties.setProperty(CHANGE_REPO_BLOBSTORE_TASK_ENABLED,
          Boolean.toString(parseBoolean(System.getenv("CHANGE_REPO_BLOBSTORE_TASK_ENABLED"))));
    }

    if (properties.getProperty(FIREWALL_QUARANTINE_FIX_ENABLED) == null) {
      properties.setProperty(FIREWALL_QUARANTINE_FIX_ENABLED,
          Boolean.toString(parseBoolean(System.getenv("FIREWALL_QUARANTINE_FIX_ENABLED"))));
    }
  }

  private void selectDatastoreFeature(final Properties properties) {
    // datastore developer mode includes datastore user mode
    if (parseBoolean(properties.getProperty(DATASTORE_DEVELOPER, "false"))) {
      properties.setProperty(DATASTORE_ENABLED, "true");
    }

    // table search should only be turned on via clustered flag
    if (parseBoolean(properties.getProperty(DATASTORE_CLUSTERED_ENABLED,
        Optional.ofNullable(System.getenv("DATASTORE_CLUSTERED_ENABLED")).orElse("false")))) {
      // As we read the ENV variable we need to enable feature flagged classes using in-memory properties hashtable
      properties.setProperty(DATASTORE_CLUSTERED_ENABLED, "true");
      properties.setProperty(DATASTORE_ENABLED, "true");
      properties.setProperty(DATASTORE_TABLE_SEARCH, "true");
      properties.setProperty(ELASTIC_SEARCH_ENABLED, "false");
      properties.setProperty(SQL_DISTRIBUTED_CACHE, "true");

      // JWT and Blobstore Metrics should also be enabled for clustered
      properties.setProperty(JWT_ENABLED, "true");
      properties.setProperty(DATASTORE_BLOBSTORE_METRICS, "true");
    }

    // datastore search mode enables datastore user mode
    // disables elastic search mode
    // table search should only be turned on via clustered flag
    if (parseBoolean(properties.getProperty(DATASTORE_TABLE_SEARCH, "false"))) {
      properties.setProperty(DATASTORE_ENABLED, "true");
      properties.setProperty(ELASTIC_SEARCH_ENABLED, "false");
    }

    // elastic search disables datastore search mode
    if (parseBoolean(properties.getProperty(ELASTIC_SEARCH_ENABLED, "false"))) {
      properties.setProperty(DATASTORE_TABLE_SEARCH, "false");
    }

    if (parseBoolean(properties.getProperty(DATASTORE_ENABLED, "false"))) {
      // datastore mode disables orient
      properties.setProperty(ORIENT_ENABLED, "false");

      // datastore mode, but not developer mode
      if (!parseBoolean(properties.getProperty(DATASTORE_DEVELOPER, "false"))) {
        // exclude unfinished format features
        properties.setProperty(NEXUS_EXCLUDE_FEATURES,
            properties.getProperty(NEXUS_EXCLUDE_FEATURES, ""));
      }
    }

    selectDbFeature(properties);
  }

  private void selectDbFeature(final Properties properties) {
    if (parseBoolean(properties.getProperty(DATASTORE_ENABLED, "false"))) {
      properties.setProperty(NEXUS_DB_FEATURE, "nexus-datastore-mybatis");
      //enable change blobstore task for only for newdb
      properties.setProperty(CHANGE_REPO_BLOBSTORE_TASK_ENABLED, "true");
      properties.setProperty("nexus.quartz.jobstore.jdbc", "true");
    }
    else {
      ensureOrientRunningWithCorrectJavaRuntime();
      properties.setProperty(NEXUS_DB_FEATURE, "nexus-orient");
      properties.setProperty(ORIENT_ENABLED, "true");
    }
  }

  @VisibleForTesting
  void ensureOrientRunningWithCorrectJavaRuntime() {
    Version currentVersion = new Version(System.getProperty("java.version").replace("_", "."));
    boolean versionAllowed = currentVersion.getMajor() <= ORIENT_MAX_JAVA_VERSION.getMajor();
    if (!versionAllowed) {
      throw new IllegalStateException("The maximum Java version for OrientDb is Java 11. " +
          "Please check current Java version meets this requirement.");
    }
  }

  private void selectAuthenticationFeature(final Properties properties) {
    if (parseBoolean(properties.getProperty(SESSION_ENABLED, "true"))) {
      properties.setProperty(SESSION_ENABLED, "true");
    }
    if (parseBoolean(properties.getProperty(JWT_ENABLED, "false"))) {
      properties.setProperty(SESSION_ENABLED, "false");
    }
  }

  private void requireProperty(final Properties properties, final String name) {
    if (!properties.containsKey(name)) {
      throw new IllegalStateException("Missing required property: " + name);
    }
  }

  @VisibleForTesting
  void ensureHACIsDisabled() {
    if (Boolean.getBoolean("nexus.clustered") || parseBoolean(System.getenv("NEXUS_CLUSTERED"))) {
      throw new IllegalStateException(
          "High Availability Clustering (HA-C) is a legacy feature and is no longer supported");
    }
  }
}
