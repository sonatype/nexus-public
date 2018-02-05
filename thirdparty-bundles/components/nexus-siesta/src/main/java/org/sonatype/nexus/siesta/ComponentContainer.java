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
package org.sonatype.nexus.siesta;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.rest.Component;
import org.sonatype.nexus.rest.Resource;

import org.eclipse.sisu.BeanEntry;

/**
 * Siesta {@link Component} (and {@link Resource} container abstraction.
 *
 * @since 3.0
 */
public interface ComponentContainer
{
  void init(final ServletConfig config) throws ServletException;

  void service(final HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;

  void destroy();

  void addComponent(BeanEntry<?,?> entry) throws Exception;

  void removeComponent(BeanEntry<?,?> entry) throws Exception;
}
