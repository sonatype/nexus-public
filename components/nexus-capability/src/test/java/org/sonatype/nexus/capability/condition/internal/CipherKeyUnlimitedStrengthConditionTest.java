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
package org.sonatype.nexus.capability.condition.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.crypto.CryptoHelper;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link CipherKeyUnlimitedStrengthCondition}.
 *
 * @since 2.7
 */
public class CipherKeyUnlimitedStrengthConditionTest
    extends TestSupport
{
  public static final String FAKE_TRANSFORMATION = "fake-transformation";

  private CipherKeyUnlimitedStrengthCondition condition;

  @Mock
  private CryptoHelper crypto;

  @Before
  public void setUp() throws Exception {
    EventManager eventManager = mock(EventManager.class);
    condition = new CipherKeyUnlimitedStrengthCondition(eventManager, crypto, FAKE_TRANSFORMATION);
  }

  @Test
  public void unsatisfiedWhenTransformStrengthIsLow() throws Exception {
    when(crypto.getCipherMaxAllowedKeyLength(FAKE_TRANSFORMATION))
        .thenReturn(CipherKeyUnlimitedStrengthCondition.MIN_BITS - 1);
    condition.bind();
    assertThat(condition.isSatisfied(), is(false));
  }

  @Test
  public void satisfiedWhenTransformStrengthIsHigh() throws Exception {
    when(crypto.getCipherMaxAllowedKeyLength(FAKE_TRANSFORMATION))
        .thenReturn(CipherKeyUnlimitedStrengthCondition.MIN_BITS);
    condition.bind();
    assertThat(condition.isSatisfied(), is(true));
  }
}
