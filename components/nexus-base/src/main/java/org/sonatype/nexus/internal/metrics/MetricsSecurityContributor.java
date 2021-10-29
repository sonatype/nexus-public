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
package org.sonatype.nexus.internal.metrics;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.security.config.MemorySecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityContributor;
import org.sonatype.nexus.security.config.SecurityContributorSupport;

import static org.apache.commons.lang.StringUtils.capitalize;

/**
 * Metrics security configuration.
 *
 * @since 3.0
 */
@Named
@Singleton
public class MetricsSecurityContributor
    extends SecurityContributorSupport
    implements SecurityContributor
{
  private static final String DOMAIN_VALUE = "metrics";

  public static final String METRICS_ALL_PRIV_ID = "nx-metrics-all";

  @Override
  public MemorySecurityConfiguration getContribution() {
    MemorySecurityConfiguration configuration = new MemorySecurityConfiguration();

    configuration.addPrivilege(
        createApplicationPrivilege(METRICS_ALL_PRIV_ID, ALL_DESCRIPTION_BASE + capitalize(DOMAIN_VALUE), DOMAIN_VALUE,
            ACTION_ALL));

    return configuration;
  }
}
