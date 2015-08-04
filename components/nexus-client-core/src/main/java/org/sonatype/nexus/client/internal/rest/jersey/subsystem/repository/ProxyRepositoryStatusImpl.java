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
package org.sonatype.nexus.client.internal.rest.jersey.subsystem.repository;

import org.sonatype.nexus.client.core.subsystem.repository.ProxyRepositoryStatus;

/**
 * Immutable {@link ProxyRepositoryStatus}.
 *
 * @since 2.3
 */
public class ProxyRepositoryStatusImpl
    extends RepositoryStatusImpl
    implements ProxyRepositoryStatus
{

  private final boolean blocked;

  private final boolean autoBlocked;

  public ProxyRepositoryStatusImpl(final boolean inService,
                                   final boolean blocked,
                                   final boolean autoBlocked)
  {
    super(inService);
    this.blocked = blocked;
    this.autoBlocked = autoBlocked;
  }

  @Override
  public boolean isBlocked() {
    return blocked;
  }

  @Override
  public boolean isAutoBlocked() {
    return autoBlocked;
  }

}
