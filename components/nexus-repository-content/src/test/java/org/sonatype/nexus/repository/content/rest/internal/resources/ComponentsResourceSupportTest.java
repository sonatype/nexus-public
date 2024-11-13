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
package org.sonatype.nexus.repository.content.rest.internal.resources;

import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.content.fluent.FluentContinuation;
import org.sonatype.nexus.repository.content.fluent.internal.FluentComponentImpl;
import org.sonatype.nexus.repository.content.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.content.rest.ComponentsResourceExtension;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.rest.api.ComponentXO;
import org.sonatype.nexus.repository.rest.api.ComponentXOFactory;
import org.sonatype.nexus.repository.rest.api.RepositoryManagerRESTAdapter;
import org.sonatype.nexus.repository.selector.ContentAuthHelper;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.upload.UploadConfiguration;
import org.sonatype.nexus.repository.upload.UploadManager;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.content.rest.internal.resources.AssetsResourceSupport.PAGE_SIZE_LIMIT;

public class ComponentsResourceSupportTest
    extends TestSupport
{
  private static final String A_FORMAT = "A_Format";

  // tests assume we've stored more assets than the page limit
  private static final int NUMBER_OF_COMPONENTS = PAGE_SIZE_LIMIT + 2;

  private static final String COMPONENT_NAME = "junit";

  private static final String REPOSITORY_NAME = "repository1";

  private static final String REPOSITORY_URL = "http://localhost:8081/repository/" + REPOSITORY_NAME;

  private static final int COMPONENT_ID = 1001;

  private static final int A_REPOSITORY_ID = 1;

  @Mock
  private ContentFacetSupport contentFacetSupport;

  @Mock
  private Repository repository;

  @Mock
  private ContentFacet contentFacet;

  @Mock
  private ContentAuthHelper contentAuthHelper;

  @Mock
  private RepositoryManagerRESTAdapter repositoryManagerRESTAdapter;

  @Mock
  private MaintenanceService maintenanceService;
  @Mock
  private UploadManager uploadManager;

  @Mock
  private UploadConfiguration uploadConfiguration;

  @Mock
  private ComponentXOFactory componentXOFactory;

  @Mock
  private FluentComponents fluentComponents;

  @Mock
  private Format aFormat;

  @Mock
  private Continuation<FluentComponent> componentContinuation;

  @Spy
  private ComponentsResourceExtension componentsResourceExtension = new TestComponentsResourceExtension();

  private ComponentsResource underTest;

  @Before
  public void setup() {
    mockRepository();
    mockContentFacet();
    mockFluentComponents();

    underTest = new ComponentsResource(repositoryManagerRESTAdapter, maintenanceService, uploadManager,
        uploadConfiguration, componentXOFactory, contentAuthHelper, ImmutableSet.of(componentsResourceExtension), null);
  }

  @Test
  public void browseShouldBeEmptyWhenNoComponents() {
    when(componentContinuation.isEmpty()).thenReturn(true);

    List<FluentComponent> components = underTest.browse(repository, null);

    assertThat(components, empty());
    verify(fluentComponents).browse(PAGE_SIZE_LIMIT, null);
    verify(contentAuthHelper, never()).checkPathPermissions(COMPONENT_NAME, A_FORMAT, REPOSITORY_NAME);
  }

  @Test
  public void browseShouldBeEmptyWhenNoPermittedComponents() {
    when(componentContinuation.isEmpty()).thenReturn(false).thenReturn(true);
    when(contentAuthHelper.checkPathPermissions(COMPONENT_NAME, A_FORMAT, REPOSITORY_NAME)).thenReturn(false);

    List<FluentComponent> components = underTest.browse(repository, null);

    assertThat(components, empty());
    verify(fluentComponents, times(2)).browse(PAGE_SIZE_LIMIT, null);
    verify(contentAuthHelper, times(NUMBER_OF_COMPONENTS))
        .checkPathPermissions(COMPONENT_NAME, A_FORMAT, REPOSITORY_NAME);
  }

  @Test
  public void browseShouldReturnPermittedComponents() {
    int numberOfComponentsNotPermitted = 4;
    int numberOfPermittedComponents = NUMBER_OF_COMPONENTS - numberOfComponentsNotPermitted;

    when(componentContinuation.isEmpty()).thenReturn(false).thenReturn(true);
    when(contentAuthHelper.checkPathPermissions(COMPONENT_NAME, A_FORMAT, REPOSITORY_NAME))
        .thenReturn(false).thenReturn(false, false, false, true);

    List<FluentComponent> components = underTest.browse(repository, null);

    assertThat(components, hasSize(numberOfPermittedComponents));
    verify(fluentComponents, times(2)).browse(PAGE_SIZE_LIMIT, null);
    verify(contentAuthHelper, times(NUMBER_OF_COMPONENTS))
        .checkPathPermissions(COMPONENT_NAME, A_FORMAT, REPOSITORY_NAME);
  }

  @Test
  public void shouldTrimNumberOfComponentsToLimit() {
    when(componentContinuation.isEmpty()).thenReturn(false);
    when(contentAuthHelper.checkPathPermissions(COMPONENT_NAME, A_FORMAT, REPOSITORY_NAME)).thenReturn(true);

    List<FluentComponent> components = underTest.browse(repository, null);

    assertThat(components, hasSize(AssetsResource.PAGE_SIZE_LIMIT));
    verify(contentAuthHelper, times(NUMBER_OF_COMPONENTS))
        .checkPathPermissions(COMPONENT_NAME, A_FORMAT, REPOSITORY_NAME);
  }

  private void mockRepository() {
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(repository.getName()).thenReturn(REPOSITORY_NAME);
    when(repository.getUrl()).thenReturn(REPOSITORY_URL);
    when(repository.getFormat()).thenReturn(aFormat);
    when(repository.getType()).thenReturn(new HostedType());
    when(aFormat.getValue()).thenReturn(A_FORMAT);
  }

  private void mockContentFacet() {
    when(contentFacet.components()).thenReturn(fluentComponents);
    when(contentFacet.contentRepositoryId()).thenReturn(A_REPOSITORY_ID);
  }

  private void mockFluentComponents() {
    when(fluentComponents.browse(PAGE_SIZE_LIMIT, null))
        .thenReturn(new FluentContinuation<>(componentContinuation, asset -> aFluentComponent()));

    List<FluentComponent> fluentComponentList =
        range(0, NUMBER_OF_COMPONENTS).mapToObj(i -> aFluentComponent()).collect(toList());
    when(componentContinuation.iterator()).thenReturn(fluentComponentList.iterator());
  }

  private FluentComponent aFluentComponent() {
    return new FluentComponentImpl(contentFacetSupport, aComponent());
  }

  private ComponentData aComponent() {
    ComponentData componentData = new ComponentData();
    componentData.setComponentId(COMPONENT_ID);
    componentData.setName(COMPONENT_NAME);
    return componentData;
  }

  private class TestComponentsResourceExtension
      implements ComponentsResourceExtension
  {
    @Override
    public ComponentXO updateComponentXO(final ComponentXO componentXO, final FluentComponent component) {
      return componentXO;
    }
  }
}
