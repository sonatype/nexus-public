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
package org.sonatype.nexus.proxy.repository.validator;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.mime.NexusMimeTypes;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.util.SystemPropertiesHelper;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper base class for implementing {@link FileTypeValidator} components that want to verify the content's MIME magic
 * signature using {@link MimeSupport}. The main method {@link #isExpectedFileType(StorageFileItem)} has to be
 * implemented by implementor in a way it collects it's "expectations" from some source (internal state, some config,
 * whatever) and invokes the {@link #isExpectedFileTypeByDetectedMimeType(StorageFileItem, Set, boolean)} method with
 * the file
 * item and the set of "expectations". If the set has intersection, file is claimed valid, otherwise invalid.
 *
 * @author cstamas
 * @since 2.0
 */
public abstract class AbstractMimeMagicFileTypeValidator
    extends AbstractFileTypeValidator
{

  public static final String XML_DETECTION_LAX_KEY = FileTypeValidator.class.getName() + ".relaxedXmlValidation";

  private static final boolean XML_DETECTION_LAX = SystemPropertiesHelper.getBoolean(
      XML_DETECTION_LAX_KEY, true
  );

  private final MimeSupport mimeSupport;

  private final NexusMimeTypes mimeTypes;

  protected AbstractMimeMagicFileTypeValidator(final MimeSupport mimeSupport) {
    this.mimeSupport = checkNotNull(mimeSupport);
    this.mimeTypes = new NexusMimeTypes();
  }

  protected AbstractMimeMagicFileTypeValidator(final NexusMimeTypes mimeTypes,
                                               final MimeSupport mimeSupport)
  {
    this.mimeSupport = checkNotNull(mimeSupport);
    this.mimeTypes = checkNotNull(mimeTypes);
  }

  /**
   * This method accepts the file item which content needs MIME magic detection, and the set of "expectations" to
   * match against. If the detected set of MIME types and passed in set of MIME types has intersection, file is
   * claimed VALID, otherwise INVALID. If the passed in set of expectations is empty of {@code null}, NEUTRAL stance
   * is claimed and nothing is done.
   *
   * @param file              to have checked content.
   * @param expectedMimeTypes the "expectations" against detected MIME types.
   * @param contentOnly       {@code true} to match content only, otherwise file name will be taken into account too
   *                          (potentially resulting in misleading matches, see "empty XML file with lax=false" case!).
   * @return {@link FileTypeValidity#VALID} if detected MIME types and passed in expectations has intersection,
   *         {@link FileTypeValidity#INVALID} otherwise. {@link FileTypeValidity#NEUTRAL} if passed in expectations
   *         are {@code null} or empty.
   * @throws IOException in case of some IO problem.
   */
  protected FileTypeValidity isExpectedFileTypeByDetectedMimeType(final StorageFileItem file,
                                                                  final Set<String> expectedMimeTypes,
                                                                  final boolean contentOnly)
      throws IOException
  {
    if (expectedMimeTypes == null || expectedMimeTypes.isEmpty()) {
      // we have nothing to work against, cannot take side
      return FileTypeValidity.NEUTRAL;
    }

    if (file.getContentLocator().getLength() == 0) {
      // zero length FILE sent for CONTENT validation: FAIL
      log.info(
          "StorageFileItem {} MIME-magic validation failed: 0 bytes length file, no content to validate",
          file.getRepositoryItemUid()
      );
      return FileTypeValidity.INVALID;
    }

    final List<String> magicMimeTypes;
    if (contentOnly) {
      magicMimeTypes = mimeSupport.detectMimeTypesListFromContent(file.getContentLocator());
    }
    else {
      magicMimeTypes = mimeSupport.detectMimeTypesListFromContent(file);
    }

    if (log.isDebugEnabled()) {
      log.debug(
          "Checking StorageFileItem {} is one of the expected mime types: {}, detected mime types are: {}",
          file.getRepositoryItemUid(), expectedMimeTypes, magicMimeTypes
      );
    }

    for (String magicMimeType : magicMimeTypes) {
      if (expectedMimeTypes.contains(magicMimeType)) {
        return FileTypeValidity.VALID;
      }
    }

    log.info(
        "StorageFileItem {} MIME-magic validation failed: expected MIME types: {}, detected MIME types: {}",
        file.getRepositoryItemUid(), expectedMimeTypes, magicMimeTypes
    );

    return FileTypeValidity.INVALID;
  }

  @Override
  public FileTypeValidity isExpectedFileType(final StorageFileItem file) {
    boolean xmlLaxValidation = isXmlLaxValidation(file);

    final String filePath = file.getPath().toLowerCase();

    final Set<String> expectedMimeTypes = new HashSet<String>();

    final NexusMimeTypes.NexusMimeType type = mimeTypes.getMimeTypes(filePath);
    if (type != null) {
      expectedMimeTypes.addAll(type.getMimetypes());
    }

    // If we expect XML content, and XML LAX is FALSE, we have to do content only MAGIC detection!
    boolean contentOnly = (expectedMimeTypes.contains("application/xml") || expectedMimeTypes.contains("text/xml")) &&
        !xmlLaxValidation;

    try {
      // the expectedMimeTypes will be empty, see map in constructor which extensions we check at all.
      // The isExpectedFileTypeByDetectedMimeType() method will claim NEUTRAL when expectancies are empty/null
      final FileTypeValidity mimeDetectionResult = isExpectedFileTypeByDetectedMimeType(
          file, expectedMimeTypes, contentOnly
      );

      if (FileTypeValidity.INVALID.equals(mimeDetectionResult)
          && xmlLaxValidation
          && filePath.endsWith(".xml")) {
        // we go LAX way, if MIME detection says INVALID (does for XMLs missing preamble too)
        // we just stay put saying we are "neutral" on this question
        log.info("StorageFileItem {} detected as INVALID XML file but relaxed XML validation is in effect.",
            file.getRepositoryItemUid());
        return FileTypeValidity.NEUTRAL;
      }

      return mimeDetectionResult;
    }
    catch (IOException e) {
      log.warn(
          "Cannot detect MIME type and validate content of StorageFileItem: " + file.getRepositoryItemUid(),
          e);

      return FileTypeValidity.NEUTRAL;
    }
  }

  protected boolean isXmlLaxValidation(final StorageFileItem file) {
    // Note: this here is an ugly hack: enables per-request control of
    // LAX XML validation: if key not present, "system wide" settings used.
    // If key present, it's interpreted as Boolean and it's value is used to
    // drive LAX XML validation enable/disable.
    boolean xmlLaxValidation = XML_DETECTION_LAX;
    if (file.getItemContext().containsKey(XML_DETECTION_LAX_KEY)) {
      xmlLaxValidation = Boolean.parseBoolean(
          String.valueOf(file.getItemContext().get(XML_DETECTION_LAX_KEY))
      );
    }
    return xmlLaxValidation;
  }

}
