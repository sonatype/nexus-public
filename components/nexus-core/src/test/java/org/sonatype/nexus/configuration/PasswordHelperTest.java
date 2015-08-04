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
package org.sonatype.nexus.configuration;

import java.util.concurrent.atomic.AtomicReference;

import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.google.common.base.Throwables;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * UT for {@link PasswordHelper}.
 * 
 * @since 2.8.0
 */
public class PasswordHelperTest
    extends TestSupport
{
  private PasswordHelper helper;

  @Before
  public void init() throws Exception {
    helper = new PasswordHelper(new DefaultPlexusCipher());
  }

  @Test
  public void testThreadSafety() throws Exception {
    final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
    final String password = "just-some-password-for-testing";
    Thread[] threads = new Thread[20];
    for (int i = 0; i < threads.length; i++) {
      threads[i] = new Thread()
      {
        @Override
        public void run() {
          for (int i = 0; i < 20; i++) {
            try {
              assertThat(helper.decrypt(helper.encrypt(password)), is(password));
            }
            catch (Throwable e) {
              error.compareAndSet(null, e);
            }
          }
        }
      };
    }
    for (Thread thread : threads) {
      thread.start();
    }
    for (Thread thread : threads) {
      thread.join();
    }
    if (error.get() != null) {
      Throwables.propagate(error.get());
    }
  }
}
