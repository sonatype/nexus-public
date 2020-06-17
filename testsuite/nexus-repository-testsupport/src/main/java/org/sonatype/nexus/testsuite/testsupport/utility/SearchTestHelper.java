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

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.repository.search.query.SearchQueryService;

@Named
@Singleton
public class SearchTestHelper
{
  @Inject
  public SearchService searchService;

  @Inject
  public SearchQueryService searchQueryService;

  @Inject
  public EventManager eventManager;

  /**
   * Waits for indexing to finish and makes sure any updates are available to search.
   *
   * General flow is component/asset events -> bulk index requests -> search indexing.
   */
  public void waitForSearch() throws Exception {
    NexusPaxExamSupport.waitFor(eventManager::isCalmPeriod);
    searchService.flush(false); // no need for full fsync here
    NexusPaxExamSupport.waitFor(searchService::isCalmPeriod);
  }
}
