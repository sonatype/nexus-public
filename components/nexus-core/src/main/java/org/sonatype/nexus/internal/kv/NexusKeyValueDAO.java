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
package org.sonatype.nexus.internal.kv;

import java.util.Optional;

import org.sonatype.nexus.datastore.api.DataAccess;
import org.sonatype.nexus.kv.NexusKeyValue;

import org.apache.ibatis.annotations.Param;

/**
 * {@link NexusKeyValue} access
 */
public interface NexusKeyValueDAO
    extends DataAccess
{
  /**
   * gets a value by the given key
   *
   * @param key a string key
   * @return {@link Optional<NexusKeyValue>}
   */
  Optional<NexusKeyValue> get(
      @Param("key") final String key);

  /**
   * sets a key_value record
   *
   * @param record {@link NexusKeyValue} to be created/updated
   */
  void set(final NexusKeyValue record);

  /**
   * removes a value by the given key
   *
   * @param key a string key
   * @return a primitive boolean indicating if the record was deleted successfully or not
   */
  boolean remove(@Param("key") final String key);
}
