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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Upgrade step to replace 'mark' and 'add' actions with 'create' in application type privileges
 *
 * @since 3.19
 */
@Named
@Singleton
@Upgrades(model = DatabaseInstanceNames.SECURITY, from = "1.2", to = "1.3")
public class SecurityDatabaseUpgrade_1_3 // NOSONAR
    extends DatabaseUpgradeSupport
{
  @VisibleForTesting
  static final String DB_CLASS = new OClassNameBuilder().type("privilege").build();

  @VisibleForTesting
  static final String P_PROPERTIES = "properties";

  @VisibleForTesting
  static final String P_ACTIONS = "actions";

  @VisibleForTesting
  static final String P_NAME = "name";

  @VisibleForTesting
  static final String P_TYPE = "type";

  private static final String QUERY = String.format("select from %s where %s = 'application'", DB_CLASS, P_TYPE);

  private final Provider<DatabaseInstance> securityDatabaseInstance;

  @Inject
  public SecurityDatabaseUpgrade_1_3(@Named(
      DatabaseInstanceNames.SECURITY) final Provider<DatabaseInstance> securityDatabaseInstance)
  {
    this.securityDatabaseInstance = checkNotNull(securityDatabaseInstance);
  }

  @Override
  public void apply() {
    withDatabaseAndClass(securityDatabaseInstance, DB_CLASS, (db, type) -> {
      List<ODocument> results = db.command(new OCommandSQL(QUERY)).execute();
      results.forEach(result -> {
        Map<String, String> properties = result.field(P_PROPERTIES, OType.EMBEDDEDMAP);
        if (properties != null) {
          String actionString = properties.get(P_ACTIONS);
          if (actionString != null) {
            List<String> actions = new ArrayList(Arrays.asList(actionString.split(",")));
            if (actions.contains("add") || actions.contains("mark")) {
              actions.remove("add");
              actions.remove("mark");
              if (!actions.contains("create")) {
                actions.add("create");
              }
              properties.put(P_ACTIONS, String.join(",", actions));
              result.field(P_PROPERTIES, properties);
              log.info("Updated application privilege {} to align with CRUD actions", (String) result.field(P_NAME));
              result.save();
            }
          }
        }
      });
    });
  }
}
