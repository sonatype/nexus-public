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

import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.DataStoreConfiguration;
import org.sonatype.nexus.transaction.Transaction;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Common support class for {@link DataStore}s.
 *
 * @since 3.next
 */
public abstract class DataStoreSupport<T extends Transaction, S extends DataSession<T>>
    extends StateGuardLifecycleSupport
    implements DataStore<S>
{
  protected DataStoreConfiguration configuration;

  @Override
  public DataStoreConfiguration getConfiguration() {
    return configuration;
  }

  @Override
  public void setConfiguration(final DataStoreConfiguration configuration) {
    this.configuration = checkNotNull(configuration);
  }
}
