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
package org.sonatype.nexus.repository.upload;

import java.util.List;
import java.util.Set;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

/**
 * Base class for format specific {@link UploadHandler} classes which
 * enables {@link UploadFieldDefinition} contributions to the format
 * specific {@link UploadDefinition}
 *
 * @since 3.10
 */
public abstract class UploadHandlerSupport
    extends ComponentSupport
    implements UploadHandler
{
  private final Set<UploadDefinitionExtension> uploadDefinitionExtensions;

  public UploadHandlerSupport(final Set<UploadDefinitionExtension> uploadDefinitionExtensions) {
    this.uploadDefinitionExtensions = checkNotNull(uploadDefinitionExtensions);
  }

  /**
   * Provides a mechanism for subclasses to generate an UploadDefinition that allows for
   * extension point contributions.
   *
   * Order of the UploadFieldDefinitions is important as it affects the presentation in the ui.
   */
  public UploadDefinition getDefinition(final String format,
                                        final boolean multipleUpload,
                                        final List<UploadFieldDefinition> componentFields,
                                        final List<UploadFieldDefinition> assetFields,
                                        final UploadRegexMap regexMap)
  {

    //Gather the existing and contributed field definitions
    List<UploadFieldDefinition> componentFieldDefinitions = concat(
        componentFields.stream(),
        uploadDefinitionExtensions.stream().map(UploadDefinitionExtension::contribute)).collect(toList());

    return new UploadDefinition(format, supportsUiUpload(), supportsApiUpload(), multipleUpload, componentFieldDefinitions, assetFields, regexMap);
  }

  /**
   * Provides a mechanism for subclasses to generate an UploadDefinition that allows for
   * extension point contributions.
   *
   * This variant only requires the format and multiple upload argument.
   */
  public UploadDefinition getDefinition(final String format, final boolean multipleUpload) {
    return getDefinition(format, multipleUpload, emptyList(), emptyList(), null);
  }
}
