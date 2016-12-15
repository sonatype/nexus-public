package org.sonatype.nexus.autorole.internal

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.security.anonymous.AnonymousPrincipalCollection

import org.apache.shiro.authz.AuthorizationInfo
import org.apache.shiro.subject.PrincipalCollection
import org.apache.shiro.subject.SimplePrincipalCollection
import org.junit.Before
import org.junit.Test

/**
 * Tests for {@link AutoRoleRealm}.
 */
class AutoRoleRealmTest
  extends TestSupport
{
  private AutoRoleRealm underTest

  @Before
  void setUp() {
    underTest = new AutoRoleRealm()
  }

  private static PrincipalCollection principals(final String userId) {
    if (userId == 'anonymous') {
      return new AnonymousPrincipalCollection(userId, 'realm')
    }
    return new SimplePrincipalCollection(userId, 'realm')
  }

  @Test
  void 'when not configured nothing is granted'() {
    underTest.role = null
    AuthorizationInfo info = underTest.maybeGrantRole(principals('test'))
    assert info == null
  }

  @Test
  void 'role granted when user is authenticated'() {
    underTest.role = 'default-role'
    AuthorizationInfo info = underTest.maybeGrantRole(principals('test'))
    assert info != null
    assert info.roles.contains('default-role')
  }

  @Test
  void 'role not granted for anonymous'() {
    underTest.role = 'default-role'
    AuthorizationInfo info = underTest.maybeGrantRole(principals('anonymous'))
    assert info == null
  }
}
