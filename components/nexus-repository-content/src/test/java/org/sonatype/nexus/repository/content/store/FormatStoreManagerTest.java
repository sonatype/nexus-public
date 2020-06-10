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

import java.util.HashMap;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityUUID;
import org.sonatype.nexus.common.time.UTC;
import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup;
import org.sonatype.nexus.datastore.api.ContentDataAccess;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.store.example.BespokeStoreModule;
import org.sonatype.nexus.repository.content.store.example.PlainStoreModule;
import org.sonatype.nexus.repository.content.store.example.TestAssetBlobDAO;
import org.sonatype.nexus.repository.content.store.example.TestAssetDAO;
import org.sonatype.nexus.repository.content.store.example.TestAssetData;
import org.sonatype.nexus.repository.content.store.example.TestAssetStore;
import org.sonatype.nexus.repository.content.store.example.TestComponentDAO;
import org.sonatype.nexus.repository.content.store.example.TestContentRepositoryDAO;
import org.sonatype.nexus.testdb.DataSessionRule;
import org.sonatype.nexus.transaction.TransactionModule;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.sonatype.nexus.datastore.mybatis.CombUUID.combUUID;

/**
 * Test {@link FormatStoreManager}.
 */
@Category(SQLTestGroup.class)
public class FormatStoreManagerTest
    extends TestSupport
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule("content")
      .handle(new BlobRefTypeHandler())
      .access(TestContentRepositoryDAO.class)
      .access(TestComponentDAO.class)
      .access(TestAssetBlobDAO.class)
      .access(TestAssetDAO.class);

  class SessionModule
      extends AbstractModule
  {
    @Override
    protected void configure() {
      bind(DataSessionSupplier.class).toInstance(sessionRule);
    }
  }

  @Test
  public void testPlainBindings() {
    Injector injector = Guice.createInjector(new PlainStoreModule(), new SessionModule(), new TransactionModule());
    FormatStoreManager underTest = injector.getInstance(Key.get(FormatStoreManager.class, Names.named("plain")));

    ContentRepositoryStore<?> contentRepositoryStore = underTest.contentRepositoryStore("content");
    ComponentStore<?> componentStore = underTest.componentStore("content");
    AssetStore<?> assetStore = underTest.assetStore("content");
    AssetBlobStore<?> assetBlobStore = underTest.assetBlobStore("content");

    // check the appropriate DAOs have been bound
    assertDaoBinding(contentRepositoryStore, TestContentRepositoryDAO.class);
    assertDaoBinding(componentStore, TestComponentDAO.class);
    assertDaoBinding(assetStore, TestAssetDAO.class);
    assertDaoBinding(assetBlobStore, TestAssetBlobDAO.class);

    // check that previously requested stores are cached to keep creation costs down
    assertThat(underTest.contentRepositoryStore("content"), sameInstance(contentRepositoryStore));
    assertThat(underTest.componentStore("content"), sameInstance(componentStore));
    assertThat(underTest.assetStore("content"), sameInstance(assetStore));
    assertThat(underTest.assetBlobStore("content"), sameInstance(assetBlobStore));
  }

  @Test
  public void testPlainOperations() {
    Injector injector = Guice.createInjector(new PlainStoreModule(), new SessionModule(), new TransactionModule());
    FormatStoreManager underTest = injector.getInstance(Key.get(FormatStoreManager.class, Names.named("plain")));

    ContentRepositoryStore<?> contentRepositoryStore = underTest.contentRepositoryStore("content");
    ComponentStore<?> componentStore = underTest.componentStore("content");
    AssetStore<?> assetStore = underTest.assetStore("content");
    AssetBlobStore<?> assetBlobStore = underTest.assetBlobStore("content");

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
    componentStore.createComponent(component);

    AssetData asset = new AssetData();
    asset.setAttributes(new NestedAttributesMap("attributes", new HashMap<>()));
    asset.setRepositoryId(repository.repositoryId);
    asset.setComponent(component);
    asset.setPath("/path/to/asset");
    asset.setKind("test");
    assetStore.createAsset(asset);

    AssetBlobData assetBlob = new AssetBlobData();
    assetBlob.setBlobRef(new BlobRef("local", "default", "testBlob"));
    assetBlob.setBlobSize(0);
    assetBlob.setContentType("text/plain");
    assetBlob.setChecksums(ImmutableMap.of());
    assetBlob.setBlobCreated(UTC.now());
    assetBlobStore.createAssetBlob(assetBlob);

    asset.setAssetBlob(assetBlob);
    assetStore.updateAssetBlobLink(asset);

    Optional<Asset> result = assetStore.readAsset(repository.repositoryId, "/path/to/asset");
    assertThat(result.get().component().get().name(), is("testComponent"));
    assertThat(result.get().blob().get().blobRef().getBlob(), is("testBlob"));
  }

  @Test
  public void testBespokeBindings() {
    Injector injector = Guice.createInjector(new BespokeStoreModule(), new SessionModule(), new TransactionModule());
    FormatStoreManager underTest = injector.getInstance(Key.get(FormatStoreManager.class, Names.named("bespoke")));

    ContentRepositoryStore<?> contentRepositoryStore = underTest.contentRepositoryStore("content");
    ComponentStore<?> componentStore = underTest.componentStore("content");
    AssetStore<?> assetStore = underTest.assetStore("content");
    AssetBlobStore<?> assetBlobStore = underTest.assetBlobStore("content");

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

  @Test
  public void testBespokeOperations() {
    Injector injector = Guice.createInjector(new BespokeStoreModule(), new SessionModule(), new TransactionModule());
    FormatStoreManager underTest = injector.getInstance(Key.get(FormatStoreManager.class, Names.named("bespoke")));

    // add our bespoke schema for testing purposes
    try (DataSession<?> session = sessionRule.openSession("content")) {
      session.access(TestAssetDAO.class).addTestSchema();
      session.getTransaction().commit();
    }

    ContentRepositoryStore<?> contentRepositoryStore = underTest.contentRepositoryStore("content");
    TestAssetStore assetStore = underTest.assetStore("content");

    ContentRepositoryData repository = new ContentRepositoryData();
    repository.setAttributes(new NestedAttributesMap("attributes", new HashMap<>()));
    repository.setConfigRepositoryId(new EntityUUID(combUUID()));
    contentRepositoryStore.createContentRepository(repository);

    TestAssetData asset = new TestAssetData();
    asset.setAttributes(new NestedAttributesMap("attributes", new HashMap<>()));
    asset.setRepositoryId(repository.repositoryId);
    asset.setPath("/path/to/asset");
    asset.setKind("test");
    assetStore.createAsset(asset);

    assertThat(assetStore.browseFlaggedAssets(repository.repositoryId, 10, null), is(emptyIterable()));

    asset.setTestFlag(true);
    assetStore.updateAssetFlag(asset);

    Optional<Asset> result = assetStore.browseFlaggedAssets(repository.repositoryId, 10, null).stream().findFirst();
    assertThat(result.get().path(), is("/path/to/asset"));
  }

  // checks the DAO access provided by the store matches our expectations
  private static void assertDaoBinding(final ContentStoreSupport<?> store,
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
}
