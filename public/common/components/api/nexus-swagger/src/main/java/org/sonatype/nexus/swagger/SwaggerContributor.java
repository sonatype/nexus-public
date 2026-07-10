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
package org.sonatype.nexus.swagger;

import io.swagger.v3.oas.models.OpenAPI;

/**
 * Listener providing a hook for customizing the {@link OpenAPI} model.
 *
 * <p>
 * NEXUS-46395: migrated from Swagger 1.x ({@code io.swagger.models.Swagger}) to OpenAPI 3.x
 * ({@code io.swagger.v3.oas.models.OpenAPI}). Implementing plugins must update their
 * {@link #contribute(OpenAPI)} signature accordingly.
 */
public interface SwaggerContributor
{
  /**
   * Called after JAX-RS resource has been scanned.
   *
   * @param openApi the OpenAPI definition
   */
  void contribute(OpenAPI openApi);
}
