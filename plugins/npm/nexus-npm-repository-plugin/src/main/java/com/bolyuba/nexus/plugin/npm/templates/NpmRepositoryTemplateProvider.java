/*
 * Copyright (c) 2007-2014 Sonatype, Inc. and Georgy Bolyuba. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.bolyuba.nexus.plugin.npm.templates;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.templates.TemplateSet;
import org.sonatype.nexus.templates.repository.AbstractRepositoryTemplateProvider;

/**
 * @author Georgy Bolyuba (georgy@bolyuba.com)
 */
@Named(NpmRepositoryTemplateProvider.PROVIDER_ID)
@Singleton
public class NpmRepositoryTemplateProvider
    extends AbstractRepositoryTemplateProvider
{

  public static final String PROVIDER_ID = "npm-repository";

  private static final String NPM_PROXY = "npm_proxy";

  private static final String NPM_HOSTED = "npm_hosted";

  private static final String NPM_GROUP = "npm_group";

  public static final String NPM_PROVIDER = "npm";

  @Override
  public TemplateSet getTemplates() {
    final TemplateSet templates = new TemplateSet(null);

    templates.add(new NpmProxyRepositoryTemplate(this, NPM_PROXY, NPM_PROVIDER));
    templates.add(new NpmHostedRepositoryTemplate(this, NPM_HOSTED, NPM_PROVIDER));
    templates.add(new NpmGroupRepositoryTemplate(this, NPM_GROUP, NPM_PROVIDER));

    return templates;
  }
}
