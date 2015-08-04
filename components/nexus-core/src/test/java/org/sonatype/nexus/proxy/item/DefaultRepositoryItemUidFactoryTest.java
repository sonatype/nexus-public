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
package org.sonatype.nexus.proxy.item;

import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.sisu.goodies.eventbus.EventBus;
import org.sonatype.sisu.litmus.testsupport.TestSupport;
import org.sonatype.sisu.locks.ResourceLockFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class DefaultRepositoryItemUidFactoryTest
    extends TestSupport
{
  @Mock
  private EventBus eventBus;

  @Mock
  private RepositoryRegistry repositoryRegistry;

  @Mock
  private ResourceLockFactory resourceLockFactory;

  @Mock
  private Repository repository;

  private DefaultRepositoryItemUidFactory subject;

  @Before
  public void prepare() {
    when(repository.getId()).thenReturn("repoid");
    subject = new DefaultRepositoryItemUidFactory(eventBus, repositoryRegistry, resourceLockFactory);
  }

  @Test
  public void createUidOk() {
    {
      final RepositoryItemUid uid = subject.createUid(repository, "/");
      assertThat(uid.getKey(), equalTo("repoid:/"));
    }
    {
      final RepositoryItemUid uid = subject.createUid(repository, "");
      assertThat(uid.getKey(), equalTo("repoid:/"));
    }
    {
      final RepositoryItemUid uid = subject.createUid(repository, "/foo/baz/");
      assertThat(uid.getKey(), equalTo("repoid:/foo/baz/"));
    }
    {
      final RepositoryItemUid uid = subject.createUid(repository, "foo/baz/");
      assertThat(uid.getKey(), equalTo("repoid:/foo/baz/"));
    }
    {
      final RepositoryItemUid uid = subject.createUid(repository, "/foo/baz/file.txt");
      assertThat(uid.getKey(), equalTo("repoid:/foo/baz/file.txt"));
    }
    {
      final RepositoryItemUid uid = subject.createUid(repository, "foo/baz/file.txt");
      assertThat(uid.getKey(), equalTo("repoid:/foo/baz/file.txt"));
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void createUidNotOk1() {
    subject.createUid(repository, "..");
  }

  @Test(expected = IllegalArgumentException.class)
  public void createUidNotOk2() {
    subject.createUid(repository, "a/b/../c/../../../");
  }
}
