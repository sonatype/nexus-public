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
package org.sonatype.nexus.wonderland.internal;

import org.sonatype.sisu.goodies.crypto.internal.CryptoHelperImpl;
import org.sonatype.sisu.goodies.crypto.internal.RandomBytesGeneratorImpl;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link AuthTicketGenerator}.
 */
public class AuthTicketGeneratorTest
    extends TestSupport
{
  @Test
  public void generateWithDefault() {
    AuthTicketGenerator generator = new AuthTicketGenerator(new RandomBytesGeneratorImpl(new CryptoHelperImpl()), 16);
    String token = generator.generate();
    log(token);
    assertNotNull(token);
  }

  private void generateWithSize(final int size) {
    AuthTicketGenerator generator = new AuthTicketGenerator(new RandomBytesGeneratorImpl(new CryptoHelperImpl()), 16);
    String token = generator.generate(size);
    log(token);
    assertNotNull(token);
  }

  @Test
  public void generateWithSize32() {
    generateWithSize(32);
  }

  @Test
  public void generateWithSize66() {
    generateWithSize(66);
  }
}
