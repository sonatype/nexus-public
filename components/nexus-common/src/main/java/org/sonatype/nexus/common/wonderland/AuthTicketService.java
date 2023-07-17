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
package org.sonatype.nexus.common.wonderland;

/**
 * Authentication ticket service.
 *
 * @since 2.7
 */
public interface AuthTicketService
{
  /**
   * Create a new authentication ticket.
   */
  String createTicket(String user, String realmName);

  /**
   * Create a new authentication ticket for the currently logged in user.
   */
  String createTicket();

  /**
   * Redeem an authentication ticket.
   *
   * @return {@code true} if the authentication ticket was redeemed, else {@code false} if the ticket is invalid.
   */
  boolean redeemTicket(String user, String ticket, String realmName);

  /**
   * Redeem an authentication ticket for the currently logged in user.
   *
   * @return {@code true} if the authentication ticket was redeemed, else {@code false} if the ticket is invalid.
   */
  boolean redeemTicket(String ticket);
}
