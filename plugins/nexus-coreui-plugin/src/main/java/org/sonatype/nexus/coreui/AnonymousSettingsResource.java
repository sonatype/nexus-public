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
package org.sonatype.nexus.coreui;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.security.anonymous.AnonymousConfiguration;
import org.sonatype.nexus.security.anonymous.AnonymousManager;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * @since 3.19
 */
@Named
@Singleton
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(AnonymousSettingsResource.RESOURCE_PATH)
public class AnonymousSettingsResource
    extends ComponentSupport
    implements Resource
{
  static final String RESOURCE_PATH = "internal/ui/anonymous-settings";

  private final AnonymousManager anonymousManager;

  @Inject
  public AnonymousSettingsResource(final AnonymousManager anonymousManager) {
    this.anonymousManager = checkNotNull(anonymousManager);
  }

  @GET
  @RequiresPermissions("nexus:settings:read")
  public AnonymousSettingsXO read() {
    AnonymousConfiguration config = anonymousManager.getConfiguration();
    AnonymousSettingsXO xo = new AnonymousSettingsXO();

    xo.setEnabled(config.isEnabled());
    xo.setUserId(config.getUserId());
    xo.setRealmName(config.getRealmName());

    return xo;
  }

  @PUT
  @RequiresAuthentication
  @RequiresPermissions("nexus:settings:update")
  public void update(@NotNull @Valid final AnonymousSettingsXO anonymousXO) {
    AnonymousConfiguration configuration = anonymousManager.newConfiguration();
    configuration.setEnabled(anonymousXO.getEnabled());
    configuration.setRealmName(anonymousXO.getRealmName());
    configuration.setUserId(anonymousXO.getUserId());
    anonymousManager.setConfiguration(configuration);
  }
}
