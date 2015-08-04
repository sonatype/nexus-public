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
 * Template manager aggregates various TemplateProviders, and adds means to select between them.
 *
 * @author cstamas
 */
public interface TemplateManager
{
  /**
   * Get templates.
   */
  TemplateSet getTemplates();

  /**
   * Get one specific template that fits of supplied class with given id.
   */
  Template getTemplate(Object clazz, String id)
      throws NoSuchTemplateIdException;
}
