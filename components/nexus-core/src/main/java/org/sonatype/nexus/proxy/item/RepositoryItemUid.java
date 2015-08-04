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

import org.sonatype.nexus.proxy.item.uid.Attribute;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.util.PathUtils;

/**
 * Repository item UID represents a key that uniquely identifies a resource in a repository. Every Item originating
 * from Nexus, that is not "virtual" is backed by UID with reference to it's originating Repository and path within that
 * repository. UIDs are immutable.
 * <p/>
 * Note about "paths" used by this class. UID Paths are not FS file paths! They are always same on every platform, they
 * are more "URL-like" hierarchical paths. Separator is always same (see {@link RepositoryItemUid#PATH_SEPARATOR}) and UIDs does
 * not support notion of "relative paths". They always start with path separator and have 0 or more
 * path elements separated by path separator. They might or might not end with a path separator, telling do you
 * deal with a "collection" (aka "directory" or "folder") or not (represented by {@link StorageCollectionItem}).
 * Still, actual checking for file or collection is NOT recommended using checks for UID ending with a
 * path separator, you'd do the by retrieving item and something like {@code item intanceof StorageCollectionItem}!
 * <p/>
 * Few examples of UID paths:
 * <pre>
 *   /
 *   /foo
 *   /foo/bar/folder/
 * </pre>
 * Constructing UID instances that does not follow these requirements might lead to unexpected results!
 * This might change in future, where the path will be validated and sanitized (and by throwing exceptions
 * show you the location of bug in the code that tried to use invalid path for UID constructor).
 * For path utilities, see {@link PathUtils}.
 *
 * @author cstamas
 */
public interface RepositoryItemUid
{
  /**
   * Constant to denote a separator in Proximity paths.
   */
  String PATH_SEPARATOR = "/";

  /**
   * Constant to represent a root of the path.
   */
  String PATH_ROOT = PATH_SEPARATOR;

  /**
   * Returns string usable as "key".
   */
  String getKey();

  /**
   * Gets the repository that is the origin of the item identified by this UID.
   */
  Repository getRepository();

  /**
   * Gets the path that is the original path in the origin repository for resource with this UID.
   */
  String getPath();

  /**
   * Gets the lock corresponding to this UID. Lock is lazily created, and re-created if needed. All this is
   * transparent to the coder, as long as usual locking patterns and correct coding is applied, something like:
   *
   * <pre>
   *   uid = ...
   *
   *   RepositoryItemUidLock uidLock = uid.getLock();
   *
   *   uidLock.lock(Action.create);
   *   try {
   *     ...
   *   } finally {
   *     uidLock.unlock();
   *   }
   * </pre>
   *
   * As you can see on {@link RepositoryItemUidLock#unlock()} method javadoc, last of the boxed unlocks (if any
   * boxing
   * at all, simply last invocation) also releases the lazily created lock.
   */
  RepositoryItemUidLock getLock();

  /**
   * Gets an "attribute" from this UID.
   */
  <T extends Attribute<?>> T getAttribute(Class<T> attr);

  /**
   * Gets the value of the attribute from this UID, or null if no attribute found.
   */
  <A extends Attribute<V>, V> V getAttributeValue(Class<A> attr);

  /**
   * Gets the value of the attribute from this UID, or null if no attribute found.
   */
  <A extends Attribute<Boolean>> boolean getBooleanAttributeValue(Class<A> attr);
}
