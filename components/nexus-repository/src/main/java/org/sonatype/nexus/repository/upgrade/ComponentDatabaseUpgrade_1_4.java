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
import org.sonatype.nexus.orient.OIndexBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.index.OIndexManager;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_GROUP;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_VERSION;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * Upgrade step to add asset and component indices.
 *
 * @since 3.3
 */
@Named
@Singleton
@Upgrades(model = DatabaseInstanceNames.COMPONENT, from = "1.3", to = "1.4")
public class ComponentDatabaseUpgrade_1_4 // NOSONAR
    extends DatabaseUpgradeSupport
{
  public static final String COMPONENT_CLASS = new OClassNameBuilder()
      .type("component")
      .build();

  public static final String ASSET_CLASS = new OClassNameBuilder()
      .type("asset")
      .build();

  public static final String I_NAME_CASE_INSENSITIVE = new OIndexNameBuilder()
      .type(COMPONENT_CLASS)
      .property(P_NAME)
      .caseInsensitive()
      .build();

  public static final String I_COMPONENT = new OIndexNameBuilder()
      .type(ASSET_CLASS)
      .property(AssetEntityAdapter.P_COMPONENT)
      .build();

  public static final String I_ASSET_NAME = new OIndexNameBuilder()
      .type(ASSET_CLASS)
      .property(P_NAME)
      .caseInsensitive()
      .build();

  public static final String I_COMPONENT_GROUP_NAME_VERSION = new OIndexNameBuilder()
      .type(COMPONENT_CLASS)
      .property(P_GROUP)
      .property(P_NAME)
      .property(P_VERSION)
      .caseInsensitive()
      .build();

  private final Provider<DatabaseInstance> componentDatabaseInstance;

  @Inject
  public ComponentDatabaseUpgrade_1_4(
      @Named(DatabaseInstanceNames.COMPONENT) final Provider<DatabaseInstance> componentDatabaseInstance)
  {
    this.componentDatabaseInstance = checkNotNull(componentDatabaseInstance);
  }

  @Override
  public void apply() throws Exception {
    withDatabaseAndClass(componentDatabaseInstance, ASSET_CLASS, (db, type) -> {
      createComponentIndex(db, type);
      createAssetNameIdx(db, type);
    });
    withDatabaseAndClass(componentDatabaseInstance, COMPONENT_CLASS, (db, type) -> {
      createNameCaseInsensitiveIndex(db, type);
      createComponentGroupNameVersionIdx(db, type);
    });
  }

  private void createNameCaseInsensitiveIndex(final ODatabaseDocumentTx db, final OClass type) {
    OIndexManager indexManager = db.getMetadata().getIndexManager();
    if (indexManager.getIndex(I_NAME_CASE_INSENSITIVE) == null) {
      new OIndexBuilder(type, I_NAME_CASE_INSENSITIVE, INDEX_TYPE.NOTUNIQUE)
          .property(P_NAME, OType.STRING)
          .caseInsensitive()
          .build(db);
    }
  }

  private void createComponentIndex(final ODatabaseDocumentTx db, final OClass type) {
    if (db.getMetadata().getIndexManager().getIndex(I_COMPONENT) == null) {
      type.createIndex(I_COMPONENT, INDEX_TYPE.NOTUNIQUE, AssetEntityAdapter.P_COMPONENT);
    }
  }

  private void createAssetNameIdx(final ODatabaseDocumentTx db, final OClass type) {
    if (db.getMetadata().getIndexManager().getIndex(I_ASSET_NAME) == null) {
      new OIndexBuilder(type, I_ASSET_NAME, INDEX_TYPE.NOTUNIQUE)
          .property(P_NAME, OType.STRING)
          .caseInsensitive()
          .build(db);
    }
  }

  private void createComponentGroupNameVersionIdx(final ODatabaseDocumentTx db, final OClass type) {
    if (db.getMetadata().getIndexManager().getIndex(I_COMPONENT_GROUP_NAME_VERSION) == null) {
      new OIndexBuilder(type, I_COMPONENT_GROUP_NAME_VERSION, INDEX_TYPE.NOTUNIQUE)
          .property(P_GROUP, OType.STRING)
          .property(P_NAME, OType.STRING)
          .property(P_VERSION, OType.STRING)
          .caseInsensitive()
          .build(db);
    }
  }
}
