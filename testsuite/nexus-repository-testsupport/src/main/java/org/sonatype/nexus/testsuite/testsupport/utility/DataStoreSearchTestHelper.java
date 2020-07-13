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

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.client.WebTarget;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.search.index.SearchIndexFacet;

/**
 * This class is temporary; when automatic Search re-indexing is implemented for SQL/DataStore (new DB) mode,
 * this class should be removed and the orient feature flag on  {@link SearchTestHelper} should be removed.
 */
@Named
@Singleton
public class DataStoreSearchTestHelper
    extends SearchTestHelper
{
  @Override
  public void verifyComponentExists(
      final WebTarget nexusSearchWebTarget,
      final Repository repository,
      final String name,
      final String version,
      final boolean exists) throws Exception
  {
    repository.facet(SearchIndexFacet.class).rebuildIndex();
    super.verifyComponentExists(nexusSearchWebTarget, repository, name, version, exists);
  }
}
