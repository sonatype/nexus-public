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
package org.sonatype.nexus.proxy.storage.local.fs;

import java.io.File;

import org.sonatype.nexus.proxy.item.StorageFileItem;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.sonatype.sisu.goodies.testsupport.hamcrest.FileMatchers.exists;
import static org.sonatype.sisu.goodies.testsupport.hamcrest.FileMatchers.isFile;

public class DefaultFSPeerTest
{

  @Test
  public void hiddenTargetHandling()
      throws Exception
  {
    // test subject
    final DefaultFSPeer subject = new DefaultFSPeer();

    // repo base
    File repoBase = new File("target/repoId");

    // the file we want to store
    File target = new File(repoBase, "foo/1.0/foo-1.0.txt");
    target.getParentFile().mkdirs();

    final StorageFileItem file = Mockito.mock(StorageFileItem.class);
    Mockito.when(file.getPath()).thenReturn("/foo/1.0/foo-1.0.txt");
    Mockito.when(file.getParentPath()).thenReturn("/foo/1.0");

    // getting hidden target for target
    File hiddenTarget = subject.getHiddenTarget(null, repoBase, target, file);
    assertThat(hiddenTarget, notNullValue());
    assertThat(hiddenTarget, isFile());
    // startsWith, as garbage is appended to it's end
    assertThat(hiddenTarget.getName(), startsWith("foo-1.0.txt"));
    // contains, as OS path from root is prefixing this, and garbage at the end suffixing it

    assertThat(hiddenTarget.getPath(),
        containsString("target/repoId/.nexus/tmp/foo-1.0.txt".replace("/", File.separator)));

    // writing to hidden target is handled elsewhere, so we simulate content being written out
    final String PAYLOAD = "dummy payload";
    FileUtils.write(hiddenTarget, PAYLOAD);

    // handle the rename
    subject.handleRenameOperation(hiddenTarget, target);

    // hidden should cease to exist
    assertThat(hiddenTarget, not(exists()));
    // target should exists
    assertThat(target, exists());
    // name should be not garbaged anymore
    assertThat(target.getName(), equalTo("foo-1.0.txt"));
    // path prefixed by OS from root, no garbage at tail
    assertThat(target.getPath(), endsWith("target/repoId/foo/1.0/foo-1.0.txt".replace("/", File.separator)));
    // content is fine too
    assertThat(FileUtils.readFileToString(target), equalTo(PAYLOAD));
  }

  @Test
  public void hiddenTargetHandlingAtRoot()
      throws Exception
  {
    // test subject
    final DefaultFSPeer subject = new DefaultFSPeer();

    // repo base
    File repoBase = new File("target/repoId");

    // the file we want to store
    File target = new File(repoBase, "archetype-catalog.xml");
    target.getParentFile().mkdirs();

    final StorageFileItem file = Mockito.mock(StorageFileItem.class);
    Mockito.when(file.getPath()).thenReturn("/archetype-catalog.xml");
    Mockito.when(file.getParentPath()).thenReturn("/");

    // getting hidden target for target
    File hiddenTarget = subject.getHiddenTarget(null, repoBase, target, file);
    assertThat(hiddenTarget, notNullValue());
    assertThat(hiddenTarget, isFile());
    // startsWith, as garbage is appended to it's end
    assertThat(hiddenTarget.getName(), startsWith("archetype-catalog.xml"));
    // contains, as OS path from root is prefixing this, and garbage at the end suffixing it
    assertThat(hiddenTarget.getPath(),
        containsString("target/repoId/.nexus/tmp/archetype-catalog.xml".replace("/", File.separator)));

    // writing to hidden target is handled elsewhere, so we simulate content being written out
    final String PAYLOAD = "dummy payload";
    FileUtils.write(hiddenTarget, PAYLOAD);

    // handle the rename
    subject.handleRenameOperation(hiddenTarget, target);

    // hidden should cease to exist
    assertThat(hiddenTarget, not(exists()));
    // target should exists
    assertThat(target, exists());
    // name should be not garbaged anymore
    assertThat(target.getName(), equalTo("archetype-catalog.xml"));
    // path prefixed by OS from root, no garbage at tail
    assertThat(target.getPath(), endsWith("target/repoId/archetype-catalog.xml".replace("/", File.separator)));
    // content is fine too
    assertThat(FileUtils.readFileToString(target), equalTo(PAYLOAD));
  }

}
