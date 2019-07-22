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
package org.sonatype.nexus.datastore;

import java.util.Map;

import javax.inject.Named;

import org.sonatype.nexus.datastore.api.DataAccess;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.transaction.Transaction;

/**
 * Temporary stubbed {@link DataStore}.
 *
 * @since 3.next
 */
@Named("jdbc")
public class StubDataStore
    extends DataStoreSupport<Transaction, DataSession<Transaction>>
{
  @Override
  protected void doStart(final String storeName, final Map<String, String> attributes) throws Exception {
    // no-op
  }

  @Override
  public void register(final Class<? extends DataAccess> accessType) {
    // no-op
  }

  @Override
  public void unregister(final Class<? extends DataAccess> accessType) {
    // no-op
  }

  @Override
  public DataSession<Transaction> openSession() {
    throw new UnsupportedOperationException("stub");
  }
}
