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
package org.sonatype.nexus.common.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Allows a custom class loader to be used with ObjectInputStream.
 *
 * @since 3.6
 */
public class ObjectInputStreamWithClassLoader
    extends ObjectInputStream
{
  @FunctionalInterface
  public interface LoadingFunction
  {
    Class<?> loadClass(String name) throws ClassNotFoundException;
  }

  private final LoadingFunction classLoading;

  public ObjectInputStreamWithClassLoader(
      final InputStream inputStream,
      final LoadingFunction classLoading) throws IOException
  {
    super(inputStream);
    this.classLoading = checkNotNull(classLoading);
  }

  public ObjectInputStreamWithClassLoader(final InputStream inputStream, final ClassLoader loader) throws IOException {
    super(inputStream);
    checkNotNull(loader);
    this.classLoading = name -> Class.forName(name, false, loader);
  }

  @Override
  protected Class<?> resolveClass(final ObjectStreamClass classDesc) throws ClassNotFoundException {
    return classLoading.loadClass(classDesc.getName());
  }
}
