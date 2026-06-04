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
package org.sonatype.nexus.cleanup.internal.datastore.search.criteria;

import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.cleanup.datastore.search.criteria.ComponentCleanupEvaluator;
import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.utils.PreReleaseEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.IS_PRERELEASE_KEY;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * An evaluator which creates a test which determines whether a component and its assets pre-release state matches the
 * specified value.
 *
 * @since 3.38
 */
@org.springframework.stereotype.Component
@Qualifier(IS_PRERELEASE_KEY)
public class PrereleaseCleanupEvaluator
    implements ComponentCleanupEvaluator
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private Map<String, PreReleaseEvaluator> matchers;

  @Autowired
  public PrereleaseCleanupEvaluator(final List<PreReleaseEvaluator> matchersList) {
    this.matchers = QualifierUtil.buildQualifierBeanMap(matchersList);
  }

  /*
   * Value is expected to be a boolean. When the value is true, the returned predicate will return true if the
   * component/assets represents a pre-release. If the value is false, then the predicate will return true if the
   * components/assets do not represent a pre-release version.
   */
  @Override
  public BiPredicate<Component, Iterable<Asset>> getPredicate(final Repository repository, final String value) {
    PreReleaseEvaluator matcher = matchers.get(repository.getFormat().getValue());
    BiPredicate<Component, Iterable<Asset>> fn = matcher != null ? matcher::isPreRelease : (c, a) -> false;

    final boolean isPreRelease = Boolean.parseBoolean(value);

    return isPreRelease ? fn : (c, a) -> !fn.test(c, a);
  }
}
