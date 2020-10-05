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
package org.sonatype.repository.conan.internal.orient.proxy.v1;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.view.Router;
import org.sonatype.repository.conan.internal.common.PingController;
import org.sonatype.repository.conan.internal.orient.common.UserController;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.28
 */
@Named
@Singleton
public class ConanProxyApiV1
{
  private static final String VERSION = "v1";

  private PingController pingController;

  private UserController userController;

  private ConanProxyControllerV1 conanProxyControllerV1;

  @Inject
  public ConanProxyApiV1(final PingController pingController,
                         final UserController userController,
                         final ConanProxyControllerV1 conanProxyControllerV1) {
    this.pingController = checkNotNull(pingController);
    this.userController = checkNotNull(userController);
    this.conanProxyControllerV1 = checkNotNull(conanProxyControllerV1);
  }

  public void create(final Router.Builder builder) {
    pingController.attach(builder, VERSION);
    userController.attach(builder, VERSION);
    conanProxyControllerV1.attach(builder);
  }
}
