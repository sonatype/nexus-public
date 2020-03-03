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
package org.sonatype.nexus.repository.proxy;

import com.google.common.collect.ListMultimap;

/**
 * @since 3.18.1
 */
public class BypassHttpErrorException
    extends RuntimeException
{
  private final int httpCode;

  private final String reason;

  private final ListMultimap<String, String> headers;

  public BypassHttpErrorException(final int httpCode, final String reason, final ListMultimap<String, String> headers) {
    this.httpCode = httpCode;
    this.reason = reason;
    this.headers = headers;
  }

  public int getStatusCode() {
    return httpCode;
  }

  public String getReason() {
    return reason;
  }

  public ListMultimap<String, String> getHeaders() {
    return headers;
  }
}
