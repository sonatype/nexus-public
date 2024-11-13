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
package org.sonatype.nexus.internal.security.secrets.rest;

import javax.validation.Valid;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * REST API to set a new encryption key and re-encrypt secrets
 */
@Api(value = "Security management: secrets encryption")
public interface SecretsEncryptionApiResourceDoc
{
  @ApiOperation(
      value = "Re-encrypt secrets using the specified key",
      notes = "Ensure all nodes have access to the key, and they use the same key"
  )
  @ApiResponses(value = {
      @ApiResponse(code = 202, message = "Re-encrypt task successfully submitted"),
      @ApiResponse(code = 400, message = "Invalid request. See the response for more information. " +
          "Possible causes: The key is not available to all nodes, upgrade needed or empty key id."),
      @ApiResponse(code = 403, message = "Insufficient permissions to re-encrypt secrets"),
      @ApiResponse(code = 409, message = "Re-encryption task in progress.")
  })
  Response reEncrypt(@Valid final ReEncryptionRequestApiXO reEncryptionRequestApiXO);
}
