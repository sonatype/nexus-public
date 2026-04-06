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

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityUUID;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.time.UTC;
import org.sonatype.nexus.datastore.api.ContentDataAccess;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AttributeChangeSet;
import org.sonatype.nexus.repository.content.event.asset.AssetAttributesEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetCreatedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetDeletedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetDownloadedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetKindEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetPreDeleteEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetPrePurgeEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetPurgedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetUpdatedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentAttributesEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentCreatedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentDeletedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentKindEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentPreDeleteEvent;
import org.sonatype.nexus.repository.content.event.repository.ContentRepositoryCreatedEvent;
import org.sonatype.nexus.repository.content.event.repository.ContentRepositoryDeletedEvent;
import org.sonatype.nexus.repository.content.event.repository.ContentRepositoryPreDeleteEvent;
import org.sonatype.nexus.repository.content.facet.ContentFacetFinder;
import org.sonatype.nexus.repository.content.store.example.TestAssetBlobDAO;
import org.sonatype.nexus.repository.content.store.example.TestAssetDAO;
import org.sonatype.nexus.repository.content.store.example.TestAssetData;
import org.sonatype.nexus.repository.content.store.example.TestAssetStore;
import org.sonatype.nexus.repository.content.store.example.TestBespokeStoreProvider;
import org.sonatype.nexus.repository.content.store.example.TestComponentDAO;
import org.sonatype.nexus.repository.content.store.example.TestContentRepositoryDAO;
import org.sonatype.nexus.repository.content.store.example.TestPlainStoreProvider;
import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import jakarta.inject.Provider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.sonatype.nexus.testdb.DatabaseTest;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.support.DefaultConversionService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.datastore.mybatis.CombUUID.combUUID;
import static org.sonatype.nexus.repository.content.AttributeOperation.SET;

/**
 * Test {@link FormatStoreManager}.
 */
@SpringBootTest
class FormatStoreManagerTest
    extends Test5Support
{
  private static final String NODE_ID = "ab761d55-5d9c22b6-3f38315a-75b3db34-0922a4d5";

  private static final String BLOB_ID = "a8f3f56f-e895-4b6e-984a-1cf1f5107d36";

  @DataSessionConfiguration(
      daos = {TestContentRepositoryDAO.class, TestComponentDAO.class, TestAssetBlobDAO.class, TestAssetDAO.class},
      typeHandlers = BlobRefTypeHandler.class)
  TestDataSessionSupplier dataSessionSupplier;

  @Mock
  Repository repository;

  @Mock
  ContentFacetFinder contentFacetFinder;

  @Mock
  EventManager eventManager;

  @Autowired
  ApplicationContext context;

  AnnotationConfigApplicationContext testContext;

  @BeforeEach
  void setUp() {
    lenient().when(contentFacetFinder.findRepository(eq("test"), anyInt())).thenReturn(Optional.of(repository));
    testContext = new AnnotationConfigApplicationContext();
    testContext.setParent(context);
    testContext.registerBean(ContentFacetFinder.class, () -> contentFacetFinder);
    testContext.registerBean(EventManager.class, () -> eventManager);
    testContext.registerBean(DataSessionSupplier.class, () -> dataSessionSupplier);
  }

  @AfterEach
  void teardown() {
    testContext.close();
  }

  @DatabaseTest
  void testPlainBindings() {
    FormatStoreManager underTest = getFormatStoreManager(new TestPlainStoreProvider());

    ContentRepositoryStore<?> contentRepositoryStore = underTest.contentRepositoryStore(DEFAULT_DATASTORE_NAME);
    ComponentStore<?> componentStore = underTest.componentStore(DEFAULT_DATASTORE_NAME);
    AssetStore<?> assetStore = underTest.assetStore(DEFAULT_DATASTORE_NAME);
    AssetBlobStore<?> assetBlobStore = underTest.assetBlobStore(DEFAULT_DATASTORE_NAME);

    // check the appropriate DAOs have been bound
    assertDaoBinding(contentRepositoryStore, TestContentRepositoryDAO.class);
    assertDaoBinding(componentStore, TestComponentDAO.class);
    assertDaoBinding(assetStore, TestAssetDAO.class);
    assertDaoBinding(assetBlobStore, TestAssetBlobDAO.class);

    // check that previously requested stores are cached to keep creation costs down
    assertThat(underTest.contentRepositoryStore(DEFAULT_DATASTORE_NAME), sameInstance(contentRepositoryStore));
    assertThat(underTest.componentStore(DEFAULT_DATASTORE_NAME), sameInstance(componentStore));
    assertThat(underTest.assetStore(DEFAULT_DATASTORE_NAME), sameInstance(assetStore));
    assertThat(underTest.assetBlobStore(DEFAULT_DATASTORE_NAME), sameInstance(assetBlobStore));
  }

  @DatabaseTest
  void testPlainOperations() {
    FormatStoreManager underTest = getFormatStoreManager(new TestPlainStoreProvider());

    ContentRepositoryStore<?> contentRepositoryStore = underTest.contentRepositoryStore(DEFAULT_DATASTORE_NAME);
    ComponentStore<?> componentStore = underTest.componentStore(DEFAULT_DATASTORE_NAME);
    AssetStore<?> assetStore = underTest.assetStore(DEFAULT_DATASTORE_NAME);
    AssetBlobStore<?> assetBlobStore = underTest.assetBlobStore(DEFAULT_DATASTORE_NAME);

    ContentRepositoryData repository = new ContentRepositoryData();
    repository.setAttributes(new NestedAttributesMap("attributes", new HashMap<>()));
    repository.setConfigRepositoryId(new EntityUUID(combUUID()));
    contentRepositoryStore.createContentRepository(repository);

    ComponentData component = new ComponentData();
    component.setAttributes(new NestedAttributesMap("attributes", new HashMap<>()));
    component.setRepositoryId(repository.repositoryId);
    component.setNamespace("");
    component.setName("testComponent");
    component.setKind("aKind");
    component.setVersion("1.0");
    component.setNormalizedVersion("0000000001.0000000000");
    component.setLastUpdated(OffsetDateTime.now());
    componentStore.createComponent(component);

    AssetData asset = new AssetData();
    asset.setAttributes(new NestedAttributesMap("attributes", new HashMap<>()));
    asset.setRepositoryId(repository.repositoryId);
    asset.setComponent(component);
    asset.setPath("/path/to/asset");
    asset.setKind("test");
    asset.setLastUpdated(OffsetDateTime.now());
    assetStore.createAsset(asset);

    AssetBlobData assetBlob = new AssetBlobData();
    assetBlob.setBlobRef(new BlobRef(NODE_ID, "default", BLOB_ID));
    assetBlob.setBlobSize(0);
    assetBlob.setContentType("text/plain");
    assetBlob.setChecksums(ImmutableMap.of());
    assetBlob.setBlobCreated(UTC.now());
    assetBlobStore.createAssetBlob(assetBlob);

    asset.setAssetBlob(assetBlob);
    assetStore.updateAssetBlobLink(asset);

    Optional<Asset> result = assetStore.readPath(repository.repositoryId, "/path/to/asset");
    assertThat(result.get().component().get().name(), is("testComponent"));
    assertThat(result.get().blob().get().blobRef().getBlob(), is(BLOB_ID));
  }

  @DatabaseTest
  void testBespokeBindings() {
    FormatStoreManager underTest = getFormatStoreManager(new TestBespokeStoreProvider());

    ContentRepositoryStore<?> contentRepositoryStore = underTest.contentRepositoryStore(DEFAULT_DATASTORE_NAME);
    ComponentStore<?> componentStore = underTest.componentStore(DEFAULT_DATASTORE_NAME);
    AssetStore<?> assetStore = underTest.assetStore(DEFAULT_DATASTORE_NAME);
    AssetBlobStore<?> assetBlobStore = underTest.assetBlobStore(DEFAULT_DATASTORE_NAME);

    // check the appropriate DAOs have been bound
    assertDaoBinding(contentRepositoryStore, TestContentRepositoryDAO.class);
    assertDaoBinding(componentStore, TestComponentDAO.class);
    assertDaoBinding(assetStore, TestAssetDAO.class);
    assertDaoBinding(assetBlobStore, TestAssetBlobDAO.class);

    // check that the asset store is the bespoke version
    assertThat(contentRepositoryStore, isA(ContentRepositoryStore.class));
    assertThat(componentStore, isA(ComponentStore.class));
    assertThat(assetStore, is(instanceOf(TestAssetStore.class)));
    assertThat(assetBlobStore, isA(AssetBlobStore.class));
  }

  @DatabaseTest
  void testBespokeOperations() {
    // our bespoke schema will be applied automatically via 'extendSchema'...
    FormatStoreManager underTest = getFormatStoreManager(new TestBespokeStoreProvider());

    ContentRepositoryStore<?> contentRepositoryStore = underTest.contentRepositoryStore(DEFAULT_DATASTORE_NAME);
    TestAssetStore assetStore = underTest.assetStore(DEFAULT_DATASTORE_NAME);

    ContentRepositoryData repository = new ContentRepositoryData();
    repository.setAttributes(new NestedAttributesMap("attributes", new HashMap<>()));
    repository.setConfigRepositoryId(new EntityUUID(combUUID()));
    contentRepositoryStore.createContentRepository(repository);

    TestAssetData asset = new TestAssetData();
    asset.setAttributes(new NestedAttributesMap("attributes", new HashMap<>()));
    asset.setRepositoryId(repository.repositoryId);
    asset.setPath("/path/to/asset");
    asset.setKind("test");
    asset.setLastUpdated(OffsetDateTime.now());
    assetStore.createAsset(asset);

    assertThat(assetStore.browseFlaggedAssets(repository.repositoryId, 10, null), is(emptyIterable()));

    asset.setTestFlag(true);
    assetStore.updateAssetFlag(asset);

    Optional<Asset> result = assetStore.browseFlaggedAssets(repository.repositoryId, 10, null).stream().findFirst();
    assertThat(result.get().path(), is("/path/to/asset"));
  }

  @DatabaseTest
  void testEventing() {
    FormatStoreManager underTest = getFormatStoreManager(new TestPlainStoreProvider());

    ContentRepositoryStore<?> contentRepositoryStore = underTest.contentRepositoryStore(DEFAULT_DATASTORE_NAME);
    ComponentStore<?> componentStore = underTest.componentStore(DEFAULT_DATASTORE_NAME);
    AssetStore<?> assetStore = underTest.assetStore(DEFAULT_DATASTORE_NAME);
    AssetBlobStore<?> assetBlobStore = underTest.assetBlobStore(DEFAULT_DATASTORE_NAME);

    ContentRepositoryData repository = new ContentRepositoryData();
    repository.setAttributes(new NestedAttributesMap("attributes", new HashMap<>()));
    repository.setConfigRepositoryId(new EntityUUID(combUUID()));
    contentRepositoryStore.createContentRepository(repository);

    ComponentData component = new ComponentData();
    component.setAttributes(new NestedAttributesMap("attributes", new HashMap<>()));
    component.setRepositoryId(repository.repositoryId);
    component.setNamespace("");
    component.setName("testComponent");
    component.setKind("aKind");
    component.setVersion("1.0");
    component.setNormalizedVersion("000000001.0000000000");
    component.setLastUpdated(OffsetDateTime.now());
    componentStore.createComponent(component);

    AssetData asset = new AssetData();
    asset.setAttributes(new NestedAttributesMap("attributes", new HashMap<>()));
    asset.setRepositoryId(repository.repositoryId);
    asset.setComponent(component);
    asset.setPath("/path/to/asset");
    asset.setKind("test");
    asset.setLastUpdated(OffsetDateTime.now());
    assetStore.createAsset(asset);

    AssetBlobData assetBlob = new AssetBlobData();
    assetBlob.setBlobRef(new BlobRef(NODE_ID, "default", BLOB_ID));
    assetBlob.setBlobSize(0);
    assetBlob.setContentType("text/plain");
    assetBlob.setChecksums(ImmutableMap.of());
    assetBlob.setBlobCreated(UTC.now());
    assetBlobStore.createAssetBlob(assetBlob);

    asset.setKind("jar");
    asset.setAssetBlob(assetBlob);

    assetStore.updateAssetAttributes(asset, new AttributeChangeSet(SET, "test-key", "test-asset"));
    assetStore.updateAssetKind(asset);
    assetStore.updateAssetBlobLink(asset);
    assetStore.markAsDownloaded(asset);

    component.setKind("pom");

    componentStore.updateComponentAttributes(component, SET, "test-key", "test-component");
    componentStore.updateComponentKind(component);
    assetStore.deleteAsset(asset);
    componentStore.deleteComponent(component);
    contentRepositoryStore.deleteContentRepository(repository);

    ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);

    verify(eventManager, times(15)).post(eventCaptor.capture());

    List<Object> events = eventCaptor.getAllValues();

    assertThat(events.get(0), instanceOf(ContentRepositoryCreatedEvent.class));
    assertThat(events.get(1), instanceOf(ComponentCreatedEvent.class));
    assertThat(events.get(2), instanceOf(AssetCreatedEvent.class));
    assertThat(events.get(3), instanceOf(AssetAttributesEvent.class));
    assertThat(events.get(4), instanceOf(AssetKindEvent.class));
    assertThat(events.get(5), instanceOf(AssetUpdatedEvent.class));
    assertThat(events.get(6), instanceOf(AssetDownloadedEvent.class));
    assertThat(events.get(7), instanceOf(ComponentAttributesEvent.class));
    assertThat(events.get(8), instanceOf(ComponentKindEvent.class));
    assertThat(events.get(9), instanceOf(AssetPreDeleteEvent.class));
    assertThat(events.get(10), instanceOf(AssetDeletedEvent.class));
    assertThat(events.get(11), instanceOf(ComponentPreDeleteEvent.class));
    assertThat(events.get(12), instanceOf(ComponentDeletedEvent.class));
    assertThat(events.get(13), instanceOf(ContentRepositoryPreDeleteEvent.class));
    assertThat(events.get(14), instanceOf(ContentRepositoryDeletedEvent.class));

    verifyNoMoreInteractions(eventManager);
  }

  @DatabaseTest
  void testPurgeEvent() {
    FormatStoreManager underTest = getFormatStoreManager(new TestPlainStoreProvider());

    ContentRepositoryStore<?> contentRepositoryStore = underTest.contentRepositoryStore(DEFAULT_DATASTORE_NAME);
    ComponentStore<?> componentStore = underTest.componentStore(DEFAULT_DATASTORE_NAME);
    AssetStore<?> assetStore = underTest.assetStore(DEFAULT_DATASTORE_NAME);
    AssetBlobStore<?> assetBlobStore = underTest.assetBlobStore(DEFAULT_DATASTORE_NAME);

    ContentRepositoryData repository = new ContentRepositoryData();
    repository.setAttributes(new NestedAttributesMap("attributes", new HashMap<>()));
    repository.setConfigRepositoryId(new EntityUUID(combUUID()));
    contentRepositoryStore.createContentRepository(repository);

    ComponentData component = new ComponentData();
    component.setAttributes(new NestedAttributesMap("attributes", new HashMap<>()));
    component.setRepositoryId(repository.repositoryId);
    component.setNamespace("");
    component.setName("testComponent");
    component.setKind("aKind");
    component.setVersion("1.0");
    component.setNormalizedVersion("000000001.0000000000");
    component.setLastUpdated(OffsetDateTime.now());
    componentStore.createComponent(component);

    AssetData asset = new AssetData();
    asset.setAttributes(new NestedAttributesMap("attributes", new HashMap<>()));
    asset.setRepositoryId(repository.repositoryId);
    asset.setComponent(component);
    asset.setPath("/path/to/asset");
    asset.setKind("test");
    asset.setLastUpdated(OffsetDateTime.now());
    assetStore.createAsset(asset);

    AssetBlobData assetBlob = new AssetBlobData();
    assetBlob.setBlobRef(new BlobRef(NODE_ID, "default", BLOB_ID));
    assetBlob.setBlobSize(0);
    assetBlob.setContentType("text/plain");
    assetBlob.setChecksums(ImmutableMap.of());
    assetBlob.setBlobCreated(UTC.now());
    assetBlobStore.createAssetBlob(assetBlob);

    asset.setKind("jar");
    asset.setAssetBlob(assetBlob);

    assetStore.updateAssetAttributes(asset, new AttributeChangeSet(SET, "test-key", "test-asset"));
    assetStore.updateAssetKind(asset);
    assetStore.updateAssetBlobLink(asset);
    assetStore.markAsDownloaded(asset);

    component.setKind("pom");

    componentStore.updateComponentAttributes(component, SET, "test-key", "test-component");
    componentStore.updateComponentKind(component);

    assetStore.deleteAssetsByPaths(repository.repositoryId, Lists.newArrayList(asset.path()));

    componentStore.deleteComponent(component);
    contentRepositoryStore.deleteContentRepository(repository);

    ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);

    verify(eventManager, times(15)).post(eventCaptor.capture());

    List<Object> events = eventCaptor.getAllValues();

    assertThat(events.get(0), instanceOf(ContentRepositoryCreatedEvent.class));
    assertThat(events.get(1), instanceOf(ComponentCreatedEvent.class));
    assertThat(events.get(2), instanceOf(AssetCreatedEvent.class));
    assertThat(events.get(3), instanceOf(AssetAttributesEvent.class));
    assertThat(events.get(4), instanceOf(AssetKindEvent.class));
    assertThat(events.get(5), instanceOf(AssetUpdatedEvent.class));
    assertThat(events.get(6), instanceOf(AssetDownloadedEvent.class));
    assertThat(events.get(7), instanceOf(ComponentAttributesEvent.class));
    assertThat(events.get(8), instanceOf(ComponentKindEvent.class));
    assertThat(events.get(9), instanceOf(AssetPrePurgeEvent.class));
    assertThat(events.get(10), instanceOf(AssetPurgedEvent.class));
    assertThat(events.get(11), instanceOf(ComponentPreDeleteEvent.class));
    assertThat(events.get(12), instanceOf(ComponentDeletedEvent.class));
    assertThat(events.get(13), instanceOf(ContentRepositoryPreDeleteEvent.class));
    assertThat(events.get(14), instanceOf(ContentRepositoryDeletedEvent.class));

    verifyNoMoreInteractions(eventManager);
  }

  @DatabaseTest
  void testFindByComponentIds() {
    FormatStoreManager underTest = getFormatStoreManager(new TestPlainStoreProvider());

    ContentRepositoryStore<?> contentRepositoryStore = underTest.contentRepositoryStore(DEFAULT_DATASTORE_NAME);
    AssetBlobStore<?> assetBlobStore = underTest.assetBlobStore(DEFAULT_DATASTORE_NAME);
    AssetStore<?> assetStore = underTest.assetStore(DEFAULT_DATASTORE_NAME);
    ComponentStore<?> componentStore = underTest.componentStore(DEFAULT_DATASTORE_NAME);

    ContentRepositoryData repository = new ContentRepositoryData();
    repository.setAttributes(new NestedAttributesMap("attributes", new HashMap<>()));
    repository.setConfigRepositoryId(new EntityUUID(combUUID()));
    contentRepositoryStore.createContentRepository(repository);

    ComponentData component = new ComponentData();
    component.setAttributes(new NestedAttributesMap("attributes", new HashMap<>()));
    component.setRepositoryId(repository.repositoryId);
    component.setNamespace("");
    component.setName("testComponent");
    component.setKind("aKind");
    component.setVersion("1.0");
    component.setNormalizedVersion("000000001.0000000000");
    component.setLastUpdated(OffsetDateTime.now());
    componentStore.createComponent(component);

    AssetBlobData assetBlob = new AssetBlobData();
    assetBlob.setBlobRef(new BlobRef(NODE_ID, "default", BLOB_ID));
    assetBlob.setBlobSize(0);
    assetBlob.setContentType("text/plain");
    assetBlob.setChecksums(ImmutableMap.of());
    assetBlob.setBlobCreated(UTC.now());
    assetBlobStore.createAssetBlob(assetBlob);

    AssetData asset = new AssetData();
    asset.setAttributes(new NestedAttributesMap("attributes", new HashMap<>()));
    asset.setRepositoryId(repository.repositoryId);
    asset.setComponent(component);
    asset.setPath("/path/to/asset");
    asset.setKind("test");
    asset.setAssetBlob(assetBlob);
    asset.setLastUpdated(OffsetDateTime.now());
    assetStore.createAsset(asset);

    Collection<Asset> assets = assetStore.findByComponentIds(Collections.singleton(1),
        null, Collections.emptyMap(), false);
    assertThat(assets.size(), is(1));

    assets = assetStore.findByComponentIds(Collections.emptySet(), null, Collections.emptyMap(), false);
    assertThat(assets.size(), is(0));

    assets = assetStore.findByComponentIds(null, null, Collections.emptyMap(), false);
    assertThat(assets.size(), is(0));
  }

  // checks the DAO access provided by the store matches our expectations
  private static void assertDaoBinding(
      final ContentStoreSupport<?> store,
      final Class<? extends ContentDataAccess> daoClass)
  {
    // internal dao() method expects to be called from a transactional method, so mimic one here
    UnitOfWork.begin(store::openSession);
    try {
      Transactional.operation.run(() -> assertThat(store.dao(), is(instanceOf(daoClass))));
    }
    finally {
      UnitOfWork.end();
    }
  }

  @Configuration
  static class FormatStoreManagerTestConfiguration
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

  private FormatStoreManager getFormatStoreManager(final BeanDefinitionRegistryPostProcessor postProcessor) {
    postProcessor.postProcessBeanDefinitionRegistry(testContext);
    testContext.refresh();

    return testContext.getBean(FormatStoreManager.class);
  }
}
