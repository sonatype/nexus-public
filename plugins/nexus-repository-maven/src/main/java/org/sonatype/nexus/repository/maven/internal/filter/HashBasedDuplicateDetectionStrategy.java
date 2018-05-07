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
package org.sonatype.nexus.repository.maven.internal.filter;

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.index.reader.Record;

import static com.google.common.hash.Hashing.md5;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonatype.nexus.repository.maven.internal.utils.RecordUtils.gavceForRecord;

/**
 * Detects duplicates using an in memory map of hashes calculated from GAV-CE. This has an upper limit and will struggle
 * on most machines to check for duplicates on central. It is, however, guaranteed to be correct and is quick.
 *
 * @since 3.11
 */
public class HashBasedDuplicateDetectionStrategy
    implements DuplicateDetectionStrategy<Record>
{
  private Set<String> gavces = new HashSet<>();

  @Override
  public boolean apply(final Record record) {
    return gavces.add(md5().hashString(gavceForRecord(record), UTF_8).toString());
  }
}
