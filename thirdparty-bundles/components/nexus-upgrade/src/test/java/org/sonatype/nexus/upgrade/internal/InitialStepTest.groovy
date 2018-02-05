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
package org.sonatype.nexus.upgrade.internal

import org.sonatype.goodies.testsupport.TestSupport

import static org.hamcrest.Matchers.is;
import org.junit.Before
import org.junit.Test
import static org.hamcrest.MatcherAssert.assertThat

/**
 * Test for {@link InitialStep}
 */
class InitialStepTest
    extends TestSupport
{
  InitialStep underTest

  @Before
  public void setup() {
    underTest = new InitialStep([config: '1.2'])
  }

  @Test
  public void 'initial step satisfies dependency with exact version match'() {
    assertThat(underTest.satisfies('config', '1.2'), is(true))
  }

  @Test
  public void 'initial step does not satisfy dependency if initial version lower than the checked version'() {
    assertThat(underTest.satisfies('config', '2.0'), is(false))
  }

  @Test
  public void ' step does not satisfy the dependency if the provided model does not exist and version is over 1.0'() {
    assertThat(underTest.satisfies('foo', '2.0'), is(false))
  }

  @Test
  public void 'the initial step does satisfy the dependency if the existing version is greater than required'() {
    assertThat(underTest.satisfies('config', '1.1'), is(true))
  }
}
