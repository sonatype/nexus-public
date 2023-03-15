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
package org.sonatype.nexus.common.cooperation2.internal;

import java.time.Duration;

import org.sonatype.nexus.common.cooperation2.Config;
import org.sonatype.nexus.common.cooperation2.Cooperation2Factory.Builder;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.41
 */
public abstract class MutableConfigSupport
    extends Config
    implements Builder
{
  protected boolean enabled = true;

  @Override
  public Builder majorTimeout(final Duration majorTimeout) {
    this.majorTimeoutSeconds = (int) checkNotNull(majorTimeout).getSeconds();
    return this;
  }

  @Override
  public Builder minorTimeout(final Duration minorTimeout) {
    this.minorTimeoutSeconds = (int) checkNotNull(minorTimeout).getSeconds();
    return this;
  }

  @Override
  public Builder threadsPerKey(final int threadsPerKey) {
    this.threadsPerKey = threadsPerKey;
    return this;
  }

  @Override
  public Builder enabled(final boolean enabled) {
    this.enabled = enabled;
    return this;
  }
}
