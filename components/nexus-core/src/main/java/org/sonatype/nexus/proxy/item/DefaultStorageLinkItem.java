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
package org.sonatype.nexus.proxy.item;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.router.RepositoryRouter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of {@link StorageLinkItem}.
 */
public class DefaultStorageLinkItem
    extends AbstractStorageItem
    implements StorageLinkItem
{
  private transient RepositoryItemUid targetUid;

  public DefaultStorageLinkItem(Repository repository, ResourceStoreRequest request, boolean canRead, boolean canWrite,
      RepositoryItemUid targetUid)
  {
    super(repository, request, canRead, canWrite);
    setTarget(targetUid);
  }

  public DefaultStorageLinkItem(RepositoryRouter router, ResourceStoreRequest request, boolean canRead,
      boolean canWrite, RepositoryItemUid targetUid)
  {
    super(router, request, canRead, canWrite);
    setTarget(targetUid);
  }

  @Override
  public RepositoryItemUid getTarget() {
    return targetUid;
  }

  @Override
  public void setTarget(RepositoryItemUid target) {
    this.targetUid = checkNotNull(target);
  }

  // ==

  @Override
  public String toString() {
    return String.format("%s (link to %s)", super.toString(), getTarget().toString());
  }
}
