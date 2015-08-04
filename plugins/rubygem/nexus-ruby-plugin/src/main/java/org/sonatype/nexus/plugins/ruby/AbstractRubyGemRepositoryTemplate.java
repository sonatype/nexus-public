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

import java.io.IOException;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.templates.repository.AbstractRepositoryTemplate;

/**
 * Support for rubygem repository templates.
 *
 * @since 2.11
 */
public abstract class AbstractRubyGemRepositoryTemplate
    extends AbstractRepositoryTemplate
{
  public AbstractRubyGemRepositoryTemplate(RubyRepositoryTemplateProvider provider,
                                           String id,
                                           String description,
                                           ContentClass contentClass,
                                           Class<?> mainFacet)
  {
    super(provider, id, description, contentClass, mainFacet);
  }

  @Override
  public RubyRepository create() throws ConfigurationException, IOException {
    return (RubyRepository) super.create();
  }
}
