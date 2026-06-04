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
package org.sonatype.nexus.repository.content.upgrades;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.repository.Format;

import org.springframework.stereotype.Component;

/**
 * Adds indexes to {format}_key_value tables for existing databases.
 *
 * Related to NEXUS-49154 - Migration created to allow removal of CREATE INDEX
 * from MyBatis createSchema() which could cause lock contention on startup.
 *
 * @since 3.87
 */
@Component
public class KeyValueIndexesMigrationStep_2_65
    extends KeyValueIndexesMigrationStepSupport
{
  @Autowired
  public KeyValueIndexesMigrationStep_2_65(final List<Format> formats) {
    super(formats.stream()
        .map(Format::getValue)
        .toList());
  }

  @Override
  public Optional<String> version() {
    return Optional.of("2.65");
  }
}
