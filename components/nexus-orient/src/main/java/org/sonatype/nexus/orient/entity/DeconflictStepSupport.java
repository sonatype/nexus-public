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
package org.sonatype.nexus.orient.entity;

import java.util.Comparator;
import java.util.Date;
import java.util.Objects;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.Entity;

import com.google.common.primitives.Longs;
import com.orientechnologies.orient.core.record.impl.ODocument;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static org.sonatype.nexus.orient.entity.ConflictState.ALLOW;
import static org.sonatype.nexus.orient.entity.ConflictState.DENY;
import static org.sonatype.nexus.orient.entity.ConflictState.IGNORE;
import static org.sonatype.nexus.orient.entity.ConflictState.MERGE;

/**
 * Provides re-usable generic deconfliction methods.
 *
 * @since 3.14
 */
public abstract class DeconflictStepSupport<T extends Entity>
    extends ComponentSupport
    implements DeconflictStep<T>
{
  private static final Comparator<Long> MILLIS_COMPARATOR = nullsFirst(naturalOrder());

  /**
   * Resolves differences in the given optional field by picking non-null values over null values.
   */
  public static ConflictState pickNonNull(final ODocument storedRecord,
                                          final ODocument changeRecord,
                                          final String fieldName)
  {
    Object storedValue = storedRecord.rawField(fieldName);
    Object changeValue = changeRecord.rawField(fieldName);
    if (Objects.equals(storedValue, changeValue)) {
      return IGNORE;
    }
    else if (storedValue == null) {
      storedRecord.field(fieldName, changeValue);
      return ALLOW;
    }
    else if (changeValue == null) {
      changeRecord.field(fieldName, storedValue);
      return MERGE;
    }
    return DENY;
  }

  /**
   * Resolves differences in the given date field by picking the latest time.
   *
   * If the incoming change has a later time we write that to our copy of the stored record
   * (the actual write to the DB happens after all resolution steps are done) so they match
   * when the final {@link EntityAdapter#compare} check happens. Otherwise if the stored time
   * is later then that value is merged with the incoming change record.
   */
  public static ConflictState pickLatest(final ODocument storedRecord,
                                         final ODocument changeRecord,
                                         final String fieldName)
  {
    Object storedValue = storedRecord.rawField(fieldName);
    Object changeValue = changeRecord.rawField(fieldName);
    // prefer later non-null times, over earlier/null times
    int comparison = MILLIS_COMPARATOR.compare(asMillis(storedValue), asMillis(changeValue));
    if (comparison < 0) {
      storedRecord.field(fieldName, changeValue);
      return ALLOW;
    }
    else if (comparison > 0) {
      changeRecord.field(fieldName, storedValue);
      return MERGE;
    }
    return IGNORE;
  }

  /**
   * Converts values to an instant in milliseconds.
   */
  @Nullable
  private static Long asMillis(@Nullable final Object value) {
    Long time = null;
    if (value instanceof Date) {
      time = ((Date) value).getTime();
    }
    else if (value instanceof Number) {
      time = ((Number) value).longValue();
    }
    else if (value instanceof String) {
      time = Longs.tryParse((String) value); // timestamp string
    }
    return time;
  }
}
