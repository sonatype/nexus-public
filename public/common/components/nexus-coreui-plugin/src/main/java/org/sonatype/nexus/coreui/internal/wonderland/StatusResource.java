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
package org.sonatype.nexus.coreui.internal.wonderland;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.sonatype.nexus.bootstrap.entrypoint.distribution.NexusDistribution;
import org.sonatype.nexus.bootstrap.entrypoint.distribution.NexusDistributionType;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.rest.Resource;

import org.apache.shiro.authz.annotation.RequiresUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;
import org.springframework.stereotype.Component;

/**
 * Status resource.
 *
 * @since 3.0
 */
@Component
@Path(StatusResource.RESOURCE_URI)
public class StatusResource
    implements Resource
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  public static final String RESOURCE_URI = "/wonderland/status";

  private final ApplicationVersion applicationVersion;

  // The distribution is fixed for the JVM's lifetime (it is a classpath property), so resolve it once at
  // construction rather than on every request to this UI-polled status endpoint (matches DockerConnectorFacetImpl).
  private final boolean isCloud;

  @Autowired
  public StatusResource(final ApplicationVersion applicationVersion) {
    this.applicationVersion = requireNonNull(applicationVersion);
    this.isCloud = NexusDistributionType.CLOUD == NexusDistribution.detect(getClass().getClassLoader());
  }

  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @RequiresUser
  public StatusXO get() {
    StatusXO result = new StatusXO();
    result.setVersion(applicationVersion.getVersion());
    // Report the real edition short name (PRO, PROPLUS, ENTERPRISE) rather than collapsing the Pro
    // family to "PRO", so the frontend can distinguish the tiers. The frontend still matches "PRO" by
    // strict equality and does not yet recognize "PROPLUS"/"ENTERPRISE"; that recognition is owned by a
    // separate frontend effort and must land before Pro+/Enterprise are shipped.
    result.setEdition(applicationVersion.getEdition());
    // Cloud is a distribution, not an edition/tier, so detect it from packaging rather than the edition id
    // (which now resolves to the tenant's licensed tier: Pro, Pro+ or Enterprise) — NEXUS-54766 / NEXUS-54772.
    result.setIsCloud(isCloud);
    return result;
  }
}
