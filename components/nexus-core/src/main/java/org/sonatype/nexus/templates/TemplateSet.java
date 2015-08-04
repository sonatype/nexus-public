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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.codehaus.plexus.util.StringUtils;

public class TemplateSet
    extends HashSet<Template>
    implements TemplateProvider
{
  private static final long serialVersionUID = 552419423510140977L;

  private final Object clazz;

  private final TemplateSet parent;

  public TemplateSet(Object clazz) {
    this(clazz, null);
  }

  public TemplateSet(Object clazz, TemplateSet templates) {
    super();

    this.clazz = clazz;

    this.parent = templates;

    if (parent != null) {
      if (clazz == null) {
        addAll(templates);
      }
      else {
        for (Template template : parent) {
          if (template.targetFits(clazz)) {
            add(template);
          }
        }
      }
    }
  }

  @Override
  public boolean add(Template elem) {
    if (getClazz() != null && elem.targetFits(getClazz())) {
      return super.add(elem);
    }
    else if (getClazz() == null) {
      return super.add(elem);
    }
    else {
      return false;
    }
  }

  /**
   * Picks the only one remained template in this set.
   */
  public Template pick()
      throws IllegalStateException
  {
    return pick(true);
  }

  /**
   * Picks one template from this set. Will enforce that set has only one member if the forceSingleHit is true.
   */
  public Template pick(boolean forceSingleHit)
      throws IllegalStateException
  {
    if (!forceSingleHit || size() == 1) {
      return iterator().next();
    }
    else {
      throw new IllegalStateException("The TemplateSet has size()==\"" + size() + "\" and not 1 as forced!");
    }
  }

  public Object getClazz() {
    return clazz;
  }

  public List<Template> getTemplatesList() {
    return new ArrayList<Template>(this);
  }

  public TemplateSet getTemplates() {
    return this;
  }

  public TemplateSet getTemplates(Object filter) {
    return new TemplateSet(filter, this);
  }

  public TemplateSet getTemplates(Object... filters) {
    TemplateSet par = this;

    for (Object filter : filters) {
      par = par.getTemplates(filter);
    }

    return par;
  }

  public Template getTemplateById(String id)
      throws NoSuchTemplateIdException
  {
    for (Template template : this) {
      if (StringUtils.equals(id, template.getId())) {
        return template;
      }
    }

    throw new NoSuchTemplateIdException("Template with Id='" + id + "' not found!");
  }
}
