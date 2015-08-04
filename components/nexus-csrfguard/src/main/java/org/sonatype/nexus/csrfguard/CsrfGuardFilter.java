/**
 * The OWASP CSRFGuard Project, BSD License
 * Eric Sheridan (eric@infraredsecurity.com), Copyright (c) 2011
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *    3. Neither the name of OWASP nor the names of its contributors may be used
 *       to endorse or promote products derived from this software without specific
 *       prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.sonatype.nexus.csrfguard;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.shiro.web.servlet.AdviceFilter;
import org.owasp.csrfguard.CsrfGuard;
import org.owasp.csrfguard.http.InterceptRedirectResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CSRF Guard Filter (copy of {@link org.owasp.csrfguard.CsrfGuardFilter}).
 *
 * Changed in order to send the CSRF token as response HTTP header (on session creation),
 * skip validation in case of new sessions and do not update session tokens as they are always valid in session.
 *
 * @since 2.9
 */
public class CsrfGuardFilter
    extends AdviceFilter
{

  private static final Logger log = LoggerFactory.getLogger(CsrfGuard.class);

  public static final boolean PROTECTION_ENABLED = getBoolean(CsrfGuardFilter.class.getName() + ".enabled", false);

  public static final String SKIP_VALIDATION = CsrfGuardFilter.class.getSimpleName() + ".skipValidation";

  private static final String LAST_SENT_CSRF_TOKEN = CsrfGuardFilter.class.getSimpleName() + ".lastSentCsrfToken";

  public CsrfGuardFilter(){
    setEnabled(PROTECTION_ENABLED);
    log.info("CSRF protection enabled: {}", PROTECTION_ENABLED);
  }

  @Override
  protected boolean preHandle(final ServletRequest request, final ServletResponse response) throws Exception {
    /** only work with HttpServletRequest objects **/
    if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpSession session = httpRequest.getSession(false);

      log.debug("Analyzing request {}", httpRequest.getRequestURI());

      if (session != null) {
        CsrfGuard csrfGuard = CsrfGuard.getInstance();
        String currentToken = (String) session.getAttribute(csrfGuard.getSessionKey());
        String lastSentToken = (String) session.getAttribute(LAST_SENT_CSRF_TOKEN);

        if (currentToken != null && !currentToken.equals(lastSentToken)) {
          ((HttpServletResponse) response).setHeader(csrfGuard.getTokenName(), currentToken);
          session.setAttribute(LAST_SENT_CSRF_TOKEN, currentToken);
          log.debug("Sending token {} back to client", currentToken);
        }
        else {
          Object skipValidation = request.getAttribute(SKIP_VALIDATION);
          if (!(skipValidation != null && skipValidation instanceof Boolean && (Boolean) skipValidation)) {
            log.debug("Validating request {}", httpRequest.getRequestURI());
            return csrfGuard.isValidRequest(httpRequest, (HttpServletResponse) response);
          }
        }
      }
    }
    else {
      log.warn("CsrfGuard does not know how to work with requests of class {}", request.getClass().getName());
    }
    return true;
  }

  @Override
  protected void executeChain(final ServletRequest request, final ServletResponse response, final FilterChain chain)
      throws Exception
  {
    ServletResponse wrappedResponse = response;
    if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
      HttpSession session = ((HttpServletRequest) request).getSession(false);
      CsrfGuard csrfGuard = CsrfGuard.getInstance();
      if (session != null && session.getAttribute(csrfGuard.getSessionKey()) != null) {
        wrappedResponse = new InterceptRedirectResponse(
            (HttpServletResponse) response, (HttpServletRequest) request, csrfGuard
        );
      }
    }
    super.executeChain(request, wrappedResponse, chain);
  }

  private static boolean getBoolean(final String key, final boolean defaultValue) {
    final String value = System.getProperty(key);

    if (value == null || value.trim().length() == 0) {
      return defaultValue;
    }

    return Boolean.valueOf(value);
  }
}
