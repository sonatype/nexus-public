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

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.event.EventManager;

import com.spotify.docker.client.shaded.javax.annotation.Priority;

import static org.awaitility.Awaitility.await;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_SEARCH_ENABLED;

/**
 * @since 3.next
 */
@Named
@Singleton
@FeatureFlag(name = DATASTORE_SEARCH_ENABLED)
@Priority(Integer.MAX_VALUE)
public class SqlSearchTestSystem
    implements SearchTestSystem
{

  @Inject
  public EventManager eventManager;

  @Override
  public void waitForSearch() {
    await().atMost(30, TimeUnit.SECONDS).until(eventManager::isCalmPeriod);
  }
}
