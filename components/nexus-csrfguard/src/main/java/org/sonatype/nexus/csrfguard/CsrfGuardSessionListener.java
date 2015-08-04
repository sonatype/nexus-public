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
package org.sonatype.nexus.csrfguard;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.SessionListenerAdapter;
import org.owasp.csrfguard.CsrfGuard;
import org.owasp.csrfguard.CsrfGuardHttpSessionListener;
import org.owasp.csrfguard.util.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CSRF Guard session listener (similar to {@link CsrfGuardHttpSessionListener}). generates CSRF token upon session
 * creation.
 *
 * @since 2.9
 */
// HACK: Disable CSRFGuard support for now, its too problematic
//@Named
//@Singleton
public class CsrfGuardSessionListener
    extends SessionListenerAdapter
{
  private static final Logger log = LoggerFactory.getLogger(CsrfGuard.class);

  @Override
  public void onStart(final Session session) {
    try {
      CsrfGuard csrfGuard = CsrfGuard.getInstance();
      String token = RandomGenerator.generateRandomId(csrfGuard.getPrng(), csrfGuard.getTokenLength());
      session.setAttribute(csrfGuard.getSessionKey(), token);
      log.debug("Assigned token {} to session {}", token, session.getId());
    }
    catch (Exception e) {
      throw new RuntimeException("unable to generate the random token - " + e.getLocalizedMessage(), e);
    }
  }
}

