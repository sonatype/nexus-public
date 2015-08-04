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
package org.sonatype.nexus.apachehttpclient;

import com.google.common.base.Preconditions;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.protocol.HttpContext;

/**
 * Nexus connection keep alive strategy, that differs from the HC4 default one only in one thing: when server does
 * not state timeout, it never says "indefinite" (meaning pool it forever), but instead a finite amount of time.
 *
 * @author cstamas
 * @since 2.3
 */
public class NexusConnectionKeepAliveStrategy
    extends DefaultConnectionKeepAliveStrategy
{

  /**
   * The max duration for how long to pool a connection in milliseconds. Used as default too, instead of
   * "indefinite" case.
   */
  private final long maxKeepAliveDuration;

  /**
   * @param maxKeepAliveDuration the max duration in millis for how long to pool the connection.
   */
  public NexusConnectionKeepAliveStrategy(final long maxKeepAliveDuration) {
    Preconditions.checkArgument(maxKeepAliveDuration > -1,
        "maxKeepAliveDuration must be 0 or higher, but is set to %s",
        maxKeepAliveDuration);
    this.maxKeepAliveDuration = maxKeepAliveDuration;
  }

  /**
   * Returns the duration of time which this connection can be safely kept
   * idle. Nexus by default does not "believe" much to remote servers, and will never
   * keep connection pooled "forever", nor will keep it pooled for unreasonable long time.
   *
   * @return the duration of time which this connection can be safely kept idle in pool.
   */
  public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
    // ask super class
    final long result = super.getKeepAliveDuration(response, context);
    if (result < 0) {
      // if "indefinite", use default
      return maxKeepAliveDuration;
    }
    else {
      // else "cap" it with max
      return Math.min(result, maxKeepAliveDuration);
    }
  }
}
