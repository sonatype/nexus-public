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
package org.sonatype.nexus.email;

import org.sonatype.nexus.crypto.secrets.Secret;

/**
 * Email configuration.
 *
 * @since 3.0
 */
public interface EmailConfiguration
{
  boolean isEnabled();

  void setEnabled(boolean enabled);

  String getHost();

  void setHost(String host);

  int getPort();

  void setPort(int port);

  String getUsername();

  void setUsername(String username);

  Secret getPassword();

  void setPassword(Secret password);

  String getFromAddress();

  void setFromAddress(String fromAddress);

  String getSubjectPrefix();

  void setSubjectPrefix(String subjectPrefix);

  boolean isStartTlsEnabled();

  void setStartTlsEnabled(boolean startTlsEnabled);

  boolean isStartTlsRequired();

  void setStartTlsRequired(boolean startTlsRequired);

  boolean isSslOnConnectEnabled();

  void setSslOnConnectEnabled(boolean sslOnConnectEnabled);

  boolean isSslCheckServerIdentityEnabled();

  void setSslCheckServerIdentityEnabled(boolean sslCheckServerIdentityEnabled);

  boolean isNexusTrustStoreEnabled();

  void setNexusTrustStoreEnabled(boolean nexusTrustStoreEnabled);

  EmailConfiguration copy();
}
