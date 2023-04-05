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
package org.sonatype.nexus.testsuite.testsupport.system;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.search.index.ElasticSearchIndexService;
import org.sonatype.nexus.repository.search.query.ElasticSearchQueryService;

import org.awaitility.core.ConditionFactory;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

/**
 * @since 3.41
 */
@Named
@Singleton
public class ElasticSearchTestSystem
    implements SearchTestSystem
{

  @Inject
  public EventManager eventManager;

  @Inject
  public ElasticSearchIndexService indexService;

  @Inject
  public ElasticSearchQueryService elasticSearchQueryService;

  @Override
  public void waitForSearch() {
    await().atMost(30, SECONDS).until(eventManager::isCalmPeriod);
    indexService.flush(false); // no need for full fsync here
    await().atMost(30, SECONDS).until(indexService::isCalmPeriod);
  }

  @Override
  public ConditionFactory waitForSearchResults() {
    return await().atMost(30, SECONDS).pollInterval(1, SECONDS);
  }

  public ElasticSearchQueryService queryService() {
    return elasticSearchQueryService;
  }

}
