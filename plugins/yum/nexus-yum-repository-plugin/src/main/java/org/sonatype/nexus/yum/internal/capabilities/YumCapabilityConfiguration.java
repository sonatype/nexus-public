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
package org.sonatype.nexus.yum.internal.capabilities;

import java.util.Map;

import org.sonatype.nexus.yum.YumRegistry;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;

/**
 * Configuration adapter for {@link YumCapability}.
 *
 * @since yum 3.0
 */
public class YumCapabilityConfiguration
{

  public static final String MAX_NUMBER_PARALLEL_THREADS = "maxNumberParallelThreads";

  public static final String CREATEREPO_PATH = "createrepoPath";

  public static final String MERGEREPO_PATH = "mergerepoPath";

  private int maxParallelThreads;

  private String createrepoPath = YumRegistry.DEFAULT_CREATEREPO_PATH;

  private String mergerepoPath = YumRegistry.DEFAULT_MERGEREPO_PATH;

  public YumCapabilityConfiguration(final int maxParallelThreads) {
    this.maxParallelThreads = maxParallelThreads;
  }

  public YumCapabilityConfiguration(final Map<String, String> properties) {
    int maxParallelThreads = YumRegistry.DEFAULT_MAX_NUMBER_PARALLEL_THREADS;
    try {
      maxParallelThreads = Integer.parseInt(properties.get(MAX_NUMBER_PARALLEL_THREADS));
    }
    catch (NumberFormatException e) {
      // will use default
    }
    this.maxParallelThreads = maxParallelThreads;
    createrepoPath = properties.get(CREATEREPO_PATH);
    if (StringUtils.isBlank(createrepoPath)) {
      createrepoPath = YumRegistry.DEFAULT_CREATEREPO_PATH;
    }
    mergerepoPath = properties.get(MERGEREPO_PATH);
    if (StringUtils.isBlank(mergerepoPath)) {
      mergerepoPath = YumRegistry.DEFAULT_MERGEREPO_PATH;
    }
  }

  public int maxNumberParallelThreads() {
    return maxParallelThreads;
  }

  public String getCreaterepoPath() {
    return createrepoPath;
  }

  public String getMergerepoPath() {
    return mergerepoPath;
  }

  public Map<String, String> asMap() {
    final Map<String, String> props = Maps.newHashMap();
    props.put(MAX_NUMBER_PARALLEL_THREADS, String.valueOf(maxParallelThreads));
    props.put(CREATEREPO_PATH, createrepoPath);
    props.put(MERGEREPO_PATH, mergerepoPath);
    return props;
  }

}
