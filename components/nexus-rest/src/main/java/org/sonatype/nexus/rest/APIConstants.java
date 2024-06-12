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
package org.sonatype.nexus.rest;

/**
 * Constants relating to REST APIs.
 *
 * @since 3.4
 */
public class APIConstants
{
  private APIConstants() {
  }

  public static final String V1_API_PREFIX = "/v1";

  /**
   * Used for REST endpoints which are labeled as beta. These endpoints may be changed though we ought to warn users via
   * release notes when this occurs.
   */
  public static final String BETA_API_PREFIX = "/beta";

  public static final String INTERNAL_API_PREFIX = "/internal";

  /**
   * Mime type for versionless REST API URLs which indicates that the Content-Type is V1 from the endpoint.
   *
   * @see <a href="https://swagger.io/docs/specification/2-0/mime-types/">Swagger 2.0 Mime Types</a>
   */
  public static final String APPLICATION_NEXUS_V1 = "application/vnd.sonatype.nexus.v1+json";
}
