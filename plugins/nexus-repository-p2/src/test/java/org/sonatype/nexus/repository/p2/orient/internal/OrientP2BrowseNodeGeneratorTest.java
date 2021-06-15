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
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

public class OrientP2BrowseNodeGeneratorTest
    extends BrowseTestSupport
{
  private static final String SUBSITE = "945d37142fbf9b4c2885a99ebcf353ab852479348e7e86446557f7f05fe87e83";

  private static final List<String> COMPONENT_NAME = Arrays.asList("org", "tigris", "subversion", "clientadapter", "svnkit");

  private static final String COMPONENT_VERSION = "1.7.5";

  private OrientP2BrowseNodeGenerator generator = new OrientP2BrowseNodeGenerator();

  @Test
  public void testRepositoryAtRoot() {
    for (String file : componentLessFiles) {
      Asset asset = createAsset(file);

      List<String> paths = generator.computeAssetPaths(asset, null).stream().map(BrowsePaths::getDisplayName)
          .collect(Collectors.toList());
      assertThat(paths, contains(file));
    }

    String assetName = COMPONENT_NAME.stream().collect(Collectors.joining(".")) + '_' + COMPONENT_VERSION;
    Component component = createComponent(String.join(".", COMPONENT_NAME), null, COMPONENT_VERSION);
    for (String directory : knownSubDirectories) {
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
    for (String file : componentLessFiles) {
      String path = SUBSITE + "/" + file;
      Asset asset = createAsset(path);

      List<String> paths = generator.computeAssetPaths(asset, null).stream().map(BrowsePaths::getDisplayName)
          .collect(Collectors.toList());
      assertThat(paths, contains(SUBSITE, file));
    }

    String assetName = COMPONENT_NAME.stream().collect(Collectors.joining(".")) + '_' + COMPONENT_VERSION;
    Component component = createComponent(String.join(".", COMPONENT_NAME), null, COMPONENT_VERSION);
    for (String directory : knownSubDirectories) {
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

  private String[] knownSubDirectories = new String[]{"plugins", "features", "binary"};

  private String[] componentLessFiles = new String[]{"p2.index", "artifacts.xml", "artifacts.jar", "artifacts.xml.xz",
      "content.xml", "content.jar", "content.xml.xz", "compositeArtifacts.xml", "compositeArtifacts.jar",
      "compositeContent.xml", "compositeContent.jar"};
}
