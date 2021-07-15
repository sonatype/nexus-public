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
package org.sonatype.nexus.testsuite.testsupport.fixtures;

import org.sonatype.goodies.common.Loggers;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class ExternalResourceSupport
    extends ExternalResource
{
  protected final Logger log = checkNotNull(this.createLogger());

  protected ExternalResourceSupport() {
  }

  protected Logger createLogger() {
    return Loggers.getLogger(this);
  }
}
