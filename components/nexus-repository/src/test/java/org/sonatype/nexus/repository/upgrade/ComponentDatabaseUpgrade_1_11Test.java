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

import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ComponentDatabaseUpgrade_1_11Test // NOSONAR
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule configDatabase = DatabaseInstanceRule.inMemory("config");

  @Rule
  public DatabaseInstanceRule componentDatabase = DatabaseInstanceRule.inMemory("component");

  private ComponentDatabaseUpgrade_1_11 upgrade;

  private static final String P_REPOSITORY_NAME = "repository_name";

  private static final String P_RECIPE_NAME = "recipe_name";

  private static final String REPOSITORY_CLASS = new OClassNameBuilder().type("repository").build();

  private static final String BROWSE_NODE_CLASS = new OClassNameBuilder().type("browse_node").build();

  private static final String DOCKER_PROXY_REPOSITORY = "docker-proxy";

  private static final String DOCKER_HOSTED_REPOSITORY = "docker-hosted";

  private static final String NPM_PROXY_REPOSITORY = "npm-proxy";

  @Before
  public void setUp() throws Exception {
    try (ODatabaseDocumentTx configDb = configDatabase.getInstance().connect()) {
      OSchema configSchema = configDb.getMetadata().getSchema();
      OClass repositoryType = configSchema.createClass(REPOSITORY_CLASS);
      repositoryType.createProperty(P_REPOSITORY_NAME,
          OType.STRING);
      repositoryType.createProperty(P_RECIPE_NAME, OType.STRING);

      repository(DOCKER_PROXY_REPOSITORY, "docker-proxy");
      repository(DOCKER_HOSTED_REPOSITORY, "docker-hosted");
      repository(NPM_PROXY_REPOSITORY, "npm-proxy");
    }

    try (ODatabaseDocumentTx componentDb = componentDatabase.getInstance().connect()) {
      OSchema componentSchema = componentDb.getMetadata().getSchema();
      OClass bucketType = componentSchema.createClass(BROWSE_NODE_CLASS);
      bucketType.createProperty(P_REPOSITORY_NAME, OType.STRING);

      browseNode(DOCKER_PROXY_REPOSITORY);
      browseNode(DOCKER_HOSTED_REPOSITORY);
      browseNode(NPM_PROXY_REPOSITORY);
    }

    upgrade = new ComponentDatabaseUpgrade_1_11(
        configDatabase.getInstanceProvider(),
        componentDatabase.getInstanceProvider());
  }

  @Test
  public void shouldRemoveBrowseNodeEntriesForDockerRepositories() throws Exception {
    assertThat(getBrowseNodeCountForRepository(DOCKER_PROXY_REPOSITORY), is(1L));
    assertThat(getBrowseNodeCountForRepository(DOCKER_HOSTED_REPOSITORY), is(1L));
    assertThat(getBrowseNodeCountForRepository(NPM_PROXY_REPOSITORY), is(1L));

    upgrade.apply();

    assertThat(getBrowseNodeCountForRepository(DOCKER_PROXY_REPOSITORY), is(0L));
    assertThat(getBrowseNodeCountForRepository(DOCKER_HOSTED_REPOSITORY), is(0L));
    assertThat(getBrowseNodeCountForRepository(NPM_PROXY_REPOSITORY), is(1L));
  }

  private long getBrowseNodeCountForRepository(final String repositoryName) {
    try (ODatabaseDocumentTx db = componentDatabase.getInstance().connect()) {
      List<ODocument> result = db.command(new OCommandSQL(
          "select count(*) from browse_node where repository_name = ?")).execute(repositoryName);
      return result.get(0).field("count");
    }
  }

  private static void repository(final String name, final String recipe) {
    ODocument repository = new ODocument(REPOSITORY_CLASS);
    repository.field(P_REPOSITORY_NAME, name);
    repository.field(P_RECIPE_NAME, recipe);
    repository.save();
  }

  private static void browseNode(final String repositoryName) {
    ODocument document = new ODocument(BROWSE_NODE_CLASS);
    document.field(P_REPOSITORY_NAME, repositoryName);
    document.save();
  }
}
