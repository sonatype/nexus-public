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

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonatype.nexus.mime.MimeRulesSource;
import org.sonatype.nexus.repository.InvalidContentException;

import com.google.common.base.Supplier;

/**
 * Content validator interface.
 *
 * @since 3.0
 */
public interface ContentValidator
{
  /**
   * Determines or confirms the content type for the given content, or throws {@link InvalidContentException} if it
   * cannot.
   *
   * @param strictContentTypeValidation whether the check should be strict or not.
   * @param contentSupplier             the supplier of the content to determine or confirm content type.
   * @param contentName                 blob name, usually a file path or file name or just extension
   *                                    (file extension is used to determine content type along with "magic" detection
   *                                    where actual content bits are used, like file headers or magic bytes). Is
   *                                    optional, but be aware that if present it improves content type detection
   *                                    reliability.
   * @param mimeRulesSource             if non-null, mime rules source to use.
   * @param declaredContentType         if non-null, the declared content type will be confirmed, if null, this method
   *                                    will attempt to determine the content type.
   * @return the content type of the content.
   * @throws InvalidContentException if type cannot be confirmed or detected.
   */
  @Nonnull
  String determineContentType(boolean strictContentTypeValidation,
                              Supplier<InputStream> contentSupplier,
                              @Nullable MimeRulesSource mimeRulesSource,
                              @Nullable String contentName,
                              @Nullable String declaredContentType) throws IOException;
}
