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
package org.sonatype.nexus.api.common.shared.state;

import java.util.Map;

import org.sonatype.nexus.common.db.DatabaseCheck;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DatastoreStateContributorTest
{
  @Test
  public void datastoreStateContributorExposesIsPostgresqlState() {
    DatabaseCheck dbCheck = mock(DatabaseCheck.class);
    when(dbCheck.isPostgresql()).thenReturn(true);

    DatastoreStateContributor contributor = new DatastoreStateContributor(dbCheck);

    assertThat(contributor.getState().get("datastore.isPostgresql"), is(true));
  }

  @Test
  public void datastoreStateContributorExposesNonPostgresqlState() {
    DatabaseCheck dbCheck = mock(DatabaseCheck.class);
    when(dbCheck.isPostgresql()).thenReturn(false);

    DatastoreStateContributor contributor = new DatastoreStateContributor(dbCheck);

    assertThat(contributor.getState().get("datastore.isPostgresql"), is(false));
  }

  @Test
  public void datastoreStateContributorExposesStaticDatastoreFlags() {
    Map<String, Object> state = new DatastoreStateContributor(mock(DatabaseCheck.class)).getState();

    assertThat(state.get("nexus.datastore.enabled"), is(true));
    assertThat(state.get("nexus.datastore.developer"), is(false));
  }

  @Test
  public void databaseCheckIsNotQueriedUntilGetStateIsCalled() {
    DatabaseCheck dbCheck = mock(DatabaseCheck.class);

    DatastoreStateContributor contributor = new DatastoreStateContributor(dbCheck);
    // isPostgresql() is @Guarded(by = STARTED); it must not be read at construction time,
    // only lazily on the first getState() call (well after the STORAGE lifecycle phase).
    verify(dbCheck, never()).isPostgresql();

    when(dbCheck.isPostgresql()).thenReturn(true);
    assertThat(contributor.getState().get("datastore.isPostgresql"), is(true));
  }
}
