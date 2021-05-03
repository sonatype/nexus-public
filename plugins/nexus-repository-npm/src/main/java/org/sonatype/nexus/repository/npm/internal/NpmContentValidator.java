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
package org.sonatype.nexus.repository.npm.internal;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.io.InputStreamSupplier;
import org.sonatype.nexus.mime.MimeRulesSource;
import org.sonatype.nexus.repository.mime.ContentValidator;
import org.sonatype.nexus.repository.mime.DefaultContentValidator;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.view.ContentTypes.APPLICATION_JSON;
import static org.sonatype.nexus.repository.view.ContentTypes.TEXT_PLAIN;

/**
 * npm specific {@link ContentValidator} that "hints" default content validator for npm metadata and format
 * specific files.
 *
 * @since 3.0
 */
@Named(NpmFormat.NAME)
@Singleton
public class NpmContentValidator
    extends ComponentSupport
    implements ContentValidator
{
  private static final List<String> NPM_METADATA_TYPES = ImmutableList.of(APPLICATION_JSON, TEXT_PLAIN);

  private final DefaultContentValidator defaultContentValidator;

  @Inject
  public NpmContentValidator(final DefaultContentValidator defaultContentValidator) {
    this.defaultContentValidator = checkNotNull(defaultContentValidator);
  }

  @Nonnull
  @Override
  public String determineContentType(final boolean strictContentTypeValidation,
                                     final InputStreamSupplier contentSupplier,
                                     @Nullable final MimeRulesSource mimeRulesSource,
                                     @Nullable final String contentName,
                                     @Nullable final String declaredContentType) throws IOException
  {
    String name = restoreFileExtension(contentName, declaredContentType);
    return defaultContentValidator.determineContentType(
        strictContentTypeValidation, contentSupplier, mimeRulesSource, name, declaredContentType
    );
  }

  @Nullable
  private String restoreFileExtension(final String name, final String declaredContentType) {
    if (name == null) {
      return null;
    }
    if (!name.endsWith(".json") && NPM_METADATA_TYPES.contains(declaredContentType)) {
      // json package root
      return name + ".json";
    }
    else if (!name.endsWith(".tgz")) {
      return name + ".tgz";
    }
    return name;
  }
}
