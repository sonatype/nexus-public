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

import com.orientechnologies.orient.core.metadata.schema.OType;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Upgrade step to fix the browse_node table's properties and indices if it was defined without them via 3.6
 *
 * @since 3.6.1
 */
@Named
@Singleton
@Upgrades(model = DatabaseInstanceNames.COMPONENT, from = "1.6", to = "1.7")
public class ComponentDatabaseUpgrade_1_7
    extends DatabaseUpgradeSupport
{
  private static final String ASSET = new OClassNameBuilder().type("asset").build();

  private static final String BROWSE_NODE = new OClassNameBuilder().type("browse_node").build();

  private final Provider<DatabaseInstance> componentDatabaseInstance;

  @Inject
  public ComponentDatabaseUpgrade_1_7(
      @Named(DatabaseInstanceNames.COMPONENT) final Provider<DatabaseInstance> componentDatabaseInstance)
  {
    this.componentDatabaseInstance = checkNotNull(componentDatabaseInstance);
  }

  @Override
  public void apply() throws Exception {
    withDatabaseAndClass(componentDatabaseInstance, BROWSE_NODE, (db, table) -> {
      db.getMetadata().getSchema().dropClass(BROWSE_NODE);
    });

    withDatabaseAndClass(componentDatabaseInstance, ASSET, (db, table) -> {
      if (!table.existsProperty(AssetEntityAdapter.P_CREATED_BY)) {
        table.createProperty(AssetEntityAdapter.P_CREATED_BY, OType.STRING);
      }
      if (!table.existsProperty(AssetEntityAdapter.P_CREATED_BY_IP)) {
        table.createProperty(AssetEntityAdapter.P_CREATED_BY_IP, OType.STRING);
      }
    });
  }
}
