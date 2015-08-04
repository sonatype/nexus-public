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

import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.router.RepositoryRouter;

/**
 * An item that is a link (idea very similar to Linux symlink). It points to some other content (generated or not) in
 * any repository within this Nexus instance (even cross repository). It cannot point to target not in repository (like
 * virtual items). Link itself might be virtual. The path where target UID points might not exists (link is still valid
 * but stale, as dereferencing it will result simply in {@link ItemNotFoundException}), but if the link target UID
 * points to a non existing {@link Repository}, link will be automatically removed by Nexus on retrieving the link
 * itself and {@link ItemNotFoundException} will be thrown.
 * <p>
 * For dereferencing links recommended method is {@link RepositoryRouter#dereferenceLink(StorageLinkItem)}.
 */
public interface StorageLinkItem
    extends StorageItem
{
  /**
   * Returns the target UID.
   */
  RepositoryItemUid getTarget();

  /**
   * Sets the target UID.
   */
  void setTarget(RepositoryItemUid target);
}
