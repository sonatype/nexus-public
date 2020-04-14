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
package org.sonatype.nexus.repository.cocoapods.internal.proxy;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.cocoapods.internal.pod.PodPathProvider;
import org.sonatype.nexus.repository.cocoapods.internal.pod.git.GitRepoUriParser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @since 3.19
 */
@Named
public class SpecTransformer
{
  private static final String POD_NAME_FIELD = "name";

  private static final String POD_VERSION_FIELD = "version";

  private static final String SOURCE_NODE_NAME = "source";

  private static final String GIT_NODE_NAME = "git";

  private static final String TAG_NODE_NAME = "tag";

  private static final String COMMIT_NODE_NAME = "commit";

  private static final String BRANCH_NODE_NAME = "branch";

  private static final String HTTP_NODE_NAME = "http";

  private static final ObjectMapper mapper = new ObjectMapper();

  private PodPathProvider podPathProvider;

  @Inject
  public SpecTransformer(final PodPathProvider podPathProvider) {
    this.podPathProvider = podPathProvider;
  }

  public String toProxiedSpec(final String specFile, final URI repoUri) throws InvalidSpecFileException {
    try {
      return transformSpec(specFile, repoUri);
    }
    catch (IOException ioe) {
      throw new InvalidSpecFileException(specFile, ioe);
    }
  }

  private String transformSpec(final String specFile, final URI repoUri) throws IOException, InvalidSpecFileException {
    ObjectNode jsonSpec = (ObjectNode) mapper.readTree(specFile);

    if (!jsonSpec.has(POD_NAME_FIELD)) {
      throw new InvalidSpecFileException("Spec file without Name");
    }
    if (!jsonSpec.has(POD_VERSION_FIELD)) {
      throw new InvalidSpecFileException("Spec file without Version");
    }
    if (!jsonSpec.has(SOURCE_NODE_NAME)) {
      throw new InvalidSpecFileException("Spec file without Source");
    }

    final String name = jsonSpec.get(POD_NAME_FIELD).asText();
    final String version = jsonSpec.get(POD_VERSION_FIELD).asText().trim();

    URI sourceUri = buidProxiedUri(jsonSpec.get(SOURCE_NODE_NAME), name, version, repoUri);

    final ObjectNode sourceNode = mapper.createObjectNode();
    sourceNode.put(HTTP_NODE_NAME, sourceUri.toString());

    jsonSpec.set(SOURCE_NODE_NAME, sourceNode);
    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonSpec);
  }

  private URI buidProxiedUri(final JsonNode sourceNode, String name, String version, final URI repoUri)
      throws InvalidSpecFileException
  {
    if (sourceNode.has(GIT_NODE_NAME)) {

      String gitRepo = sourceNode.get(GIT_NODE_NAME).textValue();
      if (gitRepo == null) {
        throw new InvalidSpecFileException("null repository Uri");
      }
      URI gitRepoUri = parseGitRepoUri(gitRepo);
      String ref = extractGitRef(sourceNode);

      return repoUri.resolve(podPathProvider.buildNxrmPath(name, version, gitRepoUri, ref));
    }

    if (sourceNode.has(HTTP_NODE_NAME)) {
      String httpDownloadUri = sourceNode.get(HTTP_NODE_NAME).textValue();
      if (httpDownloadUri == null) {
        throw new InvalidSpecFileException("null repository Uri");
      }
      return repoUri.resolve(podPathProvider.buildNxrmPath(name, version, httpDownloadUri));
    }

    throw new InvalidSpecFileException("Invalid source: " + sourceNode.toString());
  }

  @Nullable
  private String extractGitRef(final JsonNode sourceNode) {
    String[] refNodes = {COMMIT_NODE_NAME, TAG_NODE_NAME, BRANCH_NODE_NAME};
    return Arrays.stream(refNodes)
        .map(refNodeName -> sourceNode.get(refNodeName))
        .filter(refNode -> refNode != null && refNode.isTextual())
        .map(refNode -> refNode.textValue())
        .findFirst().orElse(null);
  }

  private URI parseGitRepoUri(String gitRepo) throws InvalidSpecFileException {
    URI gitRepoUri;
    try {
      gitRepoUri = new URI(gitRepo);
    }
    catch (URISyntaxException use) {
      throw new InvalidSpecFileException("invalid repository Uri: " + gitRepo, use);
    }

    if (gitRepoUri.getHost() == null || gitRepoUri.getPath() == null) {
      throw new InvalidSpecFileException("invalid repository Uri: " + gitRepo);
    }

    if (!GitRepoUriParser.isRepoSupported(gitRepoUri)) {
      throw new InvalidSpecFileException("Git repository not supported: " + gitRepo);
    }

    if (!GitRepoUriParser.isGitUriFormatSupported(gitRepoUri)) {
      throw new InvalidSpecFileException("invalid git repository Uri format:" + gitRepo);
    }
    return gitRepoUri;
  }
}
