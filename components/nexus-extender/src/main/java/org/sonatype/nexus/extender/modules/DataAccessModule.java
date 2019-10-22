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
package org.sonatype.nexus.extender.modules;

import java.net.URL;
import java.util.Enumeration;
import java.util.Optional;

import org.sonatype.nexus.datastore.api.DataAccess;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterators.forEnumeration;
import static com.google.common.collect.Streams.stream;
import static com.google.inject.name.Names.named;
import static java.util.Optional.empty;
import static java.util.Optional.of;

/**
 * Binds any DAO classes in the bundle so they can be processed later by datastore implementations.
 *
 * @since 3.19
 */
public class DataAccessModule
    extends AbstractModule
{
  private static final Logger log = LoggerFactory.getLogger(DataAccessModule.class);

  private final Bundle bundle;

  public DataAccessModule(final Bundle bundle) {
    this.bundle = checkNotNull(bundle);
  }

  @Override
  protected void configure() {
    // locate DAO class files contained in the given bundle
    Enumeration<URL> daos = bundle.findEntries("/", "*DAO.class", true);
    if (daos != null) {
      stream(forEnumeration(daos))
          .map(URL::getPath)
          .map(path -> path.substring(1, path.indexOf(".class")))
          .map(path -> path.replace('/', '.'))
          .map(this::tryLoadClass)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .filter(this::isDataAccess)
          .forEach(this::bindDataAccess);
    }
  }

  /**
   * Attempt to load the candidate type from its containing bundle.
   */
  private Optional<Class<?>> tryLoadClass(final String name) {
    try {
      return of(bundle.loadClass(name));
    }
    catch (LinkageError | Exception e) {
      log.warn("Could not load {} for DataAccess binding", name, e);
      return empty();
    }
  }

  /**
   * Is this a {@link DataAccess} type?
   */
  private boolean isDataAccess(final Class<?> clazz) {
    if (DataAccess.class.isAssignableFrom(clazz)) {
      return true;
    }
    else {
      log.debug("Skipping {} because it doesn't implement DataAccess", clazz);
      return false;
    }
  }

  /**
   * Bind the {@link DataAccess} type using a unique key so we can retrieve it later.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void bindDataAccess(final Class clazz) {
    // same approach as bindConstant, but with a more specific key type
    bind(new DAOKey(clazz)).toInstance(clazz);
  }

  /**
   * Represents a unique key for the given {@link DataAccess} type.
   */
  private static class DAOKey
      extends Key<Class<DataAccess>>
  {
    DAOKey(final Class<DataAccess> clazz) {
      super(named(clazz.getName()));
    }
  }
}
