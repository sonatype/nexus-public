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
package org.sonatype.nexus.proxy.attributes;

import java.io.IOException;

import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageItem;

/**
 * AttributesHandler manages the storage of item attributes, does their recalculation if needed (how it's done is left
 * to implementation) and offers some "shortcut" methods to perform updates on some (core used) attributes like
 * "lastRequested" and other. All {@link org.sonatype.nexus.proxy.item.StorageCollectionItem} types does not have
 * persisted attributes, and handler will simply not handle them.
 *
 * @author cstamas
 */
public interface AttributesHandler
{

  /**
   * Returns the AttributeStorage used by this AttributesHandler instance.
   *
   * @return AttributeStorage used by this instance.
   */
  AttributeStorage getAttributeStorage();

  /**
   * Fetches the item attributes and decorates the supplied item instance with it.
   *
   * @param item the item to have attributes loaded up and decorated with.
   */
  void fetchAttributes(StorageItem item)
      throws IOException;

  /**
   * Stores the item attributes. If non-null content locator is supplied, attributes will be recalculated before
   * saving.
   *
   * @param item    the item
   * @param content the content of the item if we deal with StorageFileItem
   */
  void storeAttributes(StorageItem item, ContentLocator content)
      throws IOException;

  /**
   * Stores item attributes, as-is, will not try attribute expansion either.
   *
   * @param item the item
   */
  void storeAttributes(StorageItem item)
      throws IOException;

  /**
   * Removes the item attributes from attribute storage.
   *
   * @param uid the uid
   * @return true if attributes are found and deleted, false otherwise.
   */
  boolean deleteAttributes(RepositoryItemUid uid)
      throws IOException;

  /**
   * Sets the storageItem's "checkedRemotely" attribute (a timestamp in millis) to the passed in one and persists the
   * modified attributes.
   */
  void touchItemCheckedRemotely(long timestamp, StorageItem storageItem)
      throws IOException;

  /**
   * Sets the storageItem's "lastRequested" attribute (a timestamp in millis) to the passed in one and persists the
   * modified attributes.
   */
  void touchItemLastRequested(long timestamp, StorageItem storageItem)
      throws IOException;
}
