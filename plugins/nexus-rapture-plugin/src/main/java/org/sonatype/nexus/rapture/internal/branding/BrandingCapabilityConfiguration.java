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

import java.util.Map;

import org.sonatype.nexus.capability.UniquePerCapabilityType;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.validation.group.Create;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration adapter for {@link BrandingCapability}.
 *
 * @since 3.0
 */
@UniquePerCapabilityType(value = BrandingCapabilityDescriptor.TYPE_ID, groups = Create.class)
public class BrandingCapabilityConfiguration
    extends BrandingXO
{

  public static final String HEADER_ENABLED = "headerEnabled";

  public static final String HEADER_HTML = "headerHtml";

  public static final String FOOTER_ENABLED = "footerEnabled";

  public static final String FOOTER_HTML = "footerHtml";

  public BrandingCapabilityConfiguration(final Map<String, String> properties) {
    checkNotNull(properties);
    setHeaderEnabled(parseBoolean(properties.get(HEADER_ENABLED), false));
    setHeaderHtml(parseString(properties.get(HEADER_HTML)));
    setFooterEnabled(parseBoolean(properties.get(FOOTER_ENABLED), false));
    setFooterHtml(parseString(properties.get(FOOTER_HTML)));
  }

  private boolean parseBoolean(final String value, final boolean defaultValue) {
    if (!Strings2.isEmpty(value)) {
      return Boolean.parseBoolean(value);
    }
    return defaultValue;
  }

  private String parseString(final String value) {
    if (!Strings2.isEmpty(value)) {
      return value;
    }
    return null;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{"
        + "headerEnabled=" + getHeaderEnabled()
        + ", footerEnabled=" + getFooterEnabled()
        + "}";
  }
}
