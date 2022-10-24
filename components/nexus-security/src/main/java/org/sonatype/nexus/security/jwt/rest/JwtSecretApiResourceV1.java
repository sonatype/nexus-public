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
package org.sonatype.nexus.security.jwt.rest;

import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.security.jwt.SecretStore;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.OK;
import static org.sonatype.nexus.common.app.FeatureFlags.JWT_ENABLED;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;
import static org.sonatype.nexus.security.jwt.rest.JwtSecretApiResourceV1.PATH;

/**
 * REST API to reset the stored JWT secret.
 *
 * @since 3.38
 */
@FeatureFlag(name = JWT_ENABLED)
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(PATH)
@Named
@Singleton
public class JwtSecretApiResourceV1
    extends ComponentSupport
    implements Resource, JwtSecretApiResourceDoc
{
  public static final String PATH = V1_API_PREFIX + "/security/jwt";

  private final SecretStore secretStore;

  @Inject
  public JwtSecretApiResourceV1(final SecretStore secretStore) {
    this.secretStore = checkNotNull(secretStore);
  }

  @PUT
  @RequiresAuthentication
  @RequiresPermissions("nexus:settings:update")
  @Override
  public Response resetSecret() {
    String secret = UUID.randomUUID().toString();
    secretStore.setSecret(secret);
    return status(OK).build();
  }
}
