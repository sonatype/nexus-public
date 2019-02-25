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
package org.sonatype.nexus.repository.storage;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityVersion;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.mime.internal.DefaultMimeSupport;
import org.sonatype.nexus.orient.HexRecordIdObfuscator;
import org.sonatype.nexus.orient.entity.AttachedEntityId;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.attributes.internal.AttributesFacetImpl;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.search.SearchFacet;
import org.sonatype.nexus.repository.storage.internal.ComponentSchemaRegistration;
import org.sonatype.nexus.security.ClientInfoProvider;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.common.entity.EntityHelper.id;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;
import static org.sonatype.nexus.repository.storage.StorageFacetConstants.STORAGE;

/**
 * Integration tests for {@link StorageFacetImpl}.
 */
public class StorageFacetImplIT
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inFilesystem("test");

  protected StorageFacetImpl underTest;

  protected Repository testRepository1 = mock(Repository.class);

  protected Repository testRepository2 = mock(Repository.class);

  protected TestFormat testFormat = new TestFormat();

  private ComponentSchemaRegistration schemaRegistration;

  private AssetEntityAdapter assetEntityAdapter;

  private class TestFormat
      extends Format
  {
    public TestFormat() {
      super("test");
    }
  }

  @Before
  public void setUp() throws Exception {
    BucketEntityAdapter bucketEntityAdapter = new BucketEntityAdapter();
    ComponentFactory componentFactory = new ComponentFactory(emptySet());
    HexRecordIdObfuscator recordIdObfuscator = new HexRecordIdObfuscator();
    bucketEntityAdapter.enableObfuscation(recordIdObfuscator);
    ComponentEntityAdapter componentEntityAdapter = new ComponentEntityAdapter(bucketEntityAdapter, componentFactory,
        emptySet());
    componentEntityAdapter.enableObfuscation(recordIdObfuscator);
    assetEntityAdapter = new AssetEntityAdapter(bucketEntityAdapter, componentEntityAdapter);
    assetEntityAdapter.enableObfuscation(recordIdObfuscator);

    schemaRegistration = new ComponentSchemaRegistration(
        database.getInstanceProvider(),
        bucketEntityAdapter,
        componentEntityAdapter,
        assetEntityAdapter);

    schemaRegistration.start();

    StorageFacetImpl.Config config = new StorageFacetImpl.Config();
    ConfigurationFacet configurationFacet = mock(ConfigurationFacet.class);
    when(configurationFacet.readSection(
        any(Configuration.class),
        eq(STORAGE),
        eq(StorageFacetImpl.Config.class)))
        .thenReturn(config);

    when(testRepository1.getName()).thenReturn("test-repository-1");
    when(testRepository1.getFormat()).thenReturn(testFormat);
    when(testRepository1.facet(ConfigurationFacet.class)).thenReturn(configurationFacet);
    when(testRepository1.facet(SearchFacet.class)).thenReturn(mock(SearchFacet.class));

    when(testRepository2.getName()).thenReturn("test-repository-2");
    when(testRepository2.getFormat()).thenReturn(testFormat);
    when(testRepository2.facet(ConfigurationFacet.class)).thenReturn(configurationFacet);
    when(testRepository2.facet(SearchFacet.class)).thenReturn(mock(SearchFacet.class));

    underTest = storageFacetImpl("testNodeId", bucketEntityAdapter, componentEntityAdapter, assetEntityAdapter, testRepository1);
  }

  @After
  public void tearDown() throws Exception {
    underTest.stop();

    schemaRegistration.stop();
  }

  @Test
  public void initialState() {
    try (StorageTx tx = beginTX()) {
      // We should have one bucket, which was auto-created for the repository during initialization
      checkSize(tx.browseBuckets(), 1);
    }
  }

  @Test
  public void startWithEmptyAttributes() {
    try (StorageTx tx = beginTX()) {
      Asset asset = tx.createAsset(tx.findBucket(testRepository1), testFormat);
      Component component = tx.createComponent(tx.findBucket(testRepository1), testFormat);

      NestedAttributesMap assetAttributes = asset.attributes();
      assertThat(assetAttributes, is(notNullValue()));
      assertThat(assetAttributes.isEmpty(), is(true));

      NestedAttributesMap componentAttributes = component.attributes();
      assertThat(componentAttributes, is(notNullValue()));
      assertThat(componentAttributes.isEmpty(), is(true));
    }
  }

  @Test
  public void getAndSetAttributes() {
    EntityId docId;
    try (StorageTx tx = beginTX()) {
      Asset asset = tx.createAsset(tx.findBucket(testRepository1), testFormat);
      asset.name("asset");
      NestedAttributesMap map = asset.attributes();

      assertThat(map.isEmpty(), is(true));

      map.child("bag1").set("foo", "bar");
      map.child("bag2").set("baz", "qux");

      assertThat(map.isEmpty(), is(false));

      tx.saveAsset(asset);

      tx.commit();
      docId = EntityHelper.id(asset);
    }

    try (StorageTx tx = beginTX()) {
      NestedAttributesMap map = tx.findAsset(docId, tx.findBucket(testRepository1)).attributes();

      assertThat(map.size(), is(2));
      assertThat(map.child("bag1").size(), is(1));
      assertThat((String) map.child("bag1").get("foo"), is("bar"));
      assertThat(map.child("bag2").size(), is(1));
      assertThat((String) map.child("bag2").get("baz"), is("qux"));
    }
  }

  @Test
  public void findAssets() throws Exception {
    // Setup: add an asset in both repositories
    try (StorageTx tx = beginTX()) {
      Asset asset1 = tx.createAsset(tx.findBucket(testRepository1), testFormat);
      asset1.name("asset1");
      asset1.size(42L);
      tx.saveAsset(asset1);
      tx.commit();
    }

    underTest.attach(testRepository2);
    underTest.init();
    try (StorageTx tx = beginTX()) {
      Asset asset2 = tx.createAsset(tx.findBucket(testRepository2), testFormat);
      asset2.name("asset2");
      asset2.size(42L);
      tx.saveAsset(asset2);
      tx.commit();
    }

    // Queries
    try (StorageTx tx = beginTX()) {

      // Find assets with name = "asset1"

      // ..in testRepository1, should yield 1 match
      checkSize(tx.findAssets("name = :name", ImmutableMap.of("name", (Object) "asset1"),
          ImmutableSet.of(testRepository1), null), 1);
      assertThat(tx.countAssets("name = :name", ImmutableMap.of("name", (Object) "asset1"),
          ImmutableSet.of(testRepository1), null), is(1L));
      // ...in testRepository2, should yield 0 matches
      checkSize(tx.findAssets("name = :name", ImmutableMap.of("name", (Object) "asset1"),
          ImmutableSet.of(testRepository2), null), 0);
      assertThat(tx.countAssets("name = :name", ImmutableMap.of("name", (Object) "asset1"),
          ImmutableSet.of(testRepository2), null), is(0L));
      // ..in testRepository1 or testRepository2, should yeild 1 match
      checkSize(tx.findAssets("name = :name", ImmutableMap.of("name", (Object) "asset1"),
          ImmutableSet.of(testRepository1, testRepository2), null), 1);
      assertThat(tx.countAssets("name = :name", ImmutableMap.of("name", (Object) "asset1"),
          ImmutableSet.of(testRepository1, testRepository2), null), is(1L));
      // ..in any repository should yeild 2 matches
      checkSize(tx.findAssets("name = :name", ImmutableMap.of("name", (Object) "asset1"), null, null), 1);
      assertThat(tx.countAssets("name = :name", ImmutableMap.of("name", (Object) "asset1"), null, null), is(1L));

      // Find assets with number = 42

      // ..in testRepository1, should yield 1 match
      checkSize(tx.findAssets("size = :number", ImmutableMap.of("number", (Object) 42),
          ImmutableSet.of(testRepository1), null), 1);
      assertThat(tx.countAssets("size = :number", ImmutableMap.of("number", (Object) 42),
          ImmutableSet.of(testRepository1), null), is(1L));
      // ..in testRepository2, should yield 1 match
      checkSize(tx.findAssets("size = :number", ImmutableMap.of("number", (Object) 42),
          ImmutableSet.of(testRepository2), null), 1);
      assertThat(tx.countAssets("size = :number", ImmutableMap.of("number", (Object) 42),
          ImmutableSet.of(testRepository2), null), is(1L));
      // ..in testRepository1 or testRepository2, should yield 2 matches
      checkSize(tx.findAssets("size = :number", ImmutableMap.of("number", (Object) 42),
          ImmutableSet.of(testRepository1, testRepository2), null), 2);
      assertThat(tx.countAssets("size = :number", ImmutableMap.of("number", (Object) 42),
          ImmutableSet.of(testRepository1, testRepository2), null), is(2L));
      // ..in any repository, should yield 2 matches
      checkSize(tx.findAssets("size = :number", ImmutableMap.of("number", (Object) 42),
          ImmutableSet.of(testRepository1, testRepository2), null), 2);
      assertThat(tx.countAssets("size = :number", ImmutableMap.of("number", (Object) 42),
          ImmutableSet.of(testRepository1, testRepository2), null), is(2L));

      // Find assets in any repository with name = "foo" or number = 42
      String whereClause = "name = :name or size = :number";
      Map<String, Object> parameters = ImmutableMap.of("name", (Object) "foo", "number", 42);

      // ..in ascending order by name with limit 1, should return asset1
      String suffix = "order by name limit 1";
      List<Asset> results = Lists.newArrayList(tx.findAssets(whereClause, parameters, null, suffix));
      checkSize(results, 1);
      assertThat((String) results.get(0).name(), is("asset1"));

      // ..in descending order by name with limit 1, should return asset2
      suffix = "order by name desc limit 1";
      results = Lists.newArrayList(tx.findAssets(whereClause, parameters, null, suffix));
      checkSize(results, 1);
      assertThat((String) results.get(0).name(), is("asset2"));
    }
  }

  @Test
  public void mapOfMaps() {
    Map<String, String> bag2 = ImmutableMap.of();

    // Transaction 1:
    // Create a new asset with property "attributes" that's a map of maps (stored as an embeddedmap)
    EntityId docId;
    try (StorageTx tx = beginTX()) {
      Bucket bucket = tx.findBucket(testRepository1);
      Asset asset = tx.createAsset(bucket, testFormat);
      asset.name("asset");
      asset.attributes().child("bag1").set("foo", "bar");
      asset.attributes().child("bag2").set("baz", "qux");
      tx.saveAsset(asset);
      tx.commit();
      docId = EntityHelper.id(asset);
    }

    // Transaction 2:
    // Get the asset and make sure it contains what we expect
    try (StorageTx tx = beginTX()) {
      Bucket bucket = tx.findBucket(testRepository1);
      Asset asset = tx.findAsset(docId, bucket);
      assert asset != null;

      NestedAttributesMap outputMap = asset.attributes();

      assertThat(outputMap.size(), is(2));

      Map<String, String> outputBag1 = (Map<String, String>) outputMap.get("bag1");
      assertNotNull(outputBag1);
      assertThat(outputBag1.keySet().size(), is(1));
      assertThat(outputBag1.get("foo"), is("bar"));

      Map<String, String> outputBag2 = (Map<String, String>) outputMap.get("bag2");
      assertNotNull(outputBag2);
      assertThat(outputBag2.keySet().size(), is(1));
      assertThat(outputBag2.get("baz"), is("qux"));
    }

    // Transaction 3:
    // Make sure we can use dot notation to query for the asset by some aspect of the attributes
    try (StorageTx tx = beginTX()) {
      Map<String, String> parameters = ImmutableMap.of("fooValue", "bar");
      String query = String.format("select from %s where attributes.bag1.foo = :fooValue", assetEntityAdapter.getTypeName());

      Iterable<ODocument> docs = tx.getDb().command(new OCommandSQL(query)).execute(parameters);
      List<ODocument> list = Lists.newArrayList(docs);

      assertThat(list.size(), is(1));
      assertThat(new AttachedEntityId(assetEntityAdapter, list.get(0).getIdentity()), is(docId));
    }
  }

  @Test
  public void roundTripTest() {
    EntityId asset1Id = null;
    EntityId asset2Id = null;
    EntityId componentId = null;

    try (StorageTx tx = beginTX()) {
      // Verify initial state with browse
      Bucket bucket = tx.findBucket(testRepository1);

      checkSize(tx.browseBuckets(), 1);
      checkSize(tx.browseAssets(bucket), 0);
      checkSize(tx.browseComponents(bucket), 0);

      // Create an asset and component and verify state with browse and find
      Asset asset1 = tx.createAsset(bucket, testFormat);
      asset1.name("foo");
      tx.saveAsset(asset1);

      Component component = tx.createComponent(bucket, testFormat);
      component.name("bar");
      tx.saveComponent(component);

      Asset asset2 = tx.createAsset(bucket, component);
      asset2.name("asset2");
      tx.saveAsset(asset2);

      tx.commit();

      // In transaction mode, ORIDs are placeholders until commit, so IDs should be collected after commit
      asset1Id = id(asset1);
      asset2Id = id(asset2);
      componentId = id(component);
    }

    try (StorageTx tx = beginTX()) {
      Bucket bucket = tx.findBucket(testRepository1);

      checkSize(tx.browseAssets(bucket), 2);
      checkSize(tx.browseComponents(bucket), 1);

      assertNotNull(tx.findAsset(asset1Id, bucket));
      assertNotNull(tx.findComponentInBucket(componentId, bucket));

      checkSize(tx.browseAssets(tx.findComponentInBucket(componentId, bucket)), 1);
      assertNotNull(tx.firstAsset(tx.findComponentInBucket(componentId, bucket)));
      assertNull(tx.findAsset(asset1Id, bucket).componentId());
      assertNotNull(tx.findAsset(asset2Id, bucket).componentId());

      assertNull(tx.findAssetWithProperty(P_NAME, "nomatch", bucket));
      assertNotNull(tx.findAssetWithProperty(P_NAME, "foo", bucket));

      assertNull(tx.findComponentWithProperty(P_NAME, "nomatch", bucket));
      assertNotNull(tx.findComponentWithProperty(P_NAME, "bar", bucket));

      // Delete both and make sure browse and find behave as expected
      tx.deleteAsset(tx.findAsset(asset1Id, bucket));
      tx.deleteComponent(tx.findComponentInBucket(componentId, bucket));

      tx.commit();
      tx.begin();

      checkSize(tx.browseAssets(bucket), 0);
      checkSize(tx.browseComponents(bucket), 0);
      assertNull(tx.findAsset(asset1Id, bucket));
      assertNull(tx.findComponentInBucket(componentId, bucket));

      // NOTE: It doesn't matter for this test, but you should commit when finished with one or more writes
      //       If you don't, your changes will be automatically rolled back.
      tx.commit();
    }
  }

  @Test
  public void componentAssetLinksAreDurable() {
    try (StorageTx tx = beginTX()) {
      Bucket bucket = tx.findBucket(testRepository1);
      final Component component = tx.createComponent(bucket, testFormat).name("component");
      tx.saveComponent(component);

      final Asset asset = tx.createAsset(bucket, component).name("asset");
      tx.saveAsset(asset);

      tx.commit();
    }

    try (StorageTx tx = beginTX()) {
      final Asset asset = tx.findAssetWithProperty("name", "asset", tx.findBucket(testRepository1));
      assertThat(asset, is(notNullValue()));

      final Component component = tx.findComponentInBucket(asset.componentId(), tx.findBucket(testRepository1));
      assertThat(component, is(notNullValue()));
      assertThat(component.name(), is("component"));

      final Asset tmpAsset = tx.findAssetWithProperty("name", "asset", component);
      assertThat(tmpAsset.toString(), is(equalTo(asset.toString())));
    }
  }

  @Test
  public void concurrentTransactionWithoutConflictTest() throws Exception {
    doConcurrentTransactionTest(false);
  }

  @Test
  public void concurrentTransactionWithConflictTest() throws Exception {
    doConcurrentTransactionTest(true);
  }

  private void doConcurrentTransactionTest(boolean simulateConflict) throws Exception {
    // setup:
    //   main thread: create a new asset and commit it.
    // test:
    //   main thread: start new transaction, and if simulating a conflict, read the asset
    //   aux thread: start new transaction, modify asset, and commit
    //   main thread: if not simulating a conflict, read the asset. then modify the asset, then commit it
    // expectation:
    //   if simulating a conflict: commit on main thread fails with OConcurrentModificationException
    //   if not simulating a conflict: modification made in main thread is persisted after the modification on aux

    // setup
    final EntityId assetId;
    EntityVersion firstVersion;
    try (StorageTx tx = beginTX()) {
      Bucket bucket = tx.findBucket(testRepository1);
      Asset asset = tx.createAsset(bucket, testFormat);
      asset.name("asset");
      tx.saveAsset(asset);
      assetId = EntityHelper.id(asset);
      tx.commit();
      firstVersion = EntityHelper.version(asset);
    }

    // test
    // 1. start a tx (mainTx) in the main thread
    try (StorageTx mainTx = beginTX()) {
      Bucket bucket = mainTx.findBucket(testRepository1);
      Asset asset = null;

      if (simulateConflict) {
        // cause a conflict to occur later by reading the asset before the other tx starts
        // (this causes the MVCC version comparison at commit-time to fail)
        asset = checkNotNull(mainTx.findAsset(assetId, bucket));
      }

      // 2. modify and commit the asset in a separate tx (auxTx) in another thread
      Thread auxThread = new Thread()
      {
        @Override
        public void run() {
          try (StorageTx auxTx = beginTX()) {
            Bucket bucket = auxTx.findBucket(testRepository1);
            Asset asset = checkNotNull(auxTx.findAsset(assetId, bucket));
            asset.name("firstValue");
            auxTx.saveAsset(asset);
            auxTx.commit();
          }
        }
      };
      auxThread.start();
      auxThread.join();

      // 3. modify and commit the asset in mainTx, in the main thread
      if (!simulateConflict) {
        // only read the asset we propose to change *after* the other transaction completes
        asset = checkNotNull(mainTx.findAsset(assetId, bucket));
      }
      asset.name("secondValue");
      mainTx.saveAsset(asset);
      mainTx.commit(); // if we're simulating a conflict, this call should throw OConcurrentModificationException
      assertThat(simulateConflict, is(false));
    }
    catch (OConcurrentModificationException e) {
      assertThat(simulateConflict, is(true));
      return;
    }

    // not simulating a conflict; verify the expected state
    try (StorageTx tx = beginTX()) {
      Bucket bucket = tx.findBucket(testRepository1);
      Asset asset = checkNotNull(tx.findAsset(assetId, bucket));

      String name = asset.name();
      EntityVersion finalVersion = EntityHelper.version(asset);

      assertThat(name, is("secondValue"));
      assertThat(Integer.valueOf(finalVersion.getValue()), greaterThan(Integer.valueOf(firstVersion.getValue())));
    }
  }

  @Test
  public void noDuplicateComponent() throws Exception {
    createComponent(null, "name", null);
    createComponent("group", "name", null);
    createComponent(null, "name", "1");
    createComponent("group", "name", "1");
  }

  @Test(expected = ORecordDuplicatedException.class)
  public void duplicateComponentName() throws Exception {
    createComponent(null, "name", null);
    createComponent(null, "name", null);
  }

  @Test(expected = ORecordDuplicatedException.class)
  public void duplicateComponentGroupName() throws Exception {
    createComponent("group", "name", null);
    createComponent("group", "name", null);
  }

  @Test(expected = ORecordDuplicatedException.class)
  public void duplicateComponentNameVersion() throws Exception {
    createComponent(null, "name", "1");
    createComponent(null, "name", "1");
  }

  @Test(expected = ORecordDuplicatedException.class)
  public void duplicateComponentGroupNameVersion() throws Exception {
    createComponent("group", "name", "1");
    createComponent("group", "name", "1");
  }

  private Component createComponent(final String group, final String name, final String version) throws Exception {
    try (StorageTx tx = beginTX()) {
      Bucket bucket = tx.findBucket(testRepository1);
      Component component = tx.createComponent(bucket, testFormat)
          .group(group)
          .name(name)
          .version(version);
      tx.saveComponent(component);
      tx.commit();
      return component;
    }
  }

  private Asset createAsset(final Component component, final String name) throws Exception {
    try (StorageTx tx = beginTX()) {
      Bucket bucket = tx.findBucket(testRepository1);
      Asset asset;
      if (component != null) {
        asset = tx.createAsset(bucket, component);
      }
      else {
        asset = tx.createAsset(bucket, testFormat);
      }
      asset.name(name);
      tx.saveAsset(asset);
      tx.commit();
      return asset;
    }
  }

  @Test
  public void noDuplicateAsset() throws Exception {
    Component component = createComponent("group", "name", "1");
    createAsset(component, "name");
    createAsset(null, "name");
  }

  @Test(expected = ORecordDuplicatedException.class)
  public void duplicateAssetComponentName() throws Exception {
    Component component = createComponent("group", "name", "1");
    createAsset(component, "name");
    createAsset(component, "name");
  }

  @Test(expected = ORecordDuplicatedException.class)
  public void duplicateAssetName() throws Exception {
    createAsset(null, "name");
    createAsset(null, "name");
  }

  private void checkSize(Iterable iterable, int expectedSize) {
    assertThat(Iterators.size(iterable.iterator()), is(expectedSize));
  }

  @Test
  public void repeatedAssetModificationsAreSaved() throws Exception {
    createComponent("testGroup", "testName", "testVersion");
    try (StorageTx tx = beginTX()) {
      final Component component = tx.findComponentWithProperty("version", "testVersion", tx.findBucket(testRepository1));
      final Asset asset = tx.createAsset(tx.findBucket(testRepository1), component).name("asset");

      final NestedAttributesMap attributes = asset.formatAttributes();
      attributes.set("attribute1", "original");
      tx.saveAsset(asset);

      final Iterable<Asset> assets = tx.browseAssets(component);
      final Asset reloadedAsset = assets.iterator().next();

      reloadedAsset.formatAttributes().set("attribute2", "alternate");
      tx.saveAsset(reloadedAsset);

      tx.commit();
    }

    try (StorageTx tx = beginTX()) {
      final Component component = tx.findComponentWithProperty("version", "testVersion", tx.findBucket(testRepository1));
      final Iterable<Asset> assets = tx.browseAssets(component);
      final Asset asset = assets.iterator().next();

      assertThat(asset.formatAttributes().get("attribute1", String.class), equalTo("original"));
      assertThat(asset.formatAttributes().get("attribute2", String.class), equalTo("alternate"));
    }
  }

  @Test
  public void transactionsRollBackWhenRequired() throws Exception {
    try (StorageTx tx = beginTX()) {
      final Component component = tx.createComponent(tx.findBucket(testRepository1), testFormat)
          .group("myGroup")
          .version("0.9")
          .name("myComponent");
      tx.saveComponent(component);
      tx.rollback();
    }

    try (StorageTx tx = beginTX()) {
      final Component component = tx.findComponentWithProperty("group", "myGroup", tx.findBucket(testRepository1));
      assertThat(component, is(nullValue()));
    }
  }

  @Test
  public void transactionsRollBackOnException() throws Exception {
    try {
      try (StorageTx tx = beginTX()) {
        final Component component = tx.createComponent(tx.findBucket(testRepository1), testFormat)
            .group("myGroup")
            .version("0.9")
            .name("myComponent");
        tx.saveComponent(component);
        throw new IllegalStateException();
      }
    }
    catch (IllegalStateException ignored) {
    }

    try (StorageTx tx = beginTX()) {
      final Component component = tx.findComponentWithProperty("group", "myGroup", tx.findBucket(testRepository1));
      assertThat(component, is(nullValue()));
    }
  }

  @Test
  public void transactionContentIsSaved() throws Exception {
    try (StorageTx tx = beginTX()) {
      final Component component = tx.createComponent(tx.findBucket(testRepository1), testFormat)
          .group("myGroup")
          .version("0.9")
          .name("myComponent");
      tx.saveComponent(component);
      tx.commit();
    }

    try (StorageTx tx = beginTX()) {
      final Iterable<Component> components = tx.browseComponents(tx.findBucket(testRepository1));
      final Component component = tx.findComponentWithProperty("group", "myGroup", tx.findBucket(testRepository1));
      assertThat(component, is(notNullValue()));
      assertThat(component.group(), is("myGroup"));
    }
  }

  @Test
  public void entityIdCanBeUsedInLaterTransactions() throws Exception {
    EntityId componentId;

    try (StorageTx tx = beginTX()) {
      final Component component = tx.createComponent(tx.findBucket(testRepository1), testFormat).name("component");
      tx.saveComponent(component);

      componentId = id(component);

      tx.commit();
    }

    try (StorageTx tx = beginTX()) {
      final Component component = tx.findComponentInBucket(componentId, tx.findBucket(testRepository1));
      assertThat("component", component, is(notNullValue()));
      assertThat(component.name(), is("component"));
    }
  }

  @Test
  public void entityIdCanBeReferencedBeforeCommit() throws Exception {
    EntityId componentId;
    EntityId assetId;

    try (StorageTx tx = beginTX()) {
      final Component component = tx.createComponent(tx.findBucket(testRepository1), testFormat).name("component");
      tx.saveComponent(component);

      // Implicitly reference the component's entity id
      final Asset asset = tx.createAsset(tx.findBucket(testRepository1), component).name("hello");
      tx.saveAsset(asset);

      tx.commit();
      componentId = id(component);
      assetId = id(asset);
    }

    try (StorageTx tx = beginTX()) {
      final Component component = tx.findComponentInBucket(componentId, tx.findBucket(testRepository1));
      assertThat("component", component, is(notNullValue()));
      assertThat(component.name(), is("component"));

      final Asset asset = tx.findAsset(assetId, tx.findBucket(testRepository1));
      assertThat("asset", asset, is(notNullValue()));
      assertThat(asset.name(), is("hello"));
    }
  }

  @Test
  public void dependentQueryFromUncommittedComponentDoesNotThrowException() throws Exception {
    try (StorageTx tx = beginTX()) {
      final Component component = tx.createComponent(tx.findBucket(testRepository1), testFormat).name("component");
      tx.saveComponent(component);

      // Correct use of attached entity ids prevent an exception being thrown by this line
      tx.browseAssets(component);
    }
  }

  @Test
  public void assetLastDownloaded() throws Exception {
    final String ASSET_NAME = "assetLastDownloaded";
    try (StorageTx tx = beginTX()) {
      Bucket bucket = tx.findBucket(testRepository1);
      Asset asset = tx.createAsset(bucket, testFormat).name(ASSET_NAME);
      tx.saveAsset(asset);
      tx.commit();
    }

    try (StorageTx tx = beginTX()) {
      Bucket bucket = tx.findBucket(testRepository1);
      Asset asset = tx.findAssetWithProperty(P_NAME, ASSET_NAME, bucket);
      assertThat(asset, notNullValue());
      assertThat(asset.lastDownloaded(), nullValue());
      assertThat(asset.markAsDownloaded(12), is(true));
      tx.saveAsset(asset);
      tx.commit();
    }

    try (StorageTx tx = beginTX()) {
      Bucket bucket = tx.findBucket(testRepository1);
      Asset asset = tx.findAssetWithProperty(P_NAME, ASSET_NAME, bucket);
      assertThat(asset, notNullValue());
      assertThat(asset.lastDownloaded(), notNullValue());
      assertThat(asset.markAsDownloaded(12), is(false));
    }
  }

  @Test
  public void bucketAttributesConsistentOnConcurrentAccess() throws Exception {
    // in an HA environment another node's StorageFacetImpl can make
    // changes to the underlying database
    BucketEntityAdapter otherBucketEntityAdapter = new BucketEntityAdapter();
    ComponentFactory componentFactory = new ComponentFactory(emptySet());
    ComponentEntityAdapter otherComponentEntityAdapter = new ComponentEntityAdapter(otherBucketEntityAdapter,
        componentFactory, emptySet());
    AssetEntityAdapter otherAssetEntityAdapter =
        new AssetEntityAdapter(otherBucketEntityAdapter, otherComponentEntityAdapter);
    StorageFacetImpl otherNodeStorageFacetImpl = storageFacetImpl("otherNodeId",
        otherBucketEntityAdapter, otherComponentEntityAdapter, otherAssetEntityAdapter, testRepository1);

    ComponentSchemaRegistration otherSchemaRegistration = new ComponentSchemaRegistration(
        database.getInstanceProvider(),
        otherBucketEntityAdapter,
        otherComponentEntityAdapter,
        otherAssetEntityAdapter);

    otherSchemaRegistration.start();

    AttributesFacetImpl attributesFacet = attributesFacetImpl(testRepository1);

    // access attributes through underTest
    when(testRepository1.facet(StorageFacet.class)).thenReturn(underTest);
    attributesFacet.modifyAttributes(attributes -> attributes.set("foo", "original"));
    assertThat(attributesFacet.getAttributes().require("foo", String.class), equalTo("original"));

    // update attributes through otherNodeStorageFacetImpl
    when(testRepository1.facet(StorageFacet.class)).thenReturn(otherNodeStorageFacetImpl);
    attributesFacet.modifyAttributes(attributes -> attributes.set("foo", "updated"));

    // retrieve updated attributes through underTest
    when(testRepository1.facet(StorageFacet.class)).thenReturn(underTest);
    assertThat(attributesFacet.getAttributes().require("foo", String.class), equalTo("updated"));

    otherSchemaRegistration.stop();
  }

  private StorageFacetImpl storageFacetImpl(final String nodeId,
                                            final BucketEntityAdapter bucketEntityAdapter,
                                            final ComponentEntityAdapter componentEntityAdapter,
                                            final AssetEntityAdapter assetEntityAdapter,
                                            final Repository repository) throws Exception {
    NodeAccess mockNodeAccess = mock(NodeAccess.class);
    when(mockNodeAccess.getId()).thenReturn(nodeId);
    BlobStoreManager mockBlobStoreManager = mock(BlobStoreManager.class);
    when(mockBlobStoreManager.get(anyString())).thenReturn(mock(BlobStore.class));
    ContentValidatorSelector contentValidatorSelector =
        new ContentValidatorSelector(Collections.emptyMap(), new DefaultContentValidator(new DefaultMimeSupport()));
    MimeRulesSourceSelector mimeRulesSourceSelector = new MimeRulesSourceSelector(Collections.emptyMap());
    StorageFacetManager storageFacetManager = mock(StorageFacetManager.class);
    ComponentFactory componentFactory = new ComponentFactory(emptySet());
    StorageFacetImpl storageFacetImpl = new StorageFacetImpl(
        mockNodeAccess,
        mockBlobStoreManager,
        database.getInstanceProvider(),
        bucketEntityAdapter,
        componentEntityAdapter,
        assetEntityAdapter,
        mock(ClientInfoProvider.class),
        contentValidatorSelector,
        mimeRulesSourceSelector,
        storageFacetManager,
        componentFactory,
        mock(ConstraintViolationFactory.class));
    storageFacetImpl.installDependencies(mock(EventManager.class));

    storageFacetImpl.attach(repository);
    storageFacetImpl.init();
    storageFacetImpl.start();

    return storageFacetImpl;
  }

  private AttributesFacetImpl attributesFacetImpl(Repository repository) throws Exception {
    AttributesFacetImpl attributesFacetImpl = new AttributesFacetImpl();
    attributesFacetImpl.installDependencies(mock(EventManager.class));
    attributesFacetImpl.attach(repository);
    attributesFacetImpl.init();
    attributesFacetImpl.start();
    return attributesFacetImpl;
  }

  private StorageTx beginTX() {
    final StorageTx tx = underTest.txSupplier().get();
    tx.begin();
    return tx;
  }
}
