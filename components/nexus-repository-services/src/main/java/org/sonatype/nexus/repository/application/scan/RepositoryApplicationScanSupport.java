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
package org.sonatype.nexus.repository.application.scan;

import java.util.Collections;
import java.util.Map;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;

public interface RepositoryApplicationScanSupport
{
  boolean isSupported();

  String getStageTypeId(Repository repository);

  static String getStageTypeIdFromAttributes(final Repository repository, final String... keys) {
    Configuration configuration = repository.getConfiguration();
    if (configuration == null) {
      return null;
    }
    Map<String, Map<String, Object>> attributes = configuration.getAttributes();
    if (attributes == null) {
      return null;
    }
    Object value = attributes;
    for (String key : keys) {
      if (value instanceof Map) {
        value = ((Map) value).getOrDefault(key, Collections.emptyMap());
      }
    }
    if ("RELEASE".equals(value)) {
      return "release";
    }
    return null;
  }
}
