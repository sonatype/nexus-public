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
package org.sonatype.timeline;

import org.sonatype.nexus.plugins.ithelper.log.LogHelperPlexusResource;

/**
 * This class is present here only to "mimic" the actual class to be found in "Spice Timeline"
 * (https://github.com/sonatype/spice-timeline) project. Since Nexus core is independent and decoupled from this
 * project, and in one place there is only a class name check done against "org.sonatype.timeline.TimelineException"
 * string, but due to how this helper works, it will try to instantiate the exception.
 *
 * @author cstamas
 * @see See {@link LogHelperPlexusResource} and {@link Nexus4427WarnErrorLogsToFeedsIT} IT class for how is it used.
 * @since 2.0
 * @deprecated This class has nothing to do with "real" Timeline exception. It is here only to serve the need of one
 *             IT:
 *             Nexus4427WarnErrorLogsToFeedsIT
 */
public class TimelineException
    extends Exception
{
  private static final long serialVersionUID = -6910542365717001247L;

  public TimelineException() {
    super();
  }

  public TimelineException(String message) {
    super(message);
  }

  public TimelineException(String message, Throwable cause) {
    super(message, cause);
  }

  public TimelineException(Throwable cause) {
    super(cause);
  }
}
