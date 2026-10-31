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
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sonatype.nexus.repository.maven.ContentDisposition;
import org.sonatype.nexus.repository.maven.ContentDispositionHandler;
import org.sonatype.nexus.repository.maven.internal.recipes.Maven2HostedRecipe;
import org.sonatype.nexus.repository.maven.internal.recipes.Maven2ProxyRecipe;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Upgrade to update contentDisposition of default maven repositories.
 *
 * <p>
 * Operates on the {@code repository} table directly via the migration {@link Connection}. The set of
 * default repositories is the fixed, non-group default Maven repositories ({@code maven-releases},
 * {@code maven-snapshots}, {@code maven-central}); the default group ({@code maven-public}) is excluded.
 * These names are inlined rather than obtained from {@code MavenDefaultRepositoriesContributor}, whose
 * {@code getRepositoryConfigurations()} calls {@code RepositoryManager.newConfiguration()} and so would
 * force the REPOSITORIES-phase {@code RepositoryManager} to initialize during the UPGRADE phase.
 * </p>
 */
@Component
public class MavenDefaultReposUpgrade_1_17
    implements DatabaseMigrationStep
{
  // These are the well-known default repository names created at install time. They are hardcoded
  // deliberately: resolving them via RepositoryManager/MavenDefaultRepositoriesContributor would force a
  // later-phase service to initialize during the UPGRADE phase. Trade-off: if an operator has renamed
  // the defaults, this step is a no-op for those repositories, which is acceptable for a default-seeding
  // migration.
  private static final List<String> DEFAULT_REPOSITORY_NAMES =
      List.of("maven-releases", "maven-snapshots", "maven-central");

  // Only update rows that are actually Maven hosted/proxy repositories, so we never inject a "maven"
  // attribute block into a non-Maven repository that happens to reuse a default name. References the recipe
  // NAME constants (compile-time String constants, inlined by javac -- no class initialization is triggered,
  // so this carries none of the phase-init risk of resolving the recipe beans themselves).
  private static final Set<String> MAVEN_RECIPES = Set.of(Maven2HostedRecipe.NAME, Maven2ProxyRecipe.NAME);

  private static final String MAVEN_ATTRIBUTES_KEY = "maven";

  private static final Logger log = LoggerFactory.getLogger(MavenDefaultReposUpgrade_1_17.class);

  private static final String FIND_ATTRIBUTES_BY_NAME = "SELECT attributes, recipe_name from repository " +
      "WHERE name = ?;";

  private static final String UPDATE_ATTRIBUTES_BY_NAME = "UPDATE repository " +
      "SET attributes = ? " +
      "WHERE name = ?;";

  private final ObjectMapper mapper;

  public MavenDefaultReposUpgrade_1_17() {
    this.mapper = new ObjectMapper();
  }

  @Override
  public Optional<String> version() {
    return Optional.of("1.17");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    DEFAULT_REPOSITORY_NAMES.forEach(name -> this.update(connection, name));
  }

  private void update(final Connection connection, final String repositoryName) {
    try {
      Optional<ObjectNode> maybeAttributes = getMavenRepositoryAttributes(connection, repositoryName);
      if (maybeAttributes.isEmpty()) {
        return; // absent, non-Maven, or malformed — logged in getMavenRepositoryAttributes
      }
      ObjectNode attributes = maybeAttributes.get();

      JsonNode mavenNode = attributes.get(MAVEN_ATTRIBUTES_KEY);
      if (mavenNode != null && !mavenNode.isObject()) {
        log.warn("Skipping repository '{}': 'maven' attribute is present but not a JSON object", repositoryName);
        return;
      }
      ObjectNode mavenAttributes = mavenNode != null ? (ObjectNode) mavenNode : mapper.createObjectNode();

      // Only write when contentDisposition is absent (idempotent: a re-run issues no UPDATE).
      if (mavenAttributes.get(ContentDispositionHandler.CONTENT_DISPOSITION_CONFIG_KEY) == null) {
        mavenAttributes.put(ContentDispositionHandler.CONTENT_DISPOSITION_CONFIG_KEY, ContentDisposition.INLINE.name());
        attributes.set(MAVEN_ATTRIBUTES_KEY, mavenAttributes);
        updateAttributes(connection, repositoryName, mapper.writeValueAsBytes(attributes));
      }
    }
    catch (SQLException | JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Reads the attributes of {@code repositoryName} only when it exists and is a Maven hosted/proxy
   * repository; returns empty (and logs why) for an absent row, a non-Maven recipe, or non-object
   * attributes, so the caller never writes a {@code maven} block into the wrong row.
   */
  private Optional<ObjectNode> getMavenRepositoryAttributes(
      final Connection connection,
      final String repositoryName) throws SQLException, JsonProcessingException
  {
    try (PreparedStatement ps = connection.prepareStatement(FIND_ATTRIBUTES_BY_NAME)) {
      ps.setString(1, repositoryName);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          log.info("Default Maven repository '{}' not present; skipping contentDisposition update",
              repositoryName);
          return Optional.empty();
        }
        String recipe = rs.getString("recipe_name");
        if (!MAVEN_RECIPES.contains(recipe)) {
          log.info("Repository '{}' has recipe '{}', not a default Maven hosted/proxy repository; skipping",
              repositoryName, recipe);
          return Optional.empty();
        }
        JsonNode parsed = mapper.readTree(rs.getString("attributes"));
        if (!parsed.isObject()) {
          log.warn("Skipping repository '{}': attributes is not a JSON object", repositoryName);
          return Optional.empty();
        }
        return Optional.of((ObjectNode) parsed);
      }
    }
  }

  private void updateAttributes(Connection connection, String repositoryName, byte[] attributes) throws SQLException {
    try (PreparedStatement ps = connection.prepareStatement(UPDATE_ATTRIBUTES_BY_NAME)) {
      setJsonParameter(ps, 1, attributes, isH2(connection));
      ps.setString(2, repositoryName);
      ps.executeUpdate();
    }
  }
}
