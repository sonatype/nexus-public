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
package org.sonatype.nexus.plugins.defaultrole;

import org.sonatype.nexus.common.time.Time;
import org.sonatype.nexus.security.authz.DefaultRoleExemption;
import org.sonatype.nexus.security.authz.PrincipalPermissionsCacheFactory;

import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.junit.After;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Resolves {@link DefaultRoleRealm} from a real Spring context rather than constructing it directly, so that a
 * resolution failure of the {@link DefaultRoleExemption} constructor parameter is reproduced the way it happens in
 * production (NEXUS-55375). Every test in {@link DefaultRoleRealmTest} constructs the realm directly, so none of
 * them can catch this class of bug.
 */
public class DefaultRoleRealmWiringTest
{
  private AnnotationConfigApplicationContext context;

  @After
  public void tearDown() {
    if (context != null) {
      context.close();
    }
  }

  @Test
  public void ce_noExemptionBeans_grantsDefaultRole() {
    // NEXUS-55375: CE installs no DefaultRoleExemption. Resolution must yield an empty set, not throw.
    context = newContext(NoExemptions.class);
    DefaultRoleRealm realm = context.getBean(DefaultRoleRealm.class);
    realm.setRole("default-role");

    AuthorizationInfo info = realm.doGetAuthorizationInfo(new SimplePrincipalCollection("user", "realm"));

    assertThat(info, notNullValue());
    assertThat(info.getRoles(), is(singleton("default-role")));
  }

  @Test
  public void pro_oneExemptionBean_exemptsThatRealmAndGrantsOthers() {
    context = newContext(OneExemption.class);
    DefaultRoleRealm realm = context.getBean(DefaultRoleRealm.class);
    realm.setRole("default-role");

    assertThat(realm.doGetAuthorizationInfo(new SimplePrincipalCollection("sat", "ServiceAccountRealm")), nullValue());
    assertThat(realm.doGetAuthorizationInfo(new SimplePrincipalCollection("user", "realm")), notNullValue());
  }

  @Test
  public void multipleExemptionBeans_allRealmsExempt() {
    // orderedStream() is correct for N beans; getObject()/getIfAvailable() would throw NoUniqueBeanDefinitionException.
    context = newContext(TwoExemptions.class);
    DefaultRoleRealm realm = context.getBean(DefaultRoleRealm.class);
    realm.setRole("default-role");

    assertThat(realm.doGetAuthorizationInfo(new SimplePrincipalCollection("sat", "ServiceAccountRealm")), nullValue());
    assertThat(realm.doGetAuthorizationInfo(new SimplePrincipalCollection("tok", "MyTokenRealm")), nullValue());
    assertThat(realm.doGetAuthorizationInfo(new SimplePrincipalCollection("user", "realm")), notNullValue());
  }

  private static AnnotationConfigApplicationContext newContext(final Class<?> exemptionsConfig) {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    // DefaultRoleRealm is registered directly (not built by a @Bean factory method) so that Spring resolves its
    // @Autowired constructor by reflection - the point of this test is that Spring, not the test, does the resolving.
    ctx.register(Collaborators.class, DefaultRoleRealm.class, exemptionsConfig);
    ctx.refresh();
    return ctx;
  }

  /**
   * The one collaborator every scenario needs besides the exemptions themselves.
   */
  @Configuration
  static class Collaborators
  {
    @Bean
    PrincipalPermissionsCacheFactory principalPermissionsCacheFactory() {
      return new PrincipalPermissionsCacheFactory(false, -1, () -> Time.minutes(2), false, 1);
    }
  }

  @Configuration
  static class NoExemptions
  {
  }

  @Configuration
  static class OneExemption
  {
    @Bean
    DefaultRoleExemption serviceAccount() {
      return () -> "ServiceAccountRealm";
    }
  }

  @Configuration
  static class TwoExemptions
  {
    @Bean
    DefaultRoleExemption serviceAccount() {
      return () -> "ServiceAccountRealm";
    }

    @Bean
    DefaultRoleExemption token() {
      return () -> "MyTokenRealm";
    }
  }
}
