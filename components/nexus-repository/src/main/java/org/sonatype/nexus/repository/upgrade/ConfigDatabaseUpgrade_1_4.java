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
package org.sonatype.nexus.repository.upgrade;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.orient.DatabaseUpgradeSupport;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;

import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.orientechnologies.orient.core.collate.OCaseInsensitiveCollate;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexManager;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.sonatype.nexus.orient.DatabaseInstanceNames.CONFIG;

/**
 * Migrates the contents of etc/healthcheck.properties to the config database.
 *
 * @since 3.6.1
 */
@Named
@Singleton
@Upgrades(model = DatabaseInstanceNames.CONFIG, from = "1.3", to = "1.4")
public class ConfigDatabaseUpgrade_1_4 // NOSONAR
    extends DatabaseUpgradeSupport
{
  public static final String MIGRATED = "1_4_migrated";

  private static final String HEALTHCHECK_PROPERTIES = "healthcheck.properties";

  /**
   * Healthcheck originally used ISO-8859-1 when writing to file.
   */
  private static final String RHC_PROPERTY_FILE_CHARSET = Charsets.ISO_8859_1.name();

  static final String C_HEALTHCHECKCONFIG = new OClassNameBuilder()
      .type("healthcheckconfig")
      .build();

  private static final String P_PROPERTY_NAME = "property_name";

  static final String P_PROPERTY_VALUE = "property_value";

  private static final String I_PROPERTY_NAME = new OIndexNameBuilder()
      .type(C_HEALTHCHECKCONFIG)
      .property(P_PROPERTY_NAME)
      .build();

  static final OSQLSynchQuery<ODocument> PROPERTY_QUERY = new OSQLSynchQuery<>(
      format("SELECT FROM %s WHERE %s = ?", C_HEALTHCHECKCONFIG, P_PROPERTY_NAME));

  private final Provider<DatabaseInstance> databaseInstance;

  private final ApplicationDirectories applicationDirectories;

  @Inject
  public ConfigDatabaseUpgrade_1_4(@Named(CONFIG) final Provider<DatabaseInstance> databaseInstance,
                                   final ApplicationDirectories applicationDirectories)
  {
    this.databaseInstance = checkNotNull(databaseInstance);
    this.applicationDirectories = checkNotNull(applicationDirectories);
  }

  @Override
  public void apply() throws Exception {
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      upgradeSchema(db);
      migrateProperties(db);
    }
  }

  private void upgradeSchema(final ODatabaseDocumentTx db) {
    log.debug("Upgrading schema for Repository Health Check configuration");
    OSchema schema = db.getMetadata().getSchema();
    OClass type = schema.getClass(C_HEALTHCHECKCONFIG);
    if (type == null) {
      type = schema.createClass(C_HEALTHCHECKCONFIG);
      log.debug("Created type {}", type);
    }

    if (type.getProperty(P_PROPERTY_NAME) == null) {
      OProperty property = type.createProperty(P_PROPERTY_NAME, OType.STRING)
          .setCollate(new OCaseInsensitiveCollate())
          .setMandatory(true)
          .setNotNull(true);
      log.debug("Created property {}", property);
    }

    if (type.getProperty(P_PROPERTY_VALUE) == null) {
      OProperty property = type.createProperty(P_PROPERTY_VALUE, OType.STRING)
          .setCollate(new OCaseInsensitiveCollate())
          .setMandatory(true)
          .setNotNull(false);
      log.debug("Created property {}", property);
    }

    OIndexManager indexManager = db.getMetadata().getIndexManager();
    if (indexManager.getIndex(I_PROPERTY_NAME) == null) {
      OIndex<?> index = type.createIndex(I_PROPERTY_NAME, INDEX_TYPE.UNIQUE, P_PROPERTY_NAME);
      log.debug("Created index {}", index);
    }

    log.debug("Created schema type {}: properties={}, indexes={}", type, type.properties(), type.getIndexes());
  }

  private void migrateProperties(final ODatabaseDocumentTx db) throws Exception {
    File propFile = new File(applicationDirectories.getWorkDirectory("etc"), HEALTHCHECK_PROPERTIES);
    if (propFile.exists()) {
      Properties properties = new Properties();
      try (Reader reader = new InputStreamReader(new FileInputStream(propFile), RHC_PROPERTY_FILE_CHARSET)) {
        properties.load(reader);
      }

      // only migrate if the MIGRATED property isn't present
      if (!Boolean.parseBoolean(properties.getProperty(MIGRATED))) {
        log.info("migrating {} properties from {} into configuration database",
            properties.size(), propFile.getAbsolutePath());

        for (String name : uniqueCIPropertyNames(properties.stringPropertyNames())) {
          final String value = properties.getProperty(name);
          ODocument config = Iterables.getFirst(db.command(PROPERTY_QUERY).execute(name),
              db.newInstance(C_HEALTHCHECKCONFIG)
                  .field(P_PROPERTY_NAME, name)
                  .field(P_PROPERTY_VALUE, value));
          if (config != null) {
            final String current = config.field(P_PROPERTY_VALUE);
            if (current == null ? value != null : !current.equals(value)){
              config.field(P_PROPERTY_VALUE, value);
            }
            config.save();
          }
        }

        // prefer to retain the file on disk for support if needed
        // presence of MIGRATED property will prevent it from being imported again
        properties.setProperty(MIGRATED, "true");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(propFile), RHC_PROPERTY_FILE_CHARSET)) {
          properties.store(writer, "completed ConfigDatabaseUpgrade_1_4 at " + new Date());
        }
      }
    }
  }

  private Set<String> uniqueCIPropertyNames(final Set<String> propertyNames) {
    Set<String> caseInsensitiveNames = propertyNames.stream()
        .map(Strings2::lower)
        .distinct()
        .collect(toSet());

    if (caseInsensitiveNames.size() == propertyNames.size()) {
      log.debug("No duplicate properties (case insensitive)");
      return propertyNames;
    }
    else {
      log.debug("Found {} duplicate properties (case insensitive)", propertyNames.size() - caseInsensitiveNames.size());
      Set<String> filteredNames = new LinkedHashSet<>();
      for (String propertyName : propertyNames) {
        String ciPropertyName = Strings2.lower(propertyName);
        if (caseInsensitiveNames.contains(ciPropertyName)) {
          filteredNames.add(propertyName);
          caseInsensitiveNames.remove(ciPropertyName);
        }
        else {
          log.warn("Ignoring duplicate property name in {}: {}", HEALTHCHECK_PROPERTIES, propertyName);
        }
      }
      return filteredNames;
    }
  }
}
