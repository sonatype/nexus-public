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
package org.sonatype.nexus.repository.p2.orient.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.sonatype.nexus.repository.browse.BrowsePaths;
import org.sonatype.nexus.repository.browse.BrowseTestSupport;
import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class OrientP2BrowseNodeGeneratorTest
    extends BrowseTestSupport
{
  private static final String[] KNOWN_SUB_DIRECTORIES = new String[]{"plugins", "features", "binary"};

  private static final String[]
      COMPONENT_LESS_FILES = new String[]{"p2.index", "artifacts.xml", "artifacts.jar", "artifacts.xml.xz",
                                          "content.xml", "content.jar", "content.xml.xz", "compositeArtifacts.xml", "compositeArtifacts.jar",
                                          "compositeContent.xml", "compositeContent.jar"};
  private static final String SUBSITE = "945d37142fbf9b4c2885a99ebcf353ab852479348e7e86446557f7f05fe87e83";

  private static final List<String> COMPONENT_NAME = Arrays.asList("org", "tigris", "subversion", "clientadapter", "svnkit");

  private static final String COMPONENT_VERSION = "1.7.5";

  private OrientP2BrowseNodeGenerator generator = new OrientP2BrowseNodeGenerator();

  @Test
  public void testRepositoryAtRoot() {
    for (String file : COMPONENT_LESS_FILES) {
      Asset asset = createAsset(file);

      List<String> paths = generator.computeAssetPaths(asset, null).stream().map(BrowsePaths::getDisplayName)
          .collect(Collectors.toList());
      assertThat(paths, contains(file));
    }

    String assetName = COMPONENT_NAME.stream().collect(Collectors.joining(".")) + '_' + COMPONENT_VERSION;
    Component component = createComponent(String.join(".", COMPONENT_NAME), null, COMPONENT_VERSION);
    for (String directory : KNOWN_SUB_DIRECTORIES) {
      Asset asset = createAsset(directory + '/' + assetName);

      List<String> paths = generator.computeAssetPaths(asset, component).stream()
          .map(BrowsePaths::getDisplayName)
          .collect(Collectors.toList());

      List<String> expectedPaths = new ArrayList<>();
      expectedPaths.add(directory);
      expectedPaths.addAll(COMPONENT_NAME);
      expectedPaths.add(COMPONENT_VERSION);
      expectedPaths.add(assetName);
      assertThat(paths, contains(expectedPaths.toArray()));
    }
  }

  @Test
  public void testNestedRepository() {
    for (String file : COMPONENT_LESS_FILES) {
      String path = SUBSITE + "/" + file;
      Asset asset = createAsset(path);

      List<String> paths = generator.computeAssetPaths(asset, null).stream().map(BrowsePaths::getDisplayName)
          .collect(Collectors.toList());
      assertThat(paths, contains(SUBSITE, file));
    }

    String assetName = COMPONENT_NAME.stream().collect(Collectors.joining(".")) + '_' + COMPONENT_VERSION;
    Component component = createComponent(String.join(".", COMPONENT_NAME), null, COMPONENT_VERSION);
    for (String directory : KNOWN_SUB_DIRECTORIES) {
      Asset asset = createAsset(SUBSITE + '/' + directory + '/' + assetName);

      List<String> paths = generator.computeAssetPaths(asset, component).stream().map(BrowsePaths::getDisplayName)
          .collect(Collectors.toList());

      List<String> expectedPaths = new ArrayList<>();
      expectedPaths.add(SUBSITE);
      expectedPaths.add(directory);
      expectedPaths.addAll(COMPONENT_NAME);
      expectedPaths.add(COMPONENT_VERSION);
      expectedPaths.add(assetName);
      assertThat(paths, contains(expectedPaths.toArray()));
    }
  }


  @Test
  public void testSingularAsset() {
    String featurePath = "features/edu.umd.cs.findbugs.plugin.eclipse_3.0.1.20150306-5afe4d1.jar";
    Asset asset = createAsset(featurePath);
    List<String> assetPaths = generator.computeAssetPaths(asset, null).stream().map(BrowsePath::getRequestPath).collect(toList());

    List<String> expectedAssetPaths = ImmutableList.of(
        "features/",
        "edu.umd.cs.findbugs.plugin.eclipse_3.0.1.20150306-5afe4d1.jar"
    );

    assertThat(assetPaths, is(equalTo(expectedAssetPaths)));
  }

  @Test
  public void testAssetAndComponent() {
    String featurePath = "features/edu.umd.cs.findbugs.plugin.eclipse_3.0.1.20150306-5afe4d1.jar";

    Asset asset = createAsset(featurePath);
    Component component = createComponent("edu.umd.cs.findbugs.plugin.eclipse", null, "3.0.1.20150306-5afe4d1");
    List<String> assetPaths = generator.computeAssetPaths(asset, component).stream().map(BrowsePath::getRequestPath).collect(toList());
    List<String> componentPaths = generator.computeComponentPaths(asset, component).stream().map(BrowsePath::getRequestPath).collect(toList());

    List<String> expectedAssetPaths = ImmutableList.of(
        "features/",
        "features/edu/",
        "features/edu/umd/",
        "features/edu/umd/cs/",
        "features/edu/umd/cs/findbugs/",
        "features/edu/umd/cs/findbugs/plugin/",
        "features/edu/umd/cs/findbugs/plugin/eclipse/",
        "features/edu/umd/cs/findbugs/plugin/eclipse/3.0.1.20150306-5afe4d1/",
        "edu.umd.cs.findbugs.plugin.eclipse_3.0.1.20150306-5afe4d1.jar"
    );

    assertThat(assetPaths, is(equalTo(expectedAssetPaths)));

    List<String> expectedComponentPaths = ImmutableList.of(
        "features/",
        "features/edu/",
        "features/edu/umd/",
        "features/edu/umd/cs/",
        "features/edu/umd/cs/findbugs/",
        "features/edu/umd/cs/findbugs/plugin/",
        "features/edu/umd/cs/findbugs/plugin/eclipse/",
        "features/edu/umd/cs/findbugs/plugin/eclipse/3.0.1.20150306-5afe4d1/"
    );

    assertThat(componentPaths, is(equalTo(expectedComponentPaths)));
  }
}
