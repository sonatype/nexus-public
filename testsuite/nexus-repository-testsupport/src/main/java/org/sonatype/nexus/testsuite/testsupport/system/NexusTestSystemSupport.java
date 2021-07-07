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
package org.sonatype.nexus.testsuite.testsupport.system;

import javax.inject.Inject;
import javax.inject.Provider;

import org.sonatype.nexus.testsuite.helpers.ComponentAssetTestHelper;
import org.sonatype.nexus.testsuite.testsupport.fixtures.BlobStoreRule;
import org.sonatype.nexus.testsuite.testsupport.fixtures.CapabilitiesRule;
import org.sonatype.nexus.testsuite.testsupport.fixtures.RepositoryRule;
import org.sonatype.nexus.testsuite.testsupport.fixtures.SecurityRealmRule;
import org.sonatype.nexus.testsuite.testsupport.fixtures.SecurityRule;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NexusTestSystemSupport<R extends RepositoryRule, C extends CapabilitiesRule>
    extends ExternalResource
{
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final R repositories;

  private final C capabilities;

  @Inject
  private BlobStoreRule blobstores;

  @Inject
  private CleanupTestSystem cleanup;

  @Inject
  private ComponentAssetTestHelper components;

  @Inject
  private LogTestSystem logs;

  @Inject
  private RestTestHelper restTestHelper;

  @Inject
  private SearchTestSystem searchTestSystem;

  @Inject
  private SecurityRule security;

  @Inject
  private TaskTestSystem tasks;

  @Inject
  private SecurityRealmRule securityRealms;

  protected NexusTestSystemSupport(
      final R repositories,
      final C capabilities)
  {
    this.repositories = repositories;
    this.capabilities = capabilities;
  }

  public BlobStoreRule blobStores() {
    return blobstores;
  }

  public C capabilities() {
    return capabilities;
  }

  public CleanupTestSystem cleanup() {
    return cleanup;
  }

  public ComponentAssetTestHelper components() {
    return components;
  }

  public LogTestSystem logs() {
    return logs;
  }

  public R repositories() {
    return repositories;
  }

  public RestTestHelper rest() {
    return restTestHelper;
  }

  public SearchTestSystem search() {
    return searchTestSystem;
  }

  public SecurityRule security() {
    return security;
  }

  public SecurityRealmRule securityRealms() {
    return securityRealms;
  }

  public TaskTestSystem tasks() {
    return tasks;
  }

  @Override
  protected void before() throws Throwable {
    securityRealms.before();
  }

  @Override
  protected void after() {
    log.info("Cleaning up test entities");
    cleanup.after();
    capabilities.after();
    tasks.after();
    repositories.after();
    blobstores.after();
    security.after();
    securityRealms.after();
  }

  public static class NexusTestSystemRule
      extends ExternalResource
  {
    private final Provider<? extends NexusTestSystemSupport<?, ?>> nexus;

    public NexusTestSystemRule(final Provider<? extends NexusTestSystemSupport<?, ?>> nexus) {
      this.nexus = nexus;
    }

    @Override
    protected void before() throws Throwable {
      nexus.get().before();
    }

    @Override
    protected void after() {
      nexus.get().after();
    }
  }
}
