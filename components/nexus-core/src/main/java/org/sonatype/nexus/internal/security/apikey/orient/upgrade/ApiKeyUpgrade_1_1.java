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
package org.sonatype.nexus.internal.security.apikey.orient.upgrade;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.DependsOn;
import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.internal.security.apikey.orient.OrientApiKeyEntityAdapter;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.orient.DatabaseUpgradeSupport;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;

import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.sql.OCommandSQL;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Updates {@link OrientApiKeyEntityAdapter} database index to add principals (realm) to the index
 */
@Named
@Singleton
@Upgrades(model = ApiKeyModel.NAME, from = "1.0", to = "1.1")
@DependsOn(model = DatabaseInstanceNames.SECURITY, version = "1.3", checkpoint = true)
public class ApiKeyUpgrade_1_1
    extends DatabaseUpgradeSupport
{
  private static final String DB_API_KEY_CLASS = new OClassNameBuilder()
      .type("api_key")
      .build();

  private static final String P_DOMAIN = "domain";

  private static final String P_PRIMARY_PRINCIPAL = "primary_principal";

  private static final String P_PRINCIPALS = "principals";

  private static final String API_KEY_NEW_INDEX = new OIndexNameBuilder()
      .type(DB_API_KEY_CLASS)
      .property(P_DOMAIN)
      .property(P_PRIMARY_PRINCIPAL)
      .property(P_PRINCIPALS)
      .build();

  private static final String DROP_INDEX = String.format("DROP INDEX %s", new OIndexNameBuilder()
      .type(DB_API_KEY_CLASS)
      .property(P_DOMAIN)
      .property(P_PRIMARY_PRINCIPAL)
      .build());

  private final Provider<DatabaseInstance> securityDatabaseInstance;

  @Inject
  public ApiKeyUpgrade_1_1(
      @Named(DatabaseInstanceNames.SECURITY) final Provider<DatabaseInstance> securityDatabaseInstance)
  {
    this.securityDatabaseInstance = checkNotNull(securityDatabaseInstance);
  }

  @Override
  public void apply() throws Exception {
    if (hasSchemaClass(securityDatabaseInstance, DB_API_KEY_CLASS)) {
      log.info("Updating {} index", DB_API_KEY_CLASS);

      withDatabaseAndClass(securityDatabaseInstance, DB_API_KEY_CLASS, (db, type) -> {
        log.debug("Removing existing index");
        db.command(new OCommandSQL(DROP_INDEX)).execute();
        log.debug("Create index");
        type.createIndex(API_KEY_NEW_INDEX, INDEX_TYPE.UNIQUE, P_DOMAIN, P_PRIMARY_PRINCIPAL, P_PRINCIPALS);
      });
    }
  }
}
