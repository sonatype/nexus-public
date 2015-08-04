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
package org.sonatype.nexus.proxy;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.targets.TargetSet;

/**
 * The base abstraction of Proximity. This interface is implemented by Repositories and also by Routers.
 *
 * @author cstamas
 */
public interface ResourceStore
{
  /**
   * Retrieves item from the path of the request.
   *
   * @param request the request
   * @return the storage item
   * @throws NoSuchResourceStoreException the no such store exception
   * @throws IllegalOperationException    the repository not available exception
   * @throws ItemNotFoundException        the item not found exception
   * @throws StorageException             the storage exception
   * @throws AccessDeniedException        the access denied exception
   */
  StorageItem retrieveItem(ResourceStoreRequest request)
      throws ItemNotFoundException,
             IllegalOperationException,
             StorageException,
             AccessDeniedException;

  /**
   * Copies the item from <code>from</code> to <code>to</code>. Retrieval may involve remote access unless request
   * forbids it, the storing involves local storage only.
   *
   * @param from the from
   * @param to   the to
   * @throws NoSuchResourceStoreException the no such repository exception
   * @throws IllegalOperationException    the repository not available exception
   * @throws ItemNotFoundException        the item not found exception
   * @throws StorageException             the storage exception
   * @throws AccessDeniedException        the access denied exception
   */
  void copyItem(ResourceStoreRequest from, ResourceStoreRequest to)
      throws UnsupportedStorageOperationException,
             ItemNotFoundException,
             IllegalOperationException,
             StorageException,
             AccessDeniedException;

  /**
   * Moves the item from <code>from</code> to <code>to</code>. Retrieval may involve remote access unless request
   * forbids it, the storing involves local storage only.
   *
   * @param from the from
   * @param to   the to
   * @throws NoSuchResourceStoreException the no such repository exception
   * @throws IllegalOperationException    the repository not available exception
   * @throws ItemNotFoundException        the item not found exception
   * @throws StorageException             the storage exception
   * @throws AccessDeniedException        the access denied exception
   */
  void moveItem(ResourceStoreRequest from, ResourceStoreRequest to)
      throws UnsupportedStorageOperationException,
             ItemNotFoundException,
             IllegalOperationException,
             StorageException,
             AccessDeniedException;

  /**
   * Deletes item from the request path. Involves local storage only.
   *
   * @param request the request
   * @throws StorageException             the storage exception
   * @throws NoSuchResourceStoreException the no such repository exception
   * @throws IllegalOperationException    the repository not available exception
   * @throws ItemNotFoundException        the item not found exception
   * @throws AccessDeniedException        the access denied exception
   */
  void deleteItem(ResourceStoreRequest request)
      throws UnsupportedStorageOperationException,
             ItemNotFoundException,
             IllegalOperationException,
             StorageException,
             AccessDeniedException;

  /**
   * Stores item onto request path, with content supplied by stream. Involves local storage only.
   *
   * @param request        the request
   * @param is             the is
   * @param userAttributes the user attributes
   * @throws StorageException             the storage exception
   * @throws NoSuchResourceStoreException the no such repository exception
   * @throws RepositoryNotAvailableException
   *                                      the repository not available exception
   * @throws AccessDeniedException        the access denied exception
   */
  void storeItem(ResourceStoreRequest request, InputStream is, Map<String, String> userAttributes)
      throws UnsupportedStorageOperationException,
             ItemNotFoundException,
             IllegalOperationException,
             StorageException,
             AccessDeniedException;

  /**
   * Creates a collection (directory) on requested path. Involves local storage only.
   *
   * @param request        the request
   * @param userAttributes the user attributes
   * @throws StorageException             the storage exception
   * @throws NoSuchResourceStoreException the no such repository exception
   * @throws IllegalOperationException    the repository not available exception
   * @throws AccessDeniedException        the access denied exception
   */
  void createCollection(ResourceStoreRequest request, Map<String, String> userAttributes)
      throws UnsupportedStorageOperationException,
             ItemNotFoundException,
             IllegalOperationException,
             StorageException,
             AccessDeniedException;

  /**
   * Lists the path denoted by item.
   *
   * @param request the request
   * @return the collection< storage item>
   * @throws NoSuchResourceStoreException the no such repository exception
   * @throws IllegalOperationException    the repository not available exception
   * @throws ItemNotFoundException        the item not found exception
   * @throws StorageException             the storage exception
   * @throws AccessDeniedException        the access denied exception
   */
  Collection<StorageItem> list(ResourceStoreRequest request)
      throws ItemNotFoundException,
             IllegalOperationException,
             StorageException,
             AccessDeniedException;

  /**
   * Returns the target set belonging to ResourceStoreRequest.
   */
  TargetSet getTargetsForRequest(ResourceStoreRequest request);
}
