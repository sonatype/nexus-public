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
package org.sonatype.nexus.cleanup.internal.rest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.Nullable;
import jakarta.inject.Provider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.groups.Default;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.StreamingOutput;

import org.sonatype.nexus.cleanup.config.CleanupPolicyConfiguration;
import org.sonatype.nexus.cleanup.config.DefaultCleanupPolicyConfiguration;
import org.sonatype.nexus.cleanup.content.CleanupPolicyCreatedEvent;
import org.sonatype.nexus.cleanup.content.CleanupPolicyDeletedEvent;
import org.sonatype.nexus.cleanup.content.CleanupPolicyUpdatedEvent;
import org.sonatype.nexus.cleanup.internal.preview.CsvCleanupPreviewContentWriter;
import org.sonatype.nexus.cleanup.preview.CleanupPreviewHelper;
import org.sonatype.nexus.cleanup.rest.CleanupPolicyRequestValidator;
import org.sonatype.nexus.cleanup.rest.CleanupPolicyXO;
import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyCriteria;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyPreviewXO;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyReleaseType;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyStorage;
import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.extdirect.model.PagedResponse;
import org.sonatype.nexus.repository.CleanupDryRunEvent;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.query.PageResult;
import org.sonatype.nexus.repository.query.QueryOptions;
import org.sonatype.nexus.repository.rest.api.ComponentXO;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.validation.Validate;
import org.sonatype.nexus.validation.group.Create;
import org.sonatype.nexus.validation.group.Update;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.IS_PRERELEASE_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.LAST_BLOB_UPDATED_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.LAST_DOWNLOADED_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.REGEX_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.REPOSITORIES_FIELD_SUPPORTED_FORMATS;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.RETAIN_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.RETAIN_SORT_BY_KEY;
import static org.sonatype.nexus.cleanup.internal.rest.CleanupPolicyResource.RESOURCE_URI;
import static org.sonatype.nexus.cleanup.storage.CleanupPolicy.ALL_FORMATS;
import static org.sonatype.nexus.cleanup.storage.CleanupPolicyReleaseType.PRERELEASES;
import static org.sonatype.nexus.common.app.FeatureFlags.CLEANUP_PREVIEW_ENABLED_NAMED_VALUE;
import static org.sonatype.nexus.common.app.FeatureFlags.CLEANUP_RETAIN_ALL_FORMATS_NAMED_VALUE;
import static org.sonatype.nexus.repository.CleanupDryRunEvent.FINISHED_AT_IN_MILLISECONDS;
import static org.sonatype.nexus.repository.CleanupDryRunEvent.STARTED_AT_IN_MILLISECONDS;
import static org.sonatype.nexus.rest.APIConstants.INTERNAL_API_PREFIX;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @since 3.29
 */
@Component
@Tag(name = "Cleanup policies")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(RESOURCE_URI)
public class CleanupPolicyResource
    implements Resource
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  protected static final String RESOURCE_URI = INTERNAL_API_PREFIX + "/cleanup-policies";

  protected static final String MODE_DELETE = "delete";

  private static final int PREVIEW_ITEM_COUNT = 50;

  private final CleanupPolicyStorage cleanupPolicyStorage;

  private final List<String> formatNames;

  private final List<Format> formats;

  private final Map<String, CleanupPolicyConfiguration> cleanupFormatConfigurationMap;

  private final CleanupPolicyConfiguration defaultCleanupFormatConfiguration;

  private final Provider<CleanupPreviewHelper> cleanupPreviewHelper;

  private final RepositoryManager repositoryManager;

  private final EventManager eventManager;

  private final boolean isPreviewEnabled;

  private final CsvCleanupPreviewContentWriter csvCleanupPreviewContentWriter;

  private final Collection<CleanupPolicyRequestValidator> cleanupPolicyValidators;

  private final CleanupPolicyRepositoryAssociator repositoryAssociator;

  private final boolean retainAllFormatsEnabled;

  @Autowired
  public CleanupPolicyResource(
      final CleanupPolicyStorage cleanupPolicyStorage,
      final List<Format> formats,
      final List<CleanupPolicyConfiguration> cleanupFormatConfigurationList,
      final Provider<CleanupPreviewHelper> cleanupPreviewHelper,
      final RepositoryManager repositoryManager,
      final EventManager eventManager,
      @Value(CLEANUP_PREVIEW_ENABLED_NAMED_VALUE) final boolean isPreviewEnabled,
      final CsvCleanupPreviewContentWriter csvCleanupPreviewContentWriter,
      final Collection<CleanupPolicyRequestValidator> cleanupPolicyValidators,
      final CleanupPolicyRepositoryAssociator repositoryAssociator,
      @Value(CLEANUP_RETAIN_ALL_FORMATS_NAMED_VALUE) final boolean retainAllFormatsEnabled)
  {
    this.cleanupPolicyStorage = checkNotNull(cleanupPolicyStorage);
    this.formats = checkNotNull(formats);
    this.formatNames = formats.stream().map(Format::getValue).collect(Collectors.toList());
    this.eventManager = checkNotNull(eventManager);
    this.formatNames.add(ALL_FORMATS);
    this.cleanupFormatConfigurationMap =
        QualifierUtil.buildQualifierBeanMap(checkNotNull(cleanupFormatConfigurationList));
    this.defaultCleanupFormatConfiguration =
        checkNotNull(this.cleanupFormatConfigurationMap.get(DefaultCleanupPolicyConfiguration.NAME));
    this.cleanupPreviewHelper = checkNotNull(cleanupPreviewHelper);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.isPreviewEnabled = isPreviewEnabled;
    this.csvCleanupPreviewContentWriter = checkNotNull(csvCleanupPreviewContentWriter);
    this.cleanupPolicyValidators = checkNotNull(cleanupPolicyValidators);
    this.repositoryAssociator = checkNotNull(repositoryAssociator);
    this.retainAllFormatsEnabled = retainAllFormatsEnabled;
  }

  /**
   * Whether the embedded {@code repositories} field on the {@link CleanupPolicyXO}
   * payload is accepted for the given format. Mirrors the V1 helper's gate so the
   * two entry points apply identical contracts.
   */
  private boolean isRepositoriesFieldSupported(final String format) {
    return retainAllFormatsEnabled && format != null
        && REPOSITORIES_FIELD_SUPPORTED_FORMATS.contains(format);
  }

  /**
   * Validate the embedded {@code repositories} field on create/edit. A {@code null}
   * or empty value is always accepted (it means "preserve existing attachments"
   * / "no attachment change"). When the field carries names but the format /
   * feature-flag combination does not allow it, the request is rejected with
   * 400. Per-name validation (existence, format-match) is delegated to the
   * associator on apply.
   */
  private void validateRepositoriesField(final CleanupPolicyXO cleanupPolicyXO) {
    if (cleanupPolicyXO.getRepositories() == null || cleanupPolicyXO.getRepositories().isEmpty()) {
      return;
    }
    if (!isRepositoriesFieldSupported(cleanupPolicyXO.getFormat())) {
      throw new ValidationErrorsException("repositories",
          "The 'repositories' field is not supported for format '"
              + cleanupPolicyXO.getFormat() + "'.");
    }
  }

  @GET
  @RequiresAuthentication
  @RequiresPermissions("nexus:settings:read")
  @Operation(summary = "List cleanup policies",
      description = "Returns all configured cleanup policies sorted by name. "
          + "Pass the optional `format` query parameter to return only policies for that format "
          + "(e.g. `maven2`, `npm`, `docker`); omit it or pass `*` to return policies across all formats. "
          + "Each entry includes the policy definition and the number of repositories currently using it.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Cleanup policies matching the request",
          content = @Content(
              array = @ArraySchema(schema = @Schema(implementation = CleanupPolicyXO.class)))),
      @ApiResponse(responseCode = "401", description = "Authentication required"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions")
  })
  public List<CleanupPolicyXO> get(@QueryParam("format") final String format) {
    List<CleanupPolicy> policies = isBlank(format) || format.equals(ALL_FORMATS)
        ? cleanupPolicyStorage.getAll()
        : cleanupPolicyStorage.getAllByFormat(format);
    return policies.stream()
        .map(cleanupPolicy -> {
          CleanupPolicyXO xo = CleanupPolicyXO.fromCleanupPolicy(cleanupPolicy,
              (int) repositoryManager.browseForCleanupPolicy(cleanupPolicy.getName()).count());
          xo.setRepositories(new ArrayList<>(
              repositoryAssociator.getRepositoriesForPolicy(cleanupPolicy.getName(), cleanupPolicy.getFormat())));
          return xo;
        })
        .sorted(Comparator.comparing(CleanupPolicyXO::getName))
        .collect(toList());
  }

  @POST
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Validate(groups = {Create.class, Default.class})
  @Operation(summary = "Create a cleanup policy",
      description = "Creates a new cleanup policy with the supplied name, format, and criteria. "
          + "The policy name must be unique. At least one criterion "
          + "(`criteriaLastBlobUpdated`, `criteriaLastDownloaded`, or `criteriaAssetRegex`) is required. "
          + "Only criteria supported by the selected format are accepted; unsupported criteria are rejected. "
          + "The created policy is returned with an `inUseCount` of 0.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Policy created",
          content = @Content(schema = @Schema(implementation = CleanupPolicyXO.class))),
      @ApiResponse(responseCode = "400",
          description = "Invalid payload (unknown format, missing/invalid criteria, or unsupported criterion for the format)"),
      @ApiResponse(responseCode = "401", description = "Authentication required"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
      @ApiResponse(responseCode = "409", description = "A policy with the given name already exists")
  })
  public CleanupPolicyXO add(@Valid final CleanupPolicyXO cleanupPolicyXO) {
    if (!this.formatNames.contains(cleanupPolicyXO.getFormat())) {
      throw new ValidationErrorsException("format",
          "specified format " + cleanupPolicyXO.getFormat() + " is not valid.");
    }

    // Reject embedded repositories for formats / flag-state that does not support them.
    // A null/omitted field is always accepted.
    validateRepositoriesField(cleanupPolicyXO);

    for (CleanupPolicyRequestValidator validator : cleanupPolicyValidators) {
      validator.validate(cleanupPolicyXO);
    }

    CleanupPolicy added = cleanupPolicyStorage.add(toCleanupPolicy(cleanupPolicyXO));
    // Atomicity guard: if attaching repositories fails after the policy has been
    // persisted, compensate by deleting the orphan policy so the caller can retry
    // without colliding on the unique name.
    if (cleanupPolicyXO.getRepositories() != null && !cleanupPolicyXO.getRepositories().isEmpty()) {
      try {
        repositoryAssociator.updateRepositoriesForPolicy(
            added.getName(), added.getFormat(), new HashSet<>(cleanupPolicyXO.getRepositories()));
      }
      catch (RuntimeException attachError) {
        log.warn("Failed to attach repositories to newly created cleanup policy '{}'; "
            + "removing the orphan policy. Cause: {}", added.getName(), attachError.toString());
        try {
          cleanupPolicyStorage.remove(added);
        }
        catch (RuntimeException compensationError) {
          log.error("Compensating delete failed for cleanup policy '{}'. "
              + "The policy may exist in the database without its repository attachments.",
              added.getName(), compensationError);
          attachError.addSuppressed(compensationError);
        }
        throw attachError;
      }
    }
    CleanupPolicyXO cleanupXO = CleanupPolicyXO.fromCleanupPolicy(added, 0);
    cleanupXO.setRepositories(new ArrayList<>(
        repositoryAssociator.getRepositoriesForPolicy(added.getName(), added.getFormat())));
    eventManager.post(new CleanupPolicyCreatedEvent(toCleanupPolicy(cleanupXO)));
    return cleanupXO;
  }

  @GET
  @Path("{name}")
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  public CleanupPolicyXO getByName(@PathParam("name") final String name) {
    CleanupPolicy cleanupPolicy = cleanupPolicyStorage.get(name);

    if (cleanupPolicy == null) {
      throw new NotFoundException("Cleanup policy " + name + " not found.");
    }

    CleanupPolicyXO xo = CleanupPolicyXO
        .fromCleanupPolicy(cleanupPolicy, (int) repositoryManager.browseForCleanupPolicy(name).count());
    // Internal read endpoints (get, getByName, add, edit) unconditionally populate `repositories`
    // so the internal preview UI always has the current attachment set to render, regardless of
    // format or the CLEANUP_RETAIN_ALL_FORMATS feature flag. The V1 public API
    // (CleanupPolicyResourceHelper.fromCleanupPolicy) deliberately gates this on
    // isRepositoriesFieldSupported to honor the documented V1 contract; the asymmetry is intentional.
    xo.setRepositories(new ArrayList<>(
        repositoryAssociator.getRepositoriesForPolicy(cleanupPolicy.getName(), cleanupPolicy.getFormat())));
    return xo;
  }

  @PUT
  @Path("{name}")
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Validate(groups = {Update.class, Default.class})
  public CleanupPolicyXO edit(
      @PathParam("name") final String name,
      @NotNull @Valid final CleanupPolicyXO cleanupPolicyXO)
  {
    CleanupPolicy cleanupPolicy = cleanupPolicyStorage.get(name);

    if (cleanupPolicy == null) {
      throw new NotFoundException("Cleanup policy " + cleanupPolicyXO.getName() + " not found.");
    }

    if (!this.formatNames.contains(cleanupPolicyXO.getFormat())) {
      throw new ValidationErrorsException("format",
          "specified format " + cleanupPolicyXO.getFormat() + " is not valid.");
    }

    // Reject embedded repositories for formats / flag-state that does not support them.
    // A null/omitted field is always accepted.
    validateRepositoriesField(cleanupPolicyXO);

    int inUseCount = (int) repositoryManager.browseForCleanupPolicy(name).count();
    if (!cleanupPolicyXO.getFormat().equals(ALL_FORMATS) &&
        !cleanupPolicy.getFormat().equals(cleanupPolicyXO.getFormat()) &&
        inUseCount > 0) {
      throw new ValidationErrorsException("format", "You cannot change the format of a policy that is in use.");
    }

    for (CleanupPolicyRequestValidator validator : cleanupPolicyValidators) {
      validator.validate(cleanupPolicyXO);
    }

    cleanupPolicy.setNotes(cleanupPolicyXO.getNotes());
    cleanupPolicy.setFormat(cleanupPolicyXO.getFormat());
    cleanupPolicy.setCriteria(toCriteriaMap(cleanupPolicyXO));

    CleanupPolicy updated = cleanupPolicyStorage.update(cleanupPolicy);
    // No compensation on edit failure: rolling back a successful storage update by
    // issuing a second update is not safe without a real transaction boundary, and
    // a second write is just as likely to fail as the attach call that triggered
    // recovery. Surface the attach error and let the caller retry.
    if (cleanupPolicyXO.getRepositories() != null && !cleanupPolicyXO.getRepositories().isEmpty()) {
      repositoryAssociator.updateRepositoriesForPolicy(
          updated.getName(), updated.getFormat(), new HashSet<>(cleanupPolicyXO.getRepositories()));
    }
    CleanupPolicyXO cleanupXO = CleanupPolicyXO.fromCleanupPolicy(updated, inUseCount);
    cleanupXO.setRepositories(new ArrayList<>(
        repositoryAssociator.getRepositoriesForPolicy(updated.getName(), updated.getFormat())));
    eventManager.post(new CleanupPolicyUpdatedEvent(cleanupPolicy));
    return cleanupXO;
  }

  @DELETE
  @Path("{name}")
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  public void delete(@PathParam("name") final String name) {
    CleanupPolicy cleanupPolicy = cleanupPolicyStorage.get(name);

    if (cleanupPolicy == null) {
      throw new NotFoundException("Cleanup policy " + name + " not found.");
    }

    cleanupPolicyStorage.remove(cleanupPolicy);
    eventManager.post(new CleanupPolicyDeletedEvent(cleanupPolicy));
  }

  @GET
  @Path("criteria/formats")
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Operation(hidden = true)
  public List<CleanupPolicyFormatXO> getCriteriaForFormats() {
    List<CleanupPolicyFormatXO> criteriaByFormat = new ArrayList<>();

    formats.forEach(format -> {
      CleanupPolicyConfiguration config = cleanupFormatConfigurationMap.get(format.getValue());
      if (config == null) {
        config = defaultCleanupFormatConfiguration;
      }

      // Merge format-specific config with default to inherit retain settings
      Map<String, Boolean> mergedConfig = new HashMap<>(defaultCleanupFormatConfiguration.getConfiguration());
      mergedConfig.putAll(config.getConfiguration());

      criteriaByFormat.add(new CleanupPolicyFormatXO(format.getValue(), format.getValue(),
          mergedConfig.entrySet().stream().filter(Entry::getValue).map(Entry::getKey).collect(toSet())));
    });

    criteriaByFormat.sort(Comparator.comparing(CleanupPolicyFormatXO::getName));

    criteriaByFormat.add(0, new CleanupPolicyFormatXO(ALL_FORMATS, "All Formats",
        defaultCleanupFormatConfiguration.getConfiguration()
            .entrySet()
            .stream()
            .filter(Entry::getValue)
            .map(Entry::getKey)
            .collect(toSet())));

    return criteriaByFormat;
  }

  @POST
  @Path("preview/components")
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Operation(hidden = true)
  public PageResult<ComponentXO> previewContent(final PreviewRequestXO request) {
    Repository repository = repositoryManager.get(request.getRepository());

    if (repository == null) {
      throw new NotFoundException("Repository " + request.getRepository() + " not found.");
    }

    CleanupPolicyPreviewXO xo = new CleanupPolicyPreviewXO();
    CleanupPolicyCriteria criteria = new CleanupPolicyCriteria(
        request.getCriteriaLastBlobUpdated(),
        request.getCriteriaLastDownloaded(),
        request.getCriteriaReleaseType(),
        request.getCriteriaAssetRegex(),
        request.getCriteriaRetain(),
        request.getCriteriaSortBy());
    xo.setCriteria(criteria);
    QueryOptions options = new QueryOptions(request.getFilter(), "name", "asc", 0, PREVIEW_ITEM_COUNT);

    try {
      PagedResponse<ComponentXO> response = cleanupPreviewHelper.get().getSearchResults(xo, repository, options);

      return new PageResult<>(response.getTotal(), new ArrayList<>(response.getData()));
    }
    catch (IllegalArgumentException e) {
      throw new ValidationErrorsException("filter", e.getMessage());
    }
  }

  @GET
  @Path("preview/components/csv")
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Produces(APPLICATION_OCTET_STREAM)
  @Timed
  @Operation(hidden = true)
  public Response previewContentCsv(
      @QueryParam("name") @Nullable final String name,
      @QueryParam("repository") final String repositoryName,
      @QueryParam("criteriaLastBlobUpdated") @Nullable final Integer criteriaLastBlobUpdated,
      @QueryParam("criteriaLastDownloaded") @Nullable final Integer criteriaLastDownloaded,
      @QueryParam("criteriaReleaseType") @Nullable final CleanupPolicyReleaseType criteriaReleaseType,
      @QueryParam("criteriaAssetRegex") @Nullable final String criteriaAssetRegex,
      @QueryParam("criteriaRetain") @Nullable final Integer criteriaRetain,
      @QueryParam("criteriaSortBy") @Nullable final String criteriaSortBy)
  {

    if (!isPreviewEnabled) {
      return Response.status(Status.NOT_FOUND).build();
    }

    Repository repository = repositoryManager.get(repositoryName);

    if (repository == null) {
      throw new NotFoundException("Repository " + repositoryName + " not found.");
    }

    // Sanitize the policy name for use in Content-Disposition header
    String sanitizedPolicyName = sanitizeFilename(name);

    CleanupPolicyXO cleanupPolicyXO = new CleanupPolicyXO();
    cleanupPolicyXO.setName(name);
    cleanupPolicyXO.setFormat(repository.getFormat().getValue());
    if (criteriaLastBlobUpdated != null) {
      cleanupPolicyXO.setCriteriaLastBlobUpdated(criteriaLastBlobUpdated.longValue());
    }
    if (criteriaLastDownloaded != null) {
      cleanupPolicyXO.setCriteriaLastDownloaded(criteriaLastDownloaded.longValue());
    }
    cleanupPolicyXO.setCriteriaReleaseType(criteriaReleaseType);

    // Normalize the regex from URL parameter (e.g., %7B6,%7D -> {6,})
    String normalizedCriteriaAssetRegex =
        criteriaAssetRegex != null ? normalizeAndValidateRegex(criteriaAssetRegex) : null;
    cleanupPolicyXO.setCriteriaAssetRegex(normalizedCriteriaAssetRegex);
    cleanupPolicyXO.setRetain(criteriaRetain);
    cleanupPolicyXO.setSortBy(criteriaSortBy);

    for (CleanupPolicyRequestValidator validator : cleanupPolicyValidators) {
      validator.validate(cleanupPolicyXO);
    }

    Map<String, Object> cleanupDryRunXO = new HashMap<>();
    cleanupDryRunXO.put(STARTED_AT_IN_MILLISECONDS, System.currentTimeMillis());

    StreamingOutput streamingOutput = output -> {
      CleanupPolicyPreviewXO xo = new CleanupPolicyPreviewXO();
      xo.setRepositoryName(repositoryName);
      CleanupPolicyCriteria criteria = new CleanupPolicyCriteria(
          criteriaLastBlobUpdated,
          criteriaLastDownloaded,
          criteriaReleaseType,
          normalizedCriteriaAssetRegex,
          criteriaRetain,
          criteriaSortBy);
      xo.setCriteria(criteria);

      Stream<ComponentXO> components =
          cleanupPreviewHelper.get().getSearchResultsStream(xo, repository, null);

      csvCleanupPreviewContentWriter.write(repository, components, output);

      cleanupDryRunXO.put(FINISHED_AT_IN_MILLISECONDS, System.currentTimeMillis());
      eventManager.post(new CleanupDryRunEvent(cleanupDryRunXO));
    };
    String filename = sanitizedPolicyName + "-" +
        repository.getName() + "-" +
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")) +
        ".csv";
    return Response.ok(streamingOutput)
        .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
        .build();
  }

  /**
   * Sanitizes a filename for safe use in Content-Disposition headers.
   * Removes or replaces characters that could be used for header injection attacks.
   */
  private String sanitizeFilename(@Nullable final String name) {
    if (name == null || name.isEmpty()) {
      return "CleanupPreview";
    }

    // Remove characters that could interfere with Content-Disposition header parsing:
    // - Quotes (") could break out of the filename parameter
    // - Semicolons (;) could terminate the parameter early
    // - Backslashes (\) could be used for escape sequences
    // - Newlines/carriage returns (\n, \r) for header injection
    // - Control characters
    String sanitized = name.replaceAll("[\";\\\\\\n\\r\\x00-\\x1f]", "");

    // If sanitization removed all characters, use default
    if (sanitized.isEmpty()) {
      return "CleanupPreview";
    }

    return sanitized;
  }

  private CleanupPolicy toCleanupPolicy(final CleanupPolicyXO cleanupPolicyXO) {
    CleanupPolicy policy = cleanupPolicyStorage.newCleanupPolicy();

    policy.setName(cleanupPolicyXO.getName());
    policy.setNotes(cleanupPolicyXO.getNotes());
    policy.setMode(MODE_DELETE);
    policy.setFormat(cleanupPolicyXO.getFormat());
    policy.setCriteria(toCriteriaMap(cleanupPolicyXO));

    return policy;
  }

  private Map<String, String> toCriteriaMap(final CleanupPolicyXO cleanupPolicyXO) {
    Map<String, String> criteriaMap = new HashMap<>();

    CleanupPolicyConfiguration cleanupFormatConfiguration =
        cleanupFormatConfigurationMap.get(cleanupPolicyXO.getFormat());

    if (cleanupFormatConfiguration == null) {
      cleanupFormatConfiguration = defaultCleanupFormatConfiguration;
    }

    handleCriteria(cleanupFormatConfiguration, criteriaMap, REGEX_KEY, cleanupPolicyXO.getCriteriaAssetRegex(),
        "Asset name regex", cleanupPolicyXO.getFormat());
    handleCriteria(cleanupFormatConfiguration, criteriaMap, LAST_BLOB_UPDATED_KEY,
        toSeconds(cleanupPolicyXO.getCriteriaLastBlobUpdated()), "Published before", cleanupPolicyXO.getFormat());
    handleCriteria(cleanupFormatConfiguration, criteriaMap, LAST_DOWNLOADED_KEY,
        toSeconds(cleanupPolicyXO.getCriteriaLastDownloaded()), "Last downloaded before", cleanupPolicyXO.getFormat());
    if (cleanupPolicyXO.getCriteriaReleaseType() != null) {
      handleCriteria(cleanupFormatConfiguration, criteriaMap, IS_PRERELEASE_KEY,
          PRERELEASES.equals(cleanupPolicyXO.getCriteriaReleaseType()), "Release type", cleanupPolicyXO.getFormat());
    }
    if (cleanupPolicyXO.getRetain() != null) {
      handleCriteria(cleanupFormatConfiguration, criteriaMap, RETAIN_KEY,
          cleanupPolicyXO.getRetain(), "Retain components", cleanupPolicyXO.getFormat());
    }
    if (cleanupPolicyXO.getSortBy() != null) {
      handleCriteria(cleanupFormatConfiguration, criteriaMap, RETAIN_SORT_BY_KEY,
          cleanupPolicyXO.getSortBy(), "Retain sort by", cleanupPolicyXO.getFormat());
    }

    return criteriaMap;
  }

  private void handleCriteria(
      final CleanupPolicyConfiguration cleanupPolicyConfiguration,
      final Map<String, String> criteriaMap,
      final String key,
      final Object value,
      final String keyText,
      final String format)
  {
    if (value != null) {
      Boolean val = cleanupPolicyConfiguration.getConfiguration().get(key);
      if (val != null && val.equals(TRUE)) {
        // Normalize regex if this is the regex key
        Object normalizedValue = value;
        if (REGEX_KEY.equals(key) && value instanceof String regex) {
          normalizedValue = normalizeAndValidateRegex(regex);
        }
        criteriaMap.put(key, String.valueOf(normalizedValue));
      }
      else {
        throw new BadRequestException(
            String.format("Specified format %s does not support the '%s' criteria.", format, keyText));
      }
    }
  }

  /**
   * Validates a regex pattern.
   *
   * Note: URL decoding is NOT performed here because JAX-RS {@code @QueryParam} already
   * decodes URL-encoded values before they reach the resource method. A second decode via
   * {@code URLDecoder} would corrupt regex patterns containing the {@code +} quantifier
   * (decoded as space in application/x-www-form-urlencoded). See NEXUS-51975.
   *
   * @param regex the regex pattern to validate
   * @return the validated regex pattern
   * @throws ValidationErrorsException if the regex is invalid
   */
  private String normalizeAndValidateRegex(final String regex) {
    if (isBlank(regex)) {
      return regex;
    }

    try {
      Pattern.compile(regex);
    }
    catch (PatternSyntaxException e) {
      log.warn("Invalid regex pattern: {}", regex, e);
      throw new ValidationErrorsException("criteriaAssetRegex",
          "Invalid regex pattern: " + e.getMessage());
    }

    return regex;
  }

  private static Long toSeconds(final Long days) {
    if (days == null) {
      return null;
    }
    else {
      return TimeUnit.DAYS.toSeconds(days);
    }
  }

  @GET
  @Path("{name}/repositories")
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Operation(hidden = true, summary = "List repositories attached to a cleanup policy",
      description = "Returns the repositories currently associated with the named cleanup policy. "
          + "Only repositories whose format matches the policy's format are included. "
          + "Each entry exposes the repository `name`, `format`, and `type` (hosted/proxy/group).")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Repositories associated with the policy "
          + "(empty array if none are attached)",
          content = @Content(
              array = @ArraySchema(schema = @Schema(implementation = CleanupPolicyRepositoryXO.class)))),
      @ApiResponse(responseCode = "401", description = "Authentication required"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
      @ApiResponse(responseCode = "404", description = "No cleanup policy exists with the given name")
  })
  public List<CleanupPolicyRepositoryXO> getRepositoriesForPolicy(@PathParam("name") final String policyName) {
    CleanupPolicy policy = cleanupPolicyStorage.get(policyName);
    if (policy == null) {
      throw new NotFoundException("Cleanup policy '" + policyName + "' not found.");
    }
    String policyFormat = policy.getFormat();

    List<CleanupPolicyRepositoryXO> result = new ArrayList<>();
    for (Repository repo : repositoryManager.browse()) {
      if (repositoryHasPolicy(repo, policyName) && policyFormat.equals(repo.getFormat().getValue())) {
        result.add(new CleanupPolicyRepositoryXO(
            repo.getName(),
            repo.getFormat().getValue(),
            repo.getType().getValue()));
      }
    }
    return result;
  }

  @PUT
  @Path("{name}/repositories")
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Validate
  @Operation(hidden = true, summary = "Replace the repositories attached to a cleanup policy",
      description = "Sets the complete set of repositories associated with the named cleanup policy "
          + "to the names provided in `repositories`. This is a full replace: repositories previously "
          + "attached to the policy but not present in the request are detached, and repositories in "
          + "the request that were not previously attached are added. Passing an empty list detaches "
          + "all repositories from the policy. Every repository name in the list must exist and have "
          + "a format matching the policy's format.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Repository attachments updated"),
      @ApiResponse(responseCode = "400",
          description = "Invalid request (missing body, blank entry, unknown repository, "
              + "or repository format does not match the policy format)"),
      @ApiResponse(responseCode = "401", description = "Authentication required"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
      @ApiResponse(responseCode = "404", description = "No cleanup policy exists with the given name")
  })
  public void updateRepositoriesForPolicy(
      @PathParam("name") final String policyName,
      @NotNull @Valid final CleanupPolicyRepositoriesRequestXO request)
  {
    CleanupPolicy policy = cleanupPolicyStorage.get(policyName);
    if (policy == null) {
      throw new NotFoundException("Cleanup policy '" + policyName + "' not found.");
    }
    Set<String> requested = request.getRepositories() == null
        ? new HashSet<>()
        : new HashSet<>(request.getRepositories());
    repositoryAssociator.updateRepositoriesForPolicy(policyName, policy.getFormat(), requested);
  }

  private boolean repositoryHasPolicy(final Repository repository, final String policyName) {
    return repositoryAssociator.repositoryHasPolicy(repository, policyName);
  }
}
