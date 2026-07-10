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
package org.sonatype.nexus.api.rest.selfhosted.security.usersource;

import java.util.List;

import org.sonatype.nexus.api.rest.selfhosted.security.usersource.model.ApiUserSource;
import org.sonatype.nexus.security.internal.rest.NexusSecurityApiConstants;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * Swagger documentation for {@link SecurityApiResource}
 *
 * @since 3.17
 */
@Tag(name = "Security management")
public interface SecurityApiResourceDoc
{
  @Operation(summary = "Retrieve a list of the available user sources.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "403", description = NexusSecurityApiConstants.INVALID_PERMISSIONS)
  })
  List<ApiUserSource> getUserSources();
}
