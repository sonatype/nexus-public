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
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;
import org.sonatype.nexus.repository.browse.BrowseNodeConfiguration;

import com.google.common.base.Splitter;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;

public class BrowseNodeEntityAdapterTest
    extends TestSupport
{
  private static final String REPOSITORY_NAME = "test-repo";

  private static final String FORMAT_NAME = "test-format";

  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inFilesystem("test");

  private BucketEntityAdapter bucketEntityAdapter;

  private ComponentEntityAdapter componentEntityAdapter;

  private AssetEntityAdapter assetEntityAdapter;

  private BrowseNodeEntityAdapter underTest;

  private Bucket bucket;

  private Component component;

  private Asset asset;

  @Before
  public void setUp() throws Exception {

    bucketEntityAdapter = new BucketEntityAdapter();
    ComponentFactory componentFactory = new ComponentFactory(emptySet());
    componentEntityAdapter = new ComponentEntityAdapter(bucketEntityAdapter, componentFactory, emptySet());
    assetEntityAdapter = new AssetEntityAdapter(bucketEntityAdapter, componentEntityAdapter);

    underTest = new BrowseNodeEntityAdapter(componentEntityAdapter, assetEntityAdapter, new BrowseNodeConfiguration());

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      bucketEntityAdapter.register(db);
      componentEntityAdapter.register(db);
      assetEntityAdapter.register(db);
      underTest.register(db);

      createEntities(db);
    }
  }

  @Test
  public void manageAssetBelowComponent() throws Exception {
    List<String> path = Splitter.on('/').omitEmptyStrings().splitToList(asset.name());

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      underTest.createComponentNode(db, REPOSITORY_NAME, path.subList(0, 3), component);

      assertThat(underTest.browse(db),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/")),
                  hasProperty("name", is("1.0")),
                  hasProperty("componentId", is(EntityHelper.id(component))),
                  hasProperty("assetId", nullValue()),
                  hasProperty("assetNameLowercase", nullValue()))
              ));

      underTest.createAssetNode(db, REPOSITORY_NAME, path, asset);

      assertThat(underTest.browse(db),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/")),
                  hasProperty("name", is("1.0")),
                  hasProperty("componentId", is(EntityHelper.id(component))),
                  hasProperty("assetId", nullValue()),
                  hasProperty("assetNameLowercase", nullValue())),
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/1.0/")),
                  hasProperty("name", is("foo-1.0.jar")),
                  hasProperty("componentId", nullValue()),
                  hasProperty("assetId", is(EntityHelper.id(asset))),
                  hasProperty("assetNameLowercase", is(Strings2.lower(asset.name()))))
              ));

      underTest.deleteAssetNode(db, EntityHelper.id(asset));

      assertThat(underTest.browse(db),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/")),
                  hasProperty("name", is("1.0")),
                  hasProperty("componentId", is(EntityHelper.id(component))),
                  hasProperty("assetId", nullValue()),
                  hasProperty("assetNameLowercase", nullValue()))
              ));

      underTest.deleteComponentNode(db, EntityHelper.id(component));

      assertThat(underTest.browse(db), is(emptyIterable()));
    }
  }

  @Test
  public void manageAssetSameLevelAsComponent() throws Exception {
    List<String> path = Splitter.on('/').omitEmptyStrings().splitToList(asset.name());

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      underTest.createComponentNode(db, REPOSITORY_NAME, path, component);

      assertThat(underTest.browse(db),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/1.0/")),
                  hasProperty("name", is("foo-1.0.jar")),
                  hasProperty("componentId", is(EntityHelper.id(component))),
                  hasProperty("assetId", nullValue()),
                  hasProperty("assetNameLowercase", nullValue()))
              ));

      underTest.createAssetNode(db, REPOSITORY_NAME, path, asset);

      assertThat(underTest.browse(db),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/1.0/")),
                  hasProperty("name", is("foo-1.0.jar")),
                  hasProperty("componentId", is(EntityHelper.id(component))),
                  hasProperty("assetId", is(EntityHelper.id(asset))),
                  hasProperty("assetNameLowercase", is(Strings2.lower(asset.name()))))
              ));

      underTest.deleteAssetNode(db, EntityHelper.id(asset));

      assertThat(underTest.browse(db),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/1.0/")),
                  hasProperty("name", is("foo-1.0.jar")),
                  hasProperty("componentId", is(EntityHelper.id(component))),
                  hasProperty("assetId", nullValue()),
                  hasProperty("assetNameLowercase", nullValue()))
              ));

      underTest.deleteComponentNode(db, EntityHelper.id(component));

      assertThat(underTest.browse(db), is(emptyIterable()));
    }

    // now try out-of-order to check partial delete still works

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      underTest.createAssetNode(db, REPOSITORY_NAME, path, asset);

      assertThat(underTest.browse(db),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/1.0/")),
                  hasProperty("name", is("foo-1.0.jar")),
                  hasProperty("componentId", nullValue()),
                  hasProperty("assetId", is(EntityHelper.id(asset))),
                  hasProperty("assetNameLowercase", is(Strings2.lower(asset.name()))))
              ));

      underTest.createComponentNode(db, REPOSITORY_NAME, path, component);

      assertThat(underTest.browse(db),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/1.0/")),
                  hasProperty("name", is("foo-1.0.jar")),
                  hasProperty("componentId", is(EntityHelper.id(component))),
                  hasProperty("assetId", is(EntityHelper.id(asset))),
                  hasProperty("assetNameLowercase", is(Strings2.lower(asset.name()))))
              ));

      underTest.deleteComponentNode(db, EntityHelper.id(component));

      assertThat(underTest.browse(db),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/1.0/")),
                  hasProperty("name", is("foo-1.0.jar")),
                  hasProperty("componentId", nullValue()),
                  hasProperty("assetId", is(EntityHelper.id(asset))),
                  hasProperty("assetNameLowercase", is(Strings2.lower(asset.name()))))
              ));

      underTest.deleteAssetNode(db, EntityHelper.id(asset));

      assertThat(underTest.browse(db), is(emptyIterable()));
    }
  }

  @Test
  public void manageComponentRepeatedInTree() throws Exception {

    // test that assets can have the same component at different paths in the tree if they want

    Asset patchAsset = new Asset();
    patchAsset.bucketId(EntityHelper.id(bucket));
    patchAsset.componentId(EntityHelper.id(component));
    patchAsset.attributes(new NestedAttributesMap(P_ATTRIBUTES, new HashMap<>()));
    patchAsset.format(FORMAT_NAME);
    patchAsset.name("/org/foo/1.0/foo-1.0-1.jar");
    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      assetEntityAdapter.addEntity(db, patchAsset);
    }

    List<String> path = Splitter.on('/').omitEmptyStrings().splitToList(asset.name());
    List<String> patchPath = Splitter.on('/').omitEmptyStrings().splitToList(patchAsset.name());

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      underTest.createComponentNode(db, REPOSITORY_NAME, path, component);
      underTest.createComponentNode(db, REPOSITORY_NAME, patchPath, component);

      assertThat(underTest.browse(db),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/1.0/")),
                  hasProperty("name", is("foo-1.0.jar")),
                  hasProperty("componentId", is(EntityHelper.id(component))),
                  hasProperty("assetId", nullValue()),
                  hasProperty("assetNameLowercase", nullValue()))
              ,
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/1.0/")),
                  hasProperty("name", is("foo-1.0-1.jar")),
                  hasProperty("componentId", is(EntityHelper.id(component))),
                  hasProperty("assetId", nullValue()),
                  hasProperty("assetNameLowercase", nullValue()))
              ));

      underTest.createAssetNode(db, REPOSITORY_NAME, path, asset);
      underTest.createAssetNode(db, REPOSITORY_NAME, patchPath, patchAsset);

      assertThat(underTest.browse(db),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/1.0/")),
                  hasProperty("name", is("foo-1.0.jar")),
                  hasProperty("componentId", is(EntityHelper.id(component))),
                  hasProperty("assetId", is(EntityHelper.id(asset))),
                  hasProperty("assetNameLowercase", is(Strings2.lower(asset.name()))))
              ,
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/1.0/")),
                  hasProperty("name", is("foo-1.0-1.jar")),
                  hasProperty("componentId", is(EntityHelper.id(component))),
                  hasProperty("assetId", is(EntityHelper.id(patchAsset))),
                  hasProperty("assetNameLowercase", is(Strings2.lower(patchAsset.name()))))
              ));

      underTest.deleteAssetNode(db, EntityHelper.id(asset));
      underTest.deleteAssetNode(db, EntityHelper.id(patchAsset));

      assertThat(underTest.browse(db),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/1.0/")),
                  hasProperty("name", is("foo-1.0.jar")),
                  hasProperty("componentId", is(EntityHelper.id(component))),
                  hasProperty("assetId", nullValue()),
                  hasProperty("assetNameLowercase", nullValue()))
              ,
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/1.0/")),
                  hasProperty("name", is("foo-1.0-1.jar")),
                  hasProperty("componentId", is(EntityHelper.id(component))),
                  hasProperty("assetId", nullValue()),
                  hasProperty("assetNameLowercase", nullValue()))
              ));

      underTest.deleteComponentNode(db, EntityHelper.id(component));

      assertThat(underTest.browse(db), is(emptyIterable()));
    }

    // now try out-of-order to check partial delete still works

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      underTest.createAssetNode(db, REPOSITORY_NAME, path, asset);
      underTest.createAssetNode(db, REPOSITORY_NAME, patchPath, patchAsset);

      assertThat(underTest.browse(db),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/1.0/")),
                  hasProperty("name", is("foo-1.0.jar")),
                  hasProperty("componentId", nullValue()),
                  hasProperty("assetId", is(EntityHelper.id(asset))),
                  hasProperty("assetNameLowercase", is(Strings2.lower(asset.name()))))
              ,
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/1.0/")),
                  hasProperty("name", is("foo-1.0-1.jar")),
                  hasProperty("componentId", nullValue()),
                  hasProperty("assetId", is(EntityHelper.id(patchAsset))),
                  hasProperty("assetNameLowercase", is(Strings2.lower(patchAsset.name()))))
              ));

      underTest.createComponentNode(db, REPOSITORY_NAME, path, component);
      underTest.createComponentNode(db, REPOSITORY_NAME, patchPath, component);

      assertThat(underTest.browse(db),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/1.0/")),
                  hasProperty("name", is("foo-1.0.jar")),
                  hasProperty("componentId", is(EntityHelper.id(component))),
                  hasProperty("assetId", is(EntityHelper.id(asset))),
                  hasProperty("assetNameLowercase", is(Strings2.lower(asset.name()))))
              ,
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/1.0/")),
                  hasProperty("name", is("foo-1.0-1.jar")),
                  hasProperty("componentId", is(EntityHelper.id(component))),
                  hasProperty("assetId", is(EntityHelper.id(patchAsset))),
                  hasProperty("assetNameLowercase", is(Strings2.lower(patchAsset.name()))))
              ));

      underTest.deleteComponentNode(db, EntityHelper.id(component));

      assertThat(underTest.browse(db),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/1.0/")),
                  hasProperty("name", is("foo-1.0.jar")),
                  hasProperty("componentId", nullValue()),
                  hasProperty("assetId", is(EntityHelper.id(asset))),
                  hasProperty("assetNameLowercase", is(Strings2.lower(asset.name()))))
              ,
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/1.0/")),
                  hasProperty("name", is("foo-1.0-1.jar")),
                  hasProperty("componentId", nullValue()),
                  hasProperty("assetId", is(EntityHelper.id(patchAsset))),
                  hasProperty("assetNameLowercase", is(Strings2.lower(patchAsset.name()))))
              ));

      underTest.deleteAssetNode(db, EntityHelper.id(asset));
      underTest.deleteAssetNode(db, EntityHelper.id(patchAsset));

      assertThat(underTest.browse(db), is(emptyIterable()));
    }
  }

  @Test
  public void duplicateRequestsAreMerged() throws Exception {
    List<String> path = Splitter.on('/').omitEmptyStrings().splitToList(asset.name());

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      underTest.createAssetNode(db, REPOSITORY_NAME, path, asset);
      underTest.createAssetNode(db, REPOSITORY_NAME, path, asset);
      underTest.createAssetNode(db, REPOSITORY_NAME, path, asset);

      assertThat(underTest.browse(db),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/1.0/")),
                  hasProperty("name", is("foo-1.0.jar")),
                  hasProperty("componentId", nullValue()),
                  hasProperty("assetId", is(EntityHelper.id(asset))),
                  hasProperty("assetNameLowercase", is(Strings2.lower(asset.name()))))
              ));
    }
  }

  @Test
  public void pagedDeletes() throws Exception {
    List<String> path = Splitter.on('/').omitEmptyStrings().splitToList(asset.name());

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      underTest.createComponentNode(db, REPOSITORY_NAME, path.subList(0, 3), component);
      underTest.createAssetNode(db, REPOSITORY_NAME, path, asset);

      assertThat(underTest.count(db), is(2L));

      int deleteCount = underTest.deleteByRepository(db, REPOSITORY_NAME, 1);

      assertThat(deleteCount, is(1));
      assertThat(underTest.count(db), is(1L));

      deleteCount = underTest.deleteByRepository(db, REPOSITORY_NAME, 1);

      assertThat(deleteCount, is(1));
      assertThat(underTest.count(db), is(0L));

      deleteCount = underTest.deleteByRepository(db, REPOSITORY_NAME, 1);

      assertThat(deleteCount, is(0));
      assertThat(underTest.count(db), is(0L));
    }
  }

  @Test
  public void browseAssetBelowComponent() throws Exception {
    List<String> path = Splitter.on('/').omitEmptyStrings().splitToList(asset.name());

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      underTest.createComponentNode(db, REPOSITORY_NAME, path.subList(0, 3), component);
      underTest.createAssetNode(db, REPOSITORY_NAME, path, asset);

      assertThat(underTest.getByPath(db, REPOSITORY_NAME, path.subList(0, 0), 1, "", emptyMap()),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/")),
                  hasProperty("name", is("org")),
                  hasProperty("leaf", is(false)),
                  hasProperty("componentId", nullValue()),
                  hasProperty("assetId", nullValue()),
                  hasProperty("assetNameLowercase", nullValue()))
              ));

      assertThat(underTest.getByPath(db, REPOSITORY_NAME, path.subList(0, 1), 1, "", emptyMap()),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/")),
                  hasProperty("name", is("foo")),
                  hasProperty("leaf", is(false)),
                  hasProperty("componentId", nullValue()),
                  hasProperty("assetId", nullValue()),
                  hasProperty("assetNameLowercase", nullValue()))
              ));

      assertThat(underTest.getByPath(db, REPOSITORY_NAME, path.subList(0, 2), 1, "", emptyMap()),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/")),
                  hasProperty("name", is("1.0")),
                  hasProperty("leaf", is(false)),
                  hasProperty("componentId", is(EntityHelper.id(component))),
                  hasProperty("assetId", nullValue()),
                  hasProperty("assetNameLowercase", nullValue()))
              ));

      assertThat(underTest.getByPath(db, REPOSITORY_NAME, path.subList(0, 3), 1, "", emptyMap()),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/1.0/")),
                  hasProperty("name", is("foo-1.0.jar")),
                  hasProperty("leaf", is(true)),
                  hasProperty("componentId", nullValue()),
                  hasProperty("assetId", is(EntityHelper.id(asset))),
                  hasProperty("assetNameLowercase", is(Strings2.lower(asset.name()))))
              ));

      assertThat(underTest.getByPath(db, REPOSITORY_NAME, path, 1, "", emptyMap()), is(empty()));
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void browseAssetSameLevelAsComponent() throws Exception {
    List<String> path = Splitter.on('/').omitEmptyStrings().splitToList(asset.name());

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      underTest.createComponentNode(db, REPOSITORY_NAME, path, component);
      underTest.createAssetNode(db, REPOSITORY_NAME, path, asset);

      assertThat(underTest.getByPath(db, REPOSITORY_NAME, path.subList(0, 0), 1, "", emptyMap()),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/")),
                  hasProperty("name", is("org")),
                  hasProperty("leaf", is(false)),
                  hasProperty("componentId", nullValue()),
                  hasProperty("assetId", nullValue()),
                  hasProperty("assetNameLowercase", nullValue()))
              ));

      assertThat(underTest.getByPath(db, REPOSITORY_NAME, path.subList(0, 1), 1, "", emptyMap()),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/")),
                  hasProperty("name", is("foo")),
                  hasProperty("leaf", is(false)),
                  hasProperty("componentId", nullValue()),
                  hasProperty("assetId", nullValue()),
                  hasProperty("assetNameLowercase", nullValue()))
              ));

      assertThat(underTest.getByPath(db, REPOSITORY_NAME, path.subList(0, 2), 1, "", emptyMap()),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/")),
                  hasProperty("name", is("1.0")),
                  hasProperty("leaf", is(false)),
                  hasProperty("componentId", nullValue()),
                  hasProperty("assetId", nullValue()),
                  hasProperty("assetNameLowercase", nullValue()))
              ));

      assertThat(underTest.getByPath(db, REPOSITORY_NAME, path.subList(0, 3), 1, "", emptyMap()),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/1.0/")),
                  hasProperty("name", is("foo-1.0.jar")),
                  hasProperty("leaf", is(true)),
                  hasProperty("componentId", is(EntityHelper.id(component))),
                  hasProperty("assetId", is(EntityHelper.id(asset))),
                  hasProperty("assetNameLowercase", is(Strings2.lower(asset.name()))))
              ));

      assertThat(underTest.getByPath(db, REPOSITORY_NAME, path, 1, "", emptyMap()), is(empty()));
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void browseNestedAssets() throws Exception {
    List<String> path = Splitter.on('/').omitEmptyStrings().splitToList(asset.name());

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {

      Asset parentAsset = new Asset();
      parentAsset.bucketId(EntityHelper.id(bucket));
      parentAsset.componentId(EntityHelper.id(component));
      parentAsset.attributes(new NestedAttributesMap(P_ATTRIBUTES, new HashMap<>()));
      parentAsset.format(FORMAT_NAME);
      parentAsset.name("/org/foo");
      assetEntityAdapter.addEntity(db, parentAsset);

      underTest.createAssetNode(db, REPOSITORY_NAME, path.subList(0, 2), parentAsset);
      underTest.createAssetNode(db, REPOSITORY_NAME, path, asset);

      assertThat(underTest.getByPath(db, REPOSITORY_NAME, path.subList(0, 0), 1, "", emptyMap()),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/")),
                  hasProperty("name", is("org")),
                  hasProperty("leaf", is(false)),
                  hasProperty("componentId", nullValue()),
                  hasProperty("assetId", nullValue()),
                  hasProperty("assetNameLowercase", nullValue()))
              ));

      assertThat(underTest.getByPath(db, REPOSITORY_NAME, path.subList(0, 1), 1, "", emptyMap()),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/")),
                  hasProperty("name", is("foo")),
                  hasProperty("leaf", is(false)),
                  hasProperty("componentId", nullValue()),
                  hasProperty("assetId", is(EntityHelper.id(parentAsset))),
                  hasProperty("assetNameLowercase", is(Strings2.lower(parentAsset.name()))))
              ));

      assertThat(underTest.getByPath(db, REPOSITORY_NAME, path.subList(0, 2), 1, "", emptyMap()),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/")),
                  hasProperty("name", is("1.0")),
                  hasProperty("leaf", is(false)),
                  hasProperty("componentId", nullValue()),
                  hasProperty("assetId", nullValue()),
                  hasProperty("assetNameLowercase", nullValue()))
              ));

      assertThat(underTest.getByPath(db, REPOSITORY_NAME, path.subList(0, 3), 1, "", emptyMap()),
          contains(
              allOf(
                  hasProperty("repositoryName", is(REPOSITORY_NAME)),
                  hasProperty("parentPath", is("/org/foo/1.0/")),
                  hasProperty("name", is("foo-1.0.jar")),
                  hasProperty("leaf", is(true)),
                  hasProperty("componentId", nullValue()),
                  hasProperty("assetId", is(EntityHelper.id(asset))),
                  hasProperty("assetNameLowercase", is(Strings2.lower(asset.name()))))
              ));

      assertThat(underTest.getByPath(db, REPOSITORY_NAME, path, 1, "", emptyMap()), is(empty()));
    }
  }

  private void createEntities(final ODatabaseDocumentTx db) {
    bucket = new Bucket();
    bucket.setRepositoryName(REPOSITORY_NAME);
    bucket.attributes(new NestedAttributesMap(P_ATTRIBUTES, new HashMap<>()));
    bucketEntityAdapter.addEntity(db, bucket);

    component = new DefaultComponent();
    component.bucketId(EntityHelper.id(bucket));
    component.attributes(new NestedAttributesMap(P_ATTRIBUTES, new HashMap<>()));
    component.format(FORMAT_NAME);
    component.group("org").name("foo").version("1.0");
    componentEntityAdapter.addEntity(db, component);

    asset = new Asset();
    asset.bucketId(EntityHelper.id(bucket));
    asset.componentId(EntityHelper.id(component));
    asset.attributes(new NestedAttributesMap(P_ATTRIBUTES, new HashMap<>()));
    asset.format(FORMAT_NAME);
    asset.name("/org/foo/1.0/foo-1.0.jar");
    assetEntityAdapter.addEntity(db, asset);
  }
}
