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
package org.sonatype.nexus.rapture.internal.branding;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.rapture.StateContributor;

/**
 * Branding state contributor.
 *
 * @since 3.0
 */
@Named
@Singleton
public class Branding
    implements StateContributor
{
  private BrandingCapabilityConfiguration config;

  @Override
  public Map<String, Object> getState() {
    if (config != null) {
      return Collections.singletonMap("branding",
          new BrandingXO(config.isHeaderEnabled(),
              interpolate(config.getHeaderHtml()),
              config.isFooterEnabled(),
              interpolate(config.getFooterHtml())));
    }
    return null;
  }

  @Nullable
  String interpolate(@Nullable final String html) {
    if (html != null) {
      return html.replaceAll(Matcher.quoteReplacement("$baseUrl"), BaseUrlHolder.getRelativePath());
    }
    return null;
  }

  public void set(final BrandingCapabilityConfiguration config) {
    this.config = config;
  }

  public void reset() {
    this.config = null;
  }
}
