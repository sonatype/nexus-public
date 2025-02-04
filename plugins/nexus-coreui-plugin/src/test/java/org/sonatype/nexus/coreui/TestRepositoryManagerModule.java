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
package org.sonatype.nexus.coreui;

import java.util.Arrays;
import java.util.List;

import org.sonatype.nexus.repository.manager.RepositoryManager;

import com.google.inject.AbstractModule;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Provide a mock RepositoryManager
 *
 * @since 3.0
 */
public class TestRepositoryManagerModule
    extends AbstractModule
{
  public static final List<String> NAMES = Arrays.asList("foo", "bar", "baz");

  @Override
  protected void configure() {
    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    when(repositoryManager.exists(anyString())).thenAnswer(invocation -> {
      String name = invocation.getArgument(0);
      return NAMES.stream().anyMatch(n -> n.equalsIgnoreCase(name));
    });
    bind(RepositoryManager.class).toInstance(repositoryManager);
  }
}
