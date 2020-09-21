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
package org.sonatype.repository.helm.internal.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.repository.helm.HelmAttributes;
import org.sonatype.repository.helm.internal.database.HelmProperties;

/**
 * @since 3.next
 */
@Named
@Singleton
public class ProvenanceParser
{
  public HelmAttributes parse(final InputStream inputStream) throws IOException {
    HelmAttributes attributes = new HelmAttributes();
    try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        if (line.startsWith(HelmProperties.NAME.getPropertyName())) {
          attributes.setName(getValue(line));
        }
        if (line.startsWith(HelmProperties.DESCRIPTION.getPropertyName())) {
          attributes.setDescription(getValue(line));
        }
        if (line.startsWith(HelmProperties.VERSION.getPropertyName())) {
          attributes.setVersion(getValue(line));
        }
        if (line.startsWith(HelmProperties.ICON.getPropertyName())) {
          attributes.setIcon(getValue(line));
        }
        if (line.startsWith(HelmProperties.APP_VERSION.getPropertyName())) {
          attributes.setAppVersion(getValue(line));
        }
      }
    }
    return attributes;
  }

  private String getValue(String string) {
    return string.substring(string.indexOf(":") + 1).trim();
  }
}
