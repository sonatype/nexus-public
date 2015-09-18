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

import java.io.IOException;

import org.sonatype.goodies.testsupport.TestSupport;

import com.google.common.base.Suppliers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test operations behaviour.
 */
@SuppressWarnings("boxing")
public class OperationsTest
    extends TestSupport
{
  ExampleMethods methods = new ExampleMethods();

  @Mock
  Transaction tx;

  @Before
  public void setUp() {
    UnitOfWork.begin(Suppliers.ofInstance(tx));
  }

  @After
  public void tearDown() {
    UnitOfWork.end();
  }

  @Test(expected = IOException.class)
  public void testRetryFailureBeforeWrapping() throws Exception {
    when(tx.allowRetry()).thenReturn(true).thenReturn(false);

    methods.setCountdownToSuccess(1);
    try {
      methods.retryOnCheckedException();
    }
    finally {
      verifyNoMoreInteractions(tx);
    }
  }

  @Test
  public void testRetrySuccessAfterWrapping() throws Exception {
    when(tx.allowRetry()).thenReturn(true);

    methods.setCountdownToSuccess(3);
    Operations.transactional(new Operation<String, IOException>()
    {
      @Transactional(retryOn = IOException.class)
      public String call() throws IOException {
        return methods.retryOnCheckedException();
      }
    });

    InOrder order = inOrder(tx);
    order.verify(tx).begin();
    order.verify(tx).rollback();
    order.verify(tx).allowRetry();
    order.verify(tx).begin();
    order.verify(tx).rollback();
    order.verify(tx).allowRetry();
    order.verify(tx).begin();
    order.verify(tx).rollback();
    order.verify(tx).allowRetry();
    order.verify(tx).begin();
    order.verify(tx).commit();
    order.verify(tx).close();
    verifyNoMoreInteractions(tx);
  }

  @Test(expected = IOException.class)
  public void testRetryFailureAfterWrapping() throws Exception {
    when(tx.allowRetry()).thenReturn(true).thenReturn(false);

    methods.setCountdownToSuccess(100);
    try {
      Operations.transactional(new Operation<String, IOException>()
      {
        @Transactional(retryOn = IOException.class)
        public String call() throws IOException {
          return methods.retryOnCheckedException();
        }
      });
    }
    finally {
      InOrder order = inOrder(tx);
      order.verify(tx).begin();
      order.verify(tx).rollback();
      order.verify(tx).allowRetry();
      order.verify(tx).begin();
      order.verify(tx).rollback();
      order.verify(tx).allowRetry();
      order.verify(tx).close();
      verifyNoMoreInteractions(tx);
    }
  }
}
