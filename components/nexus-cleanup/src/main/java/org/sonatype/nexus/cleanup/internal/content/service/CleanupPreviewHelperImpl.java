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
package org.sonatype.nexus.cleanup.internal.content.service;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.cleanup.content.search.CleanupBrowseServiceFactory;
import org.sonatype.nexus.cleanup.content.search.CleanupComponentBrowse;
import org.sonatype.nexus.cleanup.preview.CleanupPreviewHelper;
import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyCriteria;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyPreviewXO;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyStorage;
import org.sonatype.nexus.extdirect.model.PagedResponse;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.query.QueryOptions;
import org.sonatype.nexus.repository.rest.api.AssetXO;
import org.sonatype.nexus.repository.rest.api.ComponentXO;
import org.sonatype.nexus.repository.rest.api.DefaultComponentXO;
import org.sonatype.nexus.scheduling.CancelableHelper;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.common.time.DateHelper.offsetToDate;
import static org.sonatype.nexus.common.time.DateHelper.optionalOffsetToDate;

/**
 * {@link CleanupPreviewHelper} implementation.
 *
 * @since 3.29
 */
@Named
@Singleton
public class CleanupPreviewHelperImpl
    extends ComponentSupport
    implements CleanupPreviewHelper
{

  private final CleanupPolicyStorage cleanupPolicyStorage;

  private final Duration previewTimeout;

  private final CleanupBrowseServiceFactory browseServiceFactory;

  @Inject
  public CleanupPreviewHelperImpl(
      final CleanupPolicyStorage cleanupPolicyStorage,
      @Named("${nexus.cleanup.preview.timeout:-60s}") final Duration previewTimeout,
      CleanupBrowseServiceFactory browseServiceFactory)
  {
    this.cleanupPolicyStorage = checkNotNull(cleanupPolicyStorage);
    this.previewTimeout = checkNotNull(previewTimeout);
    this.browseServiceFactory = checkNotNull(browseServiceFactory);
  }

  @Override
  public PagedResponse<ComponentXO> getSearchResults(
      final CleanupPolicyPreviewXO previewXO,
      final Repository repository,
      final QueryOptions queryOptions)
  {
    CleanupPolicy cleanupPolicy = toCleanupPolicy(previewXO);

    return searchForComponents(repository, cleanupPolicy, queryOptions);
  }

  @Override
  public Stream<ComponentXO> getSearchResultsStream(
      final CleanupPolicyPreviewXO previewXO, final Repository repository, final QueryOptions queryOptions)
  {
    CleanupPolicy cleanupPolicy = toCleanupPolicy(previewXO);

    CleanupComponentBrowse browseService = browseServiceFactory.getPreviewService();
    Stream<FluentComponent> componentSteam =
            browseService.browseIncludingAssets(cleanupPolicy, repository);

    return componentSteam.map(component -> convert(component, repository));
  }

  private PagedResponse<ComponentXO> searchForComponents(final Repository repository,
                                                           final CleanupPolicy cleanupPolicy,
                                                           final QueryOptions queryOptions)
  {
    PagedResponse<Component> components = browse(cleanupPolicy, repository, queryOptions);

    List<ComponentXO> componentXOS = components.getData().stream()
        .map(item -> convert(item, repository))
        .collect(toList());

    return new PagedResponse<>(components.getTotal(), componentXOS);
  }

  private PagedResponse<Component> browse(
      final CleanupPolicy policy,
      final Repository repository,
      final QueryOptions queryOptions)
  {
    AtomicBoolean cancelled = new AtomicBoolean(false);

    Future<PagedResponse<Component>> future = CompletableFuture.supplyAsync(() -> {
      CancelableHelper.set(cancelled);
      try {
        // compute preview and return it
        CleanupComponentBrowse browseService = browseServiceFactory.getPreviewService();
        return browseService.browseByPage(policy, repository, queryOptions);
      }
      finally {
        CancelableHelper.remove();
      }
    });

    try {
      return future.get(previewTimeout.toMillis(), TimeUnit.MILLISECONDS);
    }
    catch (Exception e) {
      cancelled.set(true);
      log.debug("Timeout computing preview for cleanup for policy {} in repository {}", policy, repository.getName(), e);
      // indicate failure to UI
      throw new WebApplicationException(
          Response.serverError().entity("A timeout occurred while computing the preview results.").build());
    }
  }

  private CleanupPolicy toCleanupPolicy(final CleanupPolicyPreviewXO cleanupPolicyPreviewXO) {
    CleanupPolicy policy = cleanupPolicyStorage.newCleanupPolicy();

    policy.setName("preview");
    policy.setCriteria(CleanupPolicyCriteria.toMap(cleanupPolicyPreviewXO.getCriteria()));

    return policy;
  }

  private static ComponentXO convert(final Component component, final Repository repository) {
    DefaultComponentXO defaultComponentXO = new DefaultComponentXO();
    defaultComponentXO.setRepository(repository.getName());
    defaultComponentXO.setGroup(component.namespace());
    defaultComponentXO.setName(component.name());
    defaultComponentXO.setVersion(component.version());
    defaultComponentXO.setFormat(repository.getFormat().getValue());
    return defaultComponentXO;
  }

  private static ComponentXO convert(final FluentComponent component, final Repository repository) {
    ComponentXO componentXO = convert((Component) component, repository);

    List<AssetXO> assetXOS = convert(component.assets(true));

    componentXO.setAssets(assetXOS);

    return componentXO;
  }

  private static List<AssetXO> convert(final Collection<FluentAsset> assets) {
    return assets
        .stream()
        .map(it -> {
          AssetXO assetXO = new AssetXO();

          assetXO.setPath(it.path());
          assetXO.setBlobStoreName(it.blobStoreName());
          assetXO.setFileSize(it.assetBlobSize());
          assetXO.setLastDownloaded(optionalOffsetToDate(it.lastDownloaded()));
          assetXO.setBlobCreated(offsetToDate(it.created()));

          return assetXO;
        })
        .collect(Collectors.toList());
  }
}
