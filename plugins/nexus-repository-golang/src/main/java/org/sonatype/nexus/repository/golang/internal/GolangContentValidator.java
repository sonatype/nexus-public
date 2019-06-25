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
package org.sonatype.nexus.repository.golang.internal;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.mime.MimeRulesSource;
import org.sonatype.nexus.repository.golang.GolangFormat;
import org.sonatype.nexus.repository.storage.ContentValidator;
import org.sonatype.nexus.repository.storage.DefaultContentValidator;

import com.google.common.base.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Go specific {@link ContentValidator} that validates for some go specific file
 * extensions and format specific files.
 *
 * @since 3.17
 */
@Named(GolangFormat.NAME)
@Singleton
public class GolangContentValidator
    extends ComponentSupport
    implements ContentValidator
{
  private final DefaultContentValidator defaultContentValidator;

  @Inject
  public GolangContentValidator(final DefaultContentValidator defaultContentValidator) {
    this.defaultContentValidator = checkNotNull(defaultContentValidator);
  }

  @Nonnull
  @Override
  public String determineContentType(final boolean strictContentTypeValidation,
                                     final Supplier<InputStream> contentSupplier,
                                     @Nullable final MimeRulesSource mimeRulesSource,
                                     @Nullable final String contentName,
                                     @Nullable final String declaredContentType) throws IOException
  {
    if (contentName != null && contentName.endsWith(".mod")) {
      // Note: this is due to fact that Tika has glob "*.mod" extension enlisted at audio/x-mod
      return defaultContentValidator.determineContentType(
          strictContentTypeValidation, contentSupplier, mimeRulesSource, contentName + ".txt", declaredContentType
      );
    }
    return defaultContentValidator.determineContentType(
        strictContentTypeValidation, contentSupplier, mimeRulesSource, contentName, declaredContentType
    );
  }
}
