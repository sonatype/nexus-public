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
package org.sonatype.nexus.cleanup.internal.content.retain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.rapture.StateContributor;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.cleanup.CleanupFeatureCheck;
import org.sonatype.nexus.repository.content.kv.global.GlobalKeyValueStore;
import org.sonatype.nexus.repository.content.kv.global.NexusKeyValue;
import org.sonatype.nexus.repository.content.tasks.normalize.FormatVersionNormalizedEvent;
import org.sonatype.nexus.repository.content.tasks.normalize.NormalizeComponentVersionTask;

import com.google.common.base.Functions;
import com.google.common.eventbus.Subscribe;

import static org.sonatype.nexus.common.app.FeatureFlags.FORMAT_RETAIN_PATTERN;

/**
 * UI State contributor for the retain-N feature
 */
@Singleton
@Named
public class CleanupRetainStateContributor
    extends ComponentSupport
    implements StateContributor, EventAware
{
  private final Map<Format, String> stateKeyByFormat;

  private final GlobalKeyValueStore globalKeyValueStore;

  private final CleanupFeatureCheck featureCheck;

  private final Map<String, Object> state;

  @Inject
  public CleanupRetainStateContributor(
      final List<Format> formats,
      final GlobalKeyValueStore globalKeyValueStore,
      final CleanupFeatureCheck featureCheck)
  {
    this.globalKeyValueStore = globalKeyValueStore;
    this.stateKeyByFormat = buildStateKeyByFormat(formats);
    this.featureCheck = featureCheck;
    state = new HashMap<>();
    buildStateMap();
  }

  private Map<Format, String> buildStateKeyByFormat(final List<Format> formats) {
    return formats
        .stream()
        .collect(Collectors.toMap(Functions.identity(),
            (format) -> String.format(NormalizeComponentVersionTask.KEY_FORMAT, format.getValue())));
  }

  private void buildStateMap() {
    setRetainEnabledFlags();
    setNormalizationStateFlags();
  }

  /**
   * Adds all the required flags to validate if a format has retain-N enabled
   */
  private void setRetainEnabledFlags() {
    stateKeyByFormat.keySet()
        .stream().map(Format::getValue)
        .forEach(format -> state.put(FORMAT_RETAIN_PATTERN.replace("{format}", format),
            featureCheck.isRetainSupported(format)));
  }

  /**
   * Adds all the required flags to validate if the normalized_version column under the {format}_component tables is
   * ready for retain-N by version
   */
  private void setNormalizationStateFlags() {
    Map<String, Boolean> normalizationStateByFormat = stateKeyByFormat
        .values()
        .stream()
        .collect(Collectors.toMap(Functions.identity(), this::isFormatNormalized));

    state.putAll(normalizationStateByFormat);
  }

  private boolean isFormatNormalized(final String key) {
    return globalKeyValueStore.getKey(key)
        .map(NexusKeyValue::getAsBoolean)
        .orElse(false);
  }

  @Subscribe
  public void on(final FormatVersionNormalizedEvent event) {
    state.put(stateKeyByFormat.get(event.getFormat()), true);
  }

  @Nullable
  @Override
  public Map<String, Object> getState() {
    return state;
  }
}
