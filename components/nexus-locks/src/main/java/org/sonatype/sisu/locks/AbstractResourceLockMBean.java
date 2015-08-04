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
package org.sonatype.sisu.locks;

import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.StandardMBean;

/**
 * Abstract {@link ResourceLockMBean} implementation.
 */
abstract class AbstractResourceLockMBean
    extends StandardMBean
    implements ResourceLockMBean
{
  // ----------------------------------------------------------------------
  // Constructor
  // ----------------------------------------------------------------------

  AbstractResourceLockMBean() {
    super(ResourceLockMBean.class, false);
  }

  // ----------------------------------------------------------------------
  // Implementation methods
  // ----------------------------------------------------------------------

  @Override
  protected final String getParameterName(final MBeanOperationInfo op, final MBeanParameterInfo param, final int seq) {
    return op.getName().endsWith("Resources") ? "thread id #" : "resource name";
  }
}
