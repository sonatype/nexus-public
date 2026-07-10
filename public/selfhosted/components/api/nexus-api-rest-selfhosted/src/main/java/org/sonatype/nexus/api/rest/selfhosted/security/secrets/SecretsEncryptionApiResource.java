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
package org.sonatype.nexus.api.rest.selfhosted.security.secrets;

import java.util.Map;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.sonatype.nexus.api.rest.selfhosted.security.secrets.model.ReEncryptionRequestApiXO;
import org.sonatype.nexus.crypto.secrets.MissingKeyException;
import org.sonatype.nexus.crypto.secrets.ReEncryptService;
import org.sonatype.nexus.crypto.secrets.ReEncryptionNotSupportedException;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.WebApplicationMessageException;

import com.google.common.collect.ImmutableMap;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public abstract class SecretsEncryptionApiResource
    implements Resource, SecretsEncryptionApiResourceDoc
{
  public static final String RESOURCE_PATH = "/secrets/encryption";

  private final ReEncryptService reEncryptService;

  protected SecretsEncryptionApiResource(final ReEncryptService reEncryptService) {
    this.reEncryptService = checkNotNull(reEncryptService);
  }

  @PUT
  @Override
  @Path("/re-encrypt")
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  public Response reEncrypt(@Valid final ReEncryptionRequestApiXO request) {
    try {
      String taskId = reEncryptService.submitReEncryption(request.getSecretKeyId(), request.getNotifyEmail());
      Map<String, Object> response = ImmutableMap.of("status", Status.ACCEPTED.getStatusCode(), "message",
          "Task submitted. ID: " + taskId);
      return Response
          .status(Status.ACCEPTED)
          .entity(response)
          .type(APPLICATION_JSON)
          .build();
    }
    catch (MissingKeyException | ReEncryptionNotSupportedException ex) {
      throw new WebApplicationMessageException(Status.BAD_REQUEST, ex.getMessage(), APPLICATION_JSON);
    }
    catch (IllegalStateException ex) {
      throw new WebApplicationMessageException(Status.CONFLICT, ex.getMessage(), APPLICATION_JSON);
    }
  }

}
