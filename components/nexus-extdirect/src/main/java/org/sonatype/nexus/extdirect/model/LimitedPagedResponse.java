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
package org.sonatype.nexus.extdirect.model;

import java.util.Collection;

/**
 * Ext.Direct paged response with a limit on the total.
 *
 * @since 3.1
 */
public class LimitedPagedResponse<T>
    extends PagedResponse<T>
{
  private long unlimitedTotal;

  private boolean limited;

  private boolean timedOut;

  public LimitedPagedResponse(long limit, long total, Collection<T> data) {
    super(Math.min(limit, total), data);
    this.unlimitedTotal = total;
    this.limited = total != getTotal();
  }

  public LimitedPagedResponse(long limit, long total, Collection<T> data, boolean timedOut) {
    this(limit, total, data);
    this.timedOut = timedOut;
  }

  public long getUnlimitedTotal() {
    return unlimitedTotal;
  }

  public boolean isLimited() {
    return limited;
  }

  public boolean isTimedOut() {
    return timedOut;
  }
}
