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
package org.sonatype.nexus.templates;

/**
 * A template provider provides a set of templates for one implementation.
 *
 * @author cstamas
 */
public interface TemplateProvider
{
  /**
   * Lists all templates.
   */
  TemplateSet getTemplates();

  /**
   * Lists all templates that fits supplied filter.
   */
  TemplateSet getTemplates(Object filter);

  /**
   * Lists all templates that fits supplied filters.
   */
  TemplateSet getTemplates(Object... filters);

  /**
   * Search a template by it's ID.
   */
  Template getTemplateById(String id)
      throws NoSuchTemplateIdException;
}
