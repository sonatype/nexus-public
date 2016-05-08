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

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.Time;

import org.apache.shiro.codec.Base64;
import org.apache.shiro.crypto.AesCipherService;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
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

  @Inject
  public NexusCookieRememberMeManager(
      @Named("${nexus.rememberMe.cipherKey}") @Nullable final String cipherKey,
      @Named("${nexus.rememberMe.cookieName:-NXREMEMBERME}") final String cookieName,
      @Named("${nexus.rememberMe.maxAge:-30days}") final String maxAge)
  {
    if (cipherKey != null) {
      // use supplied 'rememberMe' cipher key
      setCipherKey(Base64.decode(cipherKey));
    }
    else {
      // generate a new 'rememberMe' cipher key whenever Nexus starts
      setCipherKey(new AesCipherService().generateNewKey().getEncoded());
    }

    int maxAgeInSeconds;
    try {
      maxAgeInSeconds = Time.parse(maxAge).toSecondsI();
    }
    catch (RuntimeException e) {
      maxAgeInSeconds = Time.days(Integer.parseInt(maxAge)).toSecondsI(); // no time unit, assume days
    }

    Cookie cookie = getCookie();
    cookie.setName(cookieName);
    cookie.setMaxAge(maxAgeInSeconds);

    if (cookie.getMaxAge() > 0) {
      log.info("RememberMe enabled: cookieName={}, maxAge={}s", cookie.getName(), cookie.getMaxAge());
    }
    else {
      log.info("RememberMe disabled");
    }
  }

  @Override
  protected void rememberSerializedIdentity(Subject subject, byte[] serialized) {
    if (getCookie().getMaxAge() > 0) {
      super.rememberSerializedIdentity(subject, serialized);
    }
    // disable rememberMe when maxAge is 0 or negative
  }

  @Override
  protected byte[] getRememberedSerializedIdentity(SubjectContext subjectContext) {
    if (getCookie().getMaxAge() > 0) {
      return super.getRememberedSerializedIdentity(subjectContext);
    }
    return null; // disable rememberMe when maxAge is 0 or negative
  }
}
