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

import java.util.Optional;

import org.sonatype.nexus.common.cooperation2.Cooperation2.Builder;
import org.sonatype.nexus.common.cooperation2.IOCall;
import org.sonatype.nexus.common.cooperation2.IOCheck;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstract implementation of {@link Builder}
 */
public abstract class Cooperation2Builder<RET>
    implements Builder<RET>
{
  protected boolean performWorkOnFail;

  protected IOCheck<RET> checkFunction = Optional::empty;

  protected final IOCall<RET> workFunction;

  protected Cooperation2Builder(final IOCall<RET> workFunction) {
    this.workFunction = checkNotNull(workFunction, "The work function for this co-operation is missing");
  }

  @Override
  public Cooperation2Builder<RET> checkFunction(final IOCheck<RET> checkFunction) {
    this.checkFunction = checkNotNull(checkFunction, "The check function for this co-operation is missing");
    return this;
  }

  @Override
  public Cooperation2Builder<RET> performWorkOnFail(final boolean performWorkOnFail) {
    this.performWorkOnFail = performWorkOnFail;
    return this;
  }
}
