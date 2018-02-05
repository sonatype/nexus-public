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
package org.sonatype.nexus.repository.maven.internal;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.common.template.TemplateParameters;

/**
 * @since 3.8
 */
public class MavenPomGenerator
{
  private final TemplateHelper templateHelper;

  @Inject
  public MavenPomGenerator(final TemplateHelper templateHelper) {
    this.templateHelper = templateHelper;
  }

  public String generatePom(final String groupId,
                            final String artifactId,
                            final String version,
                            @Nullable final String packaging)
  {
    TemplateParameters params = templateHelper.parameters();
    params.set("groupId", groupId);
    params.set("artifactId", artifactId);
    params.set("version", version);
    params.set("packaging", packaging);
    return templateHelper.render(getClass().getResource("pom.vm"), params);
  }
}
