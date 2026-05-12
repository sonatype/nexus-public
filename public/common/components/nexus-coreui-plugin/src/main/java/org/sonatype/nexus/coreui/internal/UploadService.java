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
package org.sonatype.nexus.coreui.internal;

import java.io.IOException;
import java.util.Collection;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.RepositoryCacheInvalidationService;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadManager;
import org.sonatype.nexus.repository.upload.UploadResponse;

import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import org.springframework.stereotype.Component;

/**
 * @since 3.7
 */
@Component
@Singleton
public class UploadService
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private UploadManager uploadManager;

  private RepositoryManager repositoryManager;

  private RepositoryCacheInvalidationService repositoryCacheInvalidationService;

  private static final String NPM_FORMAT = "npm";

  private static final String HELM_FORMAT = "helm";

  @Inject
  public UploadService(
      final RepositoryManager repositoryManager,
      final UploadManager uploadManager,
      final RepositoryCacheInvalidationService repositoryCacheInvalidationService)
  {
    this.uploadManager = checkNotNull(uploadManager);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.repositoryCacheInvalidationService = checkNotNull(repositoryCacheInvalidationService);
  }

  /**
   * Get a list of available definitions for upload.
   */
  public Collection<UploadDefinition> getAvailableDefinitions() {
    return uploadManager.getAvailableDefinitions();
  }

  /**
   * Perform an upload of assets
   *
   * @since 3.16
   *
   * @param repositoryName the repository to upload to
   * @param request a multipart form request
   * @return the query term for results in search (depending on upload could show additional results)
   * @throws IOException
   */
  public String upload(final String repositoryName, final HttpServletRequest request) throws IOException {
    checkNotNull(repositoryName);
    checkNotNull(request);

    Repository repository = checkNotNull(repositoryManager.get(repositoryName), "Specified repository is missing");

    UploadResponse uploadResponse = uploadManager.handle(repository, request);

    if (NPM_FORMAT.equals(repository.getFormat().getValue())) {
      repositoryManager.findContainingGroups(repositoryName)
          .forEach(groupRepoName -> repositoryCacheInvalidationService.processCachesInvalidation(
              repositoryManager.get(groupRepoName)));
    }

    return createSearchTerm(repository, uploadResponse.getAssetPaths());
  }

  /**
   * Create a search term from uploaded file paths by finding the common directory prefix
   * and converting it to a format-specific search term.
   *
   * @param repository the repository where files were uploaded
   * @param createdPaths collection of paths to uploaded files
   * @return format-specific search term, or null if no common prefix found
   */
  String createSearchTerm(final Repository repository, final Collection<String> createdPaths) {
    if (createdPaths.isEmpty()) {
      return null;
    }

    String pathPrefix = findLongestCommonPrefix(createdPaths);

    if (pathPrefix == null || pathPrefix.isEmpty()) {
      return null;
    }

    // The longest common prefix may be:
    // 1. A complete directory path: /org/example/myapp/1.0.0/ → use as-is (minus trailing /)
    // 2. A partial filename: /org/example/myapp/1.0.0/myapp-1.0.0 → strip to directory
    // 3. A complete filename: /org/example/myapp/1.0.0/myapp-1.0.0.jar → strip to directory
    //
    // To distinguish between a directory and a partial filename, check if all paths
    // have "/" immediately after the prefix. If yes, it's a directory. If no, it's a filename.
    String directoryPath;
    if (pathPrefix.endsWith("/")) {
      // Already a directory path, remove trailing slash
      directoryPath = pathPrefix.substring(0, pathPrefix.length() - 1);
    }
    else if (!pathPrefix.contains("/")) {
      // Edge case: prefix is just a simple name with no path separators
      directoryPath = pathPrefix;
    }
    else if (createdPaths.size() == 1) {
      // Single file: prefix is the complete filename, strip to directory
      directoryPath = removeLastSegment(pathPrefix);
    }
    else {
      // Multiple files: check if all paths have "/" immediately after the prefix (indicating it's a directory)
      boolean isDirectory = true;
      for (String path : createdPaths) {
        if (path.length() > pathPrefix.length()) {
          char nextChar = path.charAt(pathPrefix.length());
          if (nextChar != '/') {
            isDirectory = false;
            break;
          }
        }
      }

      if (isDirectory) {
        // Prefix is a directory, use as-is
        directoryPath = pathPrefix;
      }
      else {
        // Prefix is a partial filename, strip back to last /
        directoryPath = removeLastSegment(pathPrefix);
      }
    }

    // For formats with flat file structure (like Helm), directoryPath may be empty
    // In that case, use the original filename path prefix instead
    String format = repository.getFormat().getValue();
    if ((directoryPath == null || directoryPath.isEmpty()) && HELM_FORMAT.equalsIgnoreCase(format)) {
      // Helm uses flat structure, extract name from filename
      return convertPathToSearchTerm(format, pathPrefix);
    }

    if (directoryPath == null || directoryPath.isEmpty()) {
      return null;
    }

    // Convert path to format-specific search term
    return convertPathToSearchTerm(format, directoryPath);
  }

  /**
   * Convert path prefix to format-specific search term.
   *
   * For Maven: converts /group/artifact/version to group:artifact:version (GAVEC format)
   * For other formats: extracts simple name or returns path segments
   *
   * @param format the repository format (e.g., "maven2", "npm", "docker")
   * @param pathPrefix the directory path prefix from uploaded files
   * @return format-specific search term
   */
  String convertPathToSearchTerm(final String format, final String pathPrefix) {
    if ("maven2".equalsIgnoreCase(format)) {
      return convertMavenPathToGAV(pathPrefix);
    }

    // For Helm format with flat file structure like /chart-name-version.tgz,
    // extract the chart name from the filename
    if (HELM_FORMAT.equalsIgnoreCase(format)) {
      return convertHelmPathToName(pathPrefix);
    }

    // For other formats, extract name from path by removing leading/trailing slashes
    // and using the last meaningful segment
    String cleaned = pathPrefix.replaceAll("^/+", "").replaceAll("/+$", "");

    if (cleaned.isEmpty()) {
      return pathPrefix;
    }

    // For formats like npm, docker, etc., the path typically contains the package name
    // Return cleaned path segments joined by spaces for better keyword matching
    return cleaned.replace("/", " ");
  }

  /**
   * Convert Helm file path to chart name for search.
   *
   * Helm paths follow the pattern: /{chartName}-{version}.tgz
   * Example: /nginx-15.14.0.tgz converts to nginx
   * Example: /nginx-1.0.0-rc.1.tgz converts to nginx (handles pre-release versions)
   *
   * @param path the Helm file path
   * @return chart name, or the cleaned filename if pattern doesn't match
   */
  String convertHelmPathToName(final String path) {
    // Remove leading and trailing slashes
    String cleaned = path.replaceAll("^/+", "").replaceAll("/+$", "");

    if (cleaned.isEmpty()) {
      return path;
    }

    // Remove file extension (.tgz, .tar.gz, .prov)
    String nameWithVersion = cleaned.replaceAll("\\.(tgz|tar\\.gz|prov)$", "");

    // Extract chart name by removing version suffix (format: name-version)
    // Walk backwards through hyphens to find the start of a semantic version
    // This handles pre-release versions like 1.0.0-rc.1 where the last hyphen is part of the version
    int hyphenIndex = nameWithVersion.lastIndexOf('-');
    while (hyphenIndex > 0) {
      String potentialVersion = nameWithVersion.substring(hyphenIndex + 1);
      // Check if this segment starts with a digit followed by a dot (semver pattern like X.Y.Z)
      // This distinguishes version segments from chart name segments
      if (potentialVersion.matches("^\\d+\\.\\d.*")) {
        return nameWithVersion.substring(0, hyphenIndex);
      }
      // Move to the previous hyphen
      hyphenIndex = nameWithVersion.lastIndexOf('-', hyphenIndex - 1);
    }

    // Fallback: return the whole filename without extension
    return nameWithVersion;
  }

  /**
   * Convert Maven repository path to GAV format for search.
   *
   * Maven paths follow the pattern: /{groupId with slashes}/{artifactId}/{version}/
   * Example: /example/example/1.0.0/ converts to example:example:1.0.0
   *
   * @param path the Maven repository path (directory containing uploaded files)
   * @return GAV coordinates in format group:artifact:version, or fallback format for invalid paths
   */
  String convertMavenPathToGAV(final String path) {
    // Remove leading and trailing slashes
    String cleaned = path.replaceAll("^/+", "").replaceAll("/+$", "");

    if (cleaned.isEmpty()) {
      return path;
    }

    String[] segments = cleaned.split("/");

    if (segments.length < 3) {
      // Not enough segments for full GAV, return as-is
      return cleaned.replace("/", " ");
    }

    // Last segment is version
    String version = segments[segments.length - 1];

    // Second to last is artifactId
    String artifactId = segments[segments.length - 2];

    // Everything before that is groupId (replace / with .)
    StringBuilder groupId = new StringBuilder();
    for (int i = 0; i < segments.length - 2; i++) {
      if (i > 0) {
        groupId.append(".");
      }
      groupId.append(segments[i]);
    }

    // Return in GAVEC format: group:artifact:version
    return groupId.toString() + ":" + artifactId + ":" + version;
  }

  /**
   * Find the longest common prefix path among all created paths.
   */
  private String findLongestCommonPrefix(final Collection<String> createdPaths) {
    String prefix = Iterables.getFirst(createdPaths, null);

    for (String path : createdPaths) {
      prefix = longestPrefix(prefix, path);
    }

    return prefix;
  }

  private String removeLastSegment(final String path) {
    int index = path.lastIndexOf('/');
    if (index != -1) {
      return path.substring(0, index);
    }
    return path;
  }

  private String longestPrefix(final String prefix, final String path) {
    String result = prefix;
    while (result.length() > 0 && !path.startsWith(result)) {
      result = removeLastSegment(result);
    }
    return result;
  }
}
