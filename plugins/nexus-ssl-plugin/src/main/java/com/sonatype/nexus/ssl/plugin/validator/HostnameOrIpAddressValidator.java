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
package com.sonatype.nexus.ssl.plugin.validator;

import javax.validation.ConstraintValidatorContext;

import org.sonatype.nexus.validation.ConstraintValidatorSupport;

import com.google.common.net.InetAddresses;
import com.google.common.net.InternetDomainName;

/**
 * Hostname or IP address validator.
 *
 * @since 3.36
 */
public class HostnameOrIpAddressValidator
    extends ConstraintValidatorSupport<HostnameOrIpAddress, String>
{
  @Override
  public boolean isValid(final String value, final ConstraintValidatorContext context) {
    return InternetDomainName.isValid(value) || InetAddresses.isInetAddress(value);
  }
}
