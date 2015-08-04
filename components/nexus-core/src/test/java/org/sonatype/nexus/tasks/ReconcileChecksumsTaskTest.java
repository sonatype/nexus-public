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
package org.sonatype.nexus.tasks;

import org.sonatype.nexus.AbstractMavenRepoContentTests;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.attributes.inspectors.DigestCalculatingInspector;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.MUtils;
import org.sonatype.nexus.scheduling.NexusScheduler;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ReconcileChecksumsTaskTest
    extends AbstractMavenRepoContentTests
{
  private NexusScheduler nexusScheduler;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    nexusScheduler = lookup(NexusScheduler.class);
  }

  @Override
  protected boolean runWithSecurityDisabled() {
    return true;
  }

  @Test
  public void testChecksumsAreReconciled() throws Exception {

    fillInRepo();

    final ResourceStoreRequest metadataXML = new ResourceStoreRequest(
        "org/sonatype/nexus-8226/0.1-SNAPSHOT/maven-metadata.xml");
    final ResourceStoreRequest metadataSHA1 = new ResourceStoreRequest(
        "org/sonatype/nexus-8226/0.1-SNAPSHOT/maven-metadata.xml.sha1");
    final ResourceStoreRequest metadataMD5 = new ResourceStoreRequest(
        "org/sonatype/nexus-8226/0.1-SNAPSHOT/maven-metadata.xml.md5");

    StorageItem item;
    String sha1sum, md5sum;

    item = snapshots.retrieveItem(metadataXML);

    sha1sum = item.getRepositoryItemAttributes().get(DigestCalculatingInspector.DIGEST_SHA1_KEY);
    md5sum = item.getRepositoryItemAttributes().get(DigestCalculatingInspector.DIGEST_MD5_KEY);

    // confirm attributes contain checksums that need to be reconciled
    assertThat(sha1sum, is("18da84408c713e55a3c55dc34a1ccff78086fd46"));
    assertThat(md5sum, is("ec91980b1f83604ed72d749eebacea53"));

    // confirm associated checksum files also need to be reconciled
    assertThat(MUtils.readDigestFromFileItem((StorageFileItem) snapshots.retrieveItem(metadataSHA1)),
        is("18da84408c713e55a3c55dc34a1ccff78086fd46"));
    assertThat(MUtils.readDigestFromFileItem((StorageFileItem) snapshots.retrieveItem(metadataMD5)),
        is("ec91980b1f83604ed72d749eebacea53"));

    final ReconcileChecksumsTask task = nexusScheduler.createTaskInstance(ReconcileChecksumsTask.class);

    task.setRepositoryId(snapshots.getId());
    task.setResourceStorePath("/org/apache/");

    nexusScheduler.submit("testUnaffectedSubTree", task).get();

    item = snapshots.retrieveItem(metadataXML);

    sha1sum = item.getRepositoryItemAttributes().get(DigestCalculatingInspector.DIGEST_SHA1_KEY);
    md5sum = item.getRepositoryItemAttributes().get(DigestCalculatingInspector.DIGEST_MD5_KEY);

    // checksums still need to be reconciled
    assertThat(sha1sum, is("18da84408c713e55a3c55dc34a1ccff78086fd46"));
    assertThat(md5sum, is("ec91980b1f83604ed72d749eebacea53"));

    // associated checksum files still need to be reconciled
    assertThat(MUtils.readDigestFromFileItem((StorageFileItem) snapshots.retrieveItem(metadataSHA1)),
        is("18da84408c713e55a3c55dc34a1ccff78086fd46"));
    assertThat(MUtils.readDigestFromFileItem((StorageFileItem) snapshots.retrieveItem(metadataMD5)),
        is("ec91980b1f83604ed72d749eebacea53"));

    task.setResourceStorePath("/org/sonatype/");
    task.setModifiedSinceDate(DateTime.now().plusYears(1).toString("yyyy-MM-dd"));

    nexusScheduler.submit("testAffectedSubTree", task).get();

    item = snapshots.retrieveItem(metadataXML);

    sha1sum = item.getRepositoryItemAttributes().get(DigestCalculatingInspector.DIGEST_SHA1_KEY);
    md5sum = item.getRepositoryItemAttributes().get(DigestCalculatingInspector.DIGEST_MD5_KEY);

    // checksums still need to be reconciled
    assertThat(sha1sum, is("18da84408c713e55a3c55dc34a1ccff78086fd46"));
    assertThat(md5sum, is("ec91980b1f83604ed72d749eebacea53"));

    // associated checksum files still need to be reconciled
    assertThat(MUtils.readDigestFromFileItem((StorageFileItem) snapshots.retrieveItem(metadataSHA1)),
        is("18da84408c713e55a3c55dc34a1ccff78086fd46"));
    assertThat(MUtils.readDigestFromFileItem((StorageFileItem) snapshots.retrieveItem(metadataMD5)),
        is("ec91980b1f83604ed72d749eebacea53"));

    task.setModifiedSinceDate("2015-01-01");

    nexusScheduler.submit("testAffectedSubTree", task).get();

    item = snapshots.retrieveItem(metadataXML);

    sha1sum = item.getRepositoryItemAttributes().get(DigestCalculatingInspector.DIGEST_SHA1_KEY);
    md5sum = item.getRepositoryItemAttributes().get(DigestCalculatingInspector.DIGEST_MD5_KEY);

    // the checksums have now been reconciled
    assertThat(sha1sum, is("3a9b3324b9cf2a90bec1e4934cdcccacde7fd32c"));
    assertThat(md5sum, is("fc178b58edecae4b48f0195b703d7640"));

    // associated checksum files have now been reconciled
    assertThat(MUtils.readDigestFromFileItem((StorageFileItem) snapshots.retrieveItem(metadataSHA1)),
        is("3a9b3324b9cf2a90bec1e4934cdcccacde7fd32c"));
    assertThat(MUtils.readDigestFromFileItem((StorageFileItem) snapshots.retrieveItem(metadataMD5)),
        is("fc178b58edecae4b48f0195b703d7640"));
  }
}
