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
package org.sonatype.nexus.proxy.events;

import java.util.Map;

import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;

/**
 * The event fired in case of some content changes in Nexus related to an item/file.
 *
 * @author cstamas
 */
public abstract class RepositoryItemEvent
    extends RepositoryEvent
{
  /**
   * The item in question
   */
  private final StorageItem item;

  private final Map<String, Object> itemContext;

  public RepositoryItemEvent(final Repository repository, final StorageItem item) {
    super(repository);

    this.item = item;

    this.itemContext = item.getItemContext().flatten();
  }

  /**
   * Gets the item uid. Shortcut for item.getRepositoryItemUid().
   *
   * @return the item uid
   */
  public RepositoryItemUid getItemUid() {
    return item.getRepositoryItemUid();
  }

  /**
   * Gets the item context. A snapshot of item.getItemContext() in creation moment of this event, since
   * item.getItemContenxt() is mutable is is probably changed when some async processor will process this event!
   *
   * @return the item context
   */
  public Map<String, Object> getItemContext() {
    return itemContext;
  }

  /**
   * Gets the involved item.
   */
  public StorageItem getItem() {
    return item;
  }

  // ==

  public String toString() {
    return String.format("%s(sender=%s, %s)", getClass().getSimpleName(),
        RepositoryStringUtils.getHumanizedNameString(getRepository()), getItem().getRepositoryItemUid().toString());
  }

}
