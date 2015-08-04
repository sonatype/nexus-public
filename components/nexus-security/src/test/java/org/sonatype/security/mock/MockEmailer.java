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
package org.sonatype.security.mock;

import java.util.List;

import org.sonatype.security.email.SecurityEmailer;

public class MockEmailer
    implements SecurityEmailer
{

  public List<String> forgotUserIds;

  public void sendForgotUsername(String email, List<String> userIds) {
    forgotUserIds = userIds;
  }

  public void sendNewUserCreated(String email, String userid, String password) {
  }

  public void sendResetPassword(String email, String password) {
  }

  public List<String> getForgotUserIds() {
    return forgotUserIds;
  }

}
