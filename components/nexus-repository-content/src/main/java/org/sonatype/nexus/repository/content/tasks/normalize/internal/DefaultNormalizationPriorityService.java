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
package org.sonatype.nexus.repository.content.tasks.normalize.internal;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.content.tasks.normalize.NormalizationPriorityService;

/**
 * OSS implementation which does no prioritization of formats for normalization
 */
@Named
@Singleton
public class DefaultNormalizationPriorityService implements NormalizationPriorityService
{

  private final Map<Format, FormatStoreManager> prioritizedFormats;

  @Inject
  public DefaultNormalizationPriorityService(
      final Map<String, FormatStoreManager> managersByFormat,
      final List<Format> formats)
  {
    this.prioritizedFormats = formats.stream()
        .collect(Collectors.toMap(format -> format, format -> managersByFormat.get(format.getValue())));
  }

  @Override
  public Map<Format, FormatStoreManager> getPrioritizedFormats() {
    return prioritizedFormats;
  }
}
