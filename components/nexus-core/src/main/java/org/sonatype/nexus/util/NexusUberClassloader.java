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
package org.sonatype.nexus.util;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.sisu.space.ClassSpace;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * ClassLoader which exposes all {@link ClassSpace}s in the application.
 *
 * @since 2.6
 */
@Named("nexus-uber")
@Singleton
public class NexusUberClassloader
    extends ClassLoader
{
  private final List<ClassSpace> spaces;

  @Inject
  public NexusUberClassloader(List<ClassSpace> spaces) {
    this.spaces = checkNotNull(spaces);
  }

  @Override
  public Class<?> loadClass(String name)
      throws ClassNotFoundException
  {
    return loadClass(name, false);
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve)
      throws ClassNotFoundException
  {
    for (ClassSpace s : spaces) {
      try {
        return s.loadClass(name);
      }
      catch (TypeNotPresentException e) {
        // ignore it
      }
    }
    throw new ClassNotFoundException(name);
  }

  @Override
  public URL getResource(String name) {
    for (ClassSpace s : spaces) {
      URL result = s.getResource(name);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  @Override
  public Enumeration<URL> getResources(String name)
  {
    List<URL> result = new ArrayList<URL>();
    for (ClassSpace s : spaces) {
      for (Enumeration<URL> resources = s.getResources(name); resources.hasMoreElements();) {
        result.add(resources.nextElement());
      }
    }
    return Collections.enumeration(result);
  }
}
