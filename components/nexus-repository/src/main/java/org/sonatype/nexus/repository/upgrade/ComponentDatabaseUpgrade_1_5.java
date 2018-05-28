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

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.orient.DatabaseUpgradeSupport;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;

import com.orientechnologies.orient.core.collate.OCaseInsensitiveCollate;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexManager;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLQuery;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_CI_NAME;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * Upgrade step to introduce ci_name field on component for faster queries.
 *
 * @since 3.4
 */
@Named
@Singleton
@Upgrades(model = DatabaseInstanceNames.COMPONENT, from = "1.4", to = "1.5")
public class ComponentDatabaseUpgrade_1_5
    extends DatabaseUpgradeSupport
{
  private static final String COMPONENT_CLASS = new OClassNameBuilder()
      .type("component")
      .build();

  private static final String I_CI_NAME_CASE_INSENSITIVE = new OIndexNameBuilder()
      .type(COMPONENT_CLASS)
      .property(P_CI_NAME)
      .caseInsensitive()
      .build();

  private static final String I_NAME_CASE_INSENSITIVE = new OIndexNameBuilder()
      .type(COMPONENT_CLASS)
      .property(P_NAME)
      .caseInsensitive()
      .build();

  private static final int BATCH_SIZE = 500;

  private static final String SELECT_COMPONENT_BATCH_SQL = String
      .format("select from component where ci_name is null limit %d", BATCH_SIZE);

  private OSQLQuery<ODocument> SELECT_COMPONENT_BATCH = new OSQLSynchQuery<>(SELECT_COMPONENT_BATCH_SQL);

  private final Provider<DatabaseInstance> componentDatabaseInstance;

  @Inject
  public ComponentDatabaseUpgrade_1_5(
      @Named(DatabaseInstanceNames.COMPONENT) final Provider<DatabaseInstance> componentDatabaseInstance)
  {
    this.componentDatabaseInstance = checkNotNull(componentDatabaseInstance);
  }

  @Override
  public void apply() throws Exception {
    withDatabaseAndClass(componentDatabaseInstance, COMPONENT_CLASS, (db, type) -> {
      // note that the ci_name field is created as not-mandatory and nullable because we still have to populate its
      // contents as part of the upgrade, once populated we go back and change the field to mandatory and non-null
      createCaseInsensitiveNameField(type);
      createCaseInsensitiveNameCaseInsensitiveIndex(db, type);
      populateCaseInsensitiveNameField(db);
      modifyCaseInsensitiveNameField(type);
      deleteNameCaseInsensitiveIndex(db);
    });
  }

  private void createCaseInsensitiveNameField(final OClass type) {
    log.info("Creating case-insensitive name field on component");
    if (!type.existsProperty(P_CI_NAME)) {
      type.createProperty(P_CI_NAME, OType.STRING)
          .setCollate(new OCaseInsensitiveCollate())
          .setMandatory(false)
          .setNotNull(false);
    }
  }

  private void populateCaseInsensitiveNameField(final ODatabaseDocumentTx db) {
    log.info("Populating case-insensitive name field on component, this could be a long-running operation");
    try {
      boolean hasRecords = true;
      while (hasRecords) {
        hasRecords = populateCaseInsensitiveNameFieldBatch(db);
      }
    }
    finally {
      db.rollback();
    }
  }

  private boolean populateCaseInsensitiveNameFieldBatch(final ODatabaseDocumentTx db) {
    log.trace("Processing batch of {} component records...", BATCH_SIZE);
    db.begin();
    List<ODocument> components = db.query(SELECT_COMPONENT_BATCH);
    if (components.isEmpty()) {
      return false;
    }
    for (ODocument component : components) {
      String name = component.field(P_NAME, String.class);
      component.field(P_CI_NAME, name.toLowerCase(Locale.ENGLISH));
      component.save();
    }
    db.commit();
    return true;
  }

  private void modifyCaseInsensitiveNameField(final OClass type) {
    log.info("Modifying case-insensitive name field on component");
    OProperty ciNameProperty = type.getProperty(P_CI_NAME);
    if (!ciNameProperty.isMandatory()) {
      ciNameProperty.setMandatory(true);
    }
    if (!ciNameProperty.isNotNull()) {
      ciNameProperty.setNotNull(true);
    }
  }

  private void createCaseInsensitiveNameCaseInsensitiveIndex(final ODatabaseDocumentTx db, final OClass type) {
    log.info("Creating case-insensitive index on case-insensitive name field on component");
    OIndexManager indexManager = db.getMetadata().getIndexManager();
    if (indexManager.getIndex(I_CI_NAME_CASE_INSENSITIVE) == null) {
      new OIndexBuilder(type, I_CI_NAME_CASE_INSENSITIVE, INDEX_TYPE.NOTUNIQUE)
          .property(P_CI_NAME, OType.STRING)
          .caseInsensitive()
          .build(db);
    }
  }

  private void deleteNameCaseInsensitiveIndex(final ODatabaseDocumentTx db) {
    log.info("Deleting old case-insensitive name index on component");
    OIndexManager indexManager = db.getMetadata().getIndexManager();
    OIndex nameCaseInsensitiveIndex = indexManager.getIndex(I_NAME_CASE_INSENSITIVE);
    if (nameCaseInsensitiveIndex != null) {
      indexManager.dropIndex(I_NAME_CASE_INSENSITIVE);
    }
  }
}
