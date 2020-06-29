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
package org.sonatype.nexus.repository.content;

import java.time.OffsetDateTime;

import org.sonatype.nexus.common.collect.NestedAttributesMap;

/**
 * Metadata common to all repository content.
 *
 * @since 3.20
 */
public interface RepositoryContent
{
  /**
   * Schemaless content attributes.
   */
  NestedAttributesMap attributes();

  /**
   * Shortcut to content sub-attributes.
   */
  default NestedAttributesMap attributes(String key) {
    return attributes().child(key);
  }

  /**
   * When the metadata was first created.
   */
  OffsetDateTime created();

  /**
   * When the metadata was last updated.
   */
  OffsetDateTime lastUpdated();
}
