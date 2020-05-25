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
package org.sonatype.nexus.repository.browse;

import java.util.ArrayList;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.DefaultComponent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class BrowseTestSupport
  extends TestSupport
{
  protected void assertPaths(List<String> expectedPaths, List<BrowsePaths> paths) {
    assertPaths(expectedPaths, paths, false);
  }

  protected void assertPaths(List<String> expectedPaths, List<BrowsePaths> paths, boolean withTrailingSlash) {
    assertPaths(expectedPaths, expectedPaths, paths, withTrailingSlash);
  }

  protected void assertPaths(List<String> expectedBrowsePaths,
                             List<String> expectedRequestPaths,
                             List<BrowsePaths> paths)
  {
    assertPaths(expectedBrowsePaths, expectedRequestPaths, paths, false);
  }

  protected void assertPaths(List<String> expectedBrowsePaths,
                             List<String> expectedRequestPaths,
                             List<BrowsePaths> paths,
                             boolean withTrailingSlash)
  {
    assertThat(expectedRequestPaths.size(), is(expectedBrowsePaths.size()));
    assertThat(paths.size(), is(expectedBrowsePaths.size()));

    String requestPath = "";

    for (int i = 0 ; i < expectedBrowsePaths.size() ; i++) {
      requestPath += expectedRequestPaths.get(i);
      if (withTrailingSlash || i < expectedBrowsePaths.size() - 1) {
        requestPath += "/";
      }
      assertThat(paths.get(i).getBrowsePath(), is(expectedBrowsePaths.get(i)));
      assertThat(paths.get(i).getRequestPath(), is(requestPath));
    }
  }

  protected Asset createAsset(final String assetName) {
    Asset asset = mock(Asset.class);
    when(asset.name()).thenReturn(assetName);
    when(asset.blobRef()).thenReturn(mock(BlobRef.class));
    return asset;
  }

  protected Component createComponent(final String name, final String group, final String version) {
    Component component = new DefaultComponent();
    component.name(name);
    component.group(group);
    component.version(version);

    return component;
  }

  @SuppressWarnings("unchecked")
  protected <T extends BrowsePath> List<T> toBrowsePaths(List<String> paths) {
    List<BrowsePaths> results = new ArrayList<>();

    final StringBuilder requestPath = new StringBuilder();

    paths.forEach(path -> {
      requestPath.append(path);
      results.add(new BrowsePaths(path, requestPath.toString()));
      requestPath.append("/");
    });

    return (List<T>) results;
  }
}
