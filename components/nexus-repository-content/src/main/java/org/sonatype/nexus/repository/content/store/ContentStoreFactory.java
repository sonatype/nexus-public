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
package org.sonatype.nexus.repository.content.store;

import org.sonatype.nexus.datastore.api.ContentDataAccess;

/**
 * Assisted-Inject factory template to help create format-specific store instances in different data stores.
 *
 * The {@link STORE} type-parameter tells Assisted-Inject what type of store to create while the {@link DAO}
 * type-parameter helps it match the right {@link ContentStoreSupport} signature when generating the factory.
 * The format-specific DAO class is also passed into the factory at creation time because we can't reliably
 * extract it from the store implementation when the format re-uses the default STORE types.
 *
 * @see FormatStoreFactory
 *
 * @since 3.26
 */
interface ContentStoreFactory<STORE extends ContentStoreSupport<DAO>, DAO extends ContentDataAccess>
{
  /**
   * Creates a format-specific store instance for the given data store.
   *
   * @param contentStoreName The content data store
   * @param formatDaoClass The format-specific DAO class
   */
  STORE createContentStore(String contentStoreName, Class<DAO> formatDaoClass);
}
