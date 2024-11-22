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
import java.io.UncheckedIOException;

import org.sonatype.goodies.testsupport.TestSupport;

import org.aopalliance.intercept.Joinpoint;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.transaction.Transactional.DEFAULT_REASON;

/**
 * Test {@link TransactionSupport}.
 */
public class TransactionSupportTest
    extends TestSupport
{
  @Mock
  private Transactional spec;

  @Mock
  private Joinpoint aspect;

  @Mock(answer = CALLS_REAL_METHODS)
  private TransactionSupport tx;

  private TransactionalWrapper txWrapper;

  @Before
  @SuppressWarnings("unchecked")
  public void setUp() {
    doNothing().when(tx).doBegin();
    doNothing().when(tx).doCommit();
    doNothing().when(tx).doRollback();

    when(spec.reason()).thenReturn(DEFAULT_REASON);
    when(spec.commitOn()).thenReturn(new Class[]{});
    when(spec.retryOn()).thenReturn(new Class[]{UncheckedIOException.class});
    when(spec.swallow()).thenReturn(new Class[]{});

    txWrapper = new TransactionalWrapper(spec, aspect);
  }

  @Test
  public void transactionLifecycle() {
    assertThat(tx.isActive(), is(false));
    tx.begin();
    assertThat(tx.isActive(), is(true));
    tx.rollback();
    assertThat(tx.isActive(), is(false));
    tx.begin();
    assertThat(tx.isActive(), is(true));
    tx.commit();
    assertThat(tx.isActive(), is(false));
  }

  @Test(expected = IllegalStateException.class)
  public void cannotBeginTwice() {
    assertThat(tx.isActive(), is(false));
    tx.begin();
    assertThat(tx.isActive(), is(true));
    tx.begin();
  }

  @Test
  public void dontRequireExplicitBegin() {
    assertThat(tx.isActive(), is(false));
    tx.commit();
    assertThat(tx.isActive(), is(false));
    tx.rollback();
    assertThat(tx.isActive(), is(false));
  }

  @Test
  public void reasonIsTracked() {
    tx.reason("testing!");

    assertThat(tx.reason(), is("testing!"));
  }

  @Test(expected = NullPointerException.class)
  public void reasonCannotBeNull() {
    tx.reason(null);
  }

  @Test
  public void retryLimitIsResetWhenReusingTX() throws Throwable {

    // first fail every attempt to commit...
    doThrow(new UncheckedIOException("oops", new IOException())).when(tx).doCommit();

    try {
      // should eventually breach default retry limit
      txWrapper.proceedWithTransaction(tx);
      fail("Expected retry to eventually fail");
    }
    catch (UncheckedIOException e) {
      assertThat(e.getMessage(), containsString("oops"));
    }

    // now just fail the first attempt and let subsequent calls succeed
    doThrow(new UncheckedIOException("oops", new IOException())).doNothing().when(tx).doCommit();

    // assuming retry limit was reset, this should retry once and then pass
    txWrapper.proceedWithTransaction(tx);
  }
}
