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
package org.sonatype.nexus.repository.content.blobstore.metrics;

import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.testdb.DataSessionRule;

import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

public class BlobStoreMetricsStoreImplTest
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule(DEFAULT_DATASTORE_NAME).access(BlobStoreMetricsDAO.class);

  @Test
  public void renameMetricsDelegatesToDAO() {
    BlobStoreMetricsStoreImpl store = new BlobStoreMetricsStoreImpl((DataSessionSupplier) sessionRule);
    store.initializeMetrics("old-store");

    store.rename("old-store", "new-store");

    assertThat(store.get("new-store"), is(notNullValue()));
    assertThat(store.get("new-store").getBlobStoreName(), is("new-store"));
    assertThat(store.get("old-store"), is(nullValue()));
  }
}
