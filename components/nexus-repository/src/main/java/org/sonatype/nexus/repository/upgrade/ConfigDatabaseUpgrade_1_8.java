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

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.orient.DatabaseUpgradeSupport;
import org.sonatype.nexus.orient.OClassNameBuilder;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static java.util.stream.StreamSupport.stream;
import static org.sonatype.nexus.orient.DatabaseInstanceNames.CONFIG;

/**
 * Update Repositories to have multiple Cleanup Policies
 *
 * @since 3.19
 */
@Named
@Singleton
@Upgrades(model = DatabaseInstanceNames.CONFIG, from = "1.7", to = "1.8")
public class ConfigDatabaseUpgrade_1_8 // NOSONAR
    extends DatabaseUpgradeSupport
{
  private static final String REPOSITORY_CLASS_NAME = new OClassNameBuilder().type("repository").build();

  private static final String RID = "@rid";

  private static final String ATTRIBUTES_CLEANUP_POLICY_NAME = "attributes.cleanup.policyName";

  private static final String UPDATE_REPOSITORY = "UPDATE repository SET attributes.cleanup.policyName = set('%s') WHERE @rid = %s";

  private final Provider<DatabaseInstance> databaseInstance;

  @Inject
  public ConfigDatabaseUpgrade_1_8(@Named(CONFIG) final Provider<DatabaseInstance> databaseInstance)
  {
    this.databaseInstance = checkNotNull(databaseInstance);
  }

  @Override
  public void apply() {
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {

      getRepositoriesWithACleanupPolicy(db).forEach(repository -> {
        db.command(new OCommandSQL(
            format(UPDATE_REPOSITORY,
                repository.field(ATTRIBUTES_CLEANUP_POLICY_NAME),
                repository.field(RID))))
            .execute();
      });
    }
  }

  private Stream<ODocument> getRepositoriesWithACleanupPolicy(final ODatabaseDocumentTx db) {
    return stream(db.browseClass(REPOSITORY_CLASS_NAME).spliterator(), false)
        .filter(result -> nonNull(result.field(ATTRIBUTES_CLEANUP_POLICY_NAME)));
  }
}
