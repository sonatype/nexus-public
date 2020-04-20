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
package org.sonatype.nexus.repository.maven.internal.orient;

import java.util.List;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.browse.BrowseNodeGenerator;
import org.sonatype.nexus.repository.browse.BrowsePaths;
import org.sonatype.nexus.repository.browse.BrowseTestSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.DefaultComponent;

import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class Maven2BrowseNodeGeneratorTest
    extends BrowseTestSupport
{
  private BrowseNodeGenerator generator = new Maven2BrowseNodeGenerator();

  @Test
  public void computeAssetPathForAssetWithoutComponent() {
    Asset asset = createAsset("com/sonatype/example/metadata.xml");

    List<BrowsePaths> paths = generator.computeAssetPaths(asset, null);

    assertPaths(asList("com", "sonatype", "example", "metadata.xml"), paths);
  }

  @Test
  public void computeAssetPathForAssetWithComponent() {
    String baseVersion = "1.0.0";
    NestedAttributesMap attributes = new NestedAttributesMap("attributes",
        singletonMap("maven2", singletonMap("baseVersion", baseVersion)));

    Asset asset = createAsset("com/sonatype/example/1.0.0/example-1.0.0.jar");
    Component component = new DefaultComponent();
    component.group("com.sonatype");
    component.name("example");
    component.version(baseVersion);
    component.attributes(attributes);

    List<BrowsePaths> paths = generator.computeAssetPaths(asset, component);

    assertPaths(asList("com", "sonatype", "example", "1.0.0", "example-1.0.0.jar"), paths);
  }

  @Test
  public void computeAssetPathForSnapshotAssetWithComponent() {
    String baseVersion = "1.0-SNAPSHOT";
    NestedAttributesMap attributes = new NestedAttributesMap("attributes",
        singletonMap("maven2", singletonMap("baseVersion", baseVersion)));

    Asset asset = createAsset("org/foo/bar/example/1.0-SNAPSHOT/example-1.0-20171213.212030-158.jar");
    Component component = new DefaultComponent();
    component.group("org.foo.bar");
    component.name("example");
    component.version("1.0-20171213.212030-158");
    component.attributes(attributes);

    List<BrowsePaths> paths = generator.computeAssetPaths(asset, component);

    assertSNAPSHOTPaths(asList("org", "foo", "bar", "example", "1.0-SNAPSHOT", "1.0-20171213.212030-158",
        "example-1.0-20171213.212030-158.jar"), paths, false);
  }

  @Test
  public void computeComponentPathNameOnly() {
    Asset asset = createAsset("name/name.jar");
    Component component = new DefaultComponent();
    component.name("name");

    List<BrowsePaths> paths = generator.computeComponentPaths(asset, component);

    assertPaths(singletonList(component.name()), paths, true);
  }

  @Test
  public void computeComponentPathNoGroup() {
    String baseVersion = "1.0.0";
    NestedAttributesMap attributes = new NestedAttributesMap("attributes",
        singletonMap("maven2", singletonMap("baseVersion", baseVersion)));

    Asset asset = createAsset("name/1.0.0/name-1.0.0.jar");
    Component component = new DefaultComponent();
    component.name("name");
    component.version("1.0.0");
    component.attributes(attributes);

    List<BrowsePaths> paths = generator.computeComponentPaths(asset, component);

    assertPaths(asList(component.name(), component.version()), paths, true);
  }

  @Test
  public void computeComponentPath() {
    String baseVersion = "1.0.0";
    NestedAttributesMap attributes = new NestedAttributesMap("attributes",
        singletonMap("maven2", singletonMap("baseVersion", baseVersion)));

    Asset asset = createAsset("group/name/1.0.0/name-1.0.0.jar");
    Component component = new DefaultComponent();
    component.group("group");
    component.name("name");
    component.version(baseVersion);
    component.attributes(attributes);

    List<BrowsePaths> paths = generator.computeComponentPaths(asset, component);

    assertPaths(asList(component.group(), component.name(), component.version()), paths, true);
  }

  @Test
  public void computeSnapshotComponentPath() {
    String baseVersion = "1.0.0-SNAPSHOT";
    NestedAttributesMap attributes = new NestedAttributesMap("attributes",
        singletonMap("maven2", singletonMap("baseVersion", baseVersion)));


    Asset asset = createAsset("group/name/1.0.0-SNAPSHOT/name-1.0.0-20171213.212030-158.jar");

    Component component = new DefaultComponent();
    component.group("group");
    component.name("name");
    component.version("1.0.0-20171213.212030-158");
    component.attributes(attributes);

    List<BrowsePaths> paths = generator.computeComponentPaths(asset, component);

    assertSNAPSHOTPaths(asList(component.group(), component.name(), baseVersion, component.version()), paths, true);
  }

  private void assertSNAPSHOTPaths(List<String> expectedBrowsePaths,
                                   List<BrowsePaths> paths,
                                   boolean withTrailingSlash)
  {
    assertThat(paths.size(), is(expectedBrowsePaths.size()));

    for (int i = 0 ; i < expectedBrowsePaths.size() ; i++) {
      assertThat(paths.get(i).getBrowsePath(), is(expectedBrowsePaths.get(i)));
    }

    String requestPath = "";

    for (int i = 0; i < expectedBrowsePaths.size(); i++) {
      //previous node was snapshot, which means the current node will have same request path
      if (!(i > 0 && expectedBrowsePaths.get(i - 1).endsWith("-SNAPSHOT"))) {
        requestPath += expectedBrowsePaths.get(i);
        if (withTrailingSlash || i < expectedBrowsePaths.size() - 1) {
          requestPath += "/";
        }
      }
      assertThat(paths.get(i).getRequestPath(), is(requestPath));
    }
  }
}
