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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.cleanup.CleanupFeatureCheck;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;

/**
 * Service implementation to prioritize formats to be normalized for retain-N
 */
@Named
@Singleton
public class NormalizationPriorityService
{
  private final CleanupFeatureCheck cleanupFeatureCheck;

  private final Map<String, FormatStoreManager> managersByFormat;

  private final List<Format> formats;

  private final Map<Format, FormatStoreManager> prioritizedFormats;

  @Inject
  public NormalizationPriorityService(
      final CleanupFeatureCheck cleanupFeatureCheck,
      final Map<String, FormatStoreManager> managersByFormat,
      final List<Format> formats)
  {
    this.cleanupFeatureCheck = cleanupFeatureCheck;
    this.managersByFormat = managersByFormat;
    this.formats = formats;
    this.prioritizedFormats = new LinkedHashMap<>();
    this.prioritize();
  }

  private void prioritize() {
    Map<Boolean, List<Format>> formatByPriority = formats.stream()
        .collect(Collectors.partitioningBy(
            (format) -> cleanupFeatureCheck.isRetainSupported(format.getValue())));

    //first get prioritized and add them
    formatByPriority.get(true)
        .forEach(format -> prioritizedFormats.put(format, managersByFormat.get(format.getValue())));

    //then not prioritized
    formatByPriority.get(false)
        .forEach(format -> prioritizedFormats.put(format, managersByFormat.get(format.getValue())));
  }

  public Map<Format, FormatStoreManager> getPrioritizedFormats() {
    return prioritizedFormats;
  }
}
