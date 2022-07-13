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
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.maven.ContentDisposition;
import org.sonatype.nexus.repository.maven.internal.MavenDefaultRepositoriesContributor;
import org.sonatype.nexus.repository.maven.internal.recipes.Maven2GroupRecipe;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Upgrade to update contentDisposition of default maven repositories
 */
@Named
@Singleton
public class MavenDefaultReposUpgrade_1_17
    implements DatabaseMigrationStep
{
  private static final String FIND_ATTRIBUTES_BY_NAME = "SELECT attributes from repository " +
      "WHERE name = ?;";

  private static final String UPDATE_ATTRIBUTES_BY_NAME = "UPDATE repository " +
      "SET attributes = ? " +
      "WHERE name = ?;";

  private final MavenDefaultRepositoriesContributor defaultRepositoriesContributor;

  private final ObjectMapper mapper;

  @Inject
  public MavenDefaultReposUpgrade_1_17(final MavenDefaultRepositoriesContributor defaultRepositoriesContributor) {
    this.defaultRepositoriesContributor = defaultRepositoriesContributor;
    this.mapper = new ObjectMapper();
  }

  @Override
  public Optional<String> version() {
    return Optional.of("1.17");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    this.defaultRepositoriesContributor
        .getRepositoryConfigurations()
        .stream()
        .filter(configuration -> !Maven2GroupRecipe.NAME.equals(configuration.getRecipeName()))
        .map(Configuration::getRepositoryName)
        .forEach(name -> this.update(connection, name));
  }

  private void update(final Connection connection, final String repositoryName) {
    try {
      ObjectNode attributes = getCurrentAttributes(connection, repositoryName);

      ObjectNode mavenAttributes = (ObjectNode) attributes.get("maven");

      if (mavenAttributes != null) {
        JsonNode current = mavenAttributes.get("contentDisposition");
        // should put the value only if it is not present
        if (current == null) {
          mavenAttributes.put("contentDisposition", ContentDisposition.INLINE.name());
          attributes.set("maven", mavenAttributes);
        }
      }
      else {
        ObjectNode mavenNode = mapper.createObjectNode();
        mavenNode.put("contentDisposition", ContentDisposition.INLINE.name());
        attributes.set("maven", mavenNode);
      }

      updateAttributes(connection, repositoryName, mapper.writeValueAsBytes(attributes));
    }
    catch (SQLException | JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private ObjectNode getCurrentAttributes(Connection connection, String repositoryName)
      throws SQLException, JsonProcessingException
  {
    try (PreparedStatement ps = connection.prepareStatement(FIND_ATTRIBUTES_BY_NAME)) {
      ps.setString(1, repositoryName);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        String attributes = rs.getString(1);
        return (ObjectNode) mapper.readTree(attributes);
      }
      else {
        return mapper.createObjectNode();
      }
    }
  }

  private void updateAttributes(Connection connection, String repositoryName, byte[] attributes)
      throws SQLException
  {
    try (PreparedStatement ps = connection.prepareStatement(UPDATE_ATTRIBUTES_BY_NAME)) {
      if (isH2(connection)) {
        ps.setBytes(1, attributes);
      }
      else {
        ps.setString(1, new String(attributes, UTF_8));
      }
      ps.setString(2, repositoryName);

      ps.executeUpdate();
    }
  }
}
