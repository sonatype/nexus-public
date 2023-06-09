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
package org.sonatype.nexus.repository.content.search.upgrade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.repository.internal.search.index.task.SearchUpdateTask;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonatype.nexus.repository.search.index.SearchUpdateService.SEARCH_INDEX_OUTDATED;

/**
 * Database upgrade that marks existing repositories (at the time of the upgrade) as needing to be re-indexed.
 *
 * {@link SearchUpdateTask} will check for the marked repositories and re-index them.
 */
public abstract class SearchIndexUpgrade
    extends ComponentSupport
    implements DatabaseMigrationStep
{
  private static final String SELECT_REPOSITORIES = "select id, name, recipe_name from repository";

  private static final String SELECT_REPOSITORY_ATTRIBUTES =
      "select attributes from %s_content_repository where config_repository_id = ?";

  private static final String UPDATE_REPOSITORY_ATTRIBUTES =
      "update %s_content_repository set attributes = ? where config_repository_id = ?";

  public static final String ID = "id";

  public static final String NAME = "name";

  public static final String RECIPE_NAME = "recipe_name";

  private Map<String, Recipe> recipes;

  @Inject
  public final void inject(final Map<String, Recipe> recipes) {
    this.recipes = checkNotNull(recipes);
  }

  public boolean test(final String format) {
    // default implementation is to re-index all formats
    return true;
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    log.info("Searching for repositories that need a search index update");

    ObjectMapper objectMapper = new ObjectMapper();
    List<Repo> repos = getRepos(connection);

    for (Repo repo : repos) {
      if (test(repo.format)) {
        String attributes = getRepositoryAttributes(connection, repo);
        if (attributes != null) {
          // add the outdated flag to the existing attributes
          ObjectNode json = (ObjectNode) objectMapper.readTree(attributes);
          json.put(SEARCH_INDEX_OUTDATED, true);
          setRepositoryAttributes(connection, repo, objectMapper.writeValueAsBytes(json));
        }
      }
    }
  }

  private List<Repo> getRepos(final Connection connection) throws SQLException {
    List<Repo> result = new ArrayList<>();
    try (Statement s = connection.createStatement()) {
      ResultSet rs = s.executeQuery(SELECT_REPOSITORIES);
      while (rs.next()) {
        String id = rs.getString(ID);
        String name = rs.getString(NAME);
        String recipeName = rs.getString(RECIPE_NAME);

        Recipe recipe = recipes.get(recipeName);
        if (recipe != null) {
          if (!(recipe.getType() instanceof GroupType)) {
            String format = recipe.getFormat().getValue();
            result.add(new Repo(id, name, format));
          }
        }
        else {
          log.warn("Unable to upgrade search index because no recipe found {}", recipeName);
        }
      }
    }
    return result;
  }

  // Since the attributes field is non-null, this method returns null only if the row could not be found
  @Nullable
  private String getRepositoryAttributes(final Connection connection, final Repo repo) throws SQLException {
    String query = String.format(SELECT_REPOSITORY_ATTRIBUTES, repo.format);
    try (PreparedStatement ps = connection.prepareStatement(query)) {
      ps.setString(1, repo.id);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        return rs.getString(1);
      }
      else {
        log.warn("Could not find row in {}_content_repository for config_repository_id = {}", repo.format, repo.id);
        return null;
      }
    }
  }

  private void setRepositoryAttributes(final Connection connection, final Repo repo, final byte[] attributes)
      throws SQLException
  {
    log.info("Marking {} repository for search index update", repo.name);
    String update = String.format(UPDATE_REPOSITORY_ATTRIBUTES, repo.format);
    try (PreparedStatement ps = connection.prepareStatement(update)) {
      if (isH2(connection)) {
        ps.setBytes(1, attributes);
      }
      else {
        ps.setString(1, new String(attributes, UTF_8));
      }
      ps.setString(2, repo.id);

      ps.executeUpdate();
    }
  }

  // Simple data structure for holding data about a Repository Configuration.
  private static class Repo
  {
    public String id;

    public String name;

    public String format;

    public Repo(final String id, final String name, final String format) {
      this.id = id;
      this.name = name;
      this.format = format;
    }
  }
}
