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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.owasp.csrfguard.CsrfGuard;
import org.owasp.csrfguard.CsrfGuardException;
import org.owasp.csrfguard.action.AbstractAction;

/**
 * CSRF Guard action that sends an error response.
 *
 * @since 2.9
 */
public final class CsrfGuardSendError
    extends AbstractAction
{

  @Override
  public void execute(final HttpServletRequest request,
                      final HttpServletResponse response,
                      final CsrfGuardException csrfe,
                      final CsrfGuard csrfGuard)
      throws CsrfGuardException
  {
    String errorCodeParam = getParameter("ErrorCode");
    if (errorCodeParam == null) {
      errorCodeParam = "403";
    }
    try {
      response.sendError(Integer.parseInt(errorCodeParam));
    }
    catch (IOException e) {
      throw new CsrfGuardException("Could not send " + errorCodeParam, e);
    }
  }

}
