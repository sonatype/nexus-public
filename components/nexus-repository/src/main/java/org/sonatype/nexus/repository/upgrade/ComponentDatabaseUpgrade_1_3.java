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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.orient.DatabaseUpgradeSupport;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.sql.OCommandSQL;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Upgrade step to add blob_created and blob_updated fields to the asset class in the component database.
 *
 * @since 3.3
 */
@Named
@Singleton
@Upgrades(model = DatabaseInstanceNames.COMPONENT, from = "1.2", to = "1.3")
public class ComponentDatabaseUpgrade_1_3 // NOSONAR
    extends DatabaseUpgradeSupport
{
  static final String ASSET_CLASS = new OClassNameBuilder()
      .type("asset")
      .build();

  static final String P_LAST_ACCESSED = "last_accessed";

  static final String ALTER_ASSET_LAST_ACCESSED = "alter property asset.last_accessed name last_downloaded";

  private final Provider<DatabaseInstance> componentDatabaseInstance;

  @Inject
  public ComponentDatabaseUpgrade_1_3(
      @Named(DatabaseInstanceNames.COMPONENT) final Provider<DatabaseInstance> componentDatabaseInstance)
  {
    this.componentDatabaseInstance = checkNotNull(componentDatabaseInstance);
  }

  @Override
  public void apply() throws Exception {
    if (hasSchemaClass(componentDatabaseInstance, ASSET_CLASS)) {
      createAssetBlobCreatedField();
      createAssetBlobUpdatedField();
      renameAssetLastAccessedField();
    }
  }

  private void createAssetBlobCreatedField() {
    try (ODatabaseDocumentTx db = componentDatabaseInstance.get().connect()) {
      maybeCreateProperty(getAssetDbClass(db), AssetEntityAdapter.P_BLOB_CREATED, OType.DATETIME);
    }
  }

  private void createAssetBlobUpdatedField() {
    try (ODatabaseDocumentTx db = componentDatabaseInstance.get().connect()) {
      maybeCreateProperty(getAssetDbClass(db), AssetEntityAdapter.P_BLOB_UPDATED, OType.DATETIME);
    }
  }

  private void renameAssetLastAccessedField() {
    try (ODatabaseDocumentTx db = componentDatabaseInstance.get().connect()) {
      OClass assetClass = getAssetDbClass(db);
      if (assetClass.existsProperty(P_LAST_ACCESSED)) {
        db.command(new OCommandSQL(ALTER_ASSET_LAST_ACCESSED)).execute();
      }
    }
  }

  private OClass getAssetDbClass(final ODatabaseDocumentTx db) {
    OSchema schema = db.getMetadata().getSchema();
    return schema.getClass(ASSET_CLASS);
  }

  private void maybeCreateProperty(final OClass oClass, final String property, final OType oType) {
    if (!oClass.existsProperty(property)) {
      oClass.createProperty(property, oType);
    }
  }
}
