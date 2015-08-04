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
package org.sonatype.nexus.proxy.wastebasket;

import java.io.IOException;

import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.local.LocalRepositoryStorage;

public interface Wastebasket
{
  /**
   * Returns the delete operation that this Wastebasket operates upon.
   */
  DeleteOperation getDeleteOperation();

  /**
   * Sets the delete operation to have this Wastebasket operate.
   */
  void setDeleteOperation(DeleteOperation deleteOperation);

  /**
   * Returns the sum of sizes of items in the wastebasket.
   */
  Long getTotalSize();

  /**
   * Empties the wastebasket.
   */
  void purgeAll()
      throws IOException;

  /**
   * Purge the items older than the age
   *
   * @param age age of the items to be deleted, in milliseconds
   */
  void purgeAll(long age)
      throws IOException;

  /**
   * Returns the sum of sizes of items in the wastebasket.
   */
  Long getSize(Repository repository);

  /**
   * Empties the wastebasket.
   */
  void purge(Repository repository)
      throws IOException;

  /**
   * Purge the items older than the age
   *
   * @param age age of the items to be deleted, in milliseconds
   */
  void purge(Repository repository, long age)
      throws IOException;

  /**
   * Performs a delete operation. It deletes at once if item is file or link. If it is a collection, it will delete
   * it
   * and all it's sub-items (recursively).
   */
  void delete(LocalRepositoryStorage ls, Repository repository, ResourceStoreRequest request)
      throws LocalStorageException;

  /**
   * Performs an un-delete operation. If target (where undeleted item should be returned) exists, false is returned,
   * true otherwise. It undeletes at once if item is file or link. If it is a collection, it will undelete it and all
   * it's sub-items (recursively).
   */
  boolean undelete(LocalRepositoryStorage ls, Repository repository, ResourceStoreRequest request)
      throws LocalStorageException;
}
