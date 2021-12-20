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
package org.sonatype.nexus.plugins.capabilities.support.condition;

import org.sonatype.sisu.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

/**
 * {@link Conditions} UTs.
 *
 * @since capabilities 2.0
 */
public class ConditionsTest
    extends TestSupport
{

  /**
   * Passed in factories are returned.
   */
  @Test
  public void and01() {
    final LogicalConditions logicalConditions = mock(LogicalConditions.class);
    final CapabilityConditions capabilityConditions = mock(CapabilityConditions.class);
    final RepositoryConditions repositoryConditions = mock(RepositoryConditions.class);
    final NexusConditions nexusConditions = mock(NexusConditions.class);
    CryptoConditions cryptoConditions = mock(CryptoConditions.class);
    final Conditions underTest = new Conditions(
        logicalConditions, capabilityConditions, repositoryConditions, nexusConditions, cryptoConditions
    );
    assertThat(underTest.logical(), is(equalTo(logicalConditions)));
    assertThat(underTest.capabilities(), is(equalTo(capabilityConditions)));
    assertThat(underTest.repository(), is(equalTo(repositoryConditions)));
    assertThat(underTest.nexus(), is(equalTo(nexusConditions)));
  }

}
