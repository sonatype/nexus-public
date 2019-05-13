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
package org.sonatype.nexus.onboarding.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.orient.DatabaseUpgradeSupport;
import org.sonatype.nexus.orient.OClassNameBuilder;

import com.google.common.annotations.VisibleForTesting;
import com.orientechnologies.orient.core.sql.OCommandSQL;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Upgrade step to mark admin user as 'changepassword' so user must change it
 *
 * @since 3.next
 */
@Named
@Singleton
@Upgrades(model = DatabaseInstanceNames.SECURITY, from = "1.1", to = "1.2")
public class SecurityDatabaseUpgrade_1_2 // NOSONAR
    extends DatabaseUpgradeSupport
{
  @VisibleForTesting
  static final String DB_CLASS = new OClassNameBuilder().type("user").build();

  @VisibleForTesting
  static final String ADMIN_PASS = "$shiro1$SHA-512$1024$NE+wqQq/TmjZMvfI7ENh/g==$V4yPw8T64UQ6GfJfxYq2hLsVrBY8D1v+bktfOxGdt4b/9BthpWPNUy/CBk6V9iA0nHpzYzJFWO8v/tZFtES8CA==";

  private static final String QUERY = String
      .format("UPDATE %s SET status = 'changepassword' where id = 'admin' and password = ?", DB_CLASS);

  private final Provider<DatabaseInstance> securityDatabaseInstance;

  @Inject
  public SecurityDatabaseUpgrade_1_2(@Named(DatabaseInstanceNames.SECURITY) final Provider<DatabaseInstance> securityDatabaseInstance) {
    this.securityDatabaseInstance = checkNotNull(securityDatabaseInstance);
  }

  @Override
  public void apply() {
    withDatabaseAndClass(securityDatabaseInstance, DB_CLASS, (db, type) -> {
      int updates = db.command(new OCommandSQL(QUERY)).execute(ADMIN_PASS);
      if (updates > 0) {
        log.info("Updated admin user status to 'changepassword'.");
      }
    });
  }
}
