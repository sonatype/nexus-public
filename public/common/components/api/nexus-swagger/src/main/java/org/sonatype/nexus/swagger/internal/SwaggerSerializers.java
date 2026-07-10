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
package org.sonatype.nexus.swagger.internal;

import org.sonatype.nexus.rest.Component;

/**
 * Siesta-managed {@link io.swagger.v3.jaxrs2.SwaggerSerializers}.
 *
 * <p>
 * NEXUS-46395: migrated from Swagger 1.x ({@code io.swagger.jaxrs.listing.SwaggerSerializers})
 * to OpenAPI 3.x. The new base class outputs OpenAPI 3.0 JSON/YAML rather than Swagger 2.0 JSON;
 * customers consuming the {@code /v3/api-docs} endpoint will see this shape change. Pretty-printing
 * is configured at the ObjectMapper level in OpenAPI 3.x; we no longer call setPrettyPrint().
 */
@org.springframework.stereotype.Component
public class SwaggerSerializers
    extends io.swagger.v3.jaxrs2.SwaggerSerializers
    implements Component
{
  public SwaggerSerializers() { // NOSONAR
    // OpenAPI 3.x SwaggerSerializers always pretty-prints by default; no toggle needed.
  }
}
