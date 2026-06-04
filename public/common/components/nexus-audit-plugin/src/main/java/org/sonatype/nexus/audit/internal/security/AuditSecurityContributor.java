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
package org.sonatype.nexus.audit.internal.security;

import org.sonatype.nexus.security.config.MemorySecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityContributor;
import org.sonatype.nexus.security.config.SecurityContributorSupport;

import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.capitalize;

/**
 * Audit log security configuration.
 */
@Component
public class AuditSecurityContributor
    extends SecurityContributorSupport
    implements SecurityContributor
{
  private static final String DOMAIN_VALUE = "audit";

  public static final String AUDIT_READ_PRIV_ID = "nx-audit-read";

  public static final String AUDIT_ALL_PRIV_ID = "nx-audit-all";

  @Override
  public MemorySecurityConfiguration getContribution() {
    MemorySecurityConfiguration configuration = new MemorySecurityConfiguration();

    configuration.addPrivilege(
        createApplicationPrivilege(
            AUDIT_READ_PRIV_ID,
            "Read " + capitalize(DOMAIN_VALUE),
            DOMAIN_VALUE,
            ACTION_READ));

    configuration.addPrivilege(
        createApplicationPrivilege(
            AUDIT_ALL_PRIV_ID,
            ALL_DESCRIPTION_BASE + capitalize(DOMAIN_VALUE),
            DOMAIN_VALUE,
            ACTION_ALL));

    return configuration;
  }
}
