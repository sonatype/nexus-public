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
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.capability.GlobalRepositorySettings;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.event.component.ComponentPrePurgeEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentPurgedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentsPurgedAuditEvent;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetFinder;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.internal.FluentAssetsImpl;
import org.sonatype.nexus.repository.content.fluent.internal.FluentComponentImpl;
import org.sonatype.nexus.repository.content.store.ComponentStoreTestSupport.ComponentStoreTestConfiguration;
import org.sonatype.nexus.repository.content.store.example.TestAssetDAO;
import org.sonatype.nexus.repository.content.store.example.TestAssetData;
import org.sonatype.nexus.repository.content.store.example.TestBespokeStoreProvider;
import org.sonatype.nexus.repository.content.store.example.TestComponentDAO;

import jakarta.inject.Provider;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@SpringBootTest(classes = {ComponentStoreTestConfiguration.class})
public abstract class ComponentStoreTestSupport
    extends ExampleContentTestSupport
{
  private final int componentCount = 201;

  @MockitoBean
  private Repository repository;

  @MockitoBean
  private ContentFacetFinder contentFacetFinder;

  @MockitoBean
  private ContentFacetSupport contentFacetSupport;

  @MockitoBean
  private EventManager eventManager;

  private ComponentStore<TestComponentDAO> underTest;

  private Integer repositoryId;

  private boolean entityVersioningEnabled;

  @Autowired
  ApplicationContext context;

  AnnotationConfigApplicationContext testContext;

  protected void initialiseStores(final boolean entityVersioningEnabled) {
    this.entityVersioningEnabled = entityVersioningEnabled;
    testContext = new AnnotationConfigApplicationContext();
    testContext.setParent(context);
    testContext.registerBean(DataSessionSupplier.class, () -> sessionRule);
    testContext.registerBean(GlobalRepositorySettings.class, GlobalRepositorySettings::new);
    new TestBespokeStoreProvider().postProcessBeanDefinitionRegistry(testContext);
    testContext.refresh();

    FormatStoreManager fsm = testContext.getBean(FormatStoreManager.class);

    underTest = fsm.componentStore(DEFAULT_DATASTORE_NAME);
    generateRepositories(1);
    generateNamespaces(componentCount);
    generateVersions(componentCount);
    repositoryId = generatedRepositories().get(0).repositoryId;

    // create a number of components that require paging
    for (int i = 0; i < componentCount; i++) {
      createComponentWithAsset(i);
    }
  }

  protected void testPurge_byComponentIds() {
    int[] componentIds = getComponentIds();
    assertThat("Sanity check", componentIds.length, is(componentCount));

    int purged = underTest.purge(repositoryId, componentIds);

    assertThat("Number of purged components should match", purged, is(componentCount));

    assertThat("No components remaining", getComponentIds().length, is(0));

    verify(eventManager, times(3)).post(any(ComponentPrePurgeEvent.class));
    verify(eventManager, times(3)).post(any(ComponentPurgedEvent.class));
    verifyNoMoreInteractions(eventManager);
  }

  /**
   * Purges without wiring the mocked content facet, so {@code ComponentStore.fetchAssetsFromComponents} short-circuits
   * on its {@code repository() == null} guard and the purge runs with an empty asset list. That deliberately covers the
   * empty-input guard in {@code ComponentStore.resolveAssetComponents}; keep the facet unwired here and see
   * {@link #testPurge_byComponent_resolvesAssetComponents()} for the wired case.
   */
  protected void testPurge_byComponent() {
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

  /**
   * {@link ComponentPurgedEvent} is posted after the purge has been committed, and subscribers such as the IQ
   * {@code RemovedAssetSender} are {@code EventAware.Asynchronous} so they read the event from another thread. By
   * then the purged rows are gone, so any lazily-loaded association left unresolved can no longer be loaded. Assert
   * the event hands subscribers assets whose component is already resolved.
   */
  protected void testPurge_byComponent_resolvesAssetComponents() throws Exception {
    wireContentFacetForAssetLookup();

    List<FluentComponent> components = getComponents();
    assertThat("Sanity check", components, hasSize(componentCount));

    int purged = underTest.purge(repositoryId, components);

    assertThat("Purged should match requested amount", purged, is(componentCount));

    List<FluentAsset> purgedAssets = capturePurgedAssets();
    assertThat("Every purged component should contribute its asset", purgedAssets, hasSize(componentCount));

    // resolve the associations off-thread, exactly as an asynchronous subscriber would
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      List<String> pathsMissingComponent = executor.submit(() -> purgedAssets.stream()
          .filter(asset -> asset.component().isEmpty())
          .map(FluentAsset::path)
          .collect(Collectors.toList())).get();

      assertThat("Purged assets should expose their component to asynchronous subscribers",
          pathsMissingComponent, is(empty()));
    }
    finally {
      executor.shutdown();
    }
  }

  /**
   * {@code ComponentStore.fetchAssetsFromComponents} pages through the assets of each purge batch with a limit of
   * {@code ASSET_BROWSE_LIMIT} (1000). Give every component enough assets that a batch exceeds that limit, so the
   * continuation loop has to run more than once, and assert no asset is dropped or handed to subscribers twice.
   */
  protected void testPurge_byComponent_browsesEveryAssetPage() {
    wireContentFacetForAssetLookup();

    // a purge batch holds up to 100 components (nexus.component.purge.size), so 11 assets each exceeds the 1000
    // asset browse limit and forces a second page
    int assetsPerComponent = 11;
    createAdditionalAssets(assetsPerComponent - 1);

    List<FluentComponent> components = getComponents();
    assertThat("Sanity check", components, hasSize(componentCount));

    // purge returns the row count of the asset delete rather than the component count, so assert on the components
    // that are left instead
    underTest.purge(repositoryId, components);

    assertThat("No components remaining", getComponentIds().length, is(0));

    List<String> purgedPaths = capturePurgedAssets().stream().map(FluentAsset::path).collect(Collectors.toList());
    assertThat("Every asset page should be browsed", purgedPaths, hasSize(componentCount * assetsPerComponent));
    assertThat("No asset should be browsed twice", Set.copyOf(purgedPaths),
        hasSize(componentCount * assetsPerComponent));
  }

  /**
   * Stubs the mocked content facet just enough for {@code ComponentStore.fetchAssetsFromComponents} to run against
   * the real {@link AssetStore}; otherwise it short-circuits on its {@code repository() == null} guard and the purge
   * event always carries an empty asset list.
   */
  private void wireContentFacetForAssetLookup() {
    AssetStore<?> assetStore = testContext.getBean(FormatStoreManager.class).assetStore(DEFAULT_DATASTORE_NAME);
    when(contentFacetSupport.repository()).thenReturn(repository);
    when(contentFacetSupport.contentRepositoryId()).thenReturn(repositoryId);
    when(contentFacetSupport.assets()).thenReturn(new FluentAssetsImpl(contentFacetSupport, assetStore));
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacetSupport);
  }

  private List<FluentAsset> capturePurgedAssets() {
    ArgumentCaptor<Object> events = ArgumentCaptor.forClass(Object.class);
    verify(eventManager, atLeastOnce()).post(events.capture());
    return events.getAllValues()
        .stream()
        .filter(ComponentPurgedEvent.class::isInstance)
        .map(ComponentPurgedEvent.class::cast)
        .map(ComponentPurgedEvent::getAssets)
        .flatMap(List::stream)
        .collect(Collectors.toList());
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

  private void createAdditionalAssets(final int assetsPerComponent) {
    List<Component> components =
        underTest.browseComponents(Collections.singleton(repositoryId), Integer.MAX_VALUE, null)
            .stream()
            .collect(Collectors.toList());

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      for (Component component : components) {
        int componentId = InternalIds.internalComponentId(component);
        for (int i = 0; i < assetsPerComponent; i++) {
          TestAssetData asset = generateAsset(repositoryId, "/" + componentId + "/extra" + i);
          asset.setComponent(component);
          session.access(TestAssetDAO.class).createAsset(asset, entityVersioningEnabled);
        }
      }
      session.getTransaction().commit();
    }
  }

  private void createComponentWithAsset(final int num) {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ComponentData component = generateComponent(repositoryId, "namespace" + num, "" + num, "1.0." + num);
      session.access(TestComponentDAO.class).createComponent(component, entityVersioningEnabled);

      TestAssetData asset = generateAsset(repositoryId, "/" + num);
      asset.setComponent(component);
      session.access(TestAssetDAO.class).createAsset(asset, entityVersioningEnabled);
      session.getTransaction().commit();
    }
  }

  protected static class ComponentStoreTestConfiguration
  {
    @Bean
    ConversionService conversionService() {
      DefaultConversionService service = new DefaultConversionService();
      service.addConverter(new GenericConverter()
      {

        @Override
        public Set<ConvertiblePair> getConvertibleTypes() {
          return Set.of(new ConvertiblePair(Object.class, Provider.class));
        }

        @Override
        public Object convert(final Object source, final TypeDescriptor sourceType, final TypeDescriptor targetType) {
          return (Provider) () -> source;
        }
      });
      return service;
    }
  }
}
