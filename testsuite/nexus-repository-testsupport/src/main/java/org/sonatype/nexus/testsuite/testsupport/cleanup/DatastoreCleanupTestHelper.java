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
package org.sonatype.nexus.testsuite.testsupport.cleanup;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.FeatureFlag;

import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_ENABLED;

/**
 * Under SQL Cleanup uses the component/assets table thus rarely needs to wait on changes once REST endpoints have
 * returned.
 */
@Named
@Singleton
@FeatureFlag(name = DATASTORE_ENABLED)
public class DatastoreCleanupTestHelper
    implements CleanupTestHelper
{
  @Override
  public void waitForMixedSearch() {
    // noop
  }

  @Override
  public void waitForComponentsIndexed(final int count) {
    // noop
  }

  @Override
  public void waitForLastDownloadSet(final int count) {
    // noop
  }

  @Override
  public void awaitLastBlobUpdatedTimePassed(final int time) {
    try {
      Thread.sleep(time * 1000L);
    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void waitForIndex() {
    // noop
  }
}
