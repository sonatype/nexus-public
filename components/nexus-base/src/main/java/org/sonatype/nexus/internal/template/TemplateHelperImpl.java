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
package org.sonatype.nexus.internal.template;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.common.template.TemplateParameters;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link TemplateHelper}.
 *
 * @since 3.0
 */
@Named
@Singleton
public class TemplateHelperImpl
    extends ComponentSupport
    implements TemplateHelper
{
  private final ApplicationVersion applicationVersion;

  private final VelocityEngine velocityEngine;

  @Inject
  public TemplateHelperImpl(final ApplicationVersion applicationVersion,
                            final VelocityEngine velocityEngine)
  {
    this.applicationVersion = checkNotNull(applicationVersion);
    this.velocityEngine = checkNotNull(velocityEngine);
  }

  @Override
  public TemplateParameters parameters() {
    TemplateParameters params = new TemplateParameters();
    params.set("nexusVersion", applicationVersion.getVersion());
    params.set("nexusEdition", applicationVersion.getEdition());
    params.set("nexusBrandedEditionAndVersion", applicationVersion.getBrandedEditionAndVersion());
    params.set("nexusUrl", BaseUrlHolder.get());
    params.set("urlSuffix", applicationVersion.getVersion()); // for cache busting
    return params;
  }

  @Override
  public String render(final URL template, final TemplateParameters parameters) {
    checkNotNull(template);
    checkNotNull(parameters);

    log.trace("Rendering template: {} w/params: {}", template, parameters);

    try (Reader input = new InputStreamReader(template.openStream(), Charsets.UTF_8)) {
      StringWriter buff = new StringWriter();
      velocityEngine.evaluate(new VelocityContext(parameters.get()), buff, template.getFile(), input);

      String result = buff.toString();
      log.trace("Result: {}", result);

      return result;
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
