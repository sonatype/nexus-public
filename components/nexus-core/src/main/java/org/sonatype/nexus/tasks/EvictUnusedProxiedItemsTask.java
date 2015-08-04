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
package org.sonatype.nexus.tasks;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Named;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.scheduling.AbstractNexusRepositoriesTask;
import org.sonatype.nexus.tasks.descriptors.EvictUnusedItemsTaskDescriptor;

/**
 * Evicts unused proxied items.
 */
@Named(EvictUnusedItemsTaskDescriptor.ID)
public class EvictUnusedProxiedItemsTask
    extends AbstractNexusRepositoriesTask<Collection<String>>
{
  /**
   * System event action: evict unused proxied items
   */
  public static final String ACTION = "EVICT_UNUSED_PROXIED_ITEMS";

  @Override
  protected String getRepositoryFieldId() {
    return EvictUnusedItemsTaskDescriptor.REPO_OR_GROUP_FIELD_ID;
  }

  public int getEvictOlderCacheItemsThen() {
    return Integer.parseInt(getParameters().get(EvictUnusedItemsTaskDescriptor.OLDER_THAN_FIELD_ID));
  }

  public void setEvictOlderCacheItemsThen(int evictOlderCacheItemsThen) {
    getParameters().put(EvictUnusedItemsTaskDescriptor.OLDER_THAN_FIELD_ID,
        Integer.toString(evictOlderCacheItemsThen));
  }

  @Override
  protected Collection<String> doRun()
      throws Exception
  {
    ResourceStoreRequest req = new ResourceStoreRequest("/");

    long olderThan = System.currentTimeMillis() - (getEvictOlderCacheItemsThen() * A_DAY);

    if (getRepositoryId() != null) {
      return getRepositoryRegistry().getRepository(getRepositoryId()).evictUnusedItems(req, olderThan);
    }
    else {
      ArrayList<String> result = new ArrayList<String>();

      for (Repository repository : getRepositoryRegistry().getRepositories()) {
        result.addAll(repository.evictUnusedItems(req, olderThan));
      }

      return result;
    }
  }

  @Override
  protected String getAction() {
    return ACTION;
  }

  @Override
  protected String getMessage() {
    if (getRepositoryId() != null) {
      return "Evicting unused proxied items for repository " + getRepositoryName() + ".";
    }
    else {
      return "Evicting unused proxied items for all registered proxy repositories.";
    }
  }

}
