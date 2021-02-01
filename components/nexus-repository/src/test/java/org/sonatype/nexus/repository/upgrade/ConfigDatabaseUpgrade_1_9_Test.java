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
package org.sonatype.nexus.repository.upgrade;

import java.util.Collections;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class ConfigDatabaseUpgrade_1_9_Test
    extends TestSupport
{
  private static final String BLOBSTORE_COMPACT = "blobstore.compact";

  private static final String BLOBSTORE_GROUP_MEMBER_REMOVAL = "blobstore.group.memberRemoval";

  private static final String BLOBSTORE_REBUILD_COMPONENT_DB = "blobstore.rebuildComponentDB";

  private static final String NAME = ".name";

  private static final String TYPE_ID = ".typeId";

  private static final String VALUE_DATA = "value_data";

  private static final String JOB_DATA_MAP = "jobDataMap";

  private static final String MULTINODE = "multinode";

  private static final String QUARTZ_JOB_DETAIL_CLASS = new OClassNameBuilder()
      .prefix("quartz")
      .type("job_detail")
      .build();

  @Rule
  public DatabaseInstanceRule configDatabase = DatabaseInstanceRule.inMemory("test_config");

  private ConfigDatabaseUpgrade_1_9 underTest;

  @Before
  public void setup() {
    try (ODatabaseDocumentTx db = configDatabase.getInstance().connect()) {
      OSchema schema = db.getMetadata().getSchema();
      schema.createClass(QUARTZ_JOB_DETAIL_CLASS);
      job("compact", BLOBSTORE_COMPACT, true);
      job("rebuild", BLOBSTORE_REBUILD_COMPONENT_DB, true); // true here for testing
      job("remove-member", BLOBSTORE_GROUP_MEMBER_REMOVAL, null); // null here for testing
    }

    underTest = new ConfigDatabaseUpgrade_1_9(configDatabase.getInstanceProvider());
  }


  @Test
  public void upgradeSetsMultiNodeFalseOnCompact() throws Exception {
    underTest.apply();

    ODocument compactBlobstoreJob = findJob(BLOBSTORE_COMPACT);
    assertThat(getJobDataMap(compactBlobstoreJob).get(MULTINODE), is("false"));

    ODocument rebuildComponentDB = findJob(BLOBSTORE_REBUILD_COMPONENT_DB);
    assertThat(getJobDataMap(rebuildComponentDB).get(MULTINODE), is("true"));

    ODocument memberRemovalBlobstoreJob =   findJob(BLOBSTORE_GROUP_MEMBER_REMOVAL);
    assertThat(getJobDataMap(memberRemovalBlobstoreJob).get(MULTINODE), nullValue());
  }

  private ODocument findJob(final String typeId) {
    try (ODatabaseDocumentTx db = configDatabase.getInstance().connect()) {
      return StreamSupport.stream(db.browseClass(QUARTZ_JOB_DETAIL_CLASS).spliterator(), false).filter(job -> {
        return typeId.equals(getJobDataMap(job).get(TYPE_ID));
      }).findFirst().get();
    }
  }

  private static void job(final String name, final String typeId, final Boolean multinode) {
    ODocument job = new ODocument(QUARTZ_JOB_DETAIL_CLASS);

    Map<String, Object> jsonDataMap;
    if (multinode != null) {
      jsonDataMap = ImmutableMap.of(NAME, name, TYPE_ID, typeId, MULTINODE, multinode.toString());
    }
    else {
      jsonDataMap = ImmutableMap.of(NAME, name, TYPE_ID, typeId);
    }
    job.field(VALUE_DATA, Collections.singletonMap(JOB_DATA_MAP, jsonDataMap));
    job.save();
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> getJobDataMap(final ODocument document) {
    Map<String, Object> value = document.field(VALUE_DATA);

    return (Map<String, Object>) value.get(JOB_DATA_MAP);
  }
}
