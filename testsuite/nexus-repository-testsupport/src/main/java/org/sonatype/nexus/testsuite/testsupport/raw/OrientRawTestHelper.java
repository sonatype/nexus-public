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

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.orient.raw.RawContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.testsuite.helpers.ComponentAssetTestHelper;
import org.sonatype.nexus.transaction.UnitOfWork;

import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.common.app.FeatureFlags.ORIENT_ENABLED;

@FeatureFlag(name = ORIENT_ENABLED)
@Named
@Priority(Integer.MAX_VALUE)
@Singleton
public class OrientRawTestHelper
    extends RawTestHelper
{
  @Inject
  ComponentAssetTestHelper componentAssetTestHelper;

  @Override
  public Content read(final Repository repository, final String path)
      throws IOException
  {
    RawContentFacet rawFacet = repository.facet(RawContentFacet.class);
    UnitOfWork.begin(repository.facet(StorageFacet.class).txSupplier());
    try {
      return rawFacet.get(path);
    }
    finally {
      UnitOfWork.end();
    }
  }

  @Override
  public void assertRawComponent(final Repository repository, final String path, final String group) {
    assertTrue(componentAssetTestHelper.assetWithComponentExists(repository, path, group, path));
  }

  @Override
  public EntityId createAsset(
      final Repository repository, final String componentName, final String componentGroup, final String assetName)
  {
    UnitOfWork.begin(repository.facet(StorageFacet.class).txSupplier());
    try {
      String path = getGroupAndAsset(componentGroup, assetName);

      return repository.facet(RawContentFacet.class)
          .getOrCreateAsset(repository, componentName, componentGroup, path)
          .componentId();
    }
    finally {
      UnitOfWork.end();
    }
  }


  private String getGroupAndAsset(final String group, final String assetName){
    if (Strings2.isBlank(group)) {
      return assetName;
    }
    return group + "/" + assetName;
  }
}
