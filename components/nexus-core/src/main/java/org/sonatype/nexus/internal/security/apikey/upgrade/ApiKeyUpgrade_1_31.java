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
package org.sonatype.nexus.internal.security.apikey.upgrade;

import java.sql.Connection;
import java.util.Optional;

import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

/**
 * Updates api_key table to add principals to the primary key
 */
@Named
public class ApiKeyUpgrade_1_31
    extends ComponentSupport
    implements DatabaseMigrationStep
{
  private static final String DROP_CONSTRAINT =
      "ALTER TABLE api_key DROP CONSTRAINT pk_api_key_primaryprincipal_domain;";

  private static final String ADD_CONSTRAINT =
      "ALTER TABLE api_key ADD CONSTRAINT pk_api_key_primaryprincipal_domain_principals PRIMARY KEY "
          + "(primary_principal, domain, principals);";

  @Override
  public Optional<String> version() {
    return Optional.of("1.31");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    boolean tableExists = tableExists(connection, "api_key") ;
    boolean indexExists = indexExists(connection, "pk_api_key_primaryprincipal_domain");

    log.info("Updating primary key for api_key. table_exists:{} index:{} change_required:{}", tableExists, indexExists,
        tableExists && indexExists);

    if (tableExists && indexExists) {
      runStatement(connection, DROP_CONSTRAINT + ADD_CONSTRAINT);
    }
  }
}
