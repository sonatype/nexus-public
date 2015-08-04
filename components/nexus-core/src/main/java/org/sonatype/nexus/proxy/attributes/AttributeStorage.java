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

import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.storage.local.LocalRepositoryStorage;

/**
 * AttributeStorage manages persistence of StorageItem Attributes. Is used by LocalStorages and should not be directly
 * used (ie. by a plugin).
 *
 * @author cstamas
 * @see LocalRepositoryStorage
 */
public interface AttributeStorage
{

  /**
   * Returns the attributes for given key or {@code null} if no attributes found for given key.
   *
   * @param uid the key for which attributes needs to be fetched
   * @return the attributes or {@code null} if no attributes found for given uid.
   * @throws IOException in case of IO problem.
   */
  Attributes getAttributes(RepositoryItemUid uid)
      throws IOException;

  /**
   * Put attributes for given key.
   *
   * @param uid        the key
   * @param attributes the attributes to store
   * @throws IOException in case of IO problem.
   */
  void putAttributes(RepositoryItemUid uid, Attributes attributes)
      throws IOException;

  /**
   * Deletes attributes associated with given key.
   *
   * @param uid the uid
   * @return true, if delete actually happened (attributes for given uid existed).
   * @throws IOException in case of IO problem.
   */
  boolean deleteAttributes(RepositoryItemUid uid)
      throws IOException;
}
