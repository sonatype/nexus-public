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

import java.util.HashMap;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.orient.entity.ConflictHook;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.view.Content;

import com.google.common.collect.ImmutableList;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;
import static org.sonatype.nexus.repository.storage.StorageTestUtil.createAsset;
import static org.sonatype.nexus.repository.storage.StorageTestUtil.createComponent;
import static org.sonatype.nexus.repository.view.Content.CONTENT_LAST_MODIFIED;

/**
 * Tests for {@link DeconflictAssetMetadata}.
 */
public class DeconflictAssetMetadataTest
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  private final DateTime TEST_LAST_DOWNLOADED = DateTime.parse("2018-07-31T13:20");

  private final DateTime TEST_LAST_MODIFIED = DateTime.parse("2018-07-31T14:30");

  private final DateTime TEST_LAST_VERIFIED = DateTime.parse("2018-07-31T15:40");

  private final String TEST_CACHE_TOKEN = "test-cache-token";

  private final String NEW_CACHE_TOKEN = "new-cache-token";

  private final String INVALIDATED_CACHE_TOKEN = "invalidated";

  private ComponentEntityAdapter componentEntityAdapter;

  private AssetEntityAdapter assetEntityAdapter;

  private Bucket bucket;

  private Component component;

  private Asset asset;

  private ODocument initialAssetRecord;

  @Before
  public void setUp() {
    BucketEntityAdapter bucketEntityAdapter = new BucketEntityAdapter();

    ComponentFactory componentFactory = new ComponentFactory(emptySet());
    componentEntityAdapter = new ComponentEntityAdapter(bucketEntityAdapter, componentFactory, emptySet());
    assetEntityAdapter = new AssetEntityAdapter(bucketEntityAdapter, componentEntityAdapter);

    ConflictHook conflictHook = new ConflictHook(true);
    componentEntityAdapter.enableConflictHook(conflictHook);
    assetEntityAdapter.enableConflictHook(conflictHook);

    assetEntityAdapter.setDeconflictSteps(asList(new DeconflictAssetMetadata()));

    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      bucketEntityAdapter.register(db);
      componentEntityAdapter.register(db);
      assetEntityAdapter.register(db);
      bucket = new Bucket();
      bucket.attributes(new NestedAttributesMap(P_ATTRIBUTES, new HashMap<>()));
      bucket.setRepositoryName("test-repo");
      bucketEntityAdapter.addEntity(db, bucket);
    }

    // first create our test component+asset with some default values
    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      db.begin();

      component = createComponent(bucket, "some-group", "some-component", "1.0");
      componentEntityAdapter.addEntity(db, component);

      asset = createAsset(bucket, "some-asset", component);
      initialAssetRecord = assetEntityAdapter.addEntity(db, asset);

      db.commit();
    }

    // update the asset, so it moves on from the initial record
    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      db.begin();

      Content.extractFromAsset(asset, ImmutableList.of(), new AttributesMap());
      CacheInfo.extractFromAsset(asset);

      asset.attributes().child("test").set("test-asset-key", "test-asset-value");
      assetEntityAdapter.readFields(assetEntityAdapter.editEntity(db, asset), asset);

      db.commit();
    }
  }

  @Test
  public void lastUpdatedDifferencesAreDeconflicted() throws Exception {
    DateTime lastUpdated = asset.lastUpdated();

    Thread.sleep(100);
    assertTrue(tryConflictingUpdate(asset));

    assertThat(asset.lastUpdated(), is(greaterThan(lastUpdated)));
  }

  @Test
  public void lastDownloadedDifferencesAreDeconflicted() throws Exception {

    asset.lastDownloaded(TEST_LAST_DOWNLOADED);
    assertTrue(tryConflictingUpdate(asset));
    assertThat(asset.lastDownloaded(), is(TEST_LAST_DOWNLOADED));

    DateTime earlierLastDownloaded = TEST_LAST_DOWNLOADED.minusHours(1);

    asset.lastDownloaded(earlierLastDownloaded);
    assertTrue(tryConflictingUpdate(asset));
    assertThat(asset.lastDownloaded(), is(TEST_LAST_DOWNLOADED));

    DateTime laterLastDownloaded = TEST_LAST_DOWNLOADED.plusHours(1);

    asset.lastDownloaded(laterLastDownloaded);
    assertTrue(tryConflictingUpdate(asset));
    assertThat(asset.lastDownloaded(), is(laterLastDownloaded));
  }

  @Test
  public void lastModifiedDifferencesAreDeconflicted() throws Exception {

    setLastModified(asset, TEST_LAST_MODIFIED);
    assertTrue(tryConflictingUpdate(asset));
    assertThat(getLastModified(asset), is(TEST_LAST_MODIFIED));

    DateTime earlierLastModified = TEST_LAST_MODIFIED.minusHours(1);

    setLastModified(asset, earlierLastModified);
    assertTrue(tryConflictingUpdate(asset));
    assertThat(getLastModified(asset), is(TEST_LAST_MODIFIED));

    DateTime laterLastModified = TEST_LAST_MODIFIED.plusHours(1);

    setLastModified(asset, laterLastModified);
    assertTrue(tryConflictingUpdate(asset));
    assertThat(getLastModified(asset), is(laterLastModified));
  }

  @Test
  public void lastVerifiedDifferencesAreDeconflicted() throws Exception {

    setLastVerified(asset, TEST_LAST_VERIFIED);
    assertTrue(tryConflictingUpdate(asset));
    assertThat(getLastVerified(asset), is(TEST_LAST_VERIFIED));

    DateTime earlierLastVerified = TEST_LAST_VERIFIED.minusHours(1);

    setLastVerified(asset, earlierLastVerified);
    assertTrue(tryConflictingUpdate(asset));
    assertThat(getLastVerified(asset), is(TEST_LAST_VERIFIED));

    DateTime laterLastVerified = TEST_LAST_VERIFIED.plusHours(1);

    setLastVerified(asset, laterLastVerified);
    assertTrue(tryConflictingUpdate(asset));
    assertThat(getLastVerified(asset), is(laterLastVerified));
  }

  @Test
  public void cacheTokenDifferencesAreDeconflicted() throws Exception {

    setCacheToken(asset, TEST_CACHE_TOKEN);
    assertTrue(tryConflictingUpdate(asset));
    assertThat(getCacheToken(asset), is(TEST_CACHE_TOKEN));

    setCacheToken(asset, NEW_CACHE_TOKEN);
    assertFalse(tryConflictingUpdate(asset));
    assertThat(getCacheToken(asset), is(TEST_CACHE_TOKEN));

    setCacheToken(asset, INVALIDATED_CACHE_TOKEN);
    assertTrue(tryConflictingUpdate(asset));
    assertThat(getCacheToken(asset), is(INVALIDATED_CACHE_TOKEN));
  }

  private boolean tryConflictingUpdate(final Asset asset) {
    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      db.begin();

      ODocument copy = initialAssetRecord.copy();
      assetEntityAdapter.writeFields(copy, asset);
      copy.save();

      try {
        db.commit();
        return true;
      }
      catch (OConcurrentModificationException e) {
        logger.debug("Update denied due to conflict", e);
        return false;
      }
    }
    finally {
      try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
        assetEntityAdapter.readFields(db.load(assetEntityAdapter.recordIdentity(asset)), asset);
      }
    }
  }

  private void setLastModified(final Asset asset, final DateTime lastModified) {
    AttributesMap contentAttributes = new AttributesMap();
    contentAttributes.set(CONTENT_LAST_MODIFIED, lastModified);
    Content.applyToAsset(asset, contentAttributes);
  }

  private DateTime getLastModified(final Asset asset) {
    AttributesMap contentAttributes = new AttributesMap();
    Content.extractFromAsset(asset, ImmutableList.of(), contentAttributes);
    return contentAttributes.get(CONTENT_LAST_MODIFIED, DateTime.class);
  }

  private void setLastVerified(final Asset asset, final DateTime lastVerified) {
    CacheInfo.applyToAsset(asset, new CacheInfo(lastVerified, null));
  }

  private DateTime getLastVerified(final Asset asset) {
    CacheInfo cacheInfo = CacheInfo.extractFromAsset(asset);
    return cacheInfo != null ? cacheInfo.getLastVerified() : null;
  }

  private void setCacheToken(final Asset asset, final String cacheToken) {
    CacheInfo.applyToAsset(asset, new CacheInfo(DateTime.now(), cacheToken));
  }

  private String getCacheToken(final Asset asset) {
    CacheInfo cacheInfo = CacheInfo.extractFromAsset(asset);
    return cacheInfo != null ? cacheInfo.getCacheToken() : null;
  }
}
