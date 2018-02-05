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
package org.sonatype.nexus.repository.maven;

import java.util.Optional;

import org.sonatype.nexus.repository.upload.AssetUpload;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.ValidatingComponentUpload;

import static org.sonatype.nexus.common.text.Strings2.isBlank;

/**
 * A holder of {@link ComponentUpload} that's meant to validate it based on provided {@link UploadDefinition} for Maven
 *
 * @since 3.8
 */
public class MavenValidatingComponentUpload
    extends ValidatingComponentUpload
{
  private static final String EXTENSION = "extension";

  private static final String CLASSIFIER = "classifier";

  public MavenValidatingComponentUpload(final UploadDefinition uploadDefinition,
                                        final ComponentUpload componentUpload)
  {
    super(uploadDefinition, componentUpload);
  }

  @Override
  protected void validateRequiredComponentFieldPresent() {
    if (!findPomAsset().isPresent()) {
      super.validateRequiredComponentFieldPresent();
    }
  }

  private Optional<AssetUpload> findPomAsset() {
    return componentUpload.getAssetUploads().stream()
        .filter(asset -> "pom".equals(asset.getField(EXTENSION)) && isBlank(asset.getField(CLASSIFIER)))
        .findFirst();
  }
}
