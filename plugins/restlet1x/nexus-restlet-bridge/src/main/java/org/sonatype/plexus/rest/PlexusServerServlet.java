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

import java.util.Enumeration;

import com.noelios.restlet.ext.servlet.ServerServlet;
import com.noelios.restlet.ext.servlet.ServletContextAdapter;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.service.TaskService;

public class PlexusServerServlet
    extends ServerServlet
{
  private static final long serialVersionUID = 2636935931764462049L;

  public PlexusContainer getPlexusContainer() {
    return (PlexusContainer) getServletContext().getAttribute(PlexusConstants.PLEXUS_KEY);
  }

  @Override
  public void destroy() {
    // Note: to me, by inspecting this class' parent class ServerServlet, and by looking at
    // servlet "lifecycle" methods init() and destroy(), it's clear case of a bug: init starts the application only
    // while destroy() stops the component only. Maybe it's not even kicking in, since component is never started
    // actually. So, just to make sure, I am stopping application here, just a copy+paste from parent's method
    // with proper changes (component changed to application):
    if ((getApplication() != null) && (getApplication().isStarted())) {
      try {
        try {
          // here, we _ensure_ it is shut down properly, since the allowed shutdown seems never invoked?
          final TaskService taskService = getApplication().getTaskService();

          if (taskService != null) {
            taskService.setShutdownAllowed(true);
            taskService.shutdownNow();
          }
        }
        finally {
          getApplication().stop();
        }
      }
      catch (Exception e) {
        log("Error during the stopping of the Restlet Application", e);
      }
    }

    super.destroy();
  }

  public Application createApplication(Context context) {
    Application application = null;

    final String applicationRole = getInitParameter("role", Application.class.getName());

    final String applicationRoleHint = getInitParameter("roleHint", null);

    // Load the application class using the given class name
    try {
      if (applicationRoleHint != null) {
        application = (Application) getPlexusContainer().lookup(applicationRole, applicationRoleHint);
      }
      else {
        application = (Application) getPlexusContainer().lookup(applicationRole);
      }
    }
    catch (ComponentLookupException e) {
      throw new IllegalStateException("The PlexusServerServlet couldn't lookup the target component (role='"
          + applicationRole + "', hint='" + applicationRoleHint + "')", e);
    }

    // mimic constructor
    application.setName(getServletConfig().getServletName());

    application.setContext(new ServletContextAdapter(this, context));

    // setting logger explicitly, to override the stupid logger put there by ServletContextAdapter
    application.getContext().setLogger(application.getClass().getName());

    // --- FROM SUPERCLASS (as is) -- BEGIN
    // Copy all the servlet parameters into the context
    final Context applicationContext = application.getContext();

    String initParam;

    // Copy all the Servlet component initialization parameters
    final javax.servlet.ServletConfig servletConfig = getServletConfig();
    for (final Enumeration<String> enum1 = servletConfig.getInitParameterNames(); enum1.hasMoreElements(); ) {
      initParam = enum1.nextElement();

      applicationContext.getParameters().add(initParam, servletConfig.getInitParameter(initParam));
    }

    // Copy all the Servlet application initialization parameters
    for (final Enumeration<String> enum1 = getServletContext().getInitParameterNames(); enum1.hasMoreElements(); ) {
      initParam = enum1.nextElement();

      applicationContext.getParameters().add(initParam, getServletContext().getInitParameter(initParam));
    }
    // --- FROM SUPERCLASS (as is) -- END

    return application;
  }

  /**
   * A "dirty" hack to override the "one ServerServlet per webapp context" limitation of Restlet 1.0.x. This happens
   * since they puts the Application into ServletContext, but using the same key.
   */
  public String getInitParameter(String name, String defaultValue) {
    String prefixedName = getServletConfig().getServletName() + "." + name;

    String result = getServletConfig().getInitParameter(prefixedName);

    if (result == null) {
      result = getServletConfig().getServletContext().getInitParameter(prefixedName);
    }

    if (result == null && defaultValue != null) {
      result = defaultValue;
    }

    return result;
  }
}
