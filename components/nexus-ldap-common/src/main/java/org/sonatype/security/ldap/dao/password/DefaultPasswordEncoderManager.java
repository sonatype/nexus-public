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
package org.sonatype.security.ldap.dao.password;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.util.SystemPropertiesHelper;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author cstamas
 */
@Singleton
@Named
public class DefaultPasswordEncoderManager
    extends ComponentSupport
    implements PasswordEncoderManager
{

  private static final Pattern ENCODING_SPEC_PATTERN = Pattern.compile("\\{([a-zA-Z0-9]+)\\}(.+)");

  private final String preferredEncoding;

  private final Map<String, PasswordEncoder> encodersMap;

  @Inject
  public DefaultPasswordEncoderManager( final Map<String, PasswordEncoder> encodersMap) {
    this.preferredEncoding = SystemPropertiesHelper.getString("ldap.preferredEncoding", "clear");
    this.encodersMap = checkNotNull(encodersMap);
  }

  @Override
  public String encodePassword(String password, Object salt) {
    PasswordEncoder encoder = getPasswordEncoder(preferredEncoding);

    if (encoder == null) {
      throw new IllegalStateException("Preferred encoding has no associated PasswordEncoder.");
    }

    return encoder.encodePassword(password, salt);
  }

  @Override
  public boolean isPasswordValid(String encodedPassword, String password, Object salt) {
    if (encodedPassword == null) {
      return false;
    }

    String encoding = preferredEncoding;

    Matcher matcher = ENCODING_SPEC_PATTERN.matcher(encodedPassword);

    if (matcher.matches()) {
      encoding = matcher.group(1);
      encodedPassword = matcher.group(2);
    }

    PasswordEncoder encoder = getPasswordEncoder(encoding.toLowerCase());

    log.info("Verifying password with encoding: " + encoding + " (encoder: " + encoder + ").");

    if (encoder == null) {
      throw new IllegalStateException("Password encoding: " + encoding + " has no associated PasswordEncoder.");
    }

    return encoder.isPasswordValid(encodedPassword, password, salt);
  }

  @Override
  public String getPreferredEncoding() {
    return preferredEncoding;
  }

  private PasswordEncoder getPasswordEncoder(String encoding) {
    if (encodersMap.containsKey(encoding)) {
      return encodersMap.get(encoding);
    }
    else {
      return null;
    }
  }

}
