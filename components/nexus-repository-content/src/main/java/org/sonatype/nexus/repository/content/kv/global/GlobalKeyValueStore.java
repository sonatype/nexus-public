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
package org.sonatype.nexus.repository.content.kv.global;

import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.datastore.TransactionalStoreSupport;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

/**
 * MyBatis nexus_key_value access interface
 */
@Named("mybatis")
@Singleton
public class GlobalKeyValueStore
    extends TransactionalStoreSupport
{
  @Inject
  public GlobalKeyValueStore(
      final DataSessionSupplier sessionSupplier)
  {
    super(sessionSupplier, DEFAULT_DATASTORE_NAME);
  }

  private DataSession<?> currentSession() {
    return UnitOfWork.currentSession();
  }

  private NexusKeyValueDAO dao() {
    return currentSession().access(NexusKeyValueDAO.class);
  }

  /**
   * gets a value by the given key
   *
   * @param key a string key
   * @return {@link Optional<NexusKeyValue>}
   */
  @Transactional
  public Optional<NexusKeyValue> getKey(final String key) {
    return dao().get(key);
  }

  /**
   * sets a key_value record
   *
   * @param keyValue record to be created/updated
   */
  @Transactional
  public void setKey(final NexusKeyValue keyValue) {
    dao().set(keyValue);
  }

  /**
   * removes a value by the given key
   *
   * @param key a string key
   * @return a primitive boolean indicating if the record was deleted successfully or not
   */
  @Transactional
  public boolean removeKey(final String key) {
    return dao().remove(key);
  }
}
