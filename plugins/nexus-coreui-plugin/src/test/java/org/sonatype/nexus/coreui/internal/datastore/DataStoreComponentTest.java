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
package org.sonatype.nexus.coreui.internal.datastore;

import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.DataStoreConfiguration;
import org.sonatype.nexus.datastore.api.DataStoreManager;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class DataStoreComponentTest
    extends TestSupport
{
  @Mock
  private DataStoreManager dataStoreManager;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private RepositoryPermissionChecker repositoryPermissionChecker;

  @Mock
  private DataStore<?> h2DataStore;

  @Mock
  private DataStore<?> postgresqlDataStore;

  private DataStoreComponent underTest;

  @Before
  public void setup() {
    DataStoreConfiguration contentConfig = new DataStoreConfiguration();
    contentConfig.setName("content");
    contentConfig.setType("jdbc");
    contentConfig.setSource("local");
    contentConfig.setAttributes(ImmutableMap.of("jdbcUrl", "jdbc:h2:som/datastore/url"));

    DataStoreConfiguration configConfig = new DataStoreConfiguration();
    configConfig.setName("config");
    configConfig.setType("jdbc");
    configConfig.setSource("local");
    configConfig.setAttributes(ImmutableMap.of("jdbcUrl", "jdbc:postgresql:some/datastore/url"));

    when(h2DataStore.getConfiguration()).thenReturn(contentConfig);
    when(postgresqlDataStore.getConfiguration()).thenReturn(configConfig);

    when(dataStoreManager.browse()).thenReturn(asList(h2DataStore, postgresqlDataStore));

    underTest = new DataStoreComponent(dataStoreManager, repositoryManager, repositoryPermissionChecker, true);
  }

  @Test
  public void testReadingDatabase() {
    List<DataStoreXO> dataStores = underTest.read();
    assertThat(dataStores, hasSize(2));
    assertThat(dataStores.get(0).getName(), is("content"));
    assertThat(dataStores.get(1).getName(), is("config"));
  }

  @Test
  public void testReadingH2Database() {
    List<DataStoreXO> dataStores = underTest.readH2();
    assertThat(dataStores, hasSize(1));
    assertThat(dataStores.get(0).getName(), is("content"));
  }
}
