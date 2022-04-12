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
package org.sonatype.nexus.content.maven.upgrade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.content.search.upgrade.SearchIndexUpgrade;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonatype.nexus.repository.search.index.SearchUpdateService.SEARCH_INDEX_OUTDATED;

/**
 * Upgrade step that marks existing maven repositories (at the time of the upgrade) as needing to be re-indexed.
 *
 * @deprecated New database migrations should use {@link SearchIndexUpgrade}
 *
 * @since 3.38
 */
@Deprecated
public abstract class MavenIndexUpgrade
    extends ComponentSupport
    implements DatabaseMigrationStep
{
  private static final String SELECT_MAVEN_REPOSITORIES =
      "select id from repository where recipe_name in ('maven2-hosted', 'maven2-proxy')";

  private static final String SELECT_REPOSITORY_ATTRIBUTES =
      "select attributes from maven2_content_repository where config_repository_id = ?";

  private static final String UPDATE_REPOSITORY_ATTRIBUTES =
      "update maven2_content_repository set attributes = ? where config_repository_id = ?";

  public static final String ID = "id";

  @Override
  public void migrate(final Connection connection) throws Exception {
    log.info("Searching for maven repositories that need a search index update");

    ObjectMapper objectMapper = new ObjectMapper();

    List<String> repositoryIds = getRepositoryIds(connection);
    for (String id : repositoryIds) {
      String attributes = getRepositoryAttributes(connection, id);
      if (attributes != null) {
        // add the outdated flag to the existing attributes
        ObjectNode json = (ObjectNode) objectMapper.readTree(attributes);
        json.put(SEARCH_INDEX_OUTDATED, true);
        setRepositoryAttributes(connection, id, objectMapper.writeValueAsBytes(json));
      }
    }
  }

  private List<String> getRepositoryIds(final Connection connection) throws SQLException {
    List<String> repositoryIds = new ArrayList<>();
    try (Statement s = connection.createStatement()) {
      ResultSet rs = s.executeQuery(SELECT_MAVEN_REPOSITORIES);
      while (rs.next()) {
        repositoryIds.add(rs.getString(ID));
      }
    }
    return repositoryIds;
  }

  // Since the attributes field is non-null this returns null only if the row could not be found
  @Nullable
  private String getRepositoryAttributes(final Connection connection, final String repositoryId) throws SQLException {
    try (PreparedStatement ps = connection.prepareStatement(SELECT_REPOSITORY_ATTRIBUTES)) {
      ps.setString(1, repositoryId);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        return rs.getString(1);
      } else {
        log.warn("Could not find row in maven2_content_repository for config_repository_id = {}", repositoryId);
        return null;
      }
    }
  }

  private void setRepositoryAttributes(final Connection connection, final String repositoryId, final byte[] attributes)
      throws SQLException
  {
    try (PreparedStatement ps = connection.prepareStatement(UPDATE_REPOSITORY_ATTRIBUTES)) {
      if (isH2(connection)) {
        ps.setBytes(1, attributes);
      } else {
        ps.setString(1, new String(attributes, UTF_8));
      }
      ps.setString(2, repositoryId);

      ps.executeUpdate();
    }
  }
}
