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
package org.sonatype.nexus.plugins.ruby;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.plugins.ruby.group.DefaultRubyGroupRepository;
import org.sonatype.nexus.plugins.ruby.group.DefaultRubyGroupRepositoryTemplate;
import org.sonatype.nexus.plugins.ruby.hosted.DefaultHostedRubyRepository;
import org.sonatype.nexus.plugins.ruby.hosted.DefaultHostedRubyRepositoryTemplate;
import org.sonatype.nexus.plugins.ruby.proxy.DefaultProxyRubyRepository;
import org.sonatype.nexus.plugins.ruby.proxy.DefaultProxyRubyRepositoryTemplate;
import org.sonatype.nexus.templates.TemplateSet;
import org.sonatype.nexus.templates.repository.AbstractRepositoryTemplateProvider;

/**
 * Rubygems template provider.
 *
 * @since 2.11
 */
@Singleton
@Named(RubyRepositoryTemplateProvider.PROVIDER_ID)
public class RubyRepositoryTemplateProvider
    extends AbstractRepositoryTemplateProvider
{
  public static final String PROVIDER_ID = "ruby-repository";

  public TemplateSet getTemplates() {
    TemplateSet templates = new TemplateSet(null);

    try {
      templates.add(new DefaultHostedRubyRepositoryTemplate(this, DefaultHostedRubyRepository.ID, "Rubygems (hosted)"));
      templates.add(new DefaultProxyRubyRepositoryTemplate(this, DefaultProxyRubyRepository.ID, "Rubygems (proxy)"));
      templates.add(new DefaultRubyGroupRepositoryTemplate(this, DefaultRubyGroupRepository.ID, "Rubygems (group)"));
    }
    catch (Exception e) {
      // will not happen
    }

    return templates;
  }
}
