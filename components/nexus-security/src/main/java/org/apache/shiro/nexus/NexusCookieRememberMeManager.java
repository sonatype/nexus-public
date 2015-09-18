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
package org.apache.shiro.nexus;

import org.sonatype.goodies.common.Time;

import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.apache.shiro.web.servlet.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom {@link CookieRememberMeManager}.
 *
 * @since 3.0
 */
public class NexusCookieRememberMeManager
  extends CookieRememberMeManager
{
  private static final Logger log = LoggerFactory.getLogger(NexusCookieRememberMeManager.class);

  private static final String DEFAULT_REMEMBER_ME_COOKIE_NAME = "NXREMEMBERME";

  private static final Time DEFAULT_REMEMBER_ME_COOKIE_MAX_AGE = Time.days(30);

  // TODO: Expose for configuration, for now just use a more sane defaults

  public NexusCookieRememberMeManager() {
    Cookie cookie = getCookie();
    cookie.setName(DEFAULT_REMEMBER_ME_COOKIE_NAME);
    cookie.setMaxAge(DEFAULT_REMEMBER_ME_COOKIE_MAX_AGE.toSecondsI());
    log.info("Cookie prototype: name={}, max-age={}", cookie.getName(), cookie.getMaxAge());
  }
}
