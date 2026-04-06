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
package org.sonatype.nexus.repository.apt.datastore.internal.task;

import java.io.IOException;
import java.util.Set;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.data.AptKeyValueFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.hosted.metadata.AptHostedMetadataFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.proxy.metadata.AptProxyMetadataFacet;
import org.sonatype.nexus.repository.apt.internal.gpg.AptSigningFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RebuildAptMetadataTask
    extends RepositoryTaskSupport
    implements Cancelable
{
  @Override
  protected void execute(final Repository repository) {
    log.debug("Rebuilding metadata in repository {} started", repository.getName());

    if (HostedType.NAME.equals(repository.getType().getValue())) {
      executeHostedRebuild(repository);
    }
    else if (ProxyType.NAME.equals(repository.getType().getValue())) {
      executeProxyRebuild(repository);
    }
  }

  private void executeHostedRebuild(final Repository repository) {
    // Warn if proxy-only flag is set on hosted repo
    if (getConfiguration().getBoolean(RebuildAptMetadataTaskDescriptor.APT_PROXY_RESET_METADATA, false)) {
      log.warn("'Reset proxy metadata' flag ignored for hosted repository: {}", repository.getName());
    }

    boolean isFullRebuild = getConfiguration()
        .getBoolean(RebuildAptMetadataTaskDescriptor.APT_METADATA_FULL_REBUILD, false);

    if (isFullRebuild) {
      log.debug("Executing full rebuild - repopulating apt_key_value for repository {}", repository.getName());

      // Remove all data in key-value storage
      data(repository).removeAllPackageMetadata();

      // Get all assets
      Iterable<FluentAsset> assets = content(repository).getAptPackageAssets();

      // Add metadata from each asset into key-value table
      for (FluentAsset asset : assets) {
        CancelableHelper.checkCancellation();
        hostedMetadata(repository).addPackageMetadata(asset);
      }
    }
    else {
      log.debug("Executing delta rebuild - skipping apt_key_value repopulation for repository {}",
          repository.getName());
    }

    // Rebuild index files
    try {
      hostedMetadata(repository).rebuildMetadata();
    }
    catch (IOException e) {
      log.error("Error rebuilding hosted metadata", log.isDebugEnabled() ? e : null);
    }
  }

  private void executeProxyRebuild(final Repository repository) {
    boolean resetMetadata = getResetMetadataFlag();
    Set<String> trackedDistributions = null;

    if (resetMetadata) {
      warnIfFullRebuildFlagSet();
      trackedDistributions = resetProxyMetadata(repository);
    }

    if (shouldSkipProxyRebuild(repository, resetMetadata, trackedDistributions)) {
      return;
    }

    rebuildProxyMetadata(repository, resetMetadata);
  }

  @Override
  protected boolean appliesTo(final Repository repository) {
    String repositoryType = repository.getType().getValue();
    return AptFormat.NAME.equals(repository.getFormat().getValue()) &&
        (ProxyType.NAME.equals(repositoryType) || HostedType.NAME.equals(repositoryType));
  }

  @Override
  public String getMessage() {
    return "Rebuilding Apt metadata in " + getRepositoryField();
  }

  private AptContentFacet content(final Repository repository) {
    return repository.facet(AptContentFacet.class);
  }

  private AptKeyValueFacet data(final Repository repository) {
    return repository.facet(AptKeyValueFacet.class);
  }

  private AptHostedMetadataFacet hostedMetadata(final Repository repository) {
    return repository.facet(AptHostedMetadataFacet.class);
  }

  private AptProxyMetadataFacet proxyMetadata(final Repository repository) {
    return repository.facet(AptProxyMetadataFacet.class);
  }

  private boolean getResetMetadataFlag() {
    return getConfiguration().getBoolean(RebuildAptMetadataTaskDescriptor.APT_PROXY_RESET_METADATA, false);
  }

  private void warnIfFullRebuildFlagSet() {
    if (getConfiguration().getBoolean(RebuildAptMetadataTaskDescriptor.APT_METADATA_FULL_REBUILD, false)) {
      log.warn("'Full rebuild (hosted only)' flag ignored for proxy repository: {}", getRepositoryField());
    }
  }

  private Set<String> resetProxyMetadata(final Repository repository) {
    AptKeyValueFacet keyValueFacet = repository.facet(AptKeyValueFacet.class);
    Set<String> trackedDistributions = keyValueFacet.getTrackedDistributions();

    log.info("Resetting proxy metadata for {} - clearing {} tracked distributions",
        repository.getName(), trackedDistributions.size());

    keyValueFacet.clearAllTrackedDistributions();

    // Invalidate proxy caches cluster-wide
    repository.facet(ProxyFacet.class).invalidateProxyCaches();

    return trackedDistributions;
  }

  private void reinsertTrackedDistributions(final Repository repository, final Set<String> distributions) {
    if (!distributions.isEmpty()) {
      AptKeyValueFacet keyValueFacet = repository.facet(AptKeyValueFacet.class);
      log.info("Re-inserting {} tracked distributions for rebuild", distributions.size());
      distributions.forEach(keyValueFacet::addTrackedDistribution);
    }
  }

  private boolean shouldSkipProxyRebuild(
      final Repository repository,
      final boolean resetMetadata,
      final Set<String> trackedDistributions)
  {
    AptSigningFacet signing = repository.facet(AptSigningFacet.class);

    if (!signing.isConfigured()) {
      if (resetMetadata) {
        reinsertTrackedDistributions(repository, trackedDistributions);
      }
      log.info("Skipping metadata rebuild for {} - signing not configured (passthrough mode)",
          repository.getName());
      return true;
    }

    if (resetMetadata && trackedDistributions != null) {
      reinsertTrackedDistributions(repository, trackedDistributions);
    }

    return false;
  }

  private void rebuildProxyMetadata(final Repository repository, final boolean resetMetadata) {
    try {
      log.debug("Calling metadata facet rebuild for proxy repository: {}", repository.getName());
      proxyMetadata(repository).rebuildMetadata(resetMetadata);
      log.info("Successfully rebuilt metadata for proxy repository: {}", repository.getName());
    }
    catch (Exception e) {
      log.error("Failed to rebuild metadata for proxy repository: {}", repository.getName(), e);
      throw new RuntimeException("Failed to rebuild APT proxy metadata", e);
    }
  }
}
