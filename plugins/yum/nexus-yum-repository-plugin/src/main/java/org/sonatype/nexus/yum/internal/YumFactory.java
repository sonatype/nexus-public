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
package org.sonatype.nexus.yum.internal;

import java.io.File;

import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.yum.YumGroup;
import org.sonatype.nexus.yum.YumHosted;
import org.sonatype.nexus.yum.YumProxy;

/**
 * @since yum 3.0
 */
public interface YumFactory
{

  YumHosted createHosted(File temporaryDirectory, HostedRepository repository);

  /**
   * @since 2.7
   */
  YumProxy createProxy(ProxyRepository repository);

  /**
   * @since 2.7
   */
  YumGroup createGroup(GroupRepository repository);

}
