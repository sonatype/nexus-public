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
package org.sonatype.nexus.utils.httpclient;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import org.springframework.stereotype.Component;

/**
 * Generates the {@code User-Agent} header value.
 *
 * @since 3.0
 */
@Component
public class UserAgentGenerator
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private static final String PAU = "; pau)";

  private static final String PAE = "; pae)";

  private static final String PAD = "; pad)";

  private final ApplicationVersion applicationVersion;

  private String value;

  private String edition;

  @Autowired
  public UserAgentGenerator(final ApplicationVersion applicationVersion) {
    this.applicationVersion = checkNotNull(applicationVersion);
  }

  public String generate() {
    // Cache platform details or re-cache if the edition has changed
    if (value == null || !applicationVersion.getEdition().equals(edition)) {
      // track edition for cache invalidation
      edition = applicationVersion.getEdition();

      value = String.format("Nexus/%s (%s; %s; %s; %s; %s)",
          applicationVersion.getVersion(),
          edition,
          System.getProperty("os.name"),
          System.getProperty("os.version"),
          System.getProperty("os.arch"),
          System.getProperty("java.version"));
    }

    return value;
  }

  public String buildUserAgentForAnalytics(CapabilityReference capabilityReference) {
    String ua = generate();
    if (capabilityReference == null) {
      return ua.replace(")", PAU);
    }
    else if (capabilityReference.context().isEnabled()) {
      return ua.replace(")", PAE);
    }
    else {
      return ua.replace(")", PAD);
    }
  }
}
