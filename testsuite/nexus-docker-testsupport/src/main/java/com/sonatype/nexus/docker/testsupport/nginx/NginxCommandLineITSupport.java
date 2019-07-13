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
package com.sonatype.nexus.docker.testsupport.nginx;

import com.sonatype.nexus.docker.testsupport.ContainerCommandLineITSupport;
import com.sonatype.nexus.docker.testsupport.framework.DockerContainerConfig;

/**
 * Nginx implementation of a Docker Command Line enabled container.
 *
 * @since 3.16
 */
public class NginxCommandLineITSupport
    extends ContainerCommandLineITSupport
{
  private static final String CMD_SERVICE = "service ";

  private static final String CMD_NGINX = "nginx ";

  /**
   * Constructor.
   *
   * @param dockerContainerConfig {@link DockerContainerConfig}
   */
  public NginxCommandLineITSupport(DockerContainerConfig dockerContainerConfig) {
    super(dockerContainerConfig);
  }

  /**
   * Runs a nginx server by running <code>service nginx start</code>
   */
  public void nginxServiceStart() {
    exec(CMD_SERVICE + CMD_NGINX + "start > /dev/null 2>&1");
  }

  /**
   * Stops a nginx server by running <code>service nginx stop</code>
   */
  public void nginxServiceStop() {
    exec(CMD_SERVICE + CMD_NGINX + "stop > /dev/null 2>&1");
  }
}
