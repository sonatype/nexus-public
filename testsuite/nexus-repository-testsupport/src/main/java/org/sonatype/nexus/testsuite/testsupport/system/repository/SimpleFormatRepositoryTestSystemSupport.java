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
package org.sonatype.nexus.testsuite.testsupport.system.repository;

import java.util.function.Function;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.testsuite.testsupport.system.repository.config.GroupRepositoryConfig;
import org.sonatype.nexus.testsuite.testsupport.system.repository.config.HostedRepositoryConfig;
import org.sonatype.nexus.testsuite.testsupport.system.repository.config.ProxyRepositoryConfig;

public abstract class SimpleFormatRepositoryTestSystemSupport
    <HOSTED extends HostedRepositoryConfig<?>,
        PROXY extends ProxyRepositoryConfig<?>,
        GROUP extends GroupRepositoryConfig<?>>
    extends FormatRepositoryTestSystemSupport<HOSTED, PROXY, GROUP>
{
  private Class<HOSTED> hostedClass;

  private Class<PROXY> proxyClass;

  private Class<GROUP> groupClass;

  public SimpleFormatRepositoryTestSystemSupport(
      final RepositoryManager repositoryManager,
      final Class<HOSTED> hostedClass,
      final Class<PROXY> proxyClass,
      final Class<GROUP> groupClass)
  {
    super(repositoryManager);
    this.hostedClass = hostedClass;
    this.proxyClass = proxyClass;
    this.groupClass = groupClass;
  }

  public Repository createHosted(final HOSTED config) {
    return doCreate(createHostedConfiguration(config));
  }

  public Repository createProxy(final PROXY config) {
    return doCreate(createProxyConfiguration(config));
  }

  public Repository createGroup(final GROUP config) {
    return doCreate(createGroupConfiguration(config));
  }

  @SuppressWarnings("unchecked")
  public HOSTED hosted(final String name) {
    return (HOSTED) create(hostedClass, this::createHosted)
        .withName(name);
  }

  @SuppressWarnings("unchecked")
  public PROXY proxy(final String name) {
    return (PROXY) create(proxyClass, this::createProxy)
        .withName(name);
  }

  @SuppressWarnings("unchecked")
  public GROUP group(final String name) {
    return (GROUP) create(groupClass, this::createGroup)
        .withName(name);
  }

  private static <E> E create(final Class<E> clazz, final Function<E, Repository> factory) {
    try {
      return clazz.getConstructor(Function.class).newInstance(factory);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
