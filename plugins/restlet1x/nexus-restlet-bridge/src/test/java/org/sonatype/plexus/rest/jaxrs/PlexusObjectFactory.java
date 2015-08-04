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
package org.sonatype.plexus.rest.jaxrs;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.plexus.rest.jsr311.JsrComponent;

import org.restlet.ext.jaxrs.InstantiateException;
import org.restlet.ext.jaxrs.ObjectFactory;

@Named
@Singleton
public class PlexusObjectFactory
    implements ObjectFactory
{
  private Map<String, JsrComponent> hinstsToresources;

  /**
   * A lookup map filled in by getResourceClasses
   */
  private Map<Class<?>, Object> classesToComponents;

  @Inject
  public PlexusObjectFactory(final Map<String, JsrComponent> hinstsToresources) {
    this.hinstsToresources = hinstsToresources;
  }

  public Set<Class<?>> getResourceClasses() {
    classesToComponents = new HashMap<Class<?>, Object>(hinstsToresources.size());

    for (Object res : hinstsToresources.values()) {
      classesToComponents.put(res.getClass(), res);
    }

    return classesToComponents.keySet();
  }

  public <T> T getInstance(Class<T> jaxRsClass)
      throws InstantiateException
  {
    if (classesToComponents.containsKey(jaxRsClass)) {
      return (T) classesToComponents.get(jaxRsClass);
    }

    throw new InstantiateException("JsrComponent of class '" + jaxRsClass.getName() + "' not found!");
  }

}
