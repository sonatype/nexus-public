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
package org.sonatype.nexus.security.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.security.Roles;
import org.sonatype.nexus.security.config.memory.MemoryCUser;
import org.sonatype.nexus.security.config.memory.MemoryCUserRoleMapping;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authc.credential.PasswordService;

// FIXME: Perhaps this would be better in nexus-core internal.security?

/**
 * Security model configuration defaults.
 *
 * @since 3.0
 */
@Named("static")
@Singleton
@Priority(Integer.MIN_VALUE)
public class StaticSecurityConfigurationSource
    implements SecurityConfigurationSource
{
  private static final String NEXUS_SECURITY_INITIAL_PASSWORD = "NEXUS_SECURITY_INITIAL_PASSWORD";

  private final PasswordService passwordService;

  private final AdminPasswordFileManager adminPasswordFileManager;

  private final boolean randomPassword;

  private final String password;

  private SecurityConfiguration configuration;

  @Inject
  public StaticSecurityConfigurationSource(
      final PasswordService passwordService,
      final AdminPasswordFileManager adminPasswordFileManager,
      @Named("${nexus.security.randompassword:-true}") final boolean randomPassword)
  {
    this(passwordService, adminPasswordFileManager, randomPassword, System.getenv(NEXUS_SECURITY_INITIAL_PASSWORD));
  }

  public StaticSecurityConfigurationSource(
      final PasswordService passwordService,
      final AdminPasswordFileManager adminPasswordFileManager,
      final boolean randomPassword,
      @Nullable final String password)
  {
    this.passwordService = passwordService;
    this.adminPasswordFileManager = adminPasswordFileManager;
    this.password = password;

    if (StringUtils.isBlank(password)) {
      boolean enabled = Optional.ofNullable(System.getenv("NEXUS_SECURITY_RANDOMPASSWORD"))
          .map(Boolean::valueOf)
          .orElse(true);
      this.randomPassword = randomPassword && enabled;
    }
    else {
      this.randomPassword = false;
    }
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
    String encryptedPassword = passwordService.encryptPassword(getPassword());

    return new MemorySecurityConfiguration().withUsers(
        new MemoryCUser()
            .withId("admin")
            .withPassword(encryptedPassword)
            .withFirstName("Administrator")
            .withLastName("User")
            .withStatus(randomPassword ? CUser.STATUS_CHANGE_PASSWORD : CUser.STATUS_ACTIVE)
            .withEmail("admin@example.org"),
        new MemoryCUser()
            .withId("anonymous")
            // password="anonymous"
            .withPassword(
                "$shiro1$SHA-512$1024$CPJm1XWdYNg5eCAYp4L4HA==$HIGwnJhC07ZpgeVblZcFRD1F6KH+xPG8t7mIcEMbfycC+n5Ljudyoj9dzdinrLmChTrmKMCw2/z29F7HeLbTbQ==")
            .withFirstName("Anonymous")
            .withLastName("User")
            .withStatus(CUser.STATUS_ACTIVE)
            .withEmail("anonymous@example.org"))
        .withUserRoleMappings(
            new MemoryCUserRoleMapping()
                .withUserId("admin")
                .withSource("default")
                .withRoles(Roles.ADMIN_ROLE_ID),
            new MemoryCUserRoleMapping()
                .withUserId("anonymous")
                .withSource("default")
                .withRoles(Roles.ANONYMOUS_ROLE_ID));
  }

  private String getPassword() {
    if (StringUtils.isNotBlank(password)) {
      return password;
    }

    try {
      String savedPassword = adminPasswordFileManager.readFile();

      if (!Strings2.isBlank(savedPassword)) {
        return savedPassword;
      }
      else if (!randomPassword) {
        return "admin123";
      }

      savedPassword = UUID.randomUUID().toString();

      // failure writing file to disk, revert to using default
      if (!adminPasswordFileManager.writeFile(savedPassword)) {
        savedPassword = "admin123";
      }
      return savedPassword;
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
