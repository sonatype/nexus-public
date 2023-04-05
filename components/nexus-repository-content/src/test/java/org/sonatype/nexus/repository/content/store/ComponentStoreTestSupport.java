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
package org.sonatype.nexus.repository.content.store;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.event.component.ComponentPrePurgeEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentPurgedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentsPurgedAuditEvent;
import org.sonatype.nexus.repository.content.facet.ContentFacetFinder;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.internal.FluentComponentImpl;
import org.sonatype.nexus.repository.content.store.example.TestAssetDAO;
import org.sonatype.nexus.repository.content.store.example.TestAssetData;
import org.sonatype.nexus.repository.content.store.example.TestBespokeStoreModule;
import org.sonatype.nexus.repository.content.store.example.TestComponentDAO;
import org.sonatype.nexus.transaction.TransactionModule;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.eclipse.sisu.wire.WireModule;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

public class ComponentStoreTestSupport
    extends ExampleContentTestSupport
{
  private final int componentCount = 201;

  @Mock
  private Repository repository;

  @Mock
  private ContentFacetFinder contentFacetFinder;

  @Mock
  private ContentFacetSupport contentFacetSupport;

  @Mock
  private EventManager eventManager;

  private ComponentStore<TestComponentDAO> underTest;

  private Integer repositoryId;

  private boolean entityVersioningEnabled;

  public void initialiseStores(final boolean entityVersioningEnabled) {
    this.entityVersioningEnabled = entityVersioningEnabled;
    when(contentFacetFinder.findRepository(eq("test"), anyInt())).thenReturn(Optional.of(repository));
    FormatStoreManager fsm = Guice.createInjector(new WireModule(new TestBespokeStoreModule(),
                new AbstractModule()
            {
              @Override
              protected void configure() {
                bind(DataSessionSupplier.class).toInstance(sessionRule);
                bind(ContentFacetFinder.class).toInstance(contentFacetFinder);
                bind(EventManager.class).toInstance(eventManager);
              }
            }, new TransactionModule()))
        .getInstance(Key.get(FormatStoreManager.class, Names.named("test")));;

    underTest = fsm.componentStore(DEFAULT_DATASTORE_NAME);
    generateRandomRepositories(1);
    generateRandomNamespaces(componentCount);
    generateRandomVersions(componentCount);
    repositoryId = generatedRepositories().get(0).repositoryId;

    // create a number of components that require paging
    for (int i=0; i<componentCount; i++) {
      createComponentWithAsset(i);
    }
  }

  public void testPurge_byComponentIds() {
    int[] componentIds = getComponentIds();
    assertThat("Sanity check", componentIds.length, is(componentCount));

    int purged = underTest.purge(repositoryId, componentIds);

    assertThat("Number of purged components should match", purged, is(componentCount));

    assertThat("No components remaining", getComponentIds().length, is(0));

    verify(eventManager, times(3)).post(any(ComponentPrePurgeEvent.class));
    verify(eventManager, times(3)).post(any(ComponentPurgedEvent.class));
    verifyNoMoreInteractions(eventManager);
  }

  public void testPurge_byComponent() {
    List<FluentComponent> componentIds = getComponents();
    assertThat("Sanity check", componentIds, hasSize(componentCount));

    int purged = underTest.purge(repositoryId, componentIds);

    assertThat("Purged should match requested amount", purged, is(componentCount));

    assertThat("No components remaining", getComponentIds().length, is(0));

    verify(eventManager, times(3)).post(any(ComponentsPurgedAuditEvent.class));
    verify(eventManager, times(3)).post(any(ComponentPrePurgeEvent.class));
    verify(eventManager, times(3)).post(any(ComponentPurgedEvent.class));
    verifyNoMoreInteractions(eventManager);
  }

  private int[] getComponentIds() {
    return underTest.browseComponents(Collections.singleton(repositoryId), Integer.MAX_VALUE, null)
        .stream()
        .map(InternalIds::internalComponentId)
        .mapToInt(Integer::valueOf)
        .distinct()
        .toArray();
  }

  private List<FluentComponent> getComponents() {
    return underTest.browseComponents(Collections.singleton(repositoryId), Integer.MAX_VALUE, null)
        .stream()
        .map(cd -> new FluentComponentImpl(contentFacetSupport, cd))
        .map(FluentComponent.class::cast)
        .collect(Collectors.toList());
  }

  private void createComponentWithAsset(final int num) {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ComponentData component = randomComponent(repositoryId, "" + num);
      session.access(TestComponentDAO.class).createComponent(component, entityVersioningEnabled);

      TestAssetData asset = generateAsset(repositoryId, "/" + num);
      asset.setComponent(component);
      session.access(TestAssetDAO.class).createAsset(asset, entityVersioningEnabled);
      session.getTransaction().commit();
    }
  }
}
