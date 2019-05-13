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
package org.sonatype.nexus.security.config

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.common.app.ApplicationDirectories
import org.sonatype.nexus.security.Roles

import org.apache.shiro.authc.credential.PasswordService

// FIXME: Perhaps this would be better in nexus-core internal.security?

/**
 * Security model configuration defaults.
 *
 * @since 3.0
 */
@Named('static')
@Singleton
class StaticSecurityConfigurationSource
    implements SecurityConfigurationSource
{
  static final String ADMIN_PASSWORD_FILE = 'admin.password'

  private final SecurityConfiguration configuration;

  private final ApplicationDirectories applicationDirectories

  private final PasswordService passwordService

  private final boolean randomPassword

  @Inject
  StaticSecurityConfigurationSource(
      final ApplicationDirectories applicationDirectories,
      final PasswordService passwordService,
      @Named('${nexus.security.randompassword:-true}') final boolean randomPassword)
  {
    this.applicationDirectories = applicationDirectories
    this.passwordService = passwordService

    String enabled = System.getenv("NEXUS_SECURITY_RANDOMPASSWORD")
    this.randomPassword = randomPassword && (enabled != null ? Boolean.valueOf(enabled) : true)
  }

  @Override
  public SecurityConfiguration getConfiguration() {
    if (configuration != null) {
      return configuration;
    }
    return loadConfiguration();
  }

  @Override
  public synchronized SecurityConfiguration loadConfiguration() {
    String password = passwordService.encryptPassword(getPassword())

    return new MemorySecurityConfiguration(
      users: [
          new CUser(
              id: 'admin',
              password: password,
              firstName: 'Administrator',
              lastName: 'User',
              status: randomPassword ? CUser.STATUS_CHANGE_PASSWORD : CUser.STATUS_ACTIVE,
              email: 'admin@example.org'
          ),
          new CUser(
              id: 'anonymous',
              // password='anonymous'
              password: '$shiro1$SHA-512$1024$CPJm1XWdYNg5eCAYp4L4HA==$HIGwnJhC07ZpgeVblZcFRD1F6KH+xPG8t7mIcEMbfycC+n5Ljudyoj9dzdinrLmChTrmKMCw2/z29F7HeLbTbQ==',
              firstName: 'Anonymous',
              lastName: 'User',
              status: CUser.STATUS_ACTIVE,
              email: 'anonymous@example.org'
          )
      ],
      userRoleMappings: [
          new CUserRoleMapping(
              userId: 'admin',
              source: 'default',
              roles: [
                  Roles.ADMIN_ROLE_ID
              ]
          ),
          new CUserRoleMapping(
              userId: 'anonymous',
              source: 'default',
              roles: [
                  Roles.ANONYMOUS_ROLE_ID
              ]
          )
      ]
    )
  }

  private String getPassword() {
    File adminPassword = new File(applicationDirectories.getWorkDirectory(), ADMIN_PASSWORD_FILE)
    if (adminPassword.exists()) {
      return adminPassword.text
    }
    else if (!randomPassword || !adminPassword.createNewFile()) {
      return 'admin123'
    }
    adminPassword.setReadable(false)
    adminPassword.setReadable(true, true)

    applicationDirectories.getWorkDirectory().mkdirs()
    String password = UUID.randomUUID()
    adminPassword.withWriter('utf-8') { writer -> writer.write password}
    return password
  }
}
