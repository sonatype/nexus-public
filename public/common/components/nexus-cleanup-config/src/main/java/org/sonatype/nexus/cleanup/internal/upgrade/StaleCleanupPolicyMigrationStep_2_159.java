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
package org.sonatype.nexus.cleanup.internal.upgrade;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Removes stale cleanup policy references from repository configurations.
 *
 * Some repositories carry cleanup.policyName values that no longer correspond to any row in the
 * cleanup_policy table. These entries arise from three sources:
 * <ul>
 * <li>The literal {@code "string"} placeholder submitted via Swagger UI before creation-time
 * validation was introduced.</li>
 * <li>The legacy {@code "None"} sentinel written by old UI versions to mean "no policy".</li>
 * <li>A real policy name whose cleanup_policy row was later deleted without cascading the
 * change to referencing repository configurations.</li>
 * </ul>
 *
 * Stale references cause {@code CleanupConfigurationValidator} to reject
 * {@code repositoryManager.update()} calls made by unrelated internal operations such as the
 * secrets migration task, aborting them with a {@code ConstraintViolationException}.
 *
 * This step reads every repository row, evaluates each policyName entry against the
 * cleanup_policy table, and removes any entry without a matching row. Only the
 * {@code policyName} key is modified; other keys within the cleanup attribute block are
 * preserved. When all policyName entries are stale and no other cleanup attributes remain,
 * the entire cleanup block is removed. Valid entries are preserved. The step is idempotent:
 * running it a second time on already-clean data is a no-op.
 *
 * Works on both H2 (JSON column type, bytes binding) and PostgreSQL (JSONB column type, string
 * binding) using {@link #setJsonParameter}.
 */
@Component
public class StaleCleanupPolicyMigrationStep_2_159
    implements DatabaseMigrationStep
{
  private static final Logger log = LoggerFactory.getLogger(StaleCleanupPolicyMigrationStep_2_159.class);

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String REPOSITORY_TABLE = "repository";

  private static final String CLEANUP_POLICY_TABLE = "cleanup_policy";

  private static final String CLEANUP_KEY = "cleanup";

  private static final String POLICY_NAME_KEY = "policyName";

  private static final String SELECT_VALID_POLICIES = "SELECT name FROM cleanup_policy";

  private static final String SELECT_REPOSITORIES =
      "SELECT id, name, attributes FROM repository ORDER BY id";

  private static final String UPDATE_ATTRIBUTES =
      "UPDATE repository SET attributes = ? WHERE id = ?";

  private record RepoUpdate(String id, String repoName, List<String> stale, byte[] newAttributes)
  {
  }

  @Override
  public Optional<String> version() {
    return Optional.of("2.159");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    if (!tableExists(connection, REPOSITORY_TABLE) || !tableExists(connection, CLEANUP_POLICY_TABLE)) {
      log.warn("Required tables 'repository' and/or 'cleanup_policy' not present — skipping migration");
      return;
    }

    Set<String> validPolicies = loadValidPolicyNames(connection);
    log.debug("Found {} valid cleanup policies to cross-reference", validPolicies.size());

    int fixed = fixStaleReferences(connection, validPolicies);

    if (fixed > 0) {
      log.warn("Removed stale cleanup policy references from {} repository configuration(s)", fixed);
    }
    else {
      log.debug("No stale cleanup policy references found — nothing to migrate");
    }
  }

  private Set<String> loadValidPolicyNames(final Connection connection) throws SQLException {
    Set<String> names = new HashSet<>();
    try (PreparedStatement ps = connection.prepareStatement(SELECT_VALID_POLICIES);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        names.add(rs.getString("name"));
      }
    }
    return names;
  }

  private int fixStaleReferences(final Connection connection, final Set<String> validPolicies) throws Exception {
    boolean h2 = isH2(connection);

    // Read phase: stream all rows and collect pending updates; cursor closed before any write
    // avoids JDBC undefined behaviour from issuing UPDATE statements on the same connection
    // while a SELECT ResultSet is still open.
    List<RepoUpdate> pending = new ArrayList<>();
    try (PreparedStatement sel = connection.prepareStatement(SELECT_REPOSITORIES);
        ResultSet rs = sel.executeQuery()) {
      while (rs.next()) {
        evaluateRow(rs, validPolicies, h2).ifPresent(pending::add);
      }
    }

    // Write phase: apply all updates after the SELECT cursor is closed
    for (RepoUpdate update : pending) {
      log.info("Removing stale cleanup policy references {} from repository '{}'",
          update.stale(), update.repoName());
      applyUpdate(connection, update, h2);
    }

    return pending.size();
  }

  private Optional<RepoUpdate> evaluateRow(
      final ResultSet rs,
      final Set<String> validPolicies,
      final boolean h2) throws Exception
  {
    String id = rs.getString("id");
    String repoName = rs.getString("name");

    // H2 stores JSON column data as bytes (matches setJsonParameter's setBytes path).
    // PostgreSQL JSONB columns are returned as UTF-8 strings by getString().
    String rawJson = h2
        ? readJsonH2(rs, "attributes")
        : rs.getString("attributes");

    if (rawJson == null) {
      return Optional.empty();
    }

    ObjectNode attrs = (ObjectNode) MAPPER.readTree(rawJson);

    if (!attrs.has(CLEANUP_KEY)) {
      return Optional.empty();
    }

    JsonNode cleanupNode = attrs.get(CLEANUP_KEY);

    if (!cleanupNode.isObject() || !cleanupNode.has(POLICY_NAME_KEY)) {
      return Optional.empty();
    }

    JsonNode policyNamesNode = cleanupNode.get(POLICY_NAME_KEY);

    if (!policyNamesNode.isArray()) {
      return Optional.empty();
    }

    List<String> valid = new ArrayList<>();
    List<String> stale = new ArrayList<>();

    for (JsonNode entry : policyNamesNode) {
      if (entry.isTextual()) {
        String policyName = entry.asText();
        (validPolicies.contains(policyName) ? valid : stale).add(policyName);
      }
      else {
        log.warn("Unexpected non-string entry in policyName array of repository '{}' — skipping", repoName);
        stale.add(entry.toString());
      }
    }

    if (stale.isEmpty()) {
      return Optional.empty();
    }

    // Only modify policyName; preserve any other keys that may exist in the cleanup block
    if (valid.isEmpty()) {
      ((ObjectNode) cleanupNode).remove(POLICY_NAME_KEY);
      if (cleanupNode.size() == 0) {
        attrs.remove(CLEANUP_KEY);
      }
    }
    else {
      ArrayNode retained = MAPPER.createArrayNode();
      valid.forEach(retained::add);
      ((ObjectNode) cleanupNode).set(POLICY_NAME_KEY, retained);
    }

    return Optional.of(new RepoUpdate(id, repoName, stale, MAPPER.writeValueAsBytes(attrs)));
  }

  private void applyUpdate(
      final Connection connection,
      final RepoUpdate update,
      final boolean h2) throws SQLException
  {
    try (PreparedStatement upd = connection.prepareStatement(UPDATE_ATTRIBUTES)) {
      setJsonParameter(upd, 1, update.newAttributes(), h2);
      upd.setString(2, update.id());
      upd.executeUpdate();
    }
  }

  private static String readJsonH2(final ResultSet rs, final String column) throws SQLException {
    byte[] bytes = rs.getBytes(column);
    return bytes != null ? new String(bytes, StandardCharsets.UTF_8) : null;
  }
}
