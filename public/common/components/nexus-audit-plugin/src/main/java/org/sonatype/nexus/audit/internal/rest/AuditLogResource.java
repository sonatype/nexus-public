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
package org.sonatype.nexus.audit.internal.rest;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import org.sonatype.nexus.audit.internal.store.AuditEventData;
import org.sonatype.nexus.audit.internal.store.AuditEventStore;
import org.sonatype.nexus.rest.Resource;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.sonatype.nexus.common.app.FeatureFlags.PREVIEW_UI_AUDIT_ENABLED;

/**
 * REST API for querying audit events across all domains.
 */
@Component
@ConditionalOnProperty(name = PREVIEW_UI_AUDIT_ENABLED, havingValue = "true")
@Path("/internal/ui/audit-log")
@Produces(APPLICATION_JSON)
public class AuditLogResource
    implements Resource
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final AuditEventStore auditEventStore;

  /**
   * Domain-to-category mappings for frontend filtering.
   */
  private static final List<String> SECURITY_DOMAINS = Arrays.asList(
      "security.user",
      "security.role",
      "security.privilege",
      "security.user-role-mapping",
      "security.anonymous",
      "security.realm",
      "security.ldap",
      "security.crowd",
      "security.sslcertificate",
      "security.secrets",
      "security.jwt",
      "SamlRealm");

  private static final List<String> REPOSITORY_DOMAINS = Arrays.asList(
      "repository",
      "repository.component",
      "repository.asset",
      "repository.component.tag",
      "blobstore");

  private static final List<String> CONFIGURATION_DOMAINS = Arrays.asList(
      "tasks",
      "capability",
      "cleanupPolicy",
      "ContentSelector",
      "RoutingRule",
      "email",
      "httpclient",
      "logging",
      "script",
      "license",
      "freeze",
      "DataStore",
      "database-migration",
      "userToken",
      "userToken.admin");

  private static final List<String> PROTECTION_DOMAINS = Arrays.asList(
      "protection.config",
      "malware.removal",
      "firewall.quarantine");

  @Autowired
  public AuditLogResource(final AuditEventStore auditEventStore) {
    this.auditEventStore = auditEventStore;
  }

  /**
   * Query audit events with filtering, pagination, and sorting.
   *
   * @param categories Category filter (security, repository, configuration, protection)
   * @param domains Domain filter (overrides categories if specified)
   * @param types Event type filter (created, updated, deleted, etc.)
   * @param initiators Initiator (username) filter
   * @param startDate Start of date range (ISO 8601)
   * @param endDate End of date range (ISO 8601)
   * @param repositoryName Repository name filter (optional)
   * @param page Page number (1-based)
   * @param limit Items per page (max 100)
   * @return Paginated audit events with metadata
   */
  @GET
  @RequiresPermissions("nexus:audit:read")
  public Response getAuditLog(
      @QueryParam("categories") final List<String> categories,
      @QueryParam("domains") final List<String> domains,
      @QueryParam("types") final List<String> types,
      @QueryParam("initiators") final List<String> initiators,
      @QueryParam("startDate") final String startDate,
      @QueryParam("endDate") final String endDate,
      @QueryParam("repositoryName") final String repositoryName,
      @QueryParam("page") @DefaultValue("1") final int page,
      @QueryParam("limit") @DefaultValue("20") final int limit)
  {
    try {
      // Validate pagination params
      if (page < 1) {
        return Response.status(BAD_REQUEST).entity("Page must be >= 1").build();
      }
      int validatedLimit = Math.min(Math.max(1, limit), 100);

      // Parse date range
      OffsetDateTime parsedStartDate = parseDate(startDate);
      OffsetDateTime parsedEndDate = parseDate(endDate);

      // Determine domains to query
      List<String> targetDomains = buildDomainFilter(categories, domains);

      // Calculate offset
      int offset = (page - 1) * validatedLimit;

      // Query events
      List<AuditEventData> events;
      int totalCount;

      String singleType = types != null && types.size() == 1 ? types.get(0) : null;
      String singleInitiator = initiators != null && initiators.size() == 1 ? initiators.get(0) : null;

      if (targetDomains.isEmpty()) {
        // No domain filter - query all
        events = auditEventStore.findAll(
            null,
            singleType,
            singleInitiator,
            repositoryName,
            parsedStartDate,
            parsedEndDate,
            validatedLimit,
            offset);

        totalCount = auditEventStore.count(
            null,
            singleType,
            singleInitiator,
            repositoryName,
            parsedStartDate,
            parsedEndDate);
      }
      else {
        // Domain filter specified
        events = auditEventStore.findByDomains(
            targetDomains,
            singleType,
            singleInitiator,
            repositoryName,
            parsedStartDate,
            parsedEndDate,
            validatedLimit,
            offset);

        totalCount = auditEventStore.countByDomains(
            targetDomains,
            singleType,
            singleInitiator,
            repositoryName,
            parsedStartDate,
            parsedEndDate);
      }

      // Build response
      List<AuditEventXO> items = events.stream()
          .map(this::toXO)
          .collect(Collectors.toList());

      int totalPages = (int) Math.ceil((double) totalCount / validatedLimit);

      AuditLogResponseXO response = new AuditLogResponseXO(
          items,
          new AuditLogResponseXO.PaginationXO(totalCount, totalPages, page, validatedLimit));

      return Response.ok(response).build();
    }
    catch (Exception e) {
      log.error("Error querying audit log", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("Error querying audit log: " + e.getMessage())
          .build();
    }
  }

  /**
   * Export audit events to CSV format.
   *
   * @param categories Category filter (security, repository, configuration, protection)
   * @param domains Domain filter (overrides categories if specified)
   * @param types Event type filter (created, updated, deleted, etc.)
   * @param initiators Initiator (username) filter
   * @param startDate Start of date range (ISO 8601)
   * @param endDate End of date range (ISO 8601)
   * @return CSV file with audit events
   */
  @GET
  @Path("/export")
  @Produces("text/csv")
  @RequiresPermissions("nexus:audit:read")
  public Response exportAuditLog(
      @QueryParam("categories") final List<String> categories,
      @QueryParam("domains") final List<String> domains,
      @QueryParam("types") final List<String> types,
      @QueryParam("initiators") final List<String> initiators,
      @QueryParam("startDate") final String startDate,
      @QueryParam("endDate") final String endDate)
  {
    try {
      // Parse date range
      OffsetDateTime parsedStartDate = parseDate(startDate);
      OffsetDateTime parsedEndDate = parseDate(endDate);

      // Determine domains to query
      List<String> targetDomains = buildDomainFilter(categories, domains);

      // Maximum export limit to prevent memory issues
      int exportLimit = 10000;

      // Query events
      List<AuditEventData> events;

      if (targetDomains.isEmpty()) {
        String singleType = types != null && types.size() == 1 ? types.get(0) : null;
        String singleInitiator = initiators != null && initiators.size() == 1 ? initiators.get(0) : null;

        events = auditEventStore.findAll(
            null,
            singleType,
            singleInitiator,
            null,
            parsedStartDate,
            parsedEndDate,
            exportLimit,
            0);
      }
      else {
        String singleType = types != null && types.size() == 1 ? types.get(0) : null;
        String singleInitiator = initiators != null && initiators.size() == 1 ? initiators.get(0) : null;

        events = auditEventStore.findByDomains(
            targetDomains,
            singleType,
            singleInitiator,
            null,
            parsedStartDate,
            parsedEndDate,
            exportLimit,
            0);
      }

      // Generate CSV filename with timestamp
      String filename = "audit-log-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
          .format(OffsetDateTime.now()) + ".csv";

      StreamingOutput streamingOutput = outputStream -> {
        try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
          // Write CSV header
          writer.write("ID,Timestamp,Domain,Type,Context,Initiator,Node ID,Attributes\n");

          // Write data rows
          for (AuditEventData event : events) {
            writer.write(String.valueOf(event.getId()));
            writer.write(',');
            writer.write(escapeCsvValue(event.getTimestamp() != null
                ? event.getTimestamp().toString()
                : ""));
            writer.write(',');
            writer.write(escapeCsvValue(event.getDomain()));
            writer.write(',');
            writer.write(escapeCsvValue(event.getType()));
            writer.write(',');
            writer.write(escapeCsvValue(event.getContext()));
            writer.write(',');
            writer.write(escapeCsvValue(event.getInitiator()));
            writer.write(',');
            writer.write(escapeCsvValue(event.getNodeId()));
            writer.write(',');
            writer.write(escapeCsvValue(formatAttributes(event.getAttributes())));
            writer.write('\n');
          }
        }
        catch (IOException e) {
          log.error("Error writing CSV output", e);
          throw e;
        }
      };

      return Response.ok(streamingOutput)
          .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
          .build();
    }
    catch (Exception e) {
      log.error("Error exporting audit log", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("Error exporting audit log: " + e.getMessage())
          .build();
    }
  }

  /**
   * Escape a value for CSV output with CSV injection prevention.
   * Prevents formula injection by prefixing formula-triggering characters with single quote.
   */
  private String escapeCsvValue(final String value) {
    if (value == null) {
      return "";
    }
    String escaped = value;

    // Prevent CSV injection - escape formula-triggering chars at start
    if (!escaped.isEmpty()) {
      char firstChar = escaped.charAt(0);
      if (firstChar == '=' || firstChar == '+' || firstChar == '-' ||
          firstChar == '@' || firstChar == '|' || firstChar == '%') {
        escaped = "'" + escaped; // Prefix with single quote to neutralize formula
      }
    }

    // Standard CSV escaping - wrap in quotes if contains comma, quote, or newline
    if (escaped.contains(",") || escaped.contains("\"") ||
        escaped.contains("\n") || escaped.contains("\r")) {
      return "\"" + escaped.replace("\"", "\"\"") + "\"";
    }
    return escaped;
  }

  /**
   * Format attributes map as a string for CSV.
   */
  private String formatAttributes(final Map<String, Object> attributes) {
    if (attributes == null || attributes.isEmpty()) {
      return "";
    }
    return attributes.entrySet()
        .stream()
        .map(e -> e.getKey() + "=" + (e.getValue() != null ? e.getValue().toString() : ""))
        .collect(Collectors.joining("; "));
  }

  /**
   * Build domain filter from categories or explicit domains.
   */
  private List<String> buildDomainFilter(final List<String> categories, final List<String> explicitDomains) {
    // If explicit domains specified, use those
    if (explicitDomains != null && !explicitDomains.isEmpty()) {
      return explicitDomains;
    }

    // If no categories specified, return empty (query all domains)
    if (categories == null || categories.isEmpty()) {
      return Collections.emptyList();
    }

    // Map categories to domains
    List<String> result = new ArrayList<>();
    for (String category : categories) {
      switch (category.toLowerCase()) {
        case "security":
          result.addAll(SECURITY_DOMAINS);
          break;
        case "repository":
          result.addAll(REPOSITORY_DOMAINS);
          break;
        case "configuration":
          result.addAll(CONFIGURATION_DOMAINS);
          break;
        case "protection":
          result.addAll(PROTECTION_DOMAINS);
          break;
        default:
          log.warn("Unknown category: {}", category);
      }
    }
    return result;
  }

  /**
   * Parse ISO 8601 date string to OffsetDateTime.
   */
  private OffsetDateTime parseDate(final String dateStr) {
    if (dateStr == null || dateStr.trim().isEmpty()) {
      return null;
    }
    try {
      return OffsetDateTime.parse(dateStr);
    }
    catch (DateTimeParseException e) {
      log.warn("Invalid date format: {}", dateStr, e);
      return null;
    }
  }

  /**
   * Convert AuditEventData to XO.
   */
  private AuditEventXO toXO(final AuditEventData data) {
    return new AuditEventXO(
        data.getId(),
        data.getDomain(),
        data.getType(),
        data.getContext(),
        data.getTimestamp(),
        data.getInitiator(),
        data.getNodeId(),
        data.getAttributes());
  }
}
