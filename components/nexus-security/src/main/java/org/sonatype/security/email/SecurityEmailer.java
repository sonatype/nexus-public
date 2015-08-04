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
package org.sonatype.security.email;

import java.util.List;

/**
 * A Component use to notify a user when his/her password is changed or reset.
 */
public interface SecurityEmailer
{
  /**
   * Send an email to the user telling them they have a new account.
   */
  void sendNewUserCreated(String email, String userid, String password);

  /**
   * Send an email to the user telling them their password has changed.
   */
  void sendResetPassword(String email, String password);

  /**
   * Send an email to the user reminding them of their username.
   */
  void sendForgotUsername(String email, List<String> userIds);
}
