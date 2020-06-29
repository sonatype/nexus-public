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
package org.sonatype.nexus.testsuite.testsupport.utility;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.client.WebTarget;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.search.query.SearchQueryService;
import org.sonatype.nexus.testsuite.helpers.ComponentAssetTestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * This class is temporary; when Search is available/implemented for SQL/DataStore (new DB), this class
 * should be updated to use Search for verifying component existence.
 *
 * At that time, we probably will no longer need separate implementations of SearchTestHelper
 * i.e SearchTestHelper can be reverted to a class
 */
@Named
@Singleton
public class DataStoreSearchTestHelper
    implements SearchTestHelper
{
  @Inject
  protected ComponentAssetTestHelper componentAssetTestHelper;

  @Override
  public void waitForSearch() throws Exception {
    throw new UnsupportedOperationException("Search is not available.");
  }

  @Override
  public void verifyComponentExists(
      final WebTarget nexusSearchWebTarget,
      final Repository repository,
      final String name,
      final String version,
      final boolean exists)
  {
    boolean componentExists = componentAssetTestHelper.componentExists(repository, name, version);
    assertThat(componentExists, is(exists));
  }

  @Override
  public SearchQueryService queryService() {
    throw new UnsupportedOperationException("Search is not available.");
  }
}

