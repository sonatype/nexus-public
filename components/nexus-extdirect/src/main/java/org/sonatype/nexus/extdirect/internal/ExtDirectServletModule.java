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
package org.sonatype.nexus.extdirect.internal;

import java.util.Map;

import com.google.common.collect.Maps;
import com.google.inject.servlet.ServletModule;
import com.softwarementors.extjs.djn.servlet.DirectJNgineServlet.GlobalParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet module for Ext.Direct Guice module.
 *
 * @since 3.38
 */
public abstract class ExtDirectServletModule
    extends ServletModule
{
  private static final Logger log = LoggerFactory.getLogger(ExtDirectServletModule.class);

  private final String mountPoint;

  protected ExtDirectServletModule(final String mountPoint) {
    this.mountPoint = mountPoint;
  }

  @Override
  protected void configureServlets() {
    Map<String, String> config = Maps.newHashMap();
    config.put(GlobalParameters.PROVIDERS_URL, mountPoint.substring(1));
    config.put("minify", Boolean.FALSE.toString());
    config.put(GlobalParameters.DEBUG, Boolean.toString(log.isDebugEnabled()));
    config.put(GlobalParameters.JSON_REQUEST_PROCESSOR_THREAD_CLASS,
        ExtDirectJsonRequestProcessorThread.class.getName());
    config.put(GlobalParameters.GSON_BUILDER_CONFIGURATOR_CLASS,
        ExtDirectGsonBuilderConfigurator.class.getName());

    serve(mountPoint + "*").with(ExtDirectServlet.class, config);
    bindSecurityFilter();
  }

  protected abstract void bindSecurityFilter();
}
