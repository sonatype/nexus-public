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
package org.sonatype.nexus.repository.assetdownloadcount;

import org.sonatype.nexus.repository.assetdownloadcount.internal.DateUtils;

import com.google.common.annotations.VisibleForTesting;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.3
 */
@VisibleForTesting
public enum DateType
{
  DAY(30), //we keep the last 30 days of DAY type records
  MONTH(14), //we keep the last 14 months of MONTH type records
  MONTH_WHOLE_REPO(14), //these records are sums for the whole repository
  MONTH_WHOLE_REPO_VULNERABLE(14); //these records are sums for the whole repository

  private static final String UNIMPLEMENTED_DATE_TYPE_ENCOUNTERED = "Unimplemented DateType encountered ";

  private final int numberToKeep;

  DateType(final int numberToKeep) {
    this.numberToKeep = numberToKeep;
  }

  public int getNumberToKeep() {
    return numberToKeep;
  }

  public DateTime standardizeDate(final DateTime date) {
    checkNotNull(date);
    switch (this) {
      case DAY:
        return DateUtils.clearTime(date);
      case MONTH:
      case MONTH_WHOLE_REPO:
      case MONTH_WHOLE_REPO_VULNERABLE:
        return DateUtils.clearDayAndTime(date);
      default:
        throw new IllegalArgumentException(UNIMPLEMENTED_DATE_TYPE_ENCOUNTERED + this.name());
    }
  }

  public DateTime toOldestDate(final DateTime date) {
    checkNotNull(date);
    switch (this) {
      case DAY:
        return date.minusDays(getNumberToKeep());
      case MONTH:
      case MONTH_WHOLE_REPO:
      case MONTH_WHOLE_REPO_VULNERABLE:
        return date.minusMonths(getNumberToKeep());
      default:
        throw new IllegalArgumentException(UNIMPLEMENTED_DATE_TYPE_ENCOUNTERED + this.name());
    }
  }

  public DateTime decrement(final DateTime date) {
    checkNotNull(date);
    switch (this) {
      case DAY:
        return date.minusDays(1);
      case MONTH:
      case MONTH_WHOLE_REPO:
      case MONTH_WHOLE_REPO_VULNERABLE:
        return date.minusMonths(1);
      default:
        throw new IllegalArgumentException(UNIMPLEMENTED_DATE_TYPE_ENCOUNTERED + this.name());
    }
  }
}
