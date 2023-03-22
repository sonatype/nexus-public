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
package org.sonatype.nexus.quartz.internal.store;

import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup;
import org.sonatype.nexus.quartz.internal.AbstractJobStoreTest;
import org.sonatype.nexus.quartz.internal.JobStoreJdbcProvider;
import org.sonatype.nexus.quartz.internal.datastore.QuartzDAO;
import org.sonatype.nexus.quartz.internal.datastore.QuartzJobDataTypeHandler;
import org.sonatype.nexus.testdb.DataSessionRule;

import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.quartz.spi.JobStore;

@Category(SQLTestGroup.class)
public class ClusteredJobStoreJdbcStoreProviderTest
    extends AbstractJobStoreTest
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule().handle(new QuartzJobDataTypeHandler()).access(QuartzDAO.class);

  private JobStore jobStore;

  @Override
  protected JobStore createJobStore(final String name) {

    jobStore =
        new JobStoreJdbcProvider(new ConfigStoreConnectionProvider(sessionRule), new SimpleNodeAccess(), true).get();
    jobStore.setInstanceId("CLUSTERED_TEST");
    jobStore.setInstanceName(name);

    return jobStore;
  }

  @Override
  protected void destroyJobStore(final String name) {
    jobStore.shutdown();
    jobStore = null;
  }
}
