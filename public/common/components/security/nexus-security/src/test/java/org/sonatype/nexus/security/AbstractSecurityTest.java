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
package org.sonatype.nexus.security;

import java.util.ArrayList;
import java.util.Arrays;

import org.sonatype.nexus.audit.AuditRecorder;
import org.sonatype.nexus.bootstrap.entrypoint.jetty.JettyServer;
import org.sonatype.nexus.bootstrap.jetty.ManagedJetty;
import org.sonatype.nexus.bootstrap.security.WebSecurityConfiguration;
import org.sonatype.nexus.bootstrap.entrypoint.configuration.ApplicationDirectories;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.crypto.secrets.SecretsStore;
import org.sonatype.nexus.security.AbstractSecurityTest.TestSecurityConfigurationMocks;
import org.sonatype.nexus.security.anonymous.AnonymousManager;
import org.sonatype.nexus.security.config.PreconfiguredSecurityConfigurationSource;
import org.sonatype.nexus.security.config.SecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityConfigurationSource;
import org.sonatype.nexus.security.internal.AuthorizingRealmImpl;
import org.sonatype.nexus.security.realm.MemoryRealmConfigurationStore;
import org.sonatype.nexus.security.realm.RealmConfiguration;
import org.sonatype.nexus.security.realm.RealmConfigurationStore;
import org.sonatype.nexus.security.realm.TestRealmConfiguration;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.testcommon.event.SimpleEventManager;

import org.apache.shiro.authc.credential.PasswordService;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;

@DirtiesContext(classMode = AFTER_CLASS)
@SpringBootTest
@ContextConfiguration(classes = TestSecurityConfigurationMocks.class)
@ComponentScan(basePackages = "org.sonatype.nexus",
    excludeFilters = {@Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {ManagedJetty.class, JettyServer.class})})
@Import({org.sonatype.nexus.bootstrap.security.SecurityConfiguration.class, WebSecurityConfiguration.class})
public abstract class AbstractSecurityTest
{
  private static final Logger log = LoggerFactory.getLogger(AbstractSecurityTest.class);

  @Autowired
  protected ApplicationContext applicationContext;

  @MockBean
  protected PasswordService passwordService;

  @BeforeEach
  protected void setUp() throws Exception {
    getSecuritySystem().start();
  }

  @AfterEach
  protected void tearDown() throws Exception {
    try {
      getSecuritySystem().stop();
    }
    catch (Exception e) {
      log.warn("Failed to stop security-system", e);
    }

    ThreadContext.remove();
  }

  protected <T> T lookup(final Class<T> role) {
    return applicationContext.getBean(role);
  }

  protected <T> T lookup(final Class<T> role, final String hint) {
    return BeanFactoryAnnotationUtils.qualifiedBeanOfType(applicationContext.getAutowireCapableBeanFactory(), role,
        hint);
  }

  protected SecuritySystem getSecuritySystem() {
    return lookup(SecuritySystem.class);
  }

  protected UserManager getUserManager() {
    return lookup(UserManager.class);
  }

  protected final SecurityConfiguration getSecurityConfiguration() {
    return lookup(SecurityConfigurationSource.class, "default").getConfiguration();
  }

  public static class BaseSecurityConfiguration
  {
    @Qualifier("default")
    @Primary
    @Bean
    public SecurityConfigurationSource securityConfigurationSource() {
      return new PreconfiguredSecurityConfigurationSource(BaseSecurityConfig.get());
    }
  }

  @Configuration
  public static class TestSecurityConfigurationMocks
  {
    @Primary
    @Bean
    public EventManager eventManager() {
      return spy(new SimpleEventManager());
    }

    @Primary
    @Bean
    public AnonymousManager anonymousManager() {
      return mock(AnonymousManager.class);
    }

    @Qualifier("initial")
    @Primary
    @Bean
    public RealmConfiguration realmConfiguration() {
      RealmConfiguration realmConfiguration = new TestRealmConfiguration();
      realmConfiguration.setRealmNames(
          new ArrayList<>(Arrays.asList("MockRealmA", "MockRealmB", "MockRealmC", AuthorizingRealmImpl.NAME)));
      return realmConfiguration;
    }

    @Primary
    @Bean
    public NodeAccess nodeAccess() {
      return mock(NodeAccess.class);
    }

    @Primary
    @Bean
    public ApplicationDirectories applicationDirectories() {
      return mock(ApplicationDirectories.class);
    }

    /**
     * {@link org.sonatype.nexus.swagger.internal.SwaggerModel} is component-scanned; its constructor needs this bean.
     */
    @Primary
    @Bean
    public ApplicationVersion applicationVersion() {
      ApplicationVersion applicationVersion = mock(ApplicationVersion.class);
      when(applicationVersion.getVersion()).thenReturn("0.0.0-test");
      return applicationVersion;
    }

    @Bean
    public DatabaseCheck databaseCheck() {
      return mock(DatabaseCheck.class);
    }

    @Bean
    public SecretsStore secretsStore() {
      return mock(SecretsStore.class);
    }

    @Bean
    public AuditRecorder auditRecorder() {
      return mock(AuditRecorder.class);
    }

    @Bean
    RealmConfigurationStore memoryRealmConfigurationStore() {
      return new MemoryRealmConfigurationStore();
    }
  }
}
