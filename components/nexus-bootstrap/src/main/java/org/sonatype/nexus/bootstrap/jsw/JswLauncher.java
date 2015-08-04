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
package org.sonatype.nexus.bootstrap.jsw;

import org.sonatype.nexus.bootstrap.Launcher;
import org.sonatype.nexus.bootstrap.ShutdownHelper;

import org.slf4j.MDC;
import org.tanukisoftware.wrapper.WrapperManager;
import org.tanukisoftware.wrapper.WrapperUser;

import static org.sonatype.nexus.bootstrap.Launcher.SYSTEM_USERID;
import static org.tanukisoftware.wrapper.WrapperManager.WRAPPER_CTRL_LOGOFF_EVENT;

/**
 * JSW adapter for {@link Launcher}.
 *
 * @since 2.1
 */
public class JswLauncher
    extends WrapperListenerSupport
{
  private Launcher launcher;

  @Override
  protected Integer doStart(final String[] args) throws Exception {
    if (WrapperManager.isControlledByNativeWrapper()) {

      // WrapperManager.getUser(boolean) may return null if native library did not initialize
      String username = "*UNKNOWN";
      WrapperUser user = WrapperManager.getUser(false);
      if (user != null) {
        username = user.getUser();
      }
      else {
        log.warn("Failed to query native user details, local environment may have problems using JSW functionality");
      }

      log.info("JVM ID: {}, JVM PID: {}, Wrapper PID: {}, User: {}",
          WrapperManager.getJVMId(),
          WrapperManager.getJavaPID(),
          WrapperManager.getWrapperPID(),
          username);
    }

    this.launcher = new Launcher(null, null, args)
    {
      @Override
      public void commandStop() {
        WrapperManager.stopAndReturn(0);
      }
    };

    launcher.start();
    return null;
  }

  @Override
  protected int doStop(final int code) throws Exception {
    launcher.stop();
    return code;
  }

  @Override
  protected void doControlEvent(final int code) {
    if (WRAPPER_CTRL_LOGOFF_EVENT == code && WrapperManager.isLaunchedAsService()) {
      log.debug("Launched as a service; ignoring event: {}", code);
    }
    else {
      log.debug("Stopping");
      WrapperManager.stop(0);
      throw new Error("unreachable");
    }
  }

  public static void main(final String[] args) throws Exception {
    MDC.put("userId", SYSTEM_USERID);
    ShutdownHelper.setDelegate(new JswShutdownDelegate());
    WrapperManager.start(new JswLauncher(), args);
  }
}
