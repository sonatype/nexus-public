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
 * This class will be removed, when we implement real notification mechanism. Right now, this only concentrates all the
 * "cheating" elements to implement the simplest notification we need for now. When doing right, 1st step is to
 * _remove_
 * this class, and all the points needing some work will be "highlighted" by compiler ;)
 *
 * @author cstamas
 */
public class NotificationCheat
{
  public static final String AUTO_BLOCK_NOTIFICATION_GROUP_ID = "autoBlockTarget";

  protected static final String CARRIER_KEY = EmailCarrier.KEY;
}
