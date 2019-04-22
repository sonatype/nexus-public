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
package org.sonatype.nexus.cleanup.storage;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.cleanup.service.CleanupComponentBrowse;
import org.sonatype.nexus.extdirect.model.PagedResponse;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters.Filter;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters.Sort;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.rest.api.ComponentXO;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.DefaultComponent;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.sonatype.nexus.cleanup.config.CleanupPolicyConfiguration;
import org.sonatype.nexus.cleanup.config.DefaultCleanupPolicyConfiguration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.LAST_BLOB_UPDATED_KEY;

public class CleanupPolicyComponentTest
    extends TestSupport
{
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String FORMAT = "format";

  private static final String DEFAULT = "default";

  @Mock
  private CleanupPolicyStorage cleanupPolicyStorage;

  @Mock
  private CleanupComponentBrowse cleanupComponentBrowse;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private Repository repository;

  @Mock
  private StorageFacet storageFacet;

  @Mock
  private StorageTx tx;

  @Mock
  private RepositoryPermissionChecker repositoryPermissionChecker;

  private CleanupPolicyPreviewXO policyPreviewXO;

  private StoreLoadParameters storeLoadParameters;

  private String policy;

  private PagedResponse<Component> componentsFromSearch;

  @Mock
  private DefaultCleanupPolicyConfiguration defaultCleanupPolicyConfiguration;

  @Mock
  private CleanupPolicyConfiguration configuration;

  private CleanupPolicyComponent underTest;

  @Before
  public void setUp() throws Exception {
    policyPreviewXO = new CleanupPolicyPreviewXO();
    policyPreviewXO.setRepositoryName("repositoryName");
    policyPreviewXO.setCriteria(new CleanupPolicyCriteria());

    policy = MAPPER.writeValueAsString(policyPreviewXO);

    Filter filter = new Filter();
    filter.setProperty("cleanupPolicy");
    filter.setValue(policy);
    storeLoadParameters = new StoreLoadParameters();
    storeLoadParameters.setFilter(ImmutableList.of(filter));
    storeLoadParameters.setSort(ImmutableList.of(new Sort()));

    DefaultComponent component = new DefaultComponent();
    component.name("name");
    component.version("1.0.0");
    component.group("group");
    component.format("format");
    componentsFromSearch = new PagedResponse<>(1, ImmutableList.of(component));

    when(storageFacet.txSupplier()).thenReturn(() -> tx);
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);

    underTest = new CleanupPolicyComponent(cleanupPolicyStorage, cleanupComponentBrowse, repositoryManager,
        ImmutableMap.of(FORMAT, configuration, DEFAULT, defaultCleanupPolicyConfiguration),
        repositoryPermissionChecker);

    when(configuration.getConfiguration()).thenReturn(ImmutableMap.of(LAST_BLOB_UPDATED_KEY, true));
  }

  @Test
  public void shouldReturnNullIfNoCleanupPolicySet() throws Exception {
    StoreLoadParameters emptyParameters = mock(StoreLoadParameters.class);

    when(emptyParameters.getFilter("cleanupPolicy")).thenReturn(null);

    underTest.previewCleanup(emptyParameters);
  }

  @Test
  public void shouldReturnNullIfNoSortSet() throws Exception {
    StoreLoadParameters emptyParameters = mock(StoreLoadParameters.class);

    when(emptyParameters.getFilter("cleanupPolicy")).thenReturn("policy");
    when(emptyParameters.getSort()).thenReturn(null);

    underTest.previewCleanup(emptyParameters);
  }

  @Test
  public void whenNoRepositorySpecifiedReturnNull() throws Exception {
    when(repositoryManager.get(policyPreviewXO.getRepositoryName())).thenReturn(null);

    assertThat(underTest.previewCleanup(storeLoadParameters), is(nullValue()));
  }

  @Test
  public void whenValidRequestSearchResponseIsReturned() throws Exception {
    when(repositoryManager.get(policyPreviewXO.getRepositoryName())).thenReturn(repository);

    when(cleanupComponentBrowse.browseByPage(any(), any(), any())).thenReturn(componentsFromSearch);

    PagedResponse<ComponentXO> response = underTest.previewCleanup(storeLoadParameters);

    assertThat(response.getTotal(), is(1L));
    assertThat(response.getData().iterator().next().getName(), is("name"));
  }

  @Test
  public void returnTrueWhenFieldApplicable() throws Exception {
    when(defaultCleanupPolicyConfiguration.getConfiguration()).thenReturn(ImmutableMap.of(LAST_BLOB_UPDATED_KEY, true));

    assertThat(
        underTest.getApplicableFields(ImmutableList.of(LAST_BLOB_UPDATED_KEY)).get(FORMAT).get(LAST_BLOB_UPDATED_KEY),
        is(true));
  }

  @Test
  public void returnFalseWhenFieldNotApplicable() throws Exception {
    when(defaultCleanupPolicyConfiguration.getConfiguration()).thenReturn(ImmutableMap.of(LAST_BLOB_UPDATED_KEY, true));

    when(configuration.getConfiguration()).thenReturn(ImmutableMap.of(LAST_BLOB_UPDATED_KEY, false));

    assertThat(
        underTest.getApplicableFields(ImmutableList.of(LAST_BLOB_UPDATED_KEY)).get(FORMAT).get(LAST_BLOB_UPDATED_KEY),
        is(false));
  }

  @Test
  public void returnDefaultWhenFieldNotFound() throws Exception {
    when(defaultCleanupPolicyConfiguration.getConfiguration()).thenReturn(ImmutableMap.of(LAST_BLOB_UPDATED_KEY, true));
    
    when(configuration.getConfiguration()).thenReturn(ImmutableMap.of());

    assertThat(
        underTest.getApplicableFields(ImmutableList.of(LAST_BLOB_UPDATED_KEY)).get(FORMAT).get(LAST_BLOB_UPDATED_KEY),
        is(true));
  }
}
