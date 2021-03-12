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
package org.sonatype.nexus.repository.rest.internal.api;

import javax.annotation.Nullable;

/**
 * @since 3.30
 */
public class RepositoryStatusXO {

  private final boolean online;

  private final String description;

  private final String reason;

  public RepositoryStatusXO(final boolean online,
                            @Nullable final String description,
                            @Nullable final String reason) {
    this.online = online;
    this.description = description;
    this.reason = reason;
  }

  public boolean isOnline() {
    return online;
  }

  @Nullable
  public String getDescription() {
    return description;
  }

  @Nullable
  public String getReason() {
    return reason;
  }
}
