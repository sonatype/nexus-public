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
package org.sonatype.nexus.ui;

import java.io.IOException;
import java.net.URL;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.20
 */
@Component
public class UiUtil
{
  private static final String RESOURCE_PREFIX = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "/static/**/";

  private final ApplicationContext context;

  @Autowired
  public UiUtil(final ApplicationContext context) {
    this.context = checkNotNull(context);
  }

  /**
   * @param filename
   * @return the path to the requested file
   */
  public String getPathForFile(final String filename) {
    try {
      for (Resource resource : context.getResources(RESOURCE_PREFIX + filename)) {
        URL url = resource.getURL();
        String fullPath = url.getPath();
        if (fullPath.contains("/static")) {
          return fullPath.substring(fullPath.indexOf("/static"));
        }
        return url.getPath();
      }
      return null;
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
