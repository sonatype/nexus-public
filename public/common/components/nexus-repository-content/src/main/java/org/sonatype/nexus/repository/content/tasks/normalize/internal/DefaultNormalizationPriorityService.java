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

import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.content.tasks.normalize.NormalizationPriorityService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * OSS implementation which does no prioritization of formats for normalization
 */
@Component
public class DefaultNormalizationPriorityService
    implements NormalizationPriorityService
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final Map<String, FormatStoreManager> prioritizedFormats;

  @Autowired
  public DefaultNormalizationPriorityService(final List<FormatStoreManager> formatStoreManagerList) {
    prioritizedFormats = QualifierUtil.buildQualifierBeanMap(formatStoreManagerList);
  }

  @Override
  public Map<String, FormatStoreManager> getPrioritizedFormats() {
    return prioritizedFormats;
  }
}
