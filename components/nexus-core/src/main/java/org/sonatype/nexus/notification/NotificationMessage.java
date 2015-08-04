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
package org.sonatype.nexus.notification;

/**
 * The message to send out using notification carriers. This might be sourced from a simple string, or template, but it
 * has to be reusable (and cached if needed), since in case of multiple targets, the same message instance is reused!
 * TODO: rethink this. We need more subtle abstraction to support multiple message formats and/or sources. This will do
 * for now.
 *
 * @author cstamas
 */
public interface NotificationMessage
{
  String getMessageTitle();

  String getMessageBody();

  // --

  public static final NotificationMessage EMPTY_MESSAGE = new NotificationMessage()
  {
    public String getMessageTitle() {
      return "";
    }

    public String getMessageBody() {
      return "";
    }
  };
}
