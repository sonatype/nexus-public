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
package org.sonatype.nexus.plugins.p2.repository.templates;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.plugins.p2.repository.P2CompositeGroupRepository;
import org.sonatype.nexus.plugins.p2.repository.P2GroupRepository;
import org.sonatype.nexus.plugins.p2.repository.group.P2CompositeGroupRepositoryImpl;
import org.sonatype.nexus.plugins.p2.repository.group.P2GroupRepositoryImpl;
import org.sonatype.nexus.templates.TemplateSet;
import org.sonatype.nexus.templates.repository.AbstractRepositoryTemplateProvider;

@Named(P2RepositoryTemplateProvider.PROVIDER_ID)
@Singleton
public class P2RepositoryTemplateProvider
    extends AbstractRepositoryTemplateProvider
{

  public static final String PROVIDER_ID = "p2-repository";

  private static final String P2_PROXY = "p2_proxy";

  private static final String P2_UPDATE_SITE = "p2_updatesite";

  private static final String P2_GROUP = "p2_group";

  private static final String P2_COMPOSITE_GROUP = "p2_composite_group";

  @Override
  public TemplateSet getTemplates() {
    final TemplateSet templates = new TemplateSet(null);

    try {
      templates.add(new P2ProxyRepositoryTemplate(this, P2_PROXY, "P2 (proxy)"));
      templates.add(new UpdateSiteRepositoryTemplate(this, P2_UPDATE_SITE, "P2 Update Site (proxy)"));
      templates.add(new P2GroupRepositoryTemplate(
          this, P2_GROUP, "P2 Deprecated (group)",
          P2GroupRepository.class, P2GroupRepositoryImpl.ROLE_HINT
      ));
      templates.add(new P2GroupRepositoryTemplate(
          this, P2_COMPOSITE_GROUP, "P2 (group)",
          P2CompositeGroupRepository.class, P2CompositeGroupRepositoryImpl.ROLE_HINT
      ));
    }
    catch (final Exception e) {
      // will not happen
    }

    return templates;
  }

}
