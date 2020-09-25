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
package org.sonatype.nexus.testsuite.testsupport.raw;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.content.raw.RawContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.testsuite.helpers.ComponentAssetTestHelper;

import static org.junit.Assert.assertTrue;

@FeatureFlag(name = "nexus.datastore.enabled")
@Named
@Singleton
public class DatastoreRawTestHelper
    implements RawTestHelper
{
  @Inject
  ComponentAssetTestHelper componentAssetTestHelper;

  @Override
  public Content read(final Repository repository, final String path)
      throws IOException
  {
    return repository.facet(RawContentFacet.class).get(adjust(path)).orElse(null);
  }

  @Override
  public void assertRawComponent(final Repository repository, final String path, final String group) {
    assertTrue(componentAssetTestHelper.assetWithComponentExists(repository, adjust(path), group, adjust(path)));
  }

  private String adjust(final String path) {
    return (path.startsWith("/") ? "" : "/") + path;
  }
}
