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
package org.sonatype.nexus.orient.transaction;

import javax.inject.Provider;

import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.transaction.Transactional;

import com.orientechnologies.common.concur.ONeedRetryException;

/**
 * Like {@link Transactional#operation} helper, but also provides access to underlying Orient database.
 *
 * @since 3.2
 */
public interface OrientTransactional
{
  /**
   * Helper to apply transactional behaviour to lambdas.
   */
  OrientOperations<RuntimeException, ?> operation = new OrientOperations<>();

  /**
   * Helper to apply transactional behaviour to lambdas; retries when OrientDB indicates it should.
   */
  OrientOperations<RuntimeException, ?> retryOperation = operation.retryOn(ONeedRetryException.class);

  /**
   * Builds transactional behaviour for the given database.
   */
  static OrientOperations<RuntimeException, ?> inTx(final Provider<DatabaseInstance> databaseInstance) {
    return operation.withDb(databaseInstance);
  }

  /**
   * Builds transactional behaviour for the given database; retries when OrientDB indicates it should.
   */
  static OrientOperations<RuntimeException, ?> inTxRetry(final Provider<DatabaseInstance> databaseInstance) {
    return retryOperation.withDb(databaseInstance);
  }
}
