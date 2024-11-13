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
package org.sonatype.nexus.repository.rest.sql;

import org.sonatype.nexus.repository.search.SortDirection;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Fields available for search when using an SQL backend for search
 */
public enum SearchField
{
  /**
   * The format of the repository
   */
  FORMAT,

  REPOSITORY_NAME,

  COMPONENT_ID,

  COMPONENT_KIND,

  NAMESPACE,

  NAME,

  VERSION(SortDirection.DESC),

  PRERELEASE,

  TAGS,

  LAST_MODIFIED,

  /**
   * The paths associated with the assets
   */
  PATHS,

  KEYWORDS,

  /**
   * Checksums
   */
  MD5,
  SHA1,
  SHA256,
  SHA512,

  /**
   * Format-specific fields
   */
  FORMAT_FIELD_1,
  FORMAT_FIELD_2,
  FORMAT_FIELD_3,
  FORMAT_FIELD_4,
  FORMAT_FIELD_5,
  FORMAT_FIELD_6,
  FORMAT_FIELD_7,

  UPLOADERS,
  UPLOADER_IPS,
  ASSET_FORMAT_VALUE_1,
  ASSET_FORMAT_VALUE_2,
  ASSET_FORMAT_VALUE_3,
  ASSET_FORMAT_VALUE_4,
  ASSET_FORMAT_VALUE_5,
  ASSET_FORMAT_VALUE_6,
  ASSET_FORMAT_VALUE_7,
  ASSET_FORMAT_VALUE_8,
  ASSET_FORMAT_VALUE_9,
  ASSET_FORMAT_VALUE_10,
  ASSET_FORMAT_VALUE_11,
  ASSET_FORMAT_VALUE_12,
  ASSET_FORMAT_VALUE_13,
  ASSET_FORMAT_VALUE_14,
  ASSET_FORMAT_VALUE_15,
  ASSET_FORMAT_VALUE_16,
  ASSET_FORMAT_VALUE_17,
  ASSET_FORMAT_VALUE_18,
  ASSET_FORMAT_VALUE_19,
  ASSET_FORMAT_VALUE_20;

  private final SortDirection direction;

  SearchField() {
    this.direction = SortDirection.ASC;
  }

  SearchField(final SortDirection direction) {
    this.direction = checkNotNull(direction);
  }

  public SortDirection direction() {
    return direction;
  }
}
