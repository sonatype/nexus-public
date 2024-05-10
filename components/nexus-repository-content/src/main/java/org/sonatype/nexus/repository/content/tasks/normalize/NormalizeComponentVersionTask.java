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
package org.sonatype.nexus.repository.content.tasks.normalize;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.Continuations;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.logging.task.TaskLogType;
import org.sonatype.nexus.logging.task.TaskLogging;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.content.kv.global.GlobalKeyValueStore;
import org.sonatype.nexus.repository.content.kv.global.NexusKeyValue;
import org.sonatype.nexus.repository.content.kv.global.ValueType;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.content.store.ComponentStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.search.normalize.VersionNormalizerService;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.TaskInterruptedException;
import org.sonatype.nexus.scheduling.TaskSupport;

import static java.lang.String.format;
import static org.sonatype.nexus.common.app.FeatureFlags.DISABLE_NORMALIZE_VERSION_TASK;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

/**
 * System task to populate the {format}_component tables
 */
@Named
@TaskLogging(TaskLogType.TASK_LOG_ONLY_WITH_PROGRESS)
public class NormalizeComponentVersionTask
    extends TaskSupport
    implements Cancelable
{
  public static final String KEY_FORMAT = "%s.normalized.version.available";

  private final NormalizationPriorityService normalizationPriorityService;

  private final VersionNormalizerService versionNormalizerService;

  private final GlobalKeyValueStore globalKeyValueStore;

  private final EventManager eventManager;

  private ProgressLogIntervalHelper progressLogger;

  private final boolean disableTask;

  @Inject
  public NormalizeComponentVersionTask(
      final NormalizationPriorityService normalizationPriorityService,
      final VersionNormalizerService versionNormalizerService,
      final GlobalKeyValueStore globalKeyValueStore,
      final EventManager eventManager,
      @Named("${" + DISABLE_NORMALIZE_VERSION_TASK + ":-false}") final boolean disableTask)
  {
    this.normalizationPriorityService = normalizationPriorityService;
    this.versionNormalizerService = versionNormalizerService;
    this.globalKeyValueStore = globalKeyValueStore;
    this.eventManager = eventManager;
    this.disableTask = disableTask;
  }

  @Override
  public String getMessage() {
    return "populate normalized_version column on {format}_component tables";
  }

  @Override
  protected Object execute() throws Exception
  {
    if (disableTask) {
      throw new TaskInterruptedException("The normalize version task was disabled", disableTask);
    }

    progressLogger = new ProgressLogIntervalHelper(log, 10);
    Map<Format, FormatStoreManager> formats = normalizationPriorityService.getPrioritizedFormats();

    int totalCount = formats.size();
    AtomicInteger skippedCount = new AtomicInteger();
    AtomicInteger processedCount = new AtomicInteger();

    formats.forEach(
        (format, manager) -> processFormat(totalCount, skippedCount, processedCount, format, manager));

    return null;
  }

  private void processFormat(
      final int totalCount,
      final AtomicInteger skippedCount,
      final AtomicInteger processedCount,
      final Format format,
      final FormatStoreManager manager)
  {
    log.info("normalizing {} components version", format.getValue());

    ComponentStore<?> componentStore = manager.componentStore(DEFAULT_DATASTORE_NAME);

    if (!isFormatNormalized(format)) {
      //initially set normalization state as false
      setNormalizationState(format, false);
      normalizeFormat(format, componentStore);
      //once normalization is done set state as true
      setNormalizationState(format, true);
      //publish an event to let interested know the format has been normalized
      eventManager.post(new FormatVersionNormalizedEvent(format));

      int currentCount = processedCount.incrementAndGet();

      progressLogger.info(" task progress : {}% ({} of {} formats - skipped : {}) - elapsed : {}",
          Math.round(((float) currentCount / totalCount) * 100),
          currentCount, totalCount, skippedCount.get(), progressLogger.getElapsed());
    }
    else {
      log.debug("skipping {} format since is already normalized.", format.getValue());
      skippedCount.getAndIncrement();
    }
  }

  /**
   * Gets a normalization state for the given format
   *
   * @param format the format to perform the query
   * @return {@link Boolean} flag indicating the normalization state
   */
  private Boolean isFormatNormalized(final Format format) {
    return globalKeyValueStore.getKey(getFormatKey(format))
        .map(NexusKeyValue::getAsBoolean)
        .orElseGet(() -> {
          log.debug("no previous normalization state for {} format", format);
          return false;
        });
  }

  /**
   * Sets a normalization state for the given format
   *
   * @param format the format to set the as part of the key
   * @param value  a {@link Boolean} flag indicating if the normalized version is available
   */
  private void setNormalizationState(final Format format, final boolean value) {
    NexusKeyValue kv = new NexusKeyValue();
    kv.setKey(getFormatKey(format));
    kv.setType(ValueType.BOOLEAN);
    kv.setValue(value);

    globalKeyValueStore.setKey(kv);
  }

  /**
   * Builds a string key with the given format
   *
   * @param format the format to set as part of the key
   * @return a {@link String} value with the key
   */
  private String getFormatKey(final Format format) {
    return format(KEY_FORMAT, format.getValue());
  }

  /**
   * Normalizes version of  {format}_component 's records
   *
   * @param format         the given format
   * @param componentStore the format component store
   */
  private void normalizeFormat(final Format format, final ComponentStore<?> componentStore) {
    Continuation<ComponentData> page =
        componentStore.browseUnnormalized(Continuations.BROWSE_LIMIT, null);

    int totalCount = componentStore.countUnnormalized();
    int processedCount = 0;

    log.info("found {} unnormalized records on {} components", totalCount, format.getValue());

    while (!page.isEmpty() && page.nextContinuationToken() != null) {
      page.forEach((component) -> {
        String normalizedVersion = versionNormalizerService.getNormalizedVersionByFormat(component.version(), format);
        component.setNormalizedVersion(normalizedVersion);
        componentStore.updateComponentNormalizedVersion(component);
      });

      processedCount += page.size();

      page = componentStore.browseUnnormalized(Continuations.BROWSE_LIMIT, page.nextContinuationToken());

      log.info(" {} format progress : {}% ({} of {}) - elapsed : {}", format.getValue(),
          Math.round(((float) processedCount / totalCount) * 100),
          processedCount, totalCount, progressLogger.getElapsed());
    }
  }
}
