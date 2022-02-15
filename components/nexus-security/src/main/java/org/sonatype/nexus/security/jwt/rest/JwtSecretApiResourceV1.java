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

import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.distributed.event.service.api.common.JWTSecretChangedEvent;
import org.sonatype.nexus.distributed.event.service.api.common.PublisherEvent;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.security.jwt.JwtSecretChanged;
import org.sonatype.nexus.security.jwt.SecretStore;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.status;
import static org.sonatype.nexus.distributed.event.service.api.EphemeralNodeId.NODE_ID;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;
import static org.sonatype.nexus.security.jwt.rest.JwtSecretApiResourceV1.PATH;

/**
 * REST API to reset the stored JWT secret.
 *
 * @since 3.next
 */
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

  private final EventManager eventManager;

  @Inject
  public JwtSecretApiResourceV1(final SecretStore secretStore, final EventManager eventManager) {
    this.secretStore = checkNotNull(secretStore);
    this.eventManager = checkNotNull(eventManager);
  }

  @PUT
  @RequiresAuthentication
  @RequiresPermissions("nexus:settings:update")
  @Consumes(MediaType.TEXT_PLAIN)
  @Override
  public void updateSecret(@NotNull final String secret) {
    secretStore.setSecret(secret);
    eventManager.post(new JwtSecretChanged(secret));
    eventManager.post(new PublisherEvent(NODE_ID, new JWTSecretChangedEvent(NODE_ID)));
  }

  @GET
  @RequiresAuthentication
  @RequiresPermissions("nexus:settings:read")
  @Override
  public Response getSecret() {
    Optional<String> secret = secretStore.getSecret();
    if (secret.isPresent()) {
      return status(OK).entity(secret.get()).type(MediaType.TEXT_PLAIN).build();
    }
    return status(NOT_FOUND).build();
  }
}
