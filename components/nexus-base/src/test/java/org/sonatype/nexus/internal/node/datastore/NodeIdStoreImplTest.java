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
package org.sonatype.nexus.internal.node.datastore;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.testdb.DataSessionRule;
import org.sonatype.nexus.transaction.TransactionModule;

import com.google.inject.Guice;
import com.google.inject.Provides;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class NodeIdStoreImplTest
    extends TestSupport
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule().access(NodeIdDAO.class);

  @Mock
  private EventManager eventManager;

  private NodeIdStoreImpl underTest;

  @Before
  public void setup() {
    underTest = Guice.createInjector(new TransactionModule()
    {
      @Provides
      DataSessionSupplier getDataSessionSupplier() {
        return sessionRule;
      }

      @Provides
      EventManager getEventManager() {
        return eventManager;
      }
    }).getInstance(NodeIdStoreImpl.class);
  }

  @Test
  public void testGetOrCreate_existing() {
    underTest.set("foo");

    String result = underTest.getOrCreate();

    assertThat(result, is("foo"));

    assertThat(underTest.get().get(), is("foo"));
  }

  @Test
  public void testGetOrCreate_unset() {
    String result = underTest.getOrCreate();

    assertThat(underTest.get().isPresent(), is(true));
    assertThat(result, is(underTest.get().get()));
  }
}
