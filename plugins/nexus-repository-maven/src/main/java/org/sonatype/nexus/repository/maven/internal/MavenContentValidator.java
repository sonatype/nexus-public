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
package org.sonatype.nexus.repository.maven.internal;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.mime.MimeRulesSource;
import org.sonatype.nexus.repository.InvalidContentException;
import org.sonatype.nexus.repository.storage.ContentValidator;
import org.sonatype.nexus.repository.storage.DefaultContentValidator;

import com.google.common.base.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Maven 2 specific {@link ContentValidator} that "hints" default content validator for some Maven specific file
 * extensions and format specific files.
 *
 * @since 3.0
 */
@Named(Maven2Format.NAME)
@Singleton
public class MavenContentValidator
    extends ComponentSupport
    implements ContentValidator
{
  private final DefaultContentValidator defaultContentValidator;

  @Inject
  public MavenContentValidator(final DefaultContentValidator defaultContentValidator) {
    this.defaultContentValidator = checkNotNull(defaultContentValidator);
  }

  @Nonnull
  @Override
  public String determineContentType(boolean strictContentTypeValidation,
                                     Supplier<InputStream> contentSupplier,
                                     @Nullable MimeRulesSource mimeRulesSource,
                                     @Nullable String contentName,
                                     @Nullable String declaredContentType) throws IOException
  {
    if (contentName != null) {
      if (contentName.endsWith(".pom")) {
        // Note: this is due fact that Tika has glob "*.pom" extension enlisted at text/plain
        return defaultContentValidator.determineContentType(
            strictContentTypeValidation, contentSupplier, mimeRulesSource, contentName + ".xml", declaredContentType
        );
      }
      else if (strictContentTypeValidation && (contentName.endsWith(".sha1") || contentName.endsWith(".md5"))) {
        // hashes are small/simple, do it directly
        try (InputStream is = contentSupplier.get()) {
          final String digestCandidate = DigestExtractor.extract(is);
          if (!DigestExtractor.isDigest(digestCandidate)) {
            throw new InvalidContentException("Not a Maven2 digest: " + contentName);
          }
        }
      }
    }
    // everything else goes to default for now
    return defaultContentValidator.determineContentType(
        strictContentTypeValidation, contentSupplier, mimeRulesSource, contentName, declaredContentType
    );
  }
}
