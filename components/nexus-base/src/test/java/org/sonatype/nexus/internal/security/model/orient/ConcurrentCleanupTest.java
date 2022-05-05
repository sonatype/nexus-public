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
package org.sonatype.nexus.internal.security.model.orient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;
import org.sonatype.nexus.security.config.AdminPasswordFileManager;
import org.sonatype.nexus.security.config.CRole;
import org.sonatype.nexus.security.config.CUserRoleMapping;
import org.sonatype.nexus.security.config.SecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityConfigurationCleaner;
import org.sonatype.nexus.security.config.StaticSecurityConfigurationSource;
import org.sonatype.nexus.security.config.memory.MemoryCPrivilege;
import org.sonatype.nexus.security.internal.SecurityConfigurationCleanerImpl;

import org.apache.shiro.authc.credential.PasswordService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Parallel security cleanup UTs.
 */
public class ConcurrentCleanupTest
    extends TestSupport
{
  private static final Logger log = LoggerFactory.getLogger(ConcurrentCleanupTest.class);

  private static final int NUMBER_OF_THREADS = 9;

  private static final int NUMBER_OF_MAPPING_UPDATE_THREADS = 3;

  private static final int NUMBER_OF_ROLE_UPDATE_THREADS = 3;

  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("security");

  private OrientSecurityConfigurationSource source;

  private SecurityConfiguration configuration;

  private SecurityConfigurationCleaner cleaner;

  @Mock
  private PasswordService passwordService;

  @Mock
  private AdminPasswordFileManager adminPasswordFileManager;

  @Before
  public void prepare() throws Exception {
    when(passwordService.encryptPassword(any())).thenReturn("encrypted");
    when(adminPasswordFileManager.readFile()).thenReturn("password");
    source = new OrientSecurityConfigurationSource(database.getInstanceProvider(),
        new StaticSecurityConfigurationSource(passwordService, adminPasswordFileManager, true),
        new OrientCUserEntityAdapter(), new OrientCRoleEntityAdapter(), new OrientCPrivilegeEntityAdapter(),
        new OrientCUserRoleMappingEntityAdapter());
    source.start();
    source.loadConfiguration();
    configuration = source.getConfiguration();
    cleaner = new SecurityConfigurationCleanerImpl();
  }

  @After
  public void shutdown() throws Exception {
    if (source != null) {
      source.stop();
    }
  }

  /**
   * Verify that cleaning up privileges and roles in parallel does not write each other changes.
   */
  @Test
  public void cleanup() throws Exception {
    loadTestData();

    final CountDownLatch startSignal = new CountDownLatch(1);
    final CountDownLatch doneSignal =
        new CountDownLatch(NUMBER_OF_THREADS * 2 + NUMBER_OF_MAPPING_UPDATE_THREADS + NUMBER_OF_ROLE_UPDATE_THREADS);

    for (int i = 0; i < NUMBER_OF_THREADS; i++) {
      final String id = "test-" + i;

      Worker worker = new Worker();
      worker.setStartSignal(startSignal);
      worker.setDoneSignal(doneSignal);
      worker.setToDo(() -> {
        try {
          cleaner.privilegeRemoved(configuration, id);
          log.info("cleaned privilege {}", id);
        }
        catch (Exception e) {
          log.error("cleaning privilege {} failed: {}", id, e.getMessage());
        }
      });

      new Thread(worker).start();

      worker = new Worker();
      worker.setStartSignal(startSignal);
      worker.setDoneSignal(doneSignal);
      worker.setToDo(() -> {
        try {
          cleaner.roleRemoved(configuration, id);
          log.info("cleaned role {}", id);
        }
        catch (Exception e) {
          log.error("cleaning role {} failed: {}", id, e.getMessage());
        }
      });

      new Thread(worker).start();
    }

    for (int i = 0; i < NUMBER_OF_MAPPING_UPDATE_THREADS; i++) {
      final int finalI = i;
      Worker worker = new Worker();
      worker.setStartSignal(startSignal);
      worker.setDoneSignal(doneSignal);
      worker.setToDo(() -> {
        CUserRoleMapping mapping = configuration.getUserRoleMapping("test", "default");
        try {
          configuration.updateUserRoleMapping(mapping);
          log.info("{} mapping update", finalI);
        }
        catch (Exception e) {
          log.error("{} mapping update failed: {}", finalI, e.getMessage());
        }
      });

      new Thread(worker).start();
    }

    for (int i = 0; i < NUMBER_OF_ROLE_UPDATE_THREADS; i++) {
      final int finalI = i;
      Worker worker = new Worker();
      worker.setStartSignal(startSignal);
      worker.setDoneSignal(doneSignal);
      worker.setToDo(() -> {
        CRole role = configuration.getRole("test");
        try {
          configuration.updateRole(role);
          log.info("{} role update", finalI);
        }
        catch (Exception e) {
          log.error("{} role update failed: {}", finalI, e.getMessage());
        }
      });

      new Thread(worker).start();
    }

    startSignal.countDown();
    doneSignal.await(5, TimeUnit.MINUTES);

    assertThat(configuration.getUserRoleMapping("test", "default").getRoles(), hasSize(0));
    assertThat(configuration.getRole("test").getRoles(), hasSize(0));
    assertThat(configuration.getRole("test").getPrivileges(), hasSize(0));
  }

  private void loadTestData() throws Exception {
    OrientCRole orientCRole = new OrientCRole();
    orientCRole.setId("test");
    orientCRole.setName("test");
    configuration.addRole(orientCRole);

    OrientCUserRoleMapping orientCUserRoleMapping = new OrientCUserRoleMapping();
    orientCUserRoleMapping.setUserId("test");
    orientCUserRoleMapping.setSource("default");
    configuration.addUserRoleMapping(orientCUserRoleMapping);

    for (int i = 0; i < NUMBER_OF_THREADS; i++) {
      MemoryCPrivilege memoryCPrivilege = new MemoryCPrivilege();
      memoryCPrivilege.setId("test-" + i);
      memoryCPrivilege.setType("target");
      memoryCPrivilege.setName("test-" + i);

      CRole role = configuration.getRole("test");
      role.addPrivilege("test-" + i);
      configuration.updateRole(role);
    }

    for (int i = 0; i < NUMBER_OF_THREADS; i++) {
      orientCRole = new OrientCRole();
      orientCRole.setId("test-" + i);
      orientCRole.setName("test-" + i);
      configuration.addRole(orientCRole);

      CRole role = configuration.getRole("test");
      role.addRole("test-" + i);
      configuration.updateRole(role);

      CUserRoleMapping cUserRoleMapping = configuration.getUserRoleMapping("test", "default");
      cUserRoleMapping.addRole("test-" + i);
      configuration.updateUserRoleMapping(cUserRoleMapping);
    }
  }

  private static final class Worker
      implements Runnable
  {
    private CountDownLatch startSignal;

    private CountDownLatch doneSignal;

    private Runnable toDo;

    @Override
    public void run() {
      try {
        startSignal.await();
        toDo.run();
      }
      catch (Exception ignore) {
        // do nothing
      }
      finally {
        doneSignal.countDown();
      }
    }

    public CountDownLatch getStartSignal() {
      return startSignal;
    }

    public void setStartSignal(final CountDownLatch startSignal) {
      this.startSignal = startSignal;
    }

    public CountDownLatch getDoneSignal() {
      return doneSignal;
    }

    public void setDoneSignal(final CountDownLatch doneSignal) {
      this.doneSignal = doneSignal;
    }

    public Runnable getToDo() {
      return toDo;
    }

    public void setToDo(final Runnable toDo) {
      this.toDo = toDo;
    }
  }
}
