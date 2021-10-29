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
package org.sonatype.nexus.internal.atlas;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.security.config.MemorySecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityContributor;
import org.sonatype.nexus.security.config.SecurityContributorSupport;

/**
 * Atlas security configuration.
 *
 * @since 3.0
 */
@Named
@Singleton
public class AtlasSecurityContributor
    extends SecurityContributorSupport
    implements SecurityContributor
{
  public static final String ATLAS_DOMAIN = "atlas";

  public static final String ATLAS_ALL_PRIV_ID = "nx-atlas-all";

  public static final String ATLAS_ALL_PRIV_DESCRIPTION = "All permissions for Support Tools";

  @Override
  public SecurityConfiguration getContribution() {
    MemorySecurityConfiguration config = new MemorySecurityConfiguration();

    config.addPrivilege(
        createApplicationPrivilege(ATLAS_ALL_PRIV_ID, ATLAS_ALL_PRIV_DESCRIPTION, ATLAS_DOMAIN, ACTION_ALL));

    return config;
  }
}
