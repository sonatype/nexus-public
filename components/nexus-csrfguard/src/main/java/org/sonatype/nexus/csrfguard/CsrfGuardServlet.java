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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.common.base.Throwables;
import org.owasp.csrfguard.CsrfGuard;
import org.owasp.csrfguard.servlet.JavaScriptServlet;
import org.owasp.csrfguard.util.Streams;
import org.owasp.csrfguard.util.Strings;
import org.owasp.csrfguard.util.Writers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkState;

/**
 * CSRF Guard Servlet (copy of {@link JavaScriptServlet}).
 *
 * Changed in order to auto initialize {@link CsrfGuard} and do not create a session by the time
 * {@code csrfguard.js} is generated.
 *
 * @since 2.9
 */
// HACK: Disable CSRFGuard support for now, its too problematic
//@Named
//@Singleton
public final class CsrfGuardServlet
    extends HttpServlet
{

  private static final Logger log = LoggerFactory.getLogger(CsrfGuard.class);

  private static final long serialVersionUID = -1459584282530150483L;

  private static final String TOKEN_NAME_IDENTIFIER = "%TOKEN_NAME%";

  private static final String TOKEN_VALUE_IDENTIFIER = "%TOKEN_VALUE%";

  private static final String DOMAIN_ORIGIN_IDENTIFIER = "%DOMAIN_ORIGIN%";

  private static final String DOMAIN_STRICT_IDENTIFIER = "%DOMAIN_STRICT%";

  private static final String INJECT_INTO_XHR_IDENTIFIER = "%INJECT_XHR%";

  private static final String INJECT_INTO_FORMS_IDENTIFIER = "%INJECT_FORMS%";

  private static final String INJECT_INTO_ATTRIBUTES_IDENTIFIER = "%INJECT_ATTRIBUTES%";

  private static final String CONTEXT_PATH_IDENTIFIER = "%CONTEXT_PATH%";

  private static final String SERVLET_PATH_IDENTIFIER = "%SERVLET_PATH%";

  private static final String X_REQUESTED_WITH_IDENTIFIER = "%X_REQUESTED_WITH%";

  private static final String TOKENS_PER_PAGE_IDENTIFIER = "%TOKENS_PER_PAGE%";

  private String templateCode = null;

  private String injectIntoForms = null;

  private String injectIntoAttributes = null;

  private String domainStrict = null;

  private String cacheControl = null;

  private Pattern refererPattern = null;

  private String xRequestedWith = null;

  @Override
  public void init(ServletConfig servletConfig) {
    URL csrfPropertiesFile = getClass().getResource("csrfguard.properties");
    checkState(csrfPropertiesFile != null, "Missing csrfguard.properties");
    try {
      try (InputStream is = csrfPropertiesFile.openStream()) {
        Properties csrfProperties = new Properties();
        csrfProperties.load(is);
        String contextPath = servletConfig.getServletContext().getContextPath();
        for (String key : csrfProperties.stringPropertyNames()) {
          if (key.startsWith("org.owasp.csrfguard.unprotected.")) {
            String value = csrfProperties.getProperty(key);
            if (value != null && value.contains("%CONTEXT_PATH%")) {
              csrfProperties.setProperty(key, value.replace("%CONTEXT_PATH%", contextPath));
            }
          }
        }
        CsrfGuard.load(csrfProperties);
        log.debug(CsrfGuard.getInstance().toString());
      }
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }

    domainStrict = getInitParameter(servletConfig, "domain-strict", "true");
    cacheControl = getInitParameter(servletConfig, "cache-control", "private, maxage=28800");
    refererPattern = Pattern.compile(getInitParameter(servletConfig, "referer-pattern", ".*"));
    injectIntoForms = getInitParameter(servletConfig, "inject-into-forms", "true");
    injectIntoAttributes = getInitParameter(servletConfig, "inject-into-attributes", "false");
    xRequestedWith = getInitParameter(servletConfig, "x-requested-with", "OWASP CSRFGuard");
    templateCode = readFileContent();
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String refererHeader = request.getHeader("referer");

    if (refererHeader == null || refererPattern.matcher(refererHeader).matches()) {
      writeJavaScript(request, response);
    }
    else {
      response.sendError(404);
    }
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    CsrfGuard csrfGuard = CsrfGuard.getInstance();

    if (csrfGuard != null && csrfGuard.isTokenPerPageEnabled()) {
      writePageTokens(request, response);
    }
    else {
      response.sendError(404);
    }
  }

  private void writePageTokens(HttpServletRequest request, HttpServletResponse response) throws IOException {
    HttpSession session = request.getSession(true);
    @SuppressWarnings("unchecked")
    Map<String, String> pageTokens = (Map<String, String>) session.getAttribute(CsrfGuard.PAGE_TOKENS_KEY);
    String pageTokensString = (pageTokens != null ? parsePageTokens(pageTokens) : Strings.EMPTY);

    /** setup headers **/
    response.setContentType("text/plain");
    response.setContentLength(pageTokensString.length());

    /** write dynamic javascript **/
    OutputStream output = null;
    PrintWriter writer = null;

    try {
      output = response.getOutputStream();
      writer = new PrintWriter(output);

      writer.write(pageTokensString);
      writer.flush();
    }
    finally {
      Writers.close(writer);
      Streams.close(output);
    }
  }

  private void writeJavaScript(HttpServletRequest request, HttpServletResponse response) throws IOException {
    HttpSession session = request.getSession(false);
    CsrfGuard csrfGuard = CsrfGuard.getInstance();

    /** cannot cache if rotate or token-per-page is enabled **/
    if (csrfGuard.isRotateEnabled() || csrfGuard.isTokenPerPageEnabled()) {
      response.setHeader("Cache-Control", "no-cache, no-store");
      response.setHeader("Pragma", "no-cache");
      response.setHeader("Expires", "0");
    }
    else {
      response.setHeader("Cache-Control", cacheControl);
    }

    response.setContentType("text/javascript");

    /** build dynamic javascript **/
    String code = templateCode;

    code = code.replaceAll(TOKEN_NAME_IDENTIFIER, csrfGuard.getTokenName());
    code = code.replaceAll(TOKEN_VALUE_IDENTIFIER,
        session == null ? "UNKNOWN" : (String) session.getAttribute(csrfGuard.getSessionKey()));
    code = code.replaceAll(INJECT_INTO_FORMS_IDENTIFIER, injectIntoForms);
    code = code.replaceAll(INJECT_INTO_ATTRIBUTES_IDENTIFIER, injectIntoAttributes);
    code = code.replaceAll(INJECT_INTO_XHR_IDENTIFIER, String.valueOf(csrfGuard.isAjaxEnabled()));
    code = code.replaceAll(TOKENS_PER_PAGE_IDENTIFIER, String.valueOf(csrfGuard.isTokenPerPageEnabled()));
    code = code.replaceAll(DOMAIN_ORIGIN_IDENTIFIER, parseDomain(request.getRequestURL()));
    code = code.replaceAll(DOMAIN_STRICT_IDENTIFIER, domainStrict);
    code = code.replaceAll(CONTEXT_PATH_IDENTIFIER, request.getContextPath());
    code = code.replaceAll(SERVLET_PATH_IDENTIFIER, request.getContextPath() + request.getServletPath());
    code = code.replaceAll(X_REQUESTED_WITH_IDENTIFIER, xRequestedWith);

    /** write dynamic javascript **/
    OutputStream output = null;
    PrintWriter writer = null;

    try {
      output = response.getOutputStream();
      writer = new PrintWriter(output);

      writer.write(code);
      writer.flush();
    }
    finally {
      Writers.close(writer);
      Streams.close(output);
    }
  }

  private String parsePageTokens(Map<String, String> pageTokens) {
    StringBuilder sb = new StringBuilder();
    Iterator<String> keys = pageTokens.keySet().iterator();

    while (keys.hasNext()) {
      String key = keys.next();
      String value = pageTokens.get(key);

      sb.append(key);
      sb.append(':');
      sb.append(value);

      if (keys.hasNext()) {
        sb.append(',');
      }
    }

    return sb.toString();
  }

  private String getInitParameter(ServletConfig servletConfig, String name, String defaultValue) {
    String value = servletConfig.getInitParameter(name);

    if (value == null) {
      value = defaultValue;
    }

    return value;
  }

  private String readFileContent() {
    URL csrfJsFile = getClass().getResource("csrfguard.js");
    checkState(csrfJsFile != null, "Missing csrfguard.js");

    StringBuilder sb = new StringBuilder();

    try (InputStream is = csrfJsFile.openStream()) {
      int i = 0;

      while ((i = is.read()) > 0) {
        sb.append((char) i);
      }
    }
    catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }

    return sb.toString();
  }

  private String parseDomain(StringBuffer url) {
    String token = "://";
    int index = url.indexOf(token);
    String part = url.substring(index + token.length());
    StringBuilder domain = new StringBuilder();

    for (int i = 0; i < part.length(); i++) {
      char character = part.charAt(i);

      if (character == '/' || character == ':') {
        break;
      }

      domain.append(character);
    }

    return domain.toString();
  }

}
