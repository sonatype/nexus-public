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
import java.util.stream.Collectors;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.capability.GlobalRepositorySettings;
import org.sonatype.nexus.repository.content.event.component.ComponentPrePurgeEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentPurgedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentsPurgedAuditEvent;
import org.sonatype.nexus.repository.content.facet.ContentFacetFinder;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.internal.FluentComponentImpl;
import org.sonatype.nexus.repository.content.store.ComponentStoreTestSupport.ComponentStoreTestConfiguration;
import org.sonatype.nexus.repository.content.store.example.TestAssetDAO;
import org.sonatype.nexus.repository.content.store.example.TestAssetData;
import org.sonatype.nexus.repository.content.store.example.TestBespokeStoreProvider;
import org.sonatype.nexus.repository.content.store.example.TestComponentDAO;

import jakarta.inject.Provider;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.support.DefaultConversionService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@SpringBootTest(classes = {ComponentStoreTestConfiguration.class})
public abstract class ComponentStoreTestSupport
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

  @Autowired
  ApplicationContext context;

  AnnotationConfigApplicationContext testContext;

  protected void initialiseStores(final boolean entityVersioningEnabled) {
    this.entityVersioningEnabled = entityVersioningEnabled;
    testContext = new AnnotationConfigApplicationContext();
    testContext.setParent(context);
    testContext.registerBean(ContentFacetFinder.class, () -> contentFacetFinder);
    testContext.registerBean(EventManager.class, () -> eventManager);
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
