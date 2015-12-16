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
package org.sonatype.nexus.web;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Exception to be thrown by Servlets already prepared their error information.  Extending RuntimeException, this
 * gives you far greater flexibility without changing any API
 *
 * @since 2.12
 */
public class ErrorStatusRuntimeException
    extends RuntimeException
{
  private final int responseCode;

  private final String reasonPhrase;

  public ErrorStatusRuntimeException(final int responseCode,
      final String reasonPhrase,
      final String errorMessage,
      final Exception cause)
  {
    super(errorMessage, cause);
    checkArgument(responseCode >= 400, "Not an error-status code: %s", responseCode);
    this.responseCode = responseCode;
    this.reasonPhrase = reasonPhrase;
  }

  public ErrorStatusRuntimeException(final int responseCode, final String reasonPhrase, final String errorMessage) {
    this(responseCode, reasonPhrase, errorMessage, null);
  }

  public int getResponseCode() {
    return responseCode;
  }

  public String getReasonPhrase() {
    return reasonPhrase;
  }
}
