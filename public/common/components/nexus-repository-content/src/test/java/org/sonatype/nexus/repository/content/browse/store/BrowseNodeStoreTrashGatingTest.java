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
package org.sonatype.nexus.repository.content.browse.store;

import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.content.browse.store.example.TestBrowseNodeDAO;
import org.sonatype.nexus.repository.content.store.TrashSchemaState;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Verifies the browse-node read filter ({@code deleted_on IS NULL} in {@code getByDisplayPath}) gates on Trash
 * schema presence, not the feature flag (NEXUS-55410). Once content has been Trashed, turning the flag off must not
 * resurrect it in the browse tree, so the filter has to persist as long as the columns exist -- matching how the
 * asset/component content stores gate the same predicate.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class BrowseNodeStoreTrashGatingTest
{
  @Mock
  private DataSessionSupplier sessionSupplier;

  @Mock
  private TrashSchemaState trashSchemaState;

  private BrowseNodeStore<TestBrowseNodeDAO> underTest() {
    BrowseNodeStore<TestBrowseNodeDAO> store =
        new BrowseNodeStore<>(sessionSupplier, "nexus", TestBrowseNodeDAO.class);
    store.setTrashSchemaState(trashSchemaState);
    return store;
  }

  @Test
  public void trashAvailable_whenSchemaReached_isTrue() {
    when(trashSchemaState.isSchemaReached()).thenReturn(true);

    assertThat(underTest().trashAvailable(), is(true));
  }

  @Test
  public void trashAvailable_whenSchemaNotReached_isFalse() {
    when(trashSchemaState.isSchemaReached()).thenReturn(false);

    assertThat(underTest().trashAvailable(), is(false));
  }

  @Test
  public void trashAvailable_whenSchemaReachedButFeatureFlagOff_stillFiltersTrashedContent() {
    // The resurrection guard (NEXUS-55410): gating keys on schema presence, not the feature flag. With the flag off
    // but the Trash columns present, the browse read filter must still hide Trashed content. isTrashAvailable() is
    // left unstubbed (returns false) to prove the filter does not depend on it.
    when(trashSchemaState.isSchemaReached()).thenReturn(true);

    assertThat(underTest().trashAvailable(), is(true));
  }

  @Test
  public void trashAvailable_whenSchemaStateUnavailable_isFalse() {
    // The TrashSchemaState dependency is optional (@Autowired(required = false)); unit tests that construct the
    // store without Spring leave it null and must not NPE.
    BrowseNodeStore<TestBrowseNodeDAO> store =
        new BrowseNodeStore<>(sessionSupplier, "nexus", TestBrowseNodeDAO.class);

    assertThat(store.trashAvailable(), is(false));
  }
}
