package org.sonatype.nexus.autorole.internal;

import com.google.common.annotations.VisibleForTesting;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.eclipse.sisu.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.security.anonymous.AnonymousHelper;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Automatic role {@link AuthorizingRealm}.
 *
 * @since 3.2
 */
@Named(AutoRoleRealm.NAME)
@Singleton
@Description("Automatic Role")
public class AutoRoleRealm
  extends AuthorizingRealm
{
  private static final Logger log = LoggerFactory.getLogger(AutoRoleRealm.class);

  public static final String NAME = "AutoRole";

  @Nullable
  private String role;

  @Nullable
  public String getRole() {
    return role;
  }

  public void setRole(@Nullable final String role) {
    this.role = role;
  }

  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(final PrincipalCollection principals) {
    return maybeGrantRole(principals);
  }

  // TODO: sort out if this logic is needed for NX3 that has different anonymous handling that NX2 which this is ported from

  @VisibleForTesting
  @Nullable
  AuthorizationInfo maybeGrantRole(final PrincipalCollection principals) {
    if (role != null) {
      // only attempt to apply auto-role if user is not anonymous
      if (!AnonymousHelper.isAnonymous(principals)) {
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        info.addRole(role);
        log.debug("Granting {} role to {}", role, principals);
        return info;
      }
    }

    return null;
  }

  /**
   * @throws UnsupportedOperationException  Authentication is not supported
   */
  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(final AuthenticationToken token) throws AuthenticationException {
    throw new UnsupportedOperationException();
  }
}
