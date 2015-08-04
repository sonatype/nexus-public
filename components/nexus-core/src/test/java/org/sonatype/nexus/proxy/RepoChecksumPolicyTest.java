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
package org.sonatype.nexus.proxy;

import org.sonatype.jettytestsuite.ServletServer;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.maven.ChecksumContentValidator;
import org.sonatype.nexus.proxy.maven.ChecksumPolicy;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;

import org.junit.Assert;
import org.junit.Test;

public class RepoChecksumPolicyTest
    extends AbstractProxyTestEnvironment
{

  private static final String ITEM_WITH_WRONG_SHA1 =
      "/activemq-with-wrong-sha1/activemq-core/1.2/activemq-core-1.2.jar";

  private static final String ITEM = "/activemq-with-none/activemq-core/1.2/activemq-core-1.2.jar";

  private static final String ITEM_WITH_MD5 = "/activemq-with-md5/activemq-core/1.2/activemq-core-1.2.jar";

  private static final String ITEM_WITH_SHA1_AND_MD5 = "/activemq-with-all/activemq-core/1.2/activemq-core-1.2.jar";

  private M2TestsuiteEnvironmentBuilder jettyTestsuiteEnvironmentBuilder;

  @Override
  protected EnvironmentBuilder getEnvironmentBuilder()
      throws Exception
  {
    ServletServer ss = (ServletServer) lookup(ServletServer.ROLE);
    this.jettyTestsuiteEnvironmentBuilder = new M2TestsuiteEnvironmentBuilder(ss);
    return jettyTestsuiteEnvironmentBuilder;
  }

  protected M2Repository getRepository()
      throws Exception
  {
    return (M2Repository) getRepositoryRegistry().getRepository("checksumTestRepo");
  }

  public StorageFileItem requestWithPolicy(ChecksumPolicy policy, String requestPath)
      throws Exception
  {
    M2Repository repo = getRepository();

    ResourceStoreRequest request = new ResourceStoreRequest(requestPath, false, false);

    repo.setChecksumPolicy(policy);
    repo.getCurrentCoreConfiguration().commitChanges();

    StorageFileItem item = (StorageFileItem) repo.retrieveItem(request);

    return item;
  }

  private boolean cachedHashItem(final String itemPath, String suffix)
      throws Exception
  {
    final M2Repository repository = getRepository();
    try {
      AbstractStorageItem item =
          repository.getLocalStorage().retrieveItem(repository, new ResourceStoreRequest(itemPath, true, false));

      String attrname;
      if (ChecksumContentValidator.SUFFIX_SHA1.equals(suffix)) {
        attrname = ChecksumContentValidator.ATTR_REMOTE_SHA1;
      }
      else if (ChecksumContentValidator.SUFFIX_MD5.equals(suffix)) {
        attrname = ChecksumContentValidator.ATTR_REMOTE_MD5;
      }
      else {
        throw new IllegalArgumentException("Invalid checksum item suffix" + suffix);
      }

      return item.getRepositoryItemAttributes().containsKey(attrname);
    }
    catch (ItemNotFoundException e) {
      return false;
    }
  }

  @Test
  public void testPolicyIgnore()
      throws Exception
  {
    ChecksumPolicy policy = ChecksumPolicy.IGNORE;

    // it should simply pull all four without problem
    StorageFileItem file = requestWithPolicy(policy, ITEM_WITH_SHA1_AND_MD5);
    checkForFileAndMatchContents(file);
    // IGNORE: the req ignores checksum
    Assert.assertFalse(cachedHashItem(ITEM_WITH_SHA1_AND_MD5, ".sha1"));

    file = requestWithPolicy(policy, ITEM_WITH_MD5);
    checkForFileAndMatchContents(file);
    // IGNORE: the req ignores checksum
    Assert.assertFalse(cachedHashItem(ITEM_WITH_SHA1_AND_MD5, ".md5"));

    file = requestWithPolicy(policy, ITEM);
    checkForFileAndMatchContents(file);
    // IGNORE: the req ignores checksum
    Assert.assertFalse(cachedHashItem(ITEM, ".sha1"));

    file = requestWithPolicy(policy, ITEM_WITH_WRONG_SHA1);
    checkForFileAndMatchContents(file);
    // IGNORE: the req ignores checksum
    Assert.assertFalse(cachedHashItem(ITEM_WITH_WRONG_SHA1, ".sha1"));
  }

  @Test
  public void testPolicyWarn()
      throws Exception
  {
    ChecksumPolicy policy = ChecksumPolicy.WARN;

    // it should simply pull all four without problem
    StorageFileItem file = requestWithPolicy(policy, ITEM_WITH_SHA1_AND_MD5);
    checkForFileAndMatchContents(file);
    // WARN: the req implicitly gets the "best" checksum available implicitly
    Assert.assertTrue(cachedHashItem(ITEM_WITH_SHA1_AND_MD5, ".sha1"));

    file = requestWithPolicy(policy, ITEM_WITH_MD5);
    checkForFileAndMatchContents(file);
    // WARN: the req implicitly gets the "best" checksum available implicitly
    Assert.assertTrue(cachedHashItem(ITEM_WITH_MD5, ".md5"));

    file = requestWithPolicy(policy, ITEM);
    checkForFileAndMatchContents(file);
    // WARN: the req implicitly gets the "best" checksum available implicitly
    Assert.assertFalse(cachedHashItem(ITEM, ".sha1"));
    Assert.assertFalse(cachedHashItem(ITEM, ".md5"));

    file = requestWithPolicy(policy, ITEM_WITH_WRONG_SHA1);
    checkForFileAndMatchContents(file);
    // WARN: the req implicitly gets the "best" checksum available implicitly
    Assert.assertTrue(cachedHashItem(ITEM_WITH_WRONG_SHA1, ".sha1"));
  }

  @Test
  public void testPolicyStrictIfExists()
      throws Exception
  {
    ChecksumPolicy policy = ChecksumPolicy.STRICT_IF_EXISTS;

    // it should simply pull all four without problem
    StorageFileItem file = requestWithPolicy(policy, ITEM_WITH_SHA1_AND_MD5);
    checkForFileAndMatchContents(file);
    // STRICT_IF_EXISTS: the req implicitly gets the "best" checksum available implicitly
    Assert.assertTrue(cachedHashItem(ITEM_WITH_SHA1_AND_MD5, ".sha1"));

    file = requestWithPolicy(policy, ITEM_WITH_MD5);
    checkForFileAndMatchContents(file);
    // STRICT_IF_EXISTS: the req implicitly gets the "best" checksum available implicitly
    Assert.assertTrue(cachedHashItem(ITEM_WITH_MD5, ".md5"));

    file = requestWithPolicy(policy, ITEM);
    checkForFileAndMatchContents(file);
    // STRICT_IF_EXISTS: the req implicitly gets the "best" checksum available implicitly
    Assert.assertFalse(cachedHashItem(ITEM, ".sha1"));
    Assert.assertFalse(cachedHashItem(ITEM, ".md5"));

    try {
      file = requestWithPolicy(policy, ITEM_WITH_WRONG_SHA1);
      checkForFileAndMatchContents(file);
      // STRICT_IF_EXISTS: the req implicitly gets the "best" checksum available implicitly

      Assert.fail();
    }
    catch (ItemNotFoundException e) {
      // good
    }
    Assert.assertFalse(cachedHashItem(ITEM_WITH_WRONG_SHA1, ".sha1"));
  }

  @Test
  public void testPolicyStrict()
      throws Exception
  {
    ChecksumPolicy policy = ChecksumPolicy.STRICT;

    // it should simply pull all four without problem
    StorageFileItem file = requestWithPolicy(policy, ITEM_WITH_SHA1_AND_MD5);
    checkForFileAndMatchContents(file);
    // STRICT: the req implicitly gets the "best" checksum available implicitly
    Assert.assertTrue(cachedHashItem(ITEM_WITH_SHA1_AND_MD5, ".sha1"));

    file = requestWithPolicy(policy, ITEM_WITH_MD5);
    checkForFileAndMatchContents(file);
    // STRICT: the req implicitly gets the "best" checksum available implicitly
    Assert.assertTrue(cachedHashItem(ITEM_WITH_MD5, ".md5"));

    try {
      file = requestWithPolicy(policy, ITEM);
      checkForFileAndMatchContents(file);
      // STRICT: the req implicitly gets the "best" checksum available implicitly

      Assert.fail();
    }
    catch (ItemNotFoundException e) {
      // good
    }
    Assert.assertFalse(cachedHashItem(ITEM, ".sha1"));
    Assert.assertFalse(cachedHashItem(ITEM, ".md5"));

    try {
      file = requestWithPolicy(policy, ITEM_WITH_WRONG_SHA1);
      checkForFileAndMatchContents(file);
      // STRICT: the req implicitly gets the "best" checksum available implicitly

      Assert.fail();
    }
    catch (ItemNotFoundException e) {
      // good
    }
    Assert.assertFalse(cachedHashItem(ITEM_WITH_WRONG_SHA1, ".sha1"));
  }
}
