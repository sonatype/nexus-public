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
package org.sonatype.nexus.repository.cache;

import javax.annotation.Nullable;

/**
 * Emitted when the configured base URL changes.
 * Consumers that embed base-URL-dependent content in cached blobs (e.g. rendered PCCS
 * metadata views) should subscribe to this event and invalidate their caches accordingly.
 */
public class BaseUrlChangedEvent
{
  @Nullable
  private final String oldUrl;

  @Nullable
  private final String newUrl;

  public BaseUrlChangedEvent(@Nullable final String oldUrl, @Nullable final String newUrl) {
    this.oldUrl = oldUrl;
    this.newUrl = newUrl;
  }

  /**
   * The base URL that was active before the change, or {@code null} if no URL was configured.
   */
  @Nullable
  public String getOldUrl() {
    return oldUrl;
  }

  /**
   * The base URL that is now active, or {@code null} if the URL was cleared.
   */
  @Nullable
  public String getNewUrl() {
    return newUrl;
  }
}
