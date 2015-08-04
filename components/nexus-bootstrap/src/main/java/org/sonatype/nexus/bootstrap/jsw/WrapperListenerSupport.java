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

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tanukisoftware.wrapper.WrapperListener;

/**
 * Support for {@link WrapperListener} implementations.
 *
 * @since 2.1
 */
public abstract class WrapperListenerSupport
    implements WrapperListener
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public Integer start(final String[] args) {
    log.info("Starting with arguments: {}", Arrays.asList(args));

    try {
      return doStart(args);
    }
    catch (Exception e) {
      log.error("Failed to start", e);
      return 1; // exit
    }
  }

  protected abstract Integer doStart(final String[] args) throws Exception;

  @Override
  public int stop(final int code) {
    log.info("Stopping with code: {}", code);

    try {
      return doStop(code);
    }
    catch (Exception e) {
      log.error("Failed to stop cleanly", e);
      return 1; // exit
    }
  }

  protected abstract int doStop(final int code) throws Exception;

  @Override
  public void controlEvent(final int code) {
    log.info("Received control event: {}", code);

    try {
      doControlEvent(code);
    }
    catch (Exception e) {
      log.error("Failed to handle control event[{}]", code, e);
    }
  }

  protected abstract void doControlEvent(final int code) throws Exception;
}
