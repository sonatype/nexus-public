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
package org.sonatype.nexus.repository.httpclient;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Remote connection status.
 *
 * @since 3.0
 */
public class RemoteConnectionStatus
{
  private final String description;

  private final String reason;

  public RemoteConnectionStatus(final String description) {
    this(description, null);
  }

  public RemoteConnectionStatus(final String description, final @Nullable String reason) {
    this.description = checkNotNull(description);
    this.reason = reason;
  }

  public String getDescription() {
    return description;
  }

  @Nullable
  public String getReason() {
    return reason;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(description);
    if (reason != null) {
      sb.append(" - ").append(reason);
    }
    return sb.toString();
  }
}
