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

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.repository.maven.internal.MavenDefaultRepositoriesContributor;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Before;
import org.junit.Test;
import com.google.common.collect.ImmutableMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OrientMavenDefaultReposUpgrade_1_4Test
    extends MavenUpgradeTestSupport
{
  private OrientMavenDefaultReposUpgrade_1_4 migration;

  private ConfigurationData hostedRepo;

  private ConfigurationData proxyRepo;

  private ConfigurationData groupRepo;

  private ConfigurationData nonDefaultRepo;

  @Before
  public void setUp() {
    try (ODatabaseDocumentTx db = configDatabase.getInstance().connect()) {
      hostedRepo = createConfig("maven-snapshots", "maven2-hosted" , null);
      proxyRepo = createConfig("maven-releases", "maven2-proxy" , null);
      groupRepo = createConfig("maven-central", "maven2-group" , "ATTACHMENT");
      nonDefaultRepo = createConfig("maven-non-default", "maven2-hosted" , "ATTACHMENT");
    }

    MavenDefaultRepositoriesContributor mockContributor = mock(MavenDefaultRepositoriesContributor.class);
    when(mockContributor.getRepositoryConfigurations()).thenReturn(Arrays.asList(hostedRepo, proxyRepo, groupRepo));

    migration = new OrientMavenDefaultReposUpgrade_1_4(mockContributor, configDatabase.getInstanceProvider());
  }

  @Test
  public void testMigrationWorksAsExpected() throws Exception {
    migration.apply();

    assertContentDisposition(hostedRepo.getRepositoryName(), "INLINE");
    assertContentDisposition(proxyRepo.getRepositoryName(), "INLINE");

    //if it is a group repo , it shouldn't change
    assertContentDisposition(groupRepo.getRepositoryName(), "ATTACHMENT");

    //If it is a non-default repo , then the value shouldn't change
    assertContentDisposition(nonDefaultRepo.getRepositoryName(), "ATTACHMENT");
  }

  private void assertContentDisposition(String repositoryName, String expectedValue) {
    try (ODatabaseDocumentTx db = configDatabase.getInstance().connect()) {
      OIndex<?> idx = db.getMetadata().getIndexManager().getIndex(I_REPOSITORY_REPOSITORY_NAME);
      OIdentifiable idf = (OIdentifiable) idx.get(repositoryName);
      assertThat(idf, notNullValue());
      ODocument repository = idf.getRecord();
      assertThat(repository, notNullValue());
      Map<String, Map<String, Object>> attributes = repository.field(P_ATTRIBUTES);
      assertThat(attributes.get("maven").get("contentDisposition"), equalTo(expectedValue));
    }
  }

  private ConfigurationData createConfig(final String name, final String recipeName , String contentDisposition) {
    ConfigurationData config = new ConfigurationData();
    config.setName(name);
    config.setRecipeName(recipeName);
    config.setAttributes(ImmutableMap.of("maven", contentDisposition != null ?  ImmutableMap.of("contentDisposition", contentDisposition) : Collections.emptyMap()));

    repository(config.getName(), config.getRecipeName(), config.getAttributes());
    return config;
  }
}
