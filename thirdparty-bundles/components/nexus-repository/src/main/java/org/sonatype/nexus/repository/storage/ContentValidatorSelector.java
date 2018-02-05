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
import org.sonatype.nexus.repository.Repository;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Content validator selector component.
 *
 * @since 3.0
 */
@Singleton
@Named
public class ContentValidatorSelector
    extends ComponentSupport
{
  private final Map<String, ContentValidator> contentValidators;

  private final DefaultContentValidator defaultContentValidator;

  @Inject
  public ContentValidatorSelector(final Map<String, ContentValidator> contentValidators,
                                  final DefaultContentValidator defaultContentValidator)
  {
    this.contentValidators = checkNotNull(contentValidators);
    this.defaultContentValidator = checkNotNull(defaultContentValidator);
  }

  /**
   * Find content validator for given repository. If no format-specific validator is configured, the default is used.
   *
   * @param repository The repository for content validator is looked up.
   * @return the repository specific content validator to be used, or the default content validator, never {@code null}.
   */
  @Nonnull
  public ContentValidator validator(final Repository repository) {
    checkNotNull(repository);
    String format = repository.getFormat().getValue();
    log.trace("Looking for content validator for format: {}", format);
    ContentValidator contentValidator = contentValidators.get(format);
    if (contentValidator == null) {
      return defaultContentValidator;
    }
    return contentValidator;
  }
}
