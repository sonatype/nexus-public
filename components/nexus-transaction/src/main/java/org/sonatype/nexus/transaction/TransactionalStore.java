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
package org.sonatype.nexus.transaction;

/**
 * Represents some kind of storage with transactional behaviour.
 *
 * @since 3.19
 */
public interface TransactionalStore<S extends TransactionalSession<?>>
{
  /**
   * Opens a new {@link TransactionalSession}.
   */
  S openSession();

  /**
   * Open a new {@link TransactionalSession} with the provided {@link TransactionIsolation} level. May not be supported
   * by all types of stores.
   *
   * @param isolationLevel the isolation level to use for the transaction
   */
  default S openSession(final TransactionIsolation isolationLevel) {
    switch (isolationLevel) {
      case STANDARD:
        return openSession();
      default:
        throw new UnsupportedOperationException();
    }
  }
}
