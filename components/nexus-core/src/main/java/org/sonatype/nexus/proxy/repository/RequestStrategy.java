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

import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.item.StorageItem;

/**
 * A Request Strategy that is able to drive request handling and/or modify the request itself during that. Ultimately,
 * instances of this class, when registered with a {@link Repository} instance are able to change how Nexus core
 * handles
 * the request.
 *
 * @author cstamas
 * @since 2.5
 */
public interface RequestStrategy
{
  /**
   * A method that is able to modify the request after it is authorized, but before it is handled by Nexus Core at
   * all. This method is called as very first step of all repository content related methods (content related as
   * {@link Repository#retrieveItem(boolean, ResourceStoreRequest)}, but also for
   * {@link Repository#storeItem(boolean, StorageItem)}, {@link Repository#deleteItem(boolean, ResourceStoreRequest)}
   * and others).
   * <p>
   * If the method wants to completely stop/prevent the execution of this request, it should throw some exception
   * with
   * reason why. Otherwise, a clean return from the method is needed.
   *
   * @param repository from which the item is about to be attempted retrieval (not {@code null}).
   * @param request    retrieval request (not {@code null}).
   * @throws ItemNotFoundException     if Nexus needs to behave like item is not found (see {@link
   *                                   ItemNotFoundException}
   *                                   documentation).
   * @throws IllegalOperationException if Nexus needs to behave like request is illegal (see
   *                                   {@link IllegalOperationException} and subclasses documentation).
   */
  void onHandle(Repository repository, ResourceStoreRequest request, Action action)
      throws ItemNotFoundException, IllegalOperationException;

  /**
   * This method is called for {@link Repository#retrieveItem(boolean, ResourceStoreRequest)} as very last step,
   * after
   * the item is retrieved by any means (from local storage, from valid cache or proxied).
   * <p>
   * If the method wants to prevent serving up this (found and ready to be served) item to the client, it should
   * throw
   * some exception with reason why. Otherwise, a clean return from the method is needed. If the item corresponding
   * to
   * the request was not found, this method will not be called.
   *
   * @param repository from which the item is retrieved (not {@code null}).
   * @param request    retrieval request (not {@code null}).
   * @param item       item to be retrieved (not {@code null}).
   * @throws ItemNotFoundException     if Nexus needs to behave like item is not found (see {@link
   *                                   ItemNotFoundException}
   *                                   documentation).
   * @throws IllegalOperationException if Nexus needs to behave like request is illegal (see
   *                                   {@link IllegalOperationException} and subclasses documentation).
   */
  void onServing(Repository repository, ResourceStoreRequest request, StorageItem item)
      throws ItemNotFoundException, IllegalOperationException;

  /**
   * This method is called when a proxy repository decides it must go remote to fetch an item. Invocation of this
   * method means that requested item is either not present in local cache, or that it's stale (or request otherwise
   * forces remote request like remoteOnly=true or such). In short, Proxy repository is about to go remote.
   * <p>
   * To prevent Proxy repository to go remote, an exception with reason should be thrown. Otherwise, a clean return
   * from the method is needed. Note: if {@link ItemNotFoundException} is thrown from this method, it does not mean
   * that the current request will end with "not found" outcome, as it still depends on actual conditions. For
   * example, if item is present in cache but is stale, and this method prevents remote access (by throwing an
   * {@link ItemNotFoundException}), Nexus will still serve up the stale item from the cache, as this is how it's
   * behavior is defined. The {@link ItemNotFoundException} thrown here will be used if local cache did not contain
   * the request item. Any other (than {@link ItemNotFoundException} or subclass of it) exception thrown here will
   * stop handling of current request.
   *
   * @param repository the proxy repository about to perform remote access (not null).
   * @param request    retrieval request (not null).
   * @param item       item found in local cache, if any, {@code null} otherwise.
   * @throws ItemNotFoundException     if Nexus needs to behave like item is not found (see {@link
   *                                   ItemNotFoundException}
   *                                   documentation). Throwing this exception does not mean that overall processing
   *                                   will finish like item
   *                                   was not found, as if stale item was already found in cache, it will be served
   *                                   up. This exception will
   *                                   merely prevent Nexus performing remote access, and if no locally cached
   *                                   ellement exists, the thrown
   *                                   exception will be re-thrown by proxy.
   * @throws IllegalOperationException if Nexus needs to behave like request is illegal (see
   *                                   {@link IllegalOperationException} and subclasses documentation). Throwing this
   *                                   exception will stop
   *                                   handling of request, unlike when {@link ItemNotFoundException} is thrown.
   */
  void onRemoteAccess(ProxyRepository repository, ResourceStoreRequest request, StorageItem item)
      throws ItemNotFoundException, IllegalOperationException;
}
