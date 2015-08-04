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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 2.3
 */
@SuppressWarnings("serial")
public class NexusClientResponseException
    extends NexusClientException
{

  private final int responseCode;

  private final String reasonPhrase;

  private final String responseBody;

  public NexusClientResponseException(final int responseCode,
                                      final String reasonPhrase,
                                      final String responseBody)
  {
    this(null, responseCode, reasonPhrase, responseBody);
  }

  public NexusClientResponseException(final String message,
                                      final int responseCode,
                                      final String reasonPhrase,
                                      final String responseBody)
  {
    super(message == null ? String.format("%s - %s", responseCode, reasonPhrase) : message);
    this.responseCode = responseCode;
    this.reasonPhrase = checkNotNull(reasonPhrase);
    this.responseBody = responseBody;
  }

  public int getResponseCode() {
    return responseCode;
  }

  public String getReasonPhrase() {
    return reasonPhrase;
  }

  public String getResponseBody() {
    return responseBody;
  }

}
