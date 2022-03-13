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
package org.sonatype.nexus.repository.browse.internal.orient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.goodies.testsupport.concurrent.ConcurrentRunner;
import org.sonatype.nexus.common.app.VersionComparator;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;
import org.sonatype.nexus.repository.browse.BrowsePaths;
import org.sonatype.nexus.repository.browse.node.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.browse.node.DefaultBrowseNodeComparator;
import org.sonatype.nexus.repository.ossindex.PackageUrlService;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.BucketEntityAdapter;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;
import org.sonatype.nexus.repository.storage.ComponentFactory;
import org.sonatype.nexus.repository.storage.DefaultComponent;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.SelectorManager;

import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;

public class OrientBrowseNodeStoreLoadTest
    extends TestSupport
{
  private static final String REPOSITORY_NAME = "test-repo";

  private static final int DELETE_PAGE_SIZE = 80;

  private static final int ASSET_COUNT = 50;

  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private SelectorManager selectorManager;

  @Mock
  private PackageUrlService packageUrlService;

  private Bucket bucket;

  private Component component;

  private List<Asset> assets = new ArrayList<>();

  private OrientBrowseNodeStore underTest;

  @Before
  public void setUp() throws Exception {
    BrowseNodeConfiguration configuration = new BrowseNodeConfiguration(true, 1000, DELETE_PAGE_SIZE, 10_000, 10_000);

    BucketEntityAdapter bucketEntityAdapter = new BucketEntityAdapter();
    ComponentFactory componentFactory = new ComponentFactory(emptySet());
    ComponentEntityAdapter componentEntityAdapter = new ComponentEntityAdapter(bucketEntityAdapter, componentFactory,
        emptySet());
    AssetEntityAdapter assetEntityAdapter = new AssetEntityAdapter(bucketEntityAdapter, componentEntityAdapter);

    when(packageUrlService.getPackageUrl(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Optional.empty());
    BrowseNodeEntityAdapter browseNodeEntityAdapter = new BrowseNodeEntityAdapter(componentEntityAdapter,
        assetEntityAdapter,
        packageUrlService);

    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      bucketEntityAdapter.register(db);
      bucket = new Bucket();
      bucket.setRepositoryName(REPOSITORY_NAME);
      bucket.attributes(new NestedAttributesMap(P_ATTRIBUTES, new HashMap<>()));
      bucketEntityAdapter.addEntity(db, bucket);

      componentEntityAdapter.register(db);
      component = new DefaultComponent()
          .bucketId(EntityHelper.id(bucket))
          .format("test")
          .group("test-group")
          .name("test-component")
          .version("1.0")
          .attributes(new NestedAttributesMap(P_ATTRIBUTES, new HashMap<>()));
      componentEntityAdapter.addEntity(db, component);

      assetEntityAdapter.register(db);
      for (int i = 0; i < ASSET_COUNT; i++) {
        assets.add(new Asset()
            .bucketId(EntityHelper.id(bucket))
            .format("test")
            .name("test-asset-" + i)
            .attributes(new NestedAttributesMap(P_ATTRIBUTES, new HashMap<>())));
        assetEntityAdapter.addEntity(db, assets.get(i));
      }

      browseNodeEntityAdapter.register(db); // registration of the schema is now done outside of the store

      bucketEntityAdapter.register(db);
    }

    underTest = new OrientBrowseNodeStore(
        database.getInstanceProvider(),
        browseNodeEntityAdapter,
        securityHelper,
        selectorManager,
        configuration,
        new HashMap<>(),
        new HashMap<>(),
        ImmutableMap.of(DefaultBrowseNodeComparator.NAME, new DefaultBrowseNodeComparator(new VersionComparator())));

    underTest.start();
  }

  @After
  public void tearDown() throws Exception {
    underTest.stop();
  }

  @SuppressWarnings("java:S2699") // sonar wants assertions, but in this case seems best to let an exception bubble up
  @Test
  public void exercisePathContentionBetweenAssets() throws Exception {
    ConcurrentRunner runner = new ConcurrentRunner(1, 30);

    List<BrowsePath> componentPath = asList(new BrowsePaths("some", "some"), new BrowsePaths("kind", "some/kind"),
        new BrowsePaths("of", "some/kind/of"), new BrowsePaths("path", "some/kind/of/path"));

    for (int i = 0; i < ASSET_COUNT; i++) {
      int assetIndex = i;
      runner.addTask(1, () -> {
        underTest.createComponentNode(REPOSITORY_NAME, "aformat", componentPath, component);
        List<BrowsePath> assetPath = newArrayList(concat(componentPath,
            asList(new BrowsePaths("to", "some/kind/of/path/to"),
                new BrowsePaths("asset" + assetIndex, "some/kind/of/path/to/asset" + assetIndex))));
        underTest.createAssetNode(REPOSITORY_NAME, "aformat", assetPath, assets.get(assetIndex));
      });
    }

    runner.go();
  }

  @SuppressWarnings("java:S2699") // sonar wants assertions, but in this case seems best to let an exception bubble up
  @Test
  public void exercisePathContentionBetweenAssetAndComponent() throws Exception {
    ConcurrentRunner runner = new ConcurrentRunner(1, 30);

    List<BrowsePath> commonPath = asList(new BrowsePaths("some", "some"), new BrowsePaths("kind", "some/kind"),
        new BrowsePaths("of", "some/kind/of"), new BrowsePaths("path", "some/kind/of/path"));

    runner.addTask(1, () -> {
      underTest.createComponentNode(REPOSITORY_NAME, "aformat", commonPath, component);
    });

    runner.addTask(1, () -> {
      underTest.createAssetNode(REPOSITORY_NAME, "aformat", commonPath, assets.get(0));
    });

    runner.go();
  }
}
