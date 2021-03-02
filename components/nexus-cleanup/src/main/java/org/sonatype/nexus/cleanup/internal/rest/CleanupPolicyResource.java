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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.cleanup.config.CleanupPolicyConfiguration;
import org.sonatype.nexus.cleanup.preview.CleanupPreviewHelper;
import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyCriteria;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyPreviewXO;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyStorage;
import org.sonatype.nexus.extdirect.model.PagedResponse;
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

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.IS_PRERELEASE_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.LAST_BLOB_UPDATED_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.LAST_DOWNLOADED_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.REGEX_KEY;
import static org.sonatype.nexus.cleanup.internal.rest.CleanupPolicyResource.RESOURCE_URI;
import static org.sonatype.nexus.cleanup.storage.CleanupPolicy.ALL_CLEANUP_POLICY_FORMAT;
import static org.sonatype.nexus.cleanup.storage.CleanupPolicyReleaseType.PRERELEASES;
import static org.sonatype.nexus.rest.APIConstants.INTERNAL_API_PREFIX;

/**
 * @since 3.29
 */
@Named
@Singleton
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(RESOURCE_URI)
public class CleanupPolicyResource
    extends ComponentSupport
    implements Resource
{
  private static final int PREVIEW_ITEM_COUNT = 50;

  protected static final String RESOURCE_URI = INTERNAL_API_PREFIX + "/cleanup-policies";

  protected static final String ALL_FORMATS = "*";

  protected static final String MODE_DELETE = "delete";

  private final CleanupPolicyStorage cleanupPolicyStorage;

  private final List<String> formatNames;

  private final List<Format> formats;

  private final Map<String, CleanupPolicyConfiguration> cleanupFormatConfigurationMap;

  private final CleanupPolicyConfiguration defaultCleanupFormatConfiguration;

  private final Provider<CleanupPreviewHelper> cleanupPreviewHelper;

  private final RepositoryManager repositoryManager;

  @Inject
  public CleanupPolicyResource(
      final CleanupPolicyStorage cleanupPolicyStorage,
      final List<Format> formats,
      final Map<String, CleanupPolicyConfiguration> cleanupFormatConfigurationMap,
      final Provider<CleanupPreviewHelper> cleanupPreviewHelper,
      final RepositoryManager repositoryManager)
  {
    this.cleanupPolicyStorage = checkNotNull(cleanupPolicyStorage);
    this.formats = checkNotNull(formats);
    this.formatNames = formats.stream().map(Format::getValue).collect(Collectors.toList());
    this.formatNames.add("*");
    this.cleanupFormatConfigurationMap = checkNotNull(cleanupFormatConfigurationMap);
    this.defaultCleanupFormatConfiguration = checkNotNull(cleanupFormatConfigurationMap.get("default"));
    this.cleanupPreviewHelper = checkNotNull(cleanupPreviewHelper);
    this.repositoryManager = checkNotNull(repositoryManager);
  }

  @GET
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  public List<CleanupPolicyXO> get() {
    return cleanupPolicyStorage.getAll().stream().map(cleanupPolicy -> CleanupPolicyXO.fromCleanupPolicy(cleanupPolicy,
        (int) repositoryManager.browseForCleanupPolicy(cleanupPolicy.getName()).count()))
        .sorted(Comparator.comparing(CleanupPolicyXO::getName)).collect(toList());
  }

  @POST
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Validate(groups = {Create.class, Default.class})
  public CleanupPolicyXO add(@Valid final CleanupPolicyXO cleanupPolicyXO) {
    if (!this.formatNames.contains(cleanupPolicyXO.getFormat())) {
      throw new ValidationErrorsException("format", "specified format " + cleanupPolicyXO.getFormat() + " is not valid.");
    }
    return CleanupPolicyXO.fromCleanupPolicy(cleanupPolicyStorage.add(toCleanupPolicy(cleanupPolicyXO)), 0);
  }

  @GET
  @Path("{name}")
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  public CleanupPolicyXO get(@PathParam("name") final String name) {
    CleanupPolicy cleanupPolicy = cleanupPolicyStorage.get(name);

    if (cleanupPolicy == null) {
      throw new NotFoundException("Cleanup policy " + name + " not found.");
    }

    return CleanupPolicyXO
        .fromCleanupPolicy(cleanupPolicy, (int) repositoryManager.browseForCleanupPolicy(name).count());
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
      throw new ValidationErrorsException("format", "specified format " + cleanupPolicyXO.getFormat() + " is not valid.");
    }

    cleanupPolicy.setNotes(cleanupPolicyXO.getNotes());
    cleanupPolicy.setFormat(cleanupPolicyXO.getFormat());
    cleanupPolicy.setCriteria(toCriteriaMap(cleanupPolicyXO));

    return CleanupPolicyXO.fromCleanupPolicy(cleanupPolicyStorage.update(cleanupPolicy),
        (int) repositoryManager.browseForCleanupPolicy(name).count());
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
  }

  @GET
  @Path("criteria/formats")
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  public List<CleanupPolicyFormatXO> getCriteriaForFormats() {
    List<CleanupPolicyFormatXO> criteriaByFormat = new ArrayList<>();

    formats.forEach(format -> {
      CleanupPolicyConfiguration config = cleanupFormatConfigurationMap.get(format.getValue());
      if (config == null) {
        config = defaultCleanupFormatConfiguration;
      }

      criteriaByFormat.add(new CleanupPolicyFormatXO(format.getValue(), format.getValue(),
          config.getConfiguration().entrySet().stream().filter(Entry::getValue).map(Entry::getKey).collect(toSet())));
    });

    criteriaByFormat.sort(Comparator.comparing(CleanupPolicyFormatXO::getName));

    criteriaByFormat.add(0, new CleanupPolicyFormatXO(ALL_FORMATS, "All Formats",
        defaultCleanupFormatConfiguration.getConfiguration().entrySet().stream().filter(Entry::getValue)
            .map(Entry::getKey).collect(toSet())));

    return criteriaByFormat;
  }

  @POST
  @Path("preview/components")
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  public PageResult<ComponentXO> previewContent(PreviewRequestXO request)
  {
    Repository repository = repositoryManager.get(request.getRepository());

    if (repository == null) {
      throw new NotFoundException("Repository " + request.getRepository() + " not found.");
    }

    CleanupPolicyPreviewXO xo = new CleanupPolicyPreviewXO();
    xo.setCriteria(new CleanupPolicyCriteria());
    xo.getCriteria().setLastBlobUpdated(request.getCriteriaLastBlobUpdated());
    xo.getCriteria().setLastDownloaded(request.getCriteriaLastDownloaded());
    xo.getCriteria().setReleaseType(request.getCriteriaReleaseType());
    xo.getCriteria().setRegex(request.getCriteriaAssetRegex());

    QueryOptions options = new QueryOptions(request.getFilter(), "name", "asc", 0, PREVIEW_ITEM_COUNT);

    PagedResponse<ComponentXO> response = cleanupPreviewHelper.get().getSearchResults(xo, repository, options);

    return new PageResult<>(response.getTotal(), new ArrayList<>(response.getData()));
  }

  private CleanupPolicy toCleanupPolicy(final CleanupPolicyXO cleanupPolicyXO) {
    CleanupPolicy policy = cleanupPolicyStorage.newCleanupPolicy();

    policy.setName(cleanupPolicyXO.getName());
    policy.setNotes(cleanupPolicyXO.getNotes());
    policy.setMode(MODE_DELETE);
    policy.setFormat(
        cleanupPolicyXO.getFormat().equals(ALL_FORMATS) ? ALL_CLEANUP_POLICY_FORMAT : cleanupPolicyXO.getFormat());
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

    return criteriaMap;
  }

  private void handleCriteria(
      CleanupPolicyConfiguration cleanupPolicyConfiguration,
      Map<String, String> criteriaMap,
      String key,
      Object value,
      String keyText,
      String format)
  {
    if (value != null) {
      Boolean val = cleanupPolicyConfiguration.getConfiguration().get(key);
      if (val != null && val.equals(TRUE)) {
        criteriaMap.put(key, String.valueOf(value));
      }
      else {
        throw new BadRequestException(
            "Specified format " + format + " does not support the '" + keyText + "' criteria.");
      }
    }
  }

  private static Long toSeconds(final Long days) {
    if (days == null) {
      return null;
    }
    else {
      return TimeUnit.DAYS.toSeconds(days);
    }
  }
}

