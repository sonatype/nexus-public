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
package org.sonatype.nexus.repository.maven.internal.maven2;

import java.util.Locale;

import org.sonatype.nexus.repository.view.ContentTypes;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Maven 2 constants.
 *
 * @since 3.0
 */
public class Constants
{
  private Constants() {
  }

  /**
   * File name of Maven2 repository metadata files.
   */
  public static final String METADATA_FILENAME = "maven-metadata.xml";

  /**
   * {@link DateTimeFormatter} for dotted timestamps used in Maven2 repository metadata.
   */
  public static final DateTimeFormatter METADATA_DOTTED_TIMESTAMP = DateTimeFormat.forPattern("YYYYMMdd.HHmmss")
      .withZoneUTC().withLocale(Locale.ENGLISH);

  /**
   * {@link DateTimeFormatter} for dotless timestamps used in Maven2 repository metadata.
   */
  public static final DateTimeFormatter METADATA_DOTLESS_TIMESTAMP = DateTimeFormat.forPattern("YYYYMMddHHmmss")
      .withZoneUTC().withLocale(Locale.ENGLISH);

  /**
   * Content Type of Maven2 checksum files (sha1, md5).
   */
  public static final String CHECKSUM_CONTENT_TYPE = ContentTypes.TEXT_PLAIN;

  /**
   * The suffix of base version for snapshots.
   */
  public static final String SNAPSHOT_VERSION_SUFFIX = "SNAPSHOT";
}
