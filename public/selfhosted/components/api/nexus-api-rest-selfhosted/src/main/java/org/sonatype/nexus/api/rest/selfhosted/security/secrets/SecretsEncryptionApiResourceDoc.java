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

import jakarta.validation.Valid;
import jakarta.ws.rs.core.Response;

import org.sonatype.nexus.api.rest.selfhosted.security.secrets.model.ReEncryptionRequestApiXO;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * REST API to set a new encryption key and re-encrypt secrets
 */
@Tag(name = "Security management: secrets encryption")
public interface SecretsEncryptionApiResourceDoc
{
  @Operation(summary = "Re-encrypt secrets using the specified key",
      description = "Ensure all nodes have access to the key, and they use the same key")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "202", description = "Re-encrypt task successfully submitted"),
      @ApiResponse(responseCode = "400", description = "Invalid request. See the response for more information. " +
          "Possible causes: The key is not available to all nodes, upgrade needed or empty key id."),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions to re-encrypt secrets"),
      @ApiResponse(responseCode = "409", description = "Re-encryption task in progress.")
  })
  Response reEncrypt(@Valid final ReEncryptionRequestApiXO reEncryptionRequestApiXO);
}
