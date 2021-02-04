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
package org.sonatype.nexus.content.maven.internal.browse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;

import org.junit.Test;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class Maven2BrowseNodeGeneratorTest
    extends TestSupport
{
  private static final String BASE_VERSION = "1.3";

  private static final String TIMESTAMPED_VERSION = "1.3-20200717.093520-1";

  private static final String SNAPSHOT_VERSION = "1.3-SNAPSHOT";

  private Maven2BrowseNodeGenerator underTest = new Maven2BrowseNodeGenerator();

  @Test
  public void should_build_paths_to_base_versioned_asset_which_has_a_component() {
    AssetData asset = new AssetData();
    asset.setPath("/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar");
    asset.setComponent(aReleaseVersionedComponent());

    List<BrowsePath> browsePaths = underTest.computeAssetPaths(asset);

    assertThat(browsePaths.size(), is(5));
    assertThat(browsePaths, containsInAnyOrder(
        new BrowsePath("org", "/org/"),
        new BrowsePath("hamcrest", "/org/hamcrest/"),
        new BrowsePath("hamcrest-core", "/org/hamcrest/hamcrest-core/"),
        new BrowsePath(BASE_VERSION, "/org/hamcrest/hamcrest-core/1.3/"),
        new BrowsePath("hamcrest-core-1.3.jar", asset.path())));
  }

  @Test
  public void should_build_paths_to_timestamped_versioned_asset_which_has_a_component() {
    AssetData asset = new AssetData();
    asset.setPath("/org/hamcrest/hamcrest-core/1.3-SNAPSHOT/hamcrest-core-1.3-20200717.093520-1.jar");
    asset.setComponent(aSnapshotVersionedComponent());

    List<BrowsePath> browsePaths = underTest.computeAssetPaths(asset);

    assertThat(browsePaths.size(), is(6));
    assertThat(browsePaths, containsInAnyOrder(
        new BrowsePath("org", "/org/"),
        new BrowsePath("hamcrest", "/org/hamcrest/"),
        new BrowsePath("hamcrest-core", "/org/hamcrest/hamcrest-core/"),
        new BrowsePath(SNAPSHOT_VERSION, "/org/hamcrest/hamcrest-core/1.3-SNAPSHOT/"),
        new BrowsePath(TIMESTAMPED_VERSION,
            "/org/hamcrest/hamcrest-core/1.3-SNAPSHOT/1.3-20200717.093520-1/"),
        new BrowsePath("hamcrest-core-1.3-20200717.093520-1.jar", asset.path())));
  }

  @Test
  public void should_build_paths_for_asset_without_a_component() {
    AssetData asset = new AssetData();
    asset.setPath("/com/sonatype/example/metadata.xml");

    List<BrowsePath> browsePaths = underTest.computeAssetPaths(asset);

    assertThat(browsePaths.size(), is(4));
    assertThat(browsePaths, containsInAnyOrder(
        new BrowsePath("com", "/com/"),
        new BrowsePath("sonatype", "/com/sonatype/"),
        new BrowsePath("example", "/com/sonatype/example/"),
        new BrowsePath("metadata.xml", asset.path())));
  }

  @Test
  public void should_build_paths_to_base_versioned_component() {
    AssetData asset = new AssetData();
    asset.setComponent(aReleaseVersionedComponent());

    List<BrowsePath> browsePaths = underTest.computeComponentPaths(asset);

    assertThat(browsePaths.size(), is(4));
    assertThat(browsePaths, containsInAnyOrder(
        new BrowsePath("org", "/org/"),
        new BrowsePath("hamcrest", "/org/hamcrest/"),
        new BrowsePath("hamcrest-core", "/org/hamcrest/hamcrest-core/"),
        new BrowsePath(BASE_VERSION, "/org/hamcrest/hamcrest-core/1.3/")));
  }

  @Test
  public void should_build_paths_to_timestamped_versioned_component() {
    AssetData asset = new AssetData();
    asset.setComponent(aSnapshotVersionedComponent());

    List<BrowsePath> browsePaths = underTest.computeComponentPaths(asset);

    assertThat(browsePaths.size(), is(5));
    assertThat(browsePaths, containsInAnyOrder(
        new BrowsePath("org", "/org/"),
        new BrowsePath("hamcrest", "/org/hamcrest/"),
        new BrowsePath("hamcrest-core", "/org/hamcrest/hamcrest-core/"),
        new BrowsePath(SNAPSHOT_VERSION, "/org/hamcrest/hamcrest-core/1.3-SNAPSHOT/"),
        new BrowsePath(TIMESTAMPED_VERSION,
            "/org/hamcrest/hamcrest-core/1.3-SNAPSHOT/1.3-20200717.093520-1/")));
  }

  @Test
  public void should_build_paths_to_component_without_a_namespace() {
    AssetData asset = new AssetData();
    asset.setComponent(aComponentWithNoNamespace());

    List<BrowsePath> browsePaths = underTest.computeComponentPaths(asset);

    assertThat(browsePaths.size(), is(2));
    assertThat(browsePaths, containsInAnyOrder(
        new BrowsePath("hamcrest-core", "/hamcrest-core/"),
        new BrowsePath("1.3", "/hamcrest-core/1.3/")));
  }

  @Test
  public void should_build_paths_to_component_with_name_only() {
    AssetData asset = new AssetData();
    asset.setComponent(aComponentWithNameOnly());

    List<BrowsePath> browsePaths = underTest.computeComponentPaths(asset);

    assertThat(browsePaths.size(), is(1));
    assertThat(browsePaths, containsInAnyOrder(new BrowsePath("hamcrest-core", "/hamcrest-core/")));
  }

  private Component aReleaseVersionedComponent() {
    ComponentData componentData = createComponent();
    componentData.setVersion(BASE_VERSION);
    formatAttributes(componentData, BASE_VERSION);
    return componentData;
  }

  private ComponentData createComponent() {
    ComponentData componentData = new ComponentData();
    componentData.setRepositoryId(1);
    componentData.setComponentId(1);
    componentData.setName("hamcrest-core");
    componentData.setNamespace("org.hamcrest");
    return componentData;
  }

  private void formatAttributes(final ComponentData componentData, final String baseVersion) {
    Map<String, String> formatAttributes = new HashMap<>();
    formatAttributes.put(Maven2BrowseNodeGenerator.BASE_VERSION, baseVersion);
    componentData.attributes().set(Maven2Format.NAME, formatAttributes);
  }

  private ComponentData aSnapshotVersionedComponent() {
    ComponentData componentData = createComponent();
    componentData.setVersion(TIMESTAMPED_VERSION);
    formatAttributes(componentData, SNAPSHOT_VERSION);
    return componentData;
  }

  private Component aComponentWithNoNamespace() {
    ComponentData componentData = createComponent();
    componentData.setNamespace(EMPTY);
    componentData.setVersion(BASE_VERSION);
    formatAttributes(componentData, BASE_VERSION);
    return componentData;
  }

  private Component aComponentWithNameOnly() {
    ComponentData componentData = createComponent();
    componentData.setNamespace(EMPTY);
    componentData.setVersion(EMPTY);
    formatAttributes(componentData, BASE_VERSION);
    return componentData;
  }
}
