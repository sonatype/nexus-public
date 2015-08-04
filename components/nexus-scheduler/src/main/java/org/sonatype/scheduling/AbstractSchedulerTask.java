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
package org.sonatype.scheduling;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.sisu.goodies.common.Loggers;

import org.slf4j.Logger;

public abstract class AbstractSchedulerTask<T>
    implements SchedulerTask<T>
{
  protected Logger logger = Loggers.getLogger(getClass());

  private Map<String, String> parameters;

  public void addParameter(String key, String value) {
    getParameters().put(key, value);
  }

  public String getParameter(String key) {
    return getParameters().get(key);
  }

  public synchronized Map<String, String> getParameters() {
    if (parameters == null) {
      parameters = new HashMap<String, String>();
    }

    return parameters;
  }

  public abstract T call()
      throws Exception;

  // ==

  protected Logger getLogger() {
    return logger;
  }

  protected void checkInterruption()
      throws TaskInterruptedException
  {
    TaskUtil.checkInterruption();
  }
}
