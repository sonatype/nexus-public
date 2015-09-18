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

import org.sonatype.goodies.testsupport.TestSupport;

import com.google.common.base.Suppliers;
import com.google.inject.Guice;
import org.junit.Test;

/**
 * Test unit-of-work behaviour.
 */
public class UnitOfWorkTest
    extends TestSupport
{
  @Test(expected = NullPointerException.class)
  public void testCannotBeginNullWork() {
    UnitOfWork.begin(null);
  }

  @Test(expected = IllegalStateException.class)
  public void testCannotEndNoWork() {
    UnitOfWork.end();
  }

  @Test
  public void testCanPauseNoWork() {
    UnitOfWork.resume(UnitOfWork.pause());
  }

  @Test(expected = IllegalStateException.class)
  public void testCannotResumeTwice() {
    UnitOfWork.begin(Suppliers.<Transaction> ofInstance(null));
    try {
      UnitOfWork work = UnitOfWork.pause();
      UnitOfWork.resume(work);
      UnitOfWork.resume(work);
    }
    finally {
      UnitOfWork.end();
    }
  }

  @Test(expected = IllegalStateException.class)
  public void testCannotStartTransactionWithNoWork() {
    Guice.createInjector(new TransactionModule()).getInstance(ExampleMethods.class).transactional();
  }
}
