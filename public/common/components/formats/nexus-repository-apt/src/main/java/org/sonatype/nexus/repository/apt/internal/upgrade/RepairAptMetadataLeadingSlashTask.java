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
package org.sonatype.nexus.repository.apt.internal.upgrade;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.sonatype.nexus.common.entity.Continuations;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.logging.task.TaskLogging;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryParallelTaskSupport;
import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.hosted.AptHostedFacet;
import org.sonatype.nexus.repository.apt.internal.AptProperties;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AttributeOperation;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.CancelableHelper;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.logging.task.TaskLogType.NEXUS_LOG_ONLY;

/**
 * Updates apt DEB asset attributes to remove leading slash from apt metadata and triggers rebuilding metadata
 */
@Component
@TaskLogging(NEXUS_LOG_ONLY)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RepairAptMetadataLeadingSlashTask
    extends RepositoryParallelTaskSupport
    implements Cancelable
{
  @VisibleForTesting
  static final String INDEX_SECTION = "index_section";

  private static final String BAD_PATTERN = "Filename: /";

  private final Map<String, Repository> repositoryRequiringRebuild = new ConcurrentHashMap<>();

  private final AtomicInteger count = new AtomicInteger();

  @Autowired
  public RepairAptMetadataLeadingSlashTask(
      @Value("${external.metadata.repository.concurrencyLimit:5}") final int concurrencyLimit)
  {
    super(false, concurrencyLimit);
  }

  @Override
  protected Stream<Runnable> jobStream(final ProgressLogIntervalHelper progress, final Repository repository) {
    AptContentFacet content = repository.facet(AptContentFacet.class);
    return Continuations
        .streamOf(
            content.assets().byFilter("kind = #{filterParams.kind}", Map.of("kind", AptProperties.DEB))::browse)
        .filter(this::hasLeadingSlash)
        .map(asset -> createJob(progress, asset));
  }

  private Runnable createJob(final ProgressLogIntervalHelper progress, final FluentAsset asset) {
    return () -> {
      CancelableHelper.checkCancellation();
      Repository repository = asset.repository();
      log.debug("Processing {} in {}", asset, repository);
      try {
        String indexSection = asset.attributes(AptFormat.NAME).get(INDEX_SECTION, String.class);
        asset.attributes(AttributeOperation.OVERLAY, AptFormat.NAME,
            Map.of(INDEX_SECTION, indexSection.replace(BAD_PATTERN, "Filename: ")));

        repositoryRequiringRebuild.putIfAbsent(repository.getName(), repository);

        progress.info("Updated {} assets with leading slash in metadata", count.incrementAndGet());
      }
      catch (Exception e) {
        log.error("Failed to update Filename path for {} in {}", asset.path(), asset.repository().getName(), e);
      }
    };
  }

  @Override
  protected Object result() {
    for (Repository repository : repositoryRequiringRebuild.values()) {
      CancelableHelper.checkCancellation();
      try {
        log.info("Rebuilding repository metadata for {}", repository.getName());
        repository.facet(AptHostedFacet.class).rebuildMetadata();
      }
      catch (Exception e) {
        log.error("Failed to rebuild metadata for repository {}", repository.getName(), e);
      }
    }
    return null;
  }

  private boolean hasLeadingSlash(final Asset asset) {
    return Optional.ofNullable(asset.attributes(AptFormat.NAME).get(INDEX_SECTION, String.class))
        .map(indexSection -> indexSection.contains(BAD_PATTERN))
        .orElse(false);
  }

  @Override
  protected boolean appliesTo(final Repository repository) {
    return repository.getFormat() instanceof AptFormat && repository.getType() instanceof HostedType;
  }

  @Override
  public String getMessage() {
    return RepairAptMetadataLeadingSlashTaskDescriptor.TASK_DESCRIPTION;
  }
}
