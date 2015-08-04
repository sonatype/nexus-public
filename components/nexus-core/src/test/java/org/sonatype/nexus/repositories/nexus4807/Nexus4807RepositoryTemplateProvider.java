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
package org.sonatype.nexus.repositories.nexus4807;

import org.sonatype.nexus.templates.TemplateSet;
import org.sonatype.nexus.templates.repository.AbstractRepositoryTemplateProvider;

/**
 * @see Nexus4807RepositoryImpl
 */
public class Nexus4807RepositoryTemplateProvider
    extends AbstractRepositoryTemplateProvider
{

  public TemplateSet getTemplates() {
    TemplateSet templates = new TemplateSet(null);

    templates.add(new Nexus4807RepositoryTemplate(this, "nexus4807", "NEXUS-4807 (test repository)"));

    return templates;
  }

}
