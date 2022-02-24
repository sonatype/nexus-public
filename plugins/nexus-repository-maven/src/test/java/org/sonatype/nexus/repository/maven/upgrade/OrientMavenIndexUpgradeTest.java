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
package org.sonatype.nexus.repository.maven.upgrade;

import java.util.Map;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class OrientMavenIndexUpgradeTest
    extends MavenUpgradeTestSupport
{
  private OrientMavenIndexUpgrade underTest;

  @Before
  public void setUp() {
    try (ODatabaseDocumentTx db = configDatabase.getInstance().connect()) {
      repository("hosted", "maven2-hosted");
      repository("proxy", "maven2-proxy");
      repository("group", "maven2-group");
    }

    try (ODatabaseDocumentTx db = componentDatabase.getInstance().connect()) {
      bucket("hosted");
      bucket("proxy");
      bucket("group");
    }

    underTest = new OrientMavenIndexUpgrade(configDatabase.getInstanceProvider(),
        componentDatabase.getInstanceProvider());
  }

  @Test
  public void testMigrate() throws Exception {
    underTest.apply();

    assertSearchIndexOutdated("hosted", true);
    assertSearchIndexOutdated("proxy", true);
    assertSearchIndexOutdated("group", null);
  }

  private void assertSearchIndexOutdated(final String repositoryName, Object value) {
    try (ODatabaseDocumentTx db = componentDatabase.getInstance().connect()) {
      OIndex<?> idx = db.getMetadata().getIndexManager().getIndex(I_BUCKET_REPOSITORY_NAME);
      OIdentifiable idf = (OIdentifiable) idx.get(repositoryName);
      assertThat(idf, notNullValue());
      ODocument asset = idf.getRecord();
      assertThat(asset, notNullValue());
      Map<String, Object> attributes = asset.field(P_ATTRIBUTES);
      assertThat(attributes.get("maven_search_index_outdated"), equalTo(value));
    }
  }
}
