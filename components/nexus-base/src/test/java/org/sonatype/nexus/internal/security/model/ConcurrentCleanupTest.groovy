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
package org.sonatype.nexus.internal.security.model

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.common.app.ApplicationDirectories
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule
import org.sonatype.nexus.security.config.CPrivilege
import org.sonatype.nexus.security.config.CRole
import org.sonatype.nexus.security.config.CUserRoleMapping
import org.sonatype.nexus.security.config.SecurityConfiguration
import org.sonatype.nexus.security.config.SecurityConfigurationCleaner
import org.sonatype.nexus.security.config.StaticSecurityConfigurationSource
import org.sonatype.nexus.security.internal.SecurityConfigurationCleanerImpl

import org.apache.shiro.authc.credential.PasswordService
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mock
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.hasSize
import static org.mockito.Mockito.*

/**
 * Parallel security cleanup UTs.
 */
class ConcurrentCleanupTest
extends TestSupport
{
  private static final Logger log = LoggerFactory.getLogger(ConcurrentCleanupTest)

  private static final int NUMBER_OF_THREADS = 9

  private static final int NUMBER_OF_MAPPING_UPDATE_THREADS = 3

  private static final int NUMBER_OF_ROLE_UPDATE_THREADS = 3

  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory('security')

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private OrientSecurityConfigurationSource source

  private SecurityConfiguration configuration

  private SecurityConfigurationCleaner cleaner

  @Mock
  PasswordService passwordService

  @Mock
  ApplicationDirectories directories

  @Before
  public void prepare() throws Exception {
    when(directories.getWorkDirectory()).thenReturn(tempFolder.getRoot())
    when(passwordService.encryptPassword(any())).thenReturn("encrypted")
    source = new OrientSecurityConfigurationSource(
        database.instanceProvider,
        new StaticSecurityConfigurationSource(directories, passwordService, true),
        new CUserEntityAdapter(),
        new CRoleEntityAdapter(),
        new CPrivilegeEntityAdapter(),
        new CUserRoleMappingEntityAdapter()
    )
    source.start()
    source.loadConfiguration()
    configuration = source.configuration
    cleaner = new SecurityConfigurationCleanerImpl()
  }

  @After
  public void shutdown() throws Exception {
    if (source) {
      source.stop()
    }
  }

  /**
   * Verify that cleaning up privileges and roles in parallel does not write each other changes.
   */
  @Test
  public void cleanup() throws Exception {
    loadTestData()

    final CountDownLatch startSignal = new CountDownLatch(1)
    final CountDownLatch doneSignal = new CountDownLatch(
        NUMBER_OF_THREADS * 2 + NUMBER_OF_MAPPING_UPDATE_THREADS + NUMBER_OF_ROLE_UPDATE_THREADS
    )

    (1..NUMBER_OF_THREADS).each { i ->
      final String id = 'test-' + i

      new Thread(new Worker(startSignal: startSignal, doneSignal: doneSignal, toDo: {
        try {
          cleaner.privilegeRemoved(configuration, id)
          log.info('cleaned privilege {}', id)
        }
        catch (Exception e) {
          log.error('cleaning privilege {} failed: {}', id, e.getMessage())
        }
      })).start()

      new Thread(new Worker(startSignal: startSignal, doneSignal: doneSignal, toDo: {
        try {
          cleaner.roleRemoved(configuration, id)
          log.info('cleaned role {}', id)
        }
        catch (Exception e) {
          log.error('cleaning role {} failed: {}', id, e.getMessage())
        }
      })).start()
    }

    // have a number of threads updating the mappings
    (1..NUMBER_OF_MAPPING_UPDATE_THREADS).each { i ->
      new Thread(new Worker(startSignal: startSignal, doneSignal: doneSignal, toDo: {
        CUserRoleMapping mapping = configuration.getUserRoleMapping('test', 'default')
        try {
          configuration.updateUserRoleMapping(mapping)
          log.info('{} mapping update', i)
        }
        catch (Exception e) {
          log.error('{} mapping update failed: {}', i, e.getMessage())
        }
      })).start()
    }

    // have a number of threads updating the roles
    (1..NUMBER_OF_ROLE_UPDATE_THREADS).each { i ->
      new Thread(new Worker(startSignal: startSignal, doneSignal: doneSignal, toDo: {
        CRole role = configuration.getRole('test')
        try {
          configuration.updateRole(role)
          log.info('{} role update', i)
        }
        catch (Exception e) {
          log.error('{} role update failed: {}', i, e.getMessage())
        }
      })).start()
    }

    startSignal.countDown()
    doneSignal.await(5, TimeUnit.MINUTES)

    assertThat(configuration.getUserRoleMapping('test', 'default').getRoles(), hasSize(0))
    assertThat(configuration.getRole('test').getRoles(), hasSize(0))
    assertThat(configuration.getRole('test').getPrivileges(), hasSize(0))
  }

  private static class Worker
  implements Runnable
  {
    CountDownLatch startSignal
    CountDownLatch doneSignal
    Closure toDo

    @Override
    public void run() {
      try {
        startSignal.await()
        toDo.call()
      }
      catch (Exception ignore) {
        // do nothing
      }
      finally {
        doneSignal.countDown()
      }
    }
  }

  private void loadTestData() throws Exception {
    configuration.addRole(new CRole(
        id: 'test',
        name: 'test'
    ))
    configuration.addUserRoleMapping(new CUserRoleMapping(
        userId: 'test',
        source: 'default'
    ))

    // create privileges and assign them to test role
    (1..NUMBER_OF_THREADS).each { i ->
      configuration.addPrivilege(new CPrivilege(
          id: "test-${i}",
          type: 'target',
          name: "test-${i}"
      ))

      CRole role = configuration.getRole('test')
      role.addPrivilege("test-${i}")
      configuration.updateRole(role)
    }

    // create roles and assign then to test user and test role
    (1..NUMBER_OF_THREADS).each { i ->
      configuration.addRole(new CRole(
          id: "test-${i}",
          name: "test-${i}"
      ))

      CRole role = configuration.getRole('test')
      role.addRole("test-${i}")
      configuration.updateRole(role)

      CUserRoleMapping mapping = configuration.getUserRoleMapping('test', 'default')
      mapping.addRole("test-${i}")
      configuration.updateUserRoleMapping(mapping)
    }
  }

}

