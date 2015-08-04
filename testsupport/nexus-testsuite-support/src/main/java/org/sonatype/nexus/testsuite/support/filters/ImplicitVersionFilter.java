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
package org.sonatype.nexus.testsuite.support.filters;

import java.util.Map;

import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.nexus.testsuite.support.Filter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Adds an implicit version "${project.dm.version}" if none provided.
 *
 * @since 2.2
 */
public class ImplicitVersionFilter
    implements Filter
{

  /**
   * Filters by adding an implicit version "${project.dm.version}" if none provided.
   *
   * @param context filtering context. Cannot be null.
   * @param value   value to be filtered. Cannot be null.
   * @return filtered value
   */
  public String filter(final Map<String, String> context, final String value) {
    String filtered = checkNotNull(value);
    try {
      final DefaultArtifact artifact = new DefaultArtifact(value + ":${project.dm.version}");
      if ("${project.dm.version}".equals(artifact.getVersion())) {
        filtered = value + ":${project.dm.version}";
      }
    }
    catch (IllegalArgumentException ignore) {
      // do nothing
    }
    return filtered;
  }

}
