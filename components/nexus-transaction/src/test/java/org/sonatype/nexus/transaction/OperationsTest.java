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
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Matchers.any;
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

  @Test
  public void testDefaultSpec() throws Exception {

    Operations.transactional(new Operation<String, RuntimeException>()
    {
      // implicit default @Transactional
      public String call() {
        return methods.nonTransactional();
      }
    });

    InOrder order = inOrder(tx);
    order.verify(tx).begin();
    order.verify(tx).commit();
    order.verify(tx).close();
    verifyNoMoreInteractions(tx);
  }

  @Test
  public void testOperationRetrySuccess() throws Exception {
    when(tx.allowRetry(any(Exception.class))).thenReturn(true);

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
    order.verify(tx).allowRetry(any(IOException.class));
    order.verify(tx).begin();
    order.verify(tx).rollback();
    order.verify(tx).allowRetry(any(IOException.class));
    order.verify(tx).begin();
    order.verify(tx).rollback();
    order.verify(tx).allowRetry(any(IOException.class));
    order.verify(tx).begin();
    order.verify(tx).commit();
    order.verify(tx).close();
    verifyNoMoreInteractions(tx);
  }

  @Test(expected = IOException.class)
  public void testOperationRetryFailure() throws Exception {
    when(tx.allowRetry(any(Exception.class))).thenReturn(true).thenReturn(false);

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
      order.verify(tx).allowRetry(any(IOException.class));
      order.verify(tx).begin();
      order.verify(tx).rollback();
      order.verify(tx).allowRetry(any(IOException.class));
      order.verify(tx).close();
      verifyNoMoreInteractions(tx);
    }
  }

  @Test
  public void testLambdaRetrySuccess() throws Exception {
    when(tx.allowRetry(any(Exception.class))).thenReturn(true);

    methods.setCountdownToSuccess(3);
    Operations.transactional()
        .retryOn(IOException.class)
        .throwing(IOException.class)
        .call(() -> methods.retryOnCheckedException());

    InOrder order = inOrder(tx);
    order.verify(tx).begin();
    order.verify(tx).rollback();
    order.verify(tx).allowRetry(any(IOException.class));
    order.verify(tx).begin();
    order.verify(tx).rollback();
    order.verify(tx).allowRetry(any(IOException.class));
    order.verify(tx).begin();
    order.verify(tx).rollback();
    order.verify(tx).allowRetry(any(IOException.class));
    order.verify(tx).begin();
    order.verify(tx).commit();
    order.verify(tx).close();
    verifyNoMoreInteractions(tx);
  }

  @Test(expected = IOException.class)
  public void testLambdaRetryFailure() throws Exception {
    when(tx.allowRetry(any(Exception.class))).thenReturn(true).thenReturn(false);

    methods.setCountdownToSuccess(100);
    try {
      Operations.transactional()
          .retryOn(IOException.class)
          .throwing(IOException.class)
          .call(() -> methods.retryOnCheckedException());
    }
    finally {
      InOrder order = inOrder(tx);
      order.verify(tx).begin();
      order.verify(tx).rollback();
      order.verify(tx).allowRetry(any(IOException.class));
      order.verify(tx).begin();
      order.verify(tx).rollback();
      order.verify(tx).allowRetry(any(IOException.class));
      order.verify(tx).close();
      verifyNoMoreInteractions(tx);
    }
  }

  @Test
  public void testBatchModeDoesntLeakOutsideScope() {
    final Transaction[] txHolder = new Transaction[2];

    UnitOfWork.begin(() -> Mockito.mock(Transaction.class));
    try {
      UnitOfWork.beginBatch(() -> Mockito.mock(Transaction.class));
      try {
        Operations.transactional(new Operation<Void, RuntimeException>()
        {
          @Transactional
          public Void call() {
            txHolder[0] = UnitOfWork.currentTx();
            return null;
          }
        });
        Operations.transactional(new Operation<Void, RuntimeException>()
        {
          @Transactional
          public Void call() {
            txHolder[1] = UnitOfWork.currentTx();
            return null;
          }
        });

        // batched: transactions should be same
        assertThat(txHolder[0], is(txHolder[1]));
      }
      finally {
        UnitOfWork.end(); // ends inner-batch-work
      }

      Operations.transactional(new Operation<Void, RuntimeException>()
      {
        @Transactional
        public Void call() {
          txHolder[0] = UnitOfWork.currentTx();
          return null;
        }
      });
      Operations.transactional(new Operation<Void, RuntimeException>()
      {
        @Transactional
        public Void call() {
          txHolder[1] = UnitOfWork.currentTx();
          return null;
        }
      });

      // non-batched: transactions should differ
      assertThat(txHolder[0], is(not(txHolder[1])));
    }
    finally {
      UnitOfWork.end(); // ends outer-non-batch-work
    }
  }
}
