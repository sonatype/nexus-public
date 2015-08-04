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
package org.sonatype.nexus.proxy.repository;

import java.util.Collection;
import java.util.List;

import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;

/**
 * <p>
 * A common base for Proximity "web site" repository. It simply serves up static stuff, and detects "index.html" within
 * the collections.
 *
 * @author cstamas
 */
public abstract class AbstractWebSiteRepository
    extends AbstractRepository
    implements WebSiteRepository
{
  
  @Override
  protected AbstractWebSiteRepositoryConfiguration getExternalConfiguration(boolean forModification) {
    return (AbstractWebSiteRepositoryConfiguration) super.getExternalConfiguration(forModification);
  }

  @Override
  public List<String> getWelcomeFiles() {
    return getExternalConfiguration(false).getWelcomeFiles();
  }

  @Override
  public void setWelcomeFiles(List<String> vals) {
    getExternalConfiguration(true).setWelcomeFiles(vals);
  }

  @Override
  protected StorageItem doRetrieveItem(ResourceStoreRequest request)
      throws IllegalOperationException,
             ItemNotFoundException,
             StorageException
  {
    StorageItem result = super.doRetrieveItem(request);

    List<String> wf = getWelcomeFiles();

    boolean useWelcomeFiles = !request.getRequestContext().containsKey(WebSiteRepository.USE_WELCOME_FILES_KEY)
        || Boolean.TRUE.equals(request.getRequestContext().get(WebSiteRepository.USE_WELCOME_FILES_KEY));

    if (useWelcomeFiles && result instanceof StorageCollectionItem && wf.size() > 0) {
      // it is a collection, check for one of the "welcome" files
      Collection<StorageItem> collItems = list(false, (StorageCollectionItem) result);

      for (StorageItem item : collItems) {
        if (item instanceof StorageFileItem && wf.contains(item.getName())) {
          // it is a file, it's name is in welcomeFiles list, so return it instead parent collection
          return item;
        }
      }
    }

    return result;
  }

}
