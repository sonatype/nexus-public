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

import java.util.Collection;
import java.util.List;

import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.Repository;

import com.google.common.collect.ImmutableList;

/**
 * The event fired in case of some content changes in Nexus related to multiple item/file (a batch), and is being
 * announced by some component. This might be some subsystem doing "atomic deploy", or batch removal, or similar. These
 * events are NOT about deploys (or deletion, caching etc), and a (series of) deploys or deletions will be emitted
 * before this event. Single {@link RepositoryItemEventStore} will be fired for each deploy always.
 *
 * @since 2.3
 */
public abstract class RepositoryItemBatchEvent
    extends RepositoryEvent
{
  private final List<StorageItem> items;

  /**
   * Constructor.
   */
  public RepositoryItemBatchEvent(final Repository repository, final Collection<StorageItem> items) {
    super(repository);
    this.items = ImmutableList.copyOf(items);
  }

  public List<StorageItem> getItems() {
    return items;
  }
}
