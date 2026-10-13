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
package org.sonatype.nexus.repository.config.upgrade;

import java.sql.Connection;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Covers {@link RepositoryBlobStoreNameIndexMigrationStep_2_170}. The DatabaseMigrationStep dialect helpers
 * ({@code isPostgresql}, {@code tableExists}, {@code indexExists}, {@code runStatement}) are stubbed on a spy so
 * each branch of {@code migrate()} is exercised without a live database.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RepositoryBlobStoreNameIndexMigrationStep_2_170Test
{
  private static final String TABLE = "repository";

  private static final String INDEX = "idx_repository_blob_store_name";

  @Mock
  private Connection connection;

  private RepositoryBlobStoreNameIndexMigrationStep_2_170 underTest;

  @BeforeEach
  public void setUp() {
    underTest = spy(new RepositoryBlobStoreNameIndexMigrationStep_2_170());
  }

  @Test
  public void testVersion() {
    assertThat(underTest.version(), is(Optional.of("2.170")));
  }

  @Test
  public void createsExpressionIndexOnPostgresWhenMissing() throws Exception {
    doReturn(true).when(underTest).isPostgresql(connection);
    doReturn(true).when(underTest).tableExists(connection, TABLE);
    doReturn(false).when(underTest).indexExists(connection, TABLE, INDEX);
    doReturn(0).when(underTest).runStatement(eq(connection), anyString());

    underTest.migrate(connection);

    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(underTest).runStatement(eq(connection), sql.capture());
    assertThat(sql.getValue(), containsString("CREATE INDEX"));
    assertThat(sql.getValue(), containsString(INDEX));
    assertThat(sql.getValue(), containsString("attributes -> 'storage' ->> 'blobStoreName'"));
  }

  @Test
  public void skipsWhenIndexAlreadyExists() throws Exception {
    doReturn(true).when(underTest).isPostgresql(connection);
    doReturn(true).when(underTest).tableExists(connection, TABLE);
    doReturn(true).when(underTest).indexExists(connection, TABLE, INDEX);

    underTest.migrate(connection);

    verify(underTest, never()).runStatement(eq(connection), anyString());
  }

  @Test
  public void skipsWhenRepositoryTableMissing() throws Exception {
    doReturn(true).when(underTest).isPostgresql(connection);
    doReturn(false).when(underTest).tableExists(connection, TABLE);

    underTest.migrate(connection);

    verify(underTest, never()).runStatement(eq(connection), anyString());
  }

  @Test
  public void isNoOpOnH2() throws Exception {
    doReturn(false).when(underTest).isPostgresql(connection);

    underTest.migrate(connection);

    verify(underTest, never()).runStatement(eq(connection), anyString());
  }
}
