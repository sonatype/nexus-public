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
package org.sonatype.nexus.upgrade.datastore;

import org.sonatype.nexus.common.cooperation2.Cooperation2;
import org.sonatype.nexus.common.cooperation2.Cooperation2Factory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalUpgradeCooperationTest
{
  @Mock
  private Cooperation2Factory cooperationFactory;

  @Mock
  private Cooperation2Factory.Builder builder;

  @Mock
  private Cooperation2 cooperation;

  @Test
  void get_buildsCooperationFromInjectedFactory() {
    when(cooperationFactory.configure()).thenReturn(builder);
    when(builder.build("my-scope")).thenReturn(cooperation);

    Cooperation2 result = new LocalUpgradeCooperation(cooperationFactory).get("my-scope");

    assertThat(result).isSameAs(cooperation);
  }

  @Test
  void get_memoizesPerId() {
    when(cooperationFactory.configure()).thenReturn(builder);
    when(builder.build("my-scope")).thenReturn(cooperation);
    LocalUpgradeCooperation underTest = new LocalUpgradeCooperation(cooperationFactory);

    Cooperation2 first = underTest.get("my-scope");
    Cooperation2 second = underTest.get("my-scope");

    // repeated get(sameId) returns the memoized instance, and the factory is consulted only once
    assertThat(second).isSameAs(first);
    verify(cooperationFactory, times(1)).configure();
    verify(builder, times(1)).build("my-scope");
  }
}
