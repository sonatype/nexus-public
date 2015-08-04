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
package org.sonatype.nexus.proxy.item.uid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.AbstractNexusTestEnvironment;
import org.sonatype.nexus.proxy.item.DefaultRepositoryItemUidFactory;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.maven.uid.IsMavenArtifactAttribute;
import org.sonatype.nexus.proxy.maven.uid.IsMavenArtifactSignatureAttribute;
import org.sonatype.nexus.proxy.maven.uid.IsMavenChecksumAttribute;
import org.sonatype.nexus.proxy.maven.uid.IsMavenPomAttribute;
import org.sonatype.nexus.proxy.maven.uid.IsMavenRepositoryMetadataAttribute;
import org.sonatype.nexus.proxy.maven.uid.IsMavenSnapshotArtifactAttribute;
import org.sonatype.nexus.proxy.repository.Repository;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Assert;
import org.junit.Test;

public class RepositoryItemUidAttributeManagerTest
    extends AbstractNexusTestEnvironment
{
  protected M2Repository repository;

  protected RepositoryItemUidAttributeManager repositoryItemUidAttributeManager;

  @SuppressWarnings("unchecked")
  protected List<Class<? extends Attribute<Boolean>>> coreAttributeClasses = Arrays.asList(
      IsHiddenAttribute.class, IsMetadataMaintainedAttribute.class);

  @SuppressWarnings("unchecked")
  protected List<Class<? extends Attribute<Boolean>>> mavenAttributeClasses = Arrays.asList(
      IsMavenArtifactAttribute.class, IsMavenSnapshotArtifactAttribute.class, IsMavenPomAttribute.class,
      IsMavenChecksumAttribute.class, IsMavenRepositoryMetadataAttribute.class,
      IsMavenArtifactSignatureAttribute.class);

  protected List<Class<? extends Attribute<Boolean>>> allAttributeClasses;

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();

    repository = (M2Repository) lookup(Repository.class, "maven2");

    CRepository repoConf = new DefaultCRepository();

    repoConf.setProviderRole(Repository.class.getName());
    repoConf.setProviderHint("maven2");
    repoConf.setId("repo");
    repoConf.setName("repo");
    Xpp3Dom ex = new Xpp3Dom("externalConfiguration");
    repoConf.setExternalConfiguration(ex);

    repository.configure(repoConf);

    repositoryItemUidAttributeManager = lookup(RepositoryItemUidAttributeManager.class);

    allAttributeClasses = new ArrayList<Class<? extends Attribute<Boolean>>>();
    allAttributeClasses.addAll(coreAttributeClasses);
    allAttributeClasses.addAll(mavenAttributeClasses);
  }

  @Test
  public void testCoreAttributes()
      throws Exception
  {
    DefaultRepositoryItemUidFactory factory = (DefaultRepositoryItemUidFactory) getRepositoryItemUidFactory();

    // two core uids
    RepositoryItemUid plain = factory.createUid(repository, "/some/plain/file.txt");
    validateAttributes(plain, IsMetadataMaintainedAttribute.class);

    RepositoryItemUid hidden = factory.createUid(repository, "/.nexus/hiddenPath.txt");
    validateAttributes(hidden, IsMetadataMaintainedAttribute.class, IsHiddenAttribute.class);

    RepositoryItemUid trashedFile = factory.createUid(repository, "/.nexus/trash/some/plain/fileInTrash.txt");
    validateAttributes(trashedFile, IsHiddenAttribute.class);
  }

  @Test
  public void testMavenAttributes()
      throws Exception
  {
    DefaultRepositoryItemUidFactory factory = (DefaultRepositoryItemUidFactory) getRepositoryItemUidFactory();

    // Maven UIDs (will move to plugin!)
    RepositoryItemUid releaseArtifactPom =
        factory.createUid(repository, "/groupId/artifactId/1.0/artifactId-1.0.pom");
    validateAttributes(releaseArtifactPom, IsMetadataMaintainedAttribute.class, IsMavenArtifactAttribute.class,
        IsMavenPomAttribute.class);

    RepositoryItemUid releaseArtifactPomChecksum =
        factory.createUid(repository, "/groupId/artifactId/1.0/artifactId-1.0.pom.sha1");
    validateAttributes(releaseArtifactPomChecksum, IsMetadataMaintainedAttribute.class,
        IsMavenChecksumAttribute.class);

    RepositoryItemUid releaseArtifactPomSignature =
        factory.createUid(repository, "/groupId/artifactId/1.0/artifactId-1.0.pom.asc");
    validateAttributes(releaseArtifactPomSignature, IsMetadataMaintainedAttribute.class,
        IsMavenArtifactSignatureAttribute.class);

    RepositoryItemUid releaseArtifactPomSignatureChecksum =
        factory.createUid(repository, "/groupId/artifactId/1.0/artifactId-1.0.pom.asc.sha1");
    validateAttributes(releaseArtifactPomSignatureChecksum, IsMetadataMaintainedAttribute.class,
        IsMavenChecksumAttribute.class);

    RepositoryItemUid releaseArtifactJar =
        factory.createUid(repository, "/groupId/artifactId/1.0/artifactId-1.0.jar");
    validateAttributes(releaseArtifactJar, IsMetadataMaintainedAttribute.class, IsMavenArtifactAttribute.class);

    RepositoryItemUid releaseArtifactJarChecksum =
        factory.createUid(repository, "/groupId/artifactId/1.0/artifactId-1.0.jar.sha1");
    validateAttributes(releaseArtifactJarChecksum, IsMetadataMaintainedAttribute.class,
        IsMavenChecksumAttribute.class);

    RepositoryItemUid releaseArtifactJarSignature =
        factory.createUid(repository, "/groupId/artifactId/1.0/artifactId-1.0.jar.asc");
    validateAttributes(releaseArtifactJarSignature, IsMetadataMaintainedAttribute.class,
        IsMavenArtifactSignatureAttribute.class);

    RepositoryItemUid releaseArtifactJarSignatureChecksum =
        factory.createUid(repository, "/groupId/artifactId/1.0/artifactId-1.0.jar.asc.sha1");
    validateAttributes(releaseArtifactJarSignatureChecksum, IsMetadataMaintainedAttribute.class,
        IsMavenChecksumAttribute.class);

    RepositoryItemUid snapshotArtifactPom =
        factory.createUid(repository, "/groupId/artifactId/1.0-SNAPSHOT/artifactId-1.0-SNAPSHOT.pom");
    validateAttributes(snapshotArtifactPom, IsMetadataMaintainedAttribute.class, IsMavenArtifactAttribute.class,
        IsMavenPomAttribute.class, IsMavenSnapshotArtifactAttribute.class);

    RepositoryItemUid snapshotArtifactPomChecksum =
        factory.createUid(repository, "/groupId/artifactId/1.0-SNAPSHOT/artifactId-1.0-SNAPSHOT.pom.sha1");
    validateAttributes(snapshotArtifactPomChecksum, IsMetadataMaintainedAttribute.class,
        IsMavenChecksumAttribute.class);

    RepositoryItemUid snapshotArtifactPomSignature =
        factory.createUid(repository, "/groupId/artifactId/1.0-SNAPSHOT/artifactId-1.0-SNAPSHOT.pom.asc");
    validateAttributes(snapshotArtifactPomSignature, IsMetadataMaintainedAttribute.class,
        IsMavenArtifactSignatureAttribute.class);

    RepositoryItemUid snapshotArtifactPomSignatureChecksum =
        factory.createUid(repository, "/groupId/artifactId/1.0-SNAPSHOT/artifactId-1.0-SNAPSHOT.pom.asc.sha1");
    validateAttributes(snapshotArtifactPomSignatureChecksum, IsMetadataMaintainedAttribute.class,
        IsMavenChecksumAttribute.class);

    RepositoryItemUid snapshotArtifactJar =
        factory.createUid(repository, "/groupId/artifactId/1.0-SNAPSHOT/artifactId-1.0-SNAPSHOT.jar");
    validateAttributes(snapshotArtifactJar, IsMetadataMaintainedAttribute.class, IsMavenArtifactAttribute.class,
        IsMavenSnapshotArtifactAttribute.class);

    RepositoryItemUid snapshotArtifactJarChecksum =
        factory.createUid(repository, "/groupId/artifactId/1.0-SNAPSHOT/artifactId-1.0-SNAPSHOT.jar.sha1");
    validateAttributes(snapshotArtifactJarChecksum, IsMetadataMaintainedAttribute.class,
        IsMavenChecksumAttribute.class);

    RepositoryItemUid snapshotArtifactJarSignature =
        factory.createUid(repository, "/groupId/artifactId/1.0-SNAPSHOT/artifactId-1.0-SNAPSHOT.jar.asc");
    validateAttributes(snapshotArtifactJarSignature, IsMetadataMaintainedAttribute.class,
        IsMavenArtifactSignatureAttribute.class);

    RepositoryItemUid snapshotArtifactJarSignatureChecksum =
        factory.createUid(repository, "/groupId/artifactId/1.0-SNAPSHOT/artifactId-1.0-SNAPSHOT.jar.asc.sha1");
    validateAttributes(snapshotArtifactJarSignatureChecksum, IsMetadataMaintainedAttribute.class,
        IsMavenChecksumAttribute.class);

    RepositoryItemUid mavenMetadata = factory.createUid(repository, "/groupId/artifactId/maven-metadata.xml");
    validateAttributes(mavenMetadata, IsMetadataMaintainedAttribute.class,
        IsMavenRepositoryMetadataAttribute.class);
    RepositoryItemUid mavenMetadataChecksum =
        factory.createUid(repository, "/groupId/artifactId/maven-metadata.xml.sha1");
    validateAttributes(mavenMetadataChecksum, IsMetadataMaintainedAttribute.class,
        IsMavenChecksumAttribute.class);
  }

  protected void validateAttributes(RepositoryItemUid uid,
                                    Class<? extends Attribute<Boolean>>... mustBePresentAndTrue)
  {
    List<Class<? extends Attribute<Boolean>>> mbpat = Arrays.asList(mustBePresentAndTrue);

    for (Class<?> clazz : allAttributeClasses) {
      Attribute<Boolean> boolAttr =
          repositoryItemUidAttributeManager.getAttribute((Class<Attribute<Boolean>>) clazz, uid);

      if (boolAttr == null) {
        Assert.assertFalse(mbpat.contains(clazz));
      }
      else {
        if (boolAttr.getValueFor(uid)) {
          Assert.assertTrue(clazz.toString() + " is true but should not be! UID: " + uid,
              mbpat.contains(clazz));
        }
        else {
          Assert.assertFalse(clazz.toString() + " is false but should not be! UID: " + uid,
              mbpat.contains(clazz));
        }
      }
    }
  }
}
