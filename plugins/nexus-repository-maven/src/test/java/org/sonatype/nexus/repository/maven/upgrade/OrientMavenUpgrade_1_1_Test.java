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

public class OrientMavenUpgrade_1_1_Test
    extends MavenUpgradeTestSupport
{
  private OrientMavenUpgrade_1_1 underTest;

  @Before
  public void setUp() {
    try (ODatabaseDocumentTx db = configDatabase.getInstance().connect()) {
      repository("maven2Hosted", "maven2-hosted");
    }

    try (ODatabaseDocumentTx db = componentDatabase.getInstance().connect()) {
      bucket("maven2Hosted");
      OIndex<?> bucketIdx = db.getMetadata().getIndexManager().getIndex(I_BUCKET_REPOSITORY_NAME);
      asset(bucketIdx, "maven2Hosted", "asm/asm/3.1/asm-3.1.jar", "ARTIFACT");
      asset(bucketIdx, "maven2Hosted", "asm/asm/3.1/asm-3.1.pom", "ARTIFACT");
      asset(bucketIdx, "maven2Hosted", "asm/asm/3.1/asm-3.1.jar.sha1", "ARTIFACT_SUBORDINATE");
      asset(bucketIdx, "maven2Hosted", "asm/asm/3.1/asm-3.1.pom.sha1", "ARTIFACT_SUBORDINATE");
      asset(bucketIdx, "maven2Hosted", ".index/nexus-maven-repository-index.gz", "OTHER");
      asset(bucketIdx, "maven2Hosted", ".index/nexus-maven-repository-index.properties", "OTHER");
    }

    underTest = new OrientMavenUpgrade_1_1(configDatabase.getInstanceProvider(),
        componentDatabase.getInstanceProvider());
  }

  @Test
  public void upgradeStepUpdatesAssetKind() throws Exception {
    assertAssetKinds("OTHER");

    underTest.apply();

    assertAssetKinds("REPOSITORY_INDEX");
  }

  @Test
  public void upgradeStepCanBeCalledMultipleTimes() throws Exception {
    assertAssetKinds("OTHER");

    underTest.apply();
    underTest.apply();

    assertAssetKinds("REPOSITORY_INDEX");
  }

  protected void assertAssetKind(final OIndex<?> idx, final String name, final String assetKind) {
    OIdentifiable idf = (OIdentifiable) idx.get(name);
    assertThat(idf, notNullValue());
    ODocument asset = idf.getRecord();
    assertThat(asset, notNullValue());
    Map<String, Object> attributes = asset.field(P_ATTRIBUTES);
    Map<String, Object> maven2Attributes = (Map<String, Object>) attributes.get("maven2");
    assertThat(maven2Attributes.get("asset_kind"), equalTo(assetKind));
  }

  private void assertAssetKinds(String expectedIndexFileAssetKind) {
    try (ODatabaseDocumentTx db = componentDatabase.getInstance().connect()) {
      OIndex<?> idx = db.getMetadata().getIndexManager().getIndex(I_ASSET_NAME);
      assertAssetKind(idx, "asm/asm/3.1/asm-3.1.jar", "ARTIFACT");
      assertAssetKind(idx, "asm/asm/3.1/asm-3.1.pom", "ARTIFACT");
      assertAssetKind(idx, "asm/asm/3.1/asm-3.1.jar.sha1", "ARTIFACT_SUBORDINATE");
      assertAssetKind(idx, "asm/asm/3.1/asm-3.1.pom.sha1", "ARTIFACT_SUBORDINATE");
      assertAssetKind(idx, ".index/nexus-maven-repository-index.gz", expectedIndexFileAssetKind);
      assertAssetKind(idx, ".index/nexus-maven-repository-index.properties", expectedIndexFileAssetKind);
    }
  }
}
