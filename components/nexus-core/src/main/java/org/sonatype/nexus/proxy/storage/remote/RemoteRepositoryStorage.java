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
package org.sonatype.nexus.proxy.storage.remote;

import java.net.URL;

import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.RemoteAccessException;
import org.sonatype.nexus.proxy.RemoteStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;

/**
 * Remote storage.
 *
 * @author cstamas
 */
public interface RemoteRepositoryStorage
{
  /**
   * Returns a designator to identify the remote storage implementation (hint: for example the Plexus role hint).
   */
  String getProviderId();

  /**
   * Returns a version to identify the version of remote storage implementation.
   */
  String getVersion();

  /**
   * Check remote storage for reachability.
   *
   * @param uid the uid
   * @return true, if available (reachable)
   * @throws RemoteStorageException the storage exception
   */
  boolean isReachable(ProxyRepository repository, ResourceStoreRequest request)
      throws RemoteAccessException, RemoteStorageException;

  /**
   * Gets the absolute url from base.
   *
   * @param uid the uid
   * @return the absolute url from base
   * @throws RemoteStorageException when the repository in question has wrong/malformed URL set
   */
  URL getAbsoluteUrlFromBase(ProxyRepository repository, ResourceStoreRequest request)
      throws RemoteStorageException;

  /**
   * Validate that the URL that defines storage location is valid.
   */
  void validateStorageUrl(String url)
      throws RemoteStorageException;

  /**
   * Check remote storage if contains item.
   *
   * @param uid the uid
   * @return true, if successful
   * @throws RemoteStorageException the storage exception
   */
  boolean containsItem(ProxyRepository repository, ResourceStoreRequest request)
      throws RemoteAccessException, RemoteStorageException;

  /**
   * Check remote storage if contains item newer then newerThen.
   *
   * @param uid the uid
   * @return true, if successful
   * @throws RemoteStorageException the storage exception
   */
  boolean containsItem(long newerThen, ProxyRepository repository, ResourceStoreRequest request)
      throws RemoteAccessException, RemoteStorageException;

  /**
   * Retrieve item unconditionally.
   *
   * @return the abstract storage item
   * @throws ItemNotFoundException  the item not found exception
   * @throws RemoteStorageException the storage exception
   */
  AbstractStorageItem retrieveItem(ProxyRepository repository, ResourceStoreRequest request, String baseUrl)
      throws ItemNotFoundException, RemoteAccessException, RemoteStorageException;

  /**
   * Store item.
   *
   * @param item the item
   * @throws UnsupportedStorageOperationException
   *                                the unsupported storage operation exception
   * @throws RemoteStorageException the storage exception
   */
  void storeItem(ProxyRepository repository, StorageItem item)
      throws UnsupportedStorageOperationException, RemoteAccessException, RemoteStorageException;

  /**
   * Delete item.
   *
   * @param uid the uid
   * @throws ItemNotFoundException  the item not found exception
   * @throws UnsupportedStorageOperationException
   *                                the unsupported storage operation exception
   * @throws RemoteStorageException the storage exception
   */
  void deleteItem(ProxyRepository repository, ResourceStoreRequest request)
      throws ItemNotFoundException, UnsupportedStorageOperationException, RemoteAccessException,
             RemoteStorageException;
}
