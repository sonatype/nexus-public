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
package org.sonatype.nexus.repository.content.search.sql;

import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.sql.SqlSearchPermissionManager;
import org.sonatype.nexus.repository.search.sql.SqlSearchUtils;
import org.sonatype.nexus.security.SecurityHelper;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.sonatype.nexus.repository.search.index.SearchConstants.REPOSITORY_NAME;

public class SqlSearchServiceTest
    extends TestSupport
{
  private static final String REPO_1 = "repo1";

  private static final String REPO_2 = "repo2";

  private static final String REPO_3 = "repo3";

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private Map<String, FormatStoreManager> formatStoreManagersByFormat;

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private SqlSearchUtils searchUtils;

  @Mock
  private SqlSearchPermissionManager sqlSearchPermissionManager;

  @InjectMocks
  private SqlSearchService underTest;

  @Test
  public void shouldSplitRepositoryNameFilter() {
    SearchRequest searchRequest = SearchRequest.builder().searchFilter(REPOSITORY_NAME,
        String.format("%s %s   %s", REPO_1, REPO_2, REPO_3)).build();

    underTest.count(searchRequest);

    verify(repositoryManager).get(REPO_1);
    verify(repositoryManager).get(REPO_2);
    verify(repositoryManager).get(REPO_3);
    verify(repositoryManager, never()).get(EMPTY);
  }
}
