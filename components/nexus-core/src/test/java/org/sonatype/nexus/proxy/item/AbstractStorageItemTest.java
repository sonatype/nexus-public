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
package org.sonatype.nexus.proxy.item;

import org.sonatype.nexus.proxy.AbstractNexusTestEnvironment;
import org.sonatype.nexus.proxy.ResourceStore;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.router.RepositoryRouter;
import org.sonatype.sisu.litmus.testsupport.mock.MockitoRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

public abstract class AbstractStorageItemTest
    extends AbstractNexusTestEnvironment
{
  @Rule
  public TestRule mockitoRule = new MockitoRule(this);

  @Mock
  protected Repository repository;

  @Mock
  protected RepositoryRouter router;

  public void checkAbstractStorageItem(ResourceStore store, AbstractStorageItem item, boolean isVirtual,
                                       String shouldBeName, String shouldBePath, String shouldBeParentPath)
  {
    // it is backed by repo
    assertEquals(isVirtual, item.isVirtual());
    assertEquals(!isVirtual, item.getRepositoryItemUid() != null);

    // repo should be eq
    assertEquals(store, item.getStore());

    if (Repository.class.isAssignableFrom(store.getClass())) {
      // repo stuff eq
      assertEquals(repository.getId(), item.getRepositoryId());
      assertEquals(repository.getId(), ((Repository) item.getStore()).getId());
    }
    else {
      assertEquals(null, item.getRepositoryId());
      // router is only one from now on and has no ID
      // assertEquals( router.getId(), item.getStore().getId() );
    }

    // path
    assertEquals(shouldBeName, item.getName());
    assertEquals(shouldBePath, item.getPath());
    assertEquals(shouldBeParentPath, item.getParentPath());
  }

  @Test
  public void testDummy() {
    assertEquals("a", "a");
  }

}
