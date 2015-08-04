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
package org.sonatype.nexus.plugins.p2.repository.proxy.validator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.plugins.p2.repository.P2Repository;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.repository.validator.AbstractMimeMagicFileTypeValidator;
import org.sonatype.nexus.proxy.repository.validator.FileTypeValidator;

/**
 * {@link FileTypeValidator} for P2 repositories.
 *
 * @since 2.6
 */
@Named("p2")
@Singleton
public class P2FileTypeValidator
    extends AbstractMimeMagicFileTypeValidator
{

  private final byte[] PACK200_MAGIC = new byte[]{31, -117, 8, 0};

  private final byte[] JAR_MAGIC = new byte[]{80, 75, 3, 4};

  @Inject
  public P2FileTypeValidator(final MimeSupport mimeSupport) {
    super(mimeSupport);
  }

  @Override
  public FileTypeValidity isExpectedFileType(final StorageFileItem file) {
    // only check content from p2 repositories
    if (file.getRepositoryItemUid().getRepository().adaptToFacet(P2Repository.class) == null) {
      return FileTypeValidity.NEUTRAL;
    }

    if (file.getRepositoryItemUid().getPath().endsWith(".pack.gz")) {
      try (InputStream input = file.getInputStream();) {
        final byte[] magicBytes = new byte[4];

        if (input.read(magicBytes) > 0) {
          if (Arrays.equals(magicBytes, PACK200_MAGIC)  // real pack.gz
              || Arrays.equals(magicBytes, JAR_MAGIC)) // plain jar works too
          {
            return FileTypeValidity.VALID;
          }
        }
      }
      catch (final IOException e) {
        log.error("Unable to read pack200 magic bytes", e);
      }

      return FileTypeValidity.INVALID;
    }

    return super.isExpectedFileType(file);
  }
}
