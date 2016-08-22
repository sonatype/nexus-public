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
package org.sonatype.nexus.testcommon.validation;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.sonatype.goodies.httpfixture.validation.HttpValidator;
import org.sonatype.nexus.common.text.Strings2;

import com.google.common.net.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertTrue;

/**
 * HttpValidator for http headers.
 *
 * @since 3.1
 */
public class HeaderValidator
    implements HttpValidator
{
  private static final Logger log = LoggerFactory.getLogger(HeaderValidator.class);

  private static final String EXPECTED_USER_AGENT_BASE_REGEX = String.format(
      "^Nexus/[0-9\\.-]+(-SNAPSHOT)? \\((OSS|PRO){1}; %s; %s; %s; %s\\).*", System.getProperty("os.name"),
      System.getProperty("os.version"), System.getProperty("os.arch"), System.getProperty("java.version"));

  /**
   * Map of header names to expected header regex.
   */
  private final Map<String, Pattern> expectedHeaders = new HashMap<>();

  public HeaderValidator() {
    expectedHeaders.put(HttpHeaders.USER_AGENT, Pattern.compile(EXPECTED_USER_AGENT_BASE_REGEX + ".*"));
  }

  public HeaderValidator(String globalUserAgentSuffix) {
    checkArgument(!Strings2.isBlank(globalUserAgentSuffix), "User agent suffix must be non-blank.");
    expectedHeaders.put(HttpHeaders.USER_AGENT,
        Pattern.compile(EXPECTED_USER_AGENT_BASE_REGEX + " " + Pattern.quote(globalUserAgentSuffix) + ".*"));
  }

  @Override
  public void validate(HttpServletRequest httpRequest) {
    checkNotNull(httpRequest);

    log.info("Performing validation for incoming '{}' request.", httpRequest.getMethod());
    expectedHeaders.forEach((k, v) -> {
      String header = httpRequest.getHeader(k);
      validateHeader(header, v);
    });
  }

  private void validateHeader(String header, Pattern pattern) {
    assertTrue(String.format("Header value '%s' must match regex '%s'.", header, pattern),
        pattern.matcher(header).matches());
    log.info("Validated header: {}", header);
  }

}
