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

import java.util.Properties;

/**
 * Strategy for customizing SMTP Session parameters.
 *
 * @since 2.4
 */
public interface SmtpSessionParametersCustomizer
{

  /**
   * Callback for customizing SMTP session creation parameters.
   *
   * @param params existing parameters (never null)
   * @return customized parameters (never null). Implementations can decide if they wanna return same parameters as
   *         the ones they
   *         receive or return new ones.
   */
  Properties customize(Properties params);

}
