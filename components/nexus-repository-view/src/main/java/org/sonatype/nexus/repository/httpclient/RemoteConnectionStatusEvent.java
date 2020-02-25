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

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryEvent;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a change in repository status.
 *
 * @since 3.3
 */
public class RemoteConnectionStatusEvent
    extends RepositoryEvent
{
  private final RemoteConnectionStatus status;

  public RemoteConnectionStatusEvent(final RemoteConnectionStatus status, final Repository repository) {
    super(repository);
    this.status = checkNotNull(status);
  }

  public RemoteConnectionStatus getStatus() {
    return status;
  }

  @Override
  public String toString() {
    return "RemoteConnectionStatusEvent{" +
        "status=" + getStatus() +
        ", repository=" + getRepository() +
        '}';
  }
}
