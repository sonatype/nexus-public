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
package org.sonatype.nexus.orient.freeze;

import javax.inject.Provider;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventBus;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.internal.freeze.DatabaseFreezeServiceImpl;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DatabaseFreezeServiceImplTest
    extends TestSupport
{

  DatabaseFreezeServiceImpl underTest;

  @Mock
  Provider<DatabaseInstance> provider_one;

  @Mock
  DatabaseInstance databaseInstance_one;

  @Mock
  ODatabaseDocumentTx databaseDocumentTx_one;

  @Mock
  Provider<DatabaseInstance> provider_two;

  @Mock
  DatabaseInstance databaseInstance_two;

  @Mock
  EventBus eventBus;

  @Mock
  ODatabaseDocumentTx databaseDocumentTx_two;

  @Before
  public void setup() {
    when(provider_one.get()).thenReturn(databaseInstance_one);
    when(databaseInstance_one.connect()).thenReturn(databaseDocumentTx_one);

    when(provider_two.get()).thenReturn(databaseInstance_two);
    when(databaseInstance_two.connect()).thenReturn(databaseDocumentTx_two);

    underTest = new DatabaseFreezeServiceImpl(of(provider_one, provider_two).collect(toSet()), eventBus);
  }

  @Test
  public void testFreeze() {
    ArgumentCaptor<DatabaseFreezeChangeEvent> freezeChangeEventArgumentCaptor = forClass(
        DatabaseFreezeChangeEvent.class);

    underTest.freezeAllDatabases();

    verify(databaseDocumentTx_one).freeze(true);
    verify(databaseDocumentTx_two).freeze(true);

    verify(eventBus).post(freezeChangeEventArgumentCaptor.capture());

    assertThat(freezeChangeEventArgumentCaptor.getValue().isFrozen(), is(true));
    assertThat(underTest.isFrozen(), is(true));

  }

  @Test
  public void testUnfreeze() {
    ArgumentCaptor<DatabaseFreezeChangeEvent> freezeChangeEventArgumentCaptor = forClass(
        DatabaseFreezeChangeEvent.class);

    underTest.releaseAllDatabases();
    verify(databaseDocumentTx_one).release();
    verify(databaseDocumentTx_two).release();

    verify(eventBus).post(freezeChangeEventArgumentCaptor.capture());

    assertThat(freezeChangeEventArgumentCaptor.getValue().isFrozen(), is(false));
    assertThat(underTest.isFrozen(), is(false));
  }
}
