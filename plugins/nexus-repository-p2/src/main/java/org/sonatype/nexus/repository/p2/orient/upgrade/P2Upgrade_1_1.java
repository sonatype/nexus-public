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
package org.sonatype.nexus.repository.p2.orient.upgrade;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.DependsOn;
import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.orient.DatabaseUpgradeSupport;

import com.orientechnologies.orient.core.sql.OCommandSQL;

/**
 * Upgrade step to update {@code name} for p2 assets (remove redundant slash in start) and delete browse_node entries
 * for p2 repositories forcing them to be rebuilt by {@link org.sonatype.nexus.repository.browse.internal.RebuildBrowseNodesManager}.
 *
 * @since 3.28
 */
@Named
@Singleton
@Upgrades(model = P2Model.NAME, from = "1.0", to = "1.1")
@DependsOn(model = DatabaseInstanceNames.COMPONENT, version = "1.14", checkpoint = true)
@DependsOn(model = DatabaseInstanceNames.CONFIG, version = "1.8", checkpoint = true)
public class P2Upgrade_1_1
    extends AbstractP2Upgrade
{
  private static final String REMOVE_UNNECESSARY_SLASH_FROM_ASSET_NAME =
      "update asset set name = name.subString(1) where bucket = ? and name like '/%'";

  @Inject
  public P2Upgrade_1_1(
      @Named(DatabaseInstanceNames.CONFIG) final Provider<DatabaseInstance> configDatabaseInstance,
      @Named(DatabaseInstanceNames.COMPONENT) final Provider<DatabaseInstance> componentDatabaseInstance)
  {
    super(configDatabaseInstance, componentDatabaseInstance);
  }

  @Override
  public void apply() {
    if (hasSchemaClass(configDatabaseInstance, "repository") &&
        hasSchemaClass(componentDatabaseInstance, ASSET_CLASS_NAME)) {
      updateP2Repositories();
    }
  }

  private void updateP2Repositories() {
    List<String> p2RepositoryNames = getP2RepositoryNames();

    if (!p2RepositoryNames.isEmpty()) {
      updateP2AssetNames(p2RepositoryNames);
      deleteBrowseNodes(p2RepositoryNames);
    }
  }

  private void updateP2AssetNames(final List<String> p2RepositoryNames) {
    OCommandSQL updateAssetCommand = new OCommandSQL(REMOVE_UNNECESSARY_SLASH_FROM_ASSET_NAME);
    DatabaseUpgradeSupport.withDatabaseAndClass(componentDatabaseInstance, ASSET_CLASS_NAME,
        (db, type) -> p2RepositoryNames.forEach(repositoryName -> bucketFor(db, repositoryName)
            .ifPresent(bucket -> {
              int updates = db.command(updateAssetCommand).execute(bucket.getIdentity());
              if (updates > 0) {
                log.info("Updated {} p2 asset(s) names in repository {}: ", updates, repositoryName);
              }
            })));
  }
}
