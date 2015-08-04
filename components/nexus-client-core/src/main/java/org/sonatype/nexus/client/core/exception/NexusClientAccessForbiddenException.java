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
package org.sonatype.nexus.client.core.exception;

import javax.ws.rs.core.Response;

/**
 * Thrown in case of 403 Forbidden.
 *
 * @since 2.4
 */
public class NexusClientAccessForbiddenException
    extends NexusClientResponseException
{

  public NexusClientAccessForbiddenException(final String reasonPhrase,
                                             final String responseBody)
  {
    super(Response.Status.FORBIDDEN.getStatusCode(), reasonPhrase, responseBody);
  }

  public NexusClientAccessForbiddenException(final String message,
                                             final String reasonPhrase,
                                             final String responseBody)
  {
    super(message, Response.Status.FORBIDDEN.getStatusCode(), reasonPhrase, responseBody);
  }

}
