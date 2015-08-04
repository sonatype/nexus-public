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
package org.sonatype.plexus.rest;


import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;

/**
 * @author Juven Xu
 */
public class PlexusContainerContextListener
    implements ServletContextListener
{
  private static final String KEY_PLEXUS = "plexus";

  PlexusContainerConfigurationUtils plexusContainerConfigurationUtils = new PlexusContainerConfigurationUtils();

  PlexusContainerUtils plexusContainerUtils = new PlexusContainerUtils();

  public void contextInitialized(ServletContextEvent sce) {
    ServletContext context = sce.getServletContext();

    ContainerConfiguration plexusContainerConfiguration = this.buildContainerConfiguration(context);

    try {
      initizlizePlexusContainer(context, plexusContainerConfiguration);
    }
    catch (PlexusContainerException e) {
      throw new IllegalStateException("Could start plexus container", e);
    }
  }

  public void contextDestroyed(ServletContextEvent sce) {
    plexusContainerUtils.stopContainer();
  }

  protected void initizlizePlexusContainer(ServletContext context, ContainerConfiguration configuration)
      throws PlexusContainerException
  {
    PlexusContainer plexusContainer = plexusContainerUtils.startContainer(configuration);

    context.setAttribute(KEY_PLEXUS, plexusContainer);
  }

  protected ContainerConfiguration buildContainerConfiguration(ServletContext context) {
    return plexusContainerConfigurationUtils
        .buildContainerConfiguration(context);
  }
}
