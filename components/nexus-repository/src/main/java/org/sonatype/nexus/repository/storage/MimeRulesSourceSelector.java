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
package org.sonatype.nexus.repository.storage;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.mime.MimeRulesSource;
import org.sonatype.nexus.repository.Repository;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * MIME rules source selector component.
 *
 * @since 3.0
 */
@Singleton
@Named
public class MimeRulesSourceSelector
    extends ComponentSupport
{
  private final Map<String, MimeRulesSource> mimeRulesSources;

  @Inject
  public MimeRulesSourceSelector(final Map<String, MimeRulesSource> mimeRulesSources)
  {
    this.mimeRulesSources = checkNotNull(mimeRulesSources);
  }

  /**
   * Find MIME rule source for given repository. If no format-specific source is configured, {@link
   * MimeRulesSource#NOOP} is returned.
   *
   * @param repository The repository for MIME rule source is looked up.
   * @return the repository specific MIME rule source, or noop rule selector.
   */
  @Nonnull
  public MimeRulesSource ruleSource(final Repository repository) {
    checkNotNull(repository);
    String format = repository.getFormat().getValue();
    log.trace("Looking for MIME rule source for format: {}", format);
    MimeRulesSource mimeRulesSource = mimeRulesSources.get(format);
    if (mimeRulesSource != null) {
      return mimeRulesSource;
    }
    return MimeRulesSource.NOOP;
  }
}
