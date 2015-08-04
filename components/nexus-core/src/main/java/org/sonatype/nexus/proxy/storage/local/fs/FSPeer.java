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
package org.sonatype.nexus.proxy.storage.local.fs;

import java.io.File;
import java.util.Collection;

import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;

/**
 * This is file system specific component encapsulating all the "logic" with file handling, made reusable.
 *
 * @author cstamas
 */
public interface FSPeer
{
  public boolean isReachable(Repository repository, File repositoryBaseDir, ResourceStoreRequest request, File target)
      throws LocalStorageException;

  public boolean containsItem(Repository repository, File repositoryBaseDir, ResourceStoreRequest request, File target)
      throws LocalStorageException;

  public File retrieveItem(Repository repository, File repositoryBaseDir, ResourceStoreRequest request, File target)
      throws ItemNotFoundException, LocalStorageException;

  public void storeItem(Repository repository, File repositoryBaseDir, StorageItem item, File target, ContentLocator cl)
      throws UnsupportedStorageOperationException, LocalStorageException;

  public void shredItem(Repository repository, File repositoryBaseDir, ResourceStoreRequest request, File target)
      throws ItemNotFoundException, UnsupportedStorageOperationException, LocalStorageException;

  public void moveItem(Repository repository, File repositoryBaseDir, ResourceStoreRequest request1, File target1,
                       ResourceStoreRequest request2, File target2)
      throws ItemNotFoundException, UnsupportedStorageOperationException, LocalStorageException;

  public Collection<File> listItems(Repository repository, File repositoryBaseDir, ResourceStoreRequest request,
                                    File target)
      throws ItemNotFoundException, LocalStorageException;
}
