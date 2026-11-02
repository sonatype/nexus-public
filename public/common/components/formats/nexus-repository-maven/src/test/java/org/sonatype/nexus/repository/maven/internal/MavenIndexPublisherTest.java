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
package org.sonatype.nexus.repository.maven.internal;

import java.util.Collections;
import java.util.function.Predicate;

import org.sonatype.nexus.repository.maven.internal.MavenIndexPublisher.RecordTypeFilter;

import org.apache.maven.index.reader.Record;
import org.apache.maven.index.reader.Record.Type;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenIndexPublisherTest
{
  @Test
  void recordTypeFilterAcceptsOnlyTheAllowedTypes() {
    Predicate<Record> filter = new RecordTypeFilter(Type.ARTIFACT_ADD);

    assertTrue(filter.test(record(Type.ARTIFACT_ADD)));
    assertFalse(filter.test(record(Type.ARTIFACT_REMOVE)));
    assertFalse(filter.test(record(Type.DESCRIPTOR)));
  }

  @Test
  void recordTypeFilterAcceptsEveryAllowedType() {
    Predicate<Record> filter = new RecordTypeFilter(Type.ARTIFACT_ADD, Type.DESCRIPTOR);

    assertTrue(filter.test(record(Type.ARTIFACT_ADD)));
    assertTrue(filter.test(record(Type.DESCRIPTOR)));
    assertFalse(filter.test(record(Type.ARTIFACT_REMOVE)));
  }

  private static Record record(final Type type) {
    return new Record(type, Collections.emptyMap());
  }
}
