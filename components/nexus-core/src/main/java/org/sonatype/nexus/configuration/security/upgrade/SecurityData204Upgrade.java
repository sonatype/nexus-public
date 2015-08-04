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
package org.sonatype.nexus.configuration.security.upgrade;

import java.util.Arrays;
import java.util.List;

import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.upgrade.ConfigurationIsCorruptedException;
import org.sonatype.security.model.CRole;
import org.sonatype.security.model.CUserRoleMapping;
import org.sonatype.security.model.Configuration;
import org.sonatype.security.model.upgrade.AbstractDataUpgrader;
import org.sonatype.security.model.upgrade.SecurityDataUpgrader;

@Singleton
@Typed(value = SecurityDataUpgrader.class)
@Named(value = "2.0.4")
public class SecurityData204Upgrade
    extends AbstractDataUpgrader<Configuration>
    implements SecurityDataUpgrader
{

  private static final List<String> DEPRECATED_ROLES = Arrays.asList("admin", "deployment", "developer");

  @Override
  public void doUpgrade(Configuration cfg)
      throws ConfigurationIsCorruptedException
  {
    for (CRole role : cfg.getRoles()) {
      updateDeprecatedRoles(role.getRoles());
    }

    for (CUserRoleMapping map : cfg.getUserRoleMappings()) {
      updateDeprecatedRoles(map.getRoles());
    }
  }

  public static void updateDeprecatedRoles(List<String> roles) {
    for (int i = 0; i < roles.size(); i++) {
      String role = roles.get(i);
      if (DEPRECATED_ROLES.contains(role)) {
        roles.set(i, "nx-" + role);
      }
    }
  }

}
