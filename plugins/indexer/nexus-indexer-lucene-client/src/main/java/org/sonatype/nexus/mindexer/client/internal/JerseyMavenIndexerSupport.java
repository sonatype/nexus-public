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
package org.sonatype.nexus.mindexer.client.internal;

import java.io.File;
import java.io.IOException;

import org.sonatype.nexus.client.core.spi.SubsystemSupport;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.mindexer.client.ClassnameQuery;
import org.sonatype.nexus.mindexer.client.KeywordQuery;
import org.sonatype.nexus.mindexer.client.MavenCoordinatesQuery;
import org.sonatype.nexus.mindexer.client.MavenIndexer;
import org.sonatype.nexus.mindexer.client.SearchRequest;
import org.sonatype.nexus.mindexer.client.SearchResponse;
import org.sonatype.nexus.mindexer.client.Sha1Query;

public abstract class JerseyMavenIndexerSupport
    extends SubsystemSupport<JerseyNexusClient>
    implements MavenIndexer
{

  public JerseyMavenIndexerSupport(final JerseyNexusClient client) {
    super(client);
  }

  @Override
  public SearchResponse identifyBySha1(final String sha1) {
    final Sha1Query query = new Sha1Query();
    query.setSha1(sha1);
    return search(new SearchRequest(query));
  }

  @Override
  public SearchResponse identifyBySha1(File file)
      throws IOException
  {
    final String sha1 = DigesterUtils.getSha1DigestAsString(file);
    return identifyBySha1(sha1);
  }

  @Override
  public SearchResponse searchByKeyword(String kw, String repositoryId) {
    final KeywordQuery query = new KeywordQuery();
    query.setKeyword(kw);
    return search(new SearchRequest(repositoryId, query));
  }

  @Override
  public SearchResponse searchByClassname(String className, String repositoryId) {
    final ClassnameQuery query = new ClassnameQuery();
    query.setClassname(className);
    return search(new SearchRequest(repositoryId, query));
  }

  @Override
  public SearchResponse searchByGAV(String groupId, String artifactId, String version, String classifier,
                                    String type, String repositoryId)
  {
    final MavenCoordinatesQuery query = new MavenCoordinatesQuery();
    query.setGroupId(groupId);
    query.setArtifactId(artifactId);
    query.setVersion(version);
    query.setClassifier(classifier);
    query.setType(type);
    return search(new SearchRequest(repositoryId, query));
  }
}
