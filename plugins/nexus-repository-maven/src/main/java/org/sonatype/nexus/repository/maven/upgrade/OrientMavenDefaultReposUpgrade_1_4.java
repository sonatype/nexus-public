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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.DependsOn;
import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.orient.DatabaseUpgradeSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.maven.internal.MavenDefaultRepositoriesContributor;
import org.sonatype.nexus.repository.maven.internal.recipes.Maven2HostedRecipe;
import org.sonatype.nexus.repository.maven.internal.recipes.Maven2ProxyRecipe;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import static java.lang.String.format;
import static org.sonatype.nexus.orient.DatabaseInstanceNames.CONFIG;

/**
 * Migration step to add content disposition to default maven repositories
 */
@Named
@Singleton
@Upgrades(model = MavenModel.NAME, from = "1.3", to = "1.4")
@DependsOn(model = DatabaseInstanceNames.CONFIG, version = "1.9", checkpoint = true)
public class OrientMavenDefaultReposUpgrade_1_4
    extends DatabaseUpgradeSupport
{
  private static final String P_ATTRIBUTES = "attributes";

  private static final String MAVEN = "maven";

  private static final String FIND_REPO= "SELECT * " +
      "FROM repository " +
      "WHERE repository_name = '%s';";
  private static final String UPDATE_REPOS = "UPDATE repository " +
      "SET attributes.maven.contentDisposition = 'INLINE' " +
      "WHERE repository_name = '%s';";

  private final MavenDefaultRepositoriesContributor defaultRepositoriesContributor;

  private final Provider<DatabaseInstance> instanceProvider;

  private final List<String> validRecipes;

  @Inject
  public OrientMavenDefaultReposUpgrade_1_4(
      final MavenDefaultRepositoriesContributor defaultRepositoriesContributor,
      @Named(CONFIG) final Provider<DatabaseInstance> instanceProvider)
  {
    this.defaultRepositoriesContributor = defaultRepositoriesContributor;
    this.instanceProvider = instanceProvider;
    this.validRecipes = Arrays.asList(Maven2HostedRecipe.NAME , Maven2ProxyRecipe.NAME);
  }

  @Override
  public void apply() throws Exception {
    withDatabase(instanceProvider, this::updateRecords);
  }

  /**
   * Updates the default repos records adding the contentDisposition attribute
   *
   * @param db {@link ODatabaseDocumentTx} a database document object to perform the transaction
   */
  private void updateRecords(ODatabaseDocumentTx db) {
    this.defaultRepositoriesContributor
        .getRepositoryConfigurations()
        .stream()
        .filter(configuration -> validRecipes.contains(configuration.getRecipeName()))
        .map(Configuration::getRepositoryName)
        .forEach(name -> update(db, name, getContentDisposition(db, name)));
  }

  private Optional<String> getContentDisposition(ODatabaseDocumentTx db, String repositoryName) {
    Map<String, Map<String, String>> attributes =
        db.<List<ODocument>>query(new OSQLSynchQuery<ODocument>(format(FIND_REPO, repositoryName)))
            .stream()
            .map(document -> (Map<String, Map<String, String>>) document.field(P_ATTRIBUTES, OType.EMBEDDEDMAP))
            .findFirst()
            .orElse(Collections.emptyMap());

    return Optional.ofNullable(attributes.get(MAVEN))
        .map(maven -> maven.get("contentDisposition"));
  }

  private void update(ODatabaseDocumentTx db, String repositoryName, Optional<String> contentDisposition) {
    if (!contentDisposition.isPresent()) {
      db.command(new OCommandSQL(format(UPDATE_REPOS, repositoryName))).execute();
    }
  }
}
