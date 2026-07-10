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
package org.sonatype.nexus.api.rest.selfhosted.security.ssrf;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.sonatype.nexus.api.rest.selfhosted.security.ssrf.model.SsrfProtectionConfigurationXO;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.validation.ssrf.AntiSsrfService;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;

/**
 * REST resource for managing SSRF protection settings.
 */
@Component
@Path(SsrfProtectionApiResource.RESOURCE_URI)
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class SsrfProtectionApiResource
    implements Resource, SsrfProtectionApiResourceDoc
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  static final String RESOURCE_URI = V1_API_PREFIX + "/security/ssrf-protection";

  private final AntiSsrfService antiSsrfService;

  @Autowired
  public SsrfProtectionApiResource(final AntiSsrfService antiSsrfService) {
    this.antiSsrfService = checkNotNull(antiSsrfService);
  }

  @GET
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Override
  public SsrfProtectionConfigurationXO getConfiguration() {
    return new SsrfProtectionConfigurationXO(antiSsrfService.getConfiguration());
  }

  @PUT
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Override
  public SsrfProtectionConfigurationXO updateConfiguration(final SsrfProtectionConfigurationXO configuration) {
    antiSsrfService.updateConfiguration(configuration.toConfiguration());
    return new SsrfProtectionConfigurationXO(antiSsrfService.getConfiguration());
  }
}
