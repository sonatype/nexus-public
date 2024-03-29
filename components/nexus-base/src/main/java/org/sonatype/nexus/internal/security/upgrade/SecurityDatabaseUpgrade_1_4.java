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
package org.sonatype.nexus.internal.security.upgrade;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.orient.DatabaseUpgradeSupport;
import org.sonatype.nexus.orient.OClassNameBuilder;

import com.orientechnologies.orient.core.sql.OCommandSQL;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Upgrade step to delete saml_users with empty id
 *
 */
@Named
@Singleton
@Upgrades(model = DatabaseInstanceNames.SECURITY, from = "1.3", to = "1.4")
public class SecurityDatabaseUpgrade_1_4 // NOSONAR
    extends DatabaseUpgradeSupport
{
  static final String DB_CLASS = new OClassNameBuilder().type("saml_user").build();

  private static final String QUERY = String
      .format("DELETE FROM %s WHERE id = ''", DB_CLASS);

  private final Provider<DatabaseInstance> securityDatabaseInstance;

  @Inject
  public SecurityDatabaseUpgrade_1_4(@Named(DatabaseInstanceNames.SECURITY) final Provider<DatabaseInstance> securityDatabaseInstance) {
    this.securityDatabaseInstance = checkNotNull(securityDatabaseInstance);
  }

  @Override
  public void apply() {
    withDatabaseAndClass(securityDatabaseInstance, DB_CLASS, (db, type) -> {
      int updates = db.command(new OCommandSQL(QUERY)).execute();
      if (updates > 0) {
        log.debug("Deleted {} saml users with empty id.", updates);
      }
    });
  }
}
