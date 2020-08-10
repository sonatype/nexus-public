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
package org.sonatype.nexus.repository.rest.api;

import java.util.List;

import org.sonatype.nexus.repository.rest.api.model.AbstractApiRepository;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import static org.sonatype.nexus.rest.ApiDocConstants.API_REPOSITORY_MANAGEMENT;
import static org.sonatype.nexus.rest.ApiDocConstants.AUTHENTICATION_REQUIRED;
import static org.sonatype.nexus.rest.ApiDocConstants.INSUFFICIENT_PERMISSIONS;

/**
 * @since 3.26
 */
@Api(value = API_REPOSITORY_MANAGEMENT)
public interface RepositorySettingsApiResourceDoc
{
  @ApiOperation("List repositories")
  @ApiResponses(value = {
      @ApiResponse(code = 200,
          message = "Repositories list returned",
          response = AbstractApiRepository.class,
          responseContainer = "List"),
      @ApiResponse(code = 401, message = AUTHENTICATION_REQUIRED),
      @ApiResponse(code = 403, message = INSUFFICIENT_PERMISSIONS)
  })
  List<AbstractApiRepository> getRepositories();
}
