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

import org.sonatype.nexus.proxy.RequestContext;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.attributes.Attributes;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.router.RepositoryRouter;

/**
 * The Interface StorageItem, a top of the item abstraction.
 */
public interface StorageItem
{
  /**
   * The request used to retrieve this item.
   */
  ResourceStoreRequest getResourceStoreRequest();

  /**
   * Gets the repository item UID of this item, pointing to repository and path from where originates this item. Note:
   * not all items have UID! See {@link #isVirtual()}.
   */
  RepositoryItemUid getRepositoryItemUid();

  /**
   * Sets the repository item UID.
   */
  void setRepositoryItemUid(RepositoryItemUid repositoryItemUid);

  /**
   * Gets the repository ID from where originates item. Shortcut method (uses {@link #getRepositoryItemAttributes()}).
   */
  String getRepositoryId();

  /**
   * Gets the item creation timestamp. Shortcut method (uses {@link #getRepositoryItemAttributes()}).
   */
  long getCreated();

  /**
   * Gets the item modification timestamp. Shortcut method (uses {@link #getRepositoryItemAttributes()}).
   */
  long getModified();

  /**
   * Gets the timestamp when item was stored in local storage. Shortcut method (uses
   * {@link #getRepositoryItemAttributes()}).
   * 
   * @return the stored locally
   */
  long getStoredLocally();

  /**
   * Sets the timestamp when item was stored in local storage. Shortcut method (uses
   * {@link #getRepositoryItemAttributes()}).
   */
  void setStoredLocally(long ts);

  /**
   * Gets the timestamp when item was last remotely checked (remote storage consulted for newer version). Shortcut
   * method (uses {@link #getRepositoryItemAttributes()}).
   */
  long getRemoteChecked();

  /**
   * Sets the timestamp when item was last remotely checked. Shortcut method (uses
   * {@link #getRepositoryItemAttributes()}).
   */
  void setRemoteChecked(long ts);

  /**
   * Gets the timestamp when item was last requested externally (from a client, not from internal task, see
   * {@link ResourceStoreRequest#isExternal()}). Shortcut method (uses {@link #getRepositoryItemAttributes()}).
   */
  long getLastRequested();

  /**
   * Sets the timestamp when item was last requested externally. Shortcut method (uses
   * {@link #getRepositoryItemAttributes()}).
   */
  void setLastRequested(long ts);

  /**
   * Returns {@code true} if item is "virtual". Virtual items have no backing repository (for example item coming from a
   * {@link RepositoryRouter}, a path that is not deep enough to dive into any repository), hence, they have no UIDs
   * either ({@link #getRepositoryItemUid()} returns {@code null}. Still, they have {@link #getPath()} and might even
   * have content!
   */
  boolean isVirtual();

  /**
   * Returns {@code true} if item is readable. Shortcut method (uses {@link #getRepositoryItemAttributes()}).
   */
  boolean isReadable();

  /**
   * Returns {@code true} if item is writable. Shortcut method (uses {@link #getRepositoryItemAttributes()}).
   */
  boolean isWritable();

  /**
   * Returns {@code true} if item is expired. This flag has effect only in {@link ProxyRepository}s, and will make
   * item's proxy repository to re-check remotely for existence of a newer version when this item is being retrieved
   * from it. Shortcut method (uses {@link #getRepositoryItemAttributes()}).
   */
  boolean isExpired();

  /**
   * Sets expired flag on item, see {@link #isExpired()}. Shortcut method (uses {@link #getRepositoryItemAttributes()}).
   */
  void setExpired(boolean expired);

  /**
   * Returns the item path, that is <b>not the same as path returned by UID</b> got from {@link #getRepositoryItemUid()}
   * ! The path depends and changes, depending from where was it retrieved, while {@link RepositoryItemUid#getPath()} is
   * immutable! If this item was retrieved over {@link RepositoryRouter}, it will contain all the first and second level
   * selector path elements (for example "/repositories/repo-foo"). In case item is retrieved from {@link Repository}
   * instance directly, then this method return value usually equals to the UID path, but does not have to. Shortcut
   * method (uses {@link #getRepositoryItemAttributes()}).
   */
  String getPath();

  /**
   * Returns the item name (last path element in path returned by {@link #getPath()}).
   */
  String getName();

  /**
   * Returns the item parent path (all but last path element in path returned by {@link #getPath()}.
   */
  String getParentPath();

  /**
   * Returns the depth of this item's path, 0 being root.
   *
   * @since 2.7.0
   */
  int getPathDepth();

  /**
   * Returns this items remote URL (full URL from where this item was proxied), if item originates from a
   * {@link ProxyRepository}, {@code null} otherwise.
   */
  String getRemoteUrl();

  /**
   * Returns the item attributes. Item attributes are persisted, and they share same lifecycle as the item's content in
   * Nexus (they get created when item is deployed, and they are deleted when item is deleted from Nexus).
   */
  Attributes getRepositoryItemAttributes();

  /**
   * Gets the item context. Item context, similarly as request context (see
   * {@link ResourceStoreRequest#getRequestContext()} is not persisted, and it exists only during existence of the item
   * instance. This item's context has parent set to request's context used to retrieve it, meaning in the moment of
   * creation of this instance those share same content, but some code might modify this context, as multiple items
   * might share same request context (like dereferencing links, where one request generates a series of retrievals to
   * find the non-link target).
   */
  RequestContext getItemContext();
}
