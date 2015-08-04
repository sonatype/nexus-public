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
package org.sonatype.nexus.templates.repository;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.templates.TemplateSet;
import org.sonatype.nexus.templates.repository.maven.Maven1GroupRepositoryTemplate;
import org.sonatype.nexus.templates.repository.maven.Maven1HostedRepositoryTemplate;
import org.sonatype.nexus.templates.repository.maven.Maven1Maven2ShadowRepositoryTemplate;
import org.sonatype.nexus.templates.repository.maven.Maven1ProxyRepositoryTemplate;
import org.sonatype.nexus.templates.repository.maven.Maven2GroupRepositoryTemplate;
import org.sonatype.nexus.templates.repository.maven.Maven2HostedRepositoryTemplate;
import org.sonatype.nexus.templates.repository.maven.Maven2Maven1ShadowRepositoryTemplate;
import org.sonatype.nexus.templates.repository.maven.Maven2ProxyRepositoryTemplate;

/**
 * A template provider implementation that covers core-supported repositories.
 *
 * @author cstamas
 */
@Named(DefaultRepositoryTemplateProvider.PROVIDER_ID)
@Singleton
public class DefaultRepositoryTemplateProvider
    extends AbstractRepositoryTemplateProvider
{

  public static final String PROVIDER_ID = "default-repository";

  private static final String DEFAULT_HOSTED_RELEASE = "default_hosted_release";

  private static final String DEFAULT_HOSTED_SNAPSHOT = "default_hosted_snapshot";

  private static final String DEFAULT_PROXY_RELEASE = "default_proxy_release";

  private static final String DEFAULT_PROXY_SNAPSHOT = "default_proxy_snapshot";

  private static final String DEFAULT_VIRTUAL_M2_M1 = "default_virtual_m2_m1";

  private static final String DEFAULT_VIRTUAL_M1_M2 = "default_virtual_m1_m2";

  private static final String DEFAULT_GROUP = "default_group";

  public TemplateSet getTemplates() {
    TemplateSet templates = new TemplateSet(null);

    try {
      templates.add(new Maven2HostedRepositoryTemplate(this, DEFAULT_HOSTED_RELEASE,
          "Maven2 (hosted, release)", RepositoryPolicy.RELEASE));

      templates.add(new Maven2HostedRepositoryTemplate(this, DEFAULT_HOSTED_SNAPSHOT,
          "Maven2 (hosted, snapshot)",
          RepositoryPolicy.SNAPSHOT));

      templates.add(new Maven2ProxyRepositoryTemplate(this, DEFAULT_PROXY_RELEASE,
          "Maven2 (proxy, release)", RepositoryPolicy.RELEASE));

      templates.add(new Maven2ProxyRepositoryTemplate(this, DEFAULT_PROXY_SNAPSHOT,
          "Maven2 (proxy, snapshot)", RepositoryPolicy.SNAPSHOT));

      templates.add(new Maven1Maven2ShadowRepositoryTemplate(this, DEFAULT_VIRTUAL_M1_M2,
          "Maven1 to Maven2 (virtual)"));

      templates.add(new Maven2Maven1ShadowRepositoryTemplate(this, DEFAULT_VIRTUAL_M2_M1,
          "Maven2 to Maven1 (virtual)"));

      templates.add(new Maven1HostedRepositoryTemplate(this, "maven1_hosted_release",
          "Maven1 (hosted, release)", RepositoryPolicy.RELEASE));

      templates.add(new Maven1HostedRepositoryTemplate(this, "maven1_hosted_snapshot",
          "Maven1 (hosted, snapshot)",
          RepositoryPolicy.SNAPSHOT));

      templates.add(new Maven1ProxyRepositoryTemplate(this, "maven1_proxy_release",
          "Maven1 (proxy, release)", RepositoryPolicy.RELEASE));

      templates.add(new Maven1ProxyRepositoryTemplate(this, "maven1_proxy_snapshot",
          "Maven1 (proxy, snapshot)", RepositoryPolicy.SNAPSHOT));

      templates.add(new Maven1GroupRepositoryTemplate(this, "maven1_group", "Maven1 (group)"));

      templates.add(new Maven2GroupRepositoryTemplate(this, DEFAULT_GROUP, "Maven2 (group)"));
    }
    catch (Exception e) {
      // will not happen
    }

    return templates;
  }
}
