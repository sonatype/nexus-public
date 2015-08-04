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
package org.sonatype.nexus.mime;

import java.io.IOException;
import java.util.List;

import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.StorageFileItem;

/**
 * A utility component for working with MIME type detection, either "without touching" (the content), that is
 * "best effort guess" based, or doing proper MIME type detection based on MIME magic database.
 *
 * @author cstamas
 * @since 2.0
 */
public interface MimeSupport
{
  /**
   * Makes a "guess" (usually based on file extension) about the MIME type that is most applicable to the given path
   * taking into consideration the requester MimeRulesSource MIME rules. When no "hard rule" present from
   * {@link MimeRulesSource}, this method falls back to {@link #guessMimeTypeFromPath(String)}. The "guess" is fast,
   * but not so precise as detection, where content is needed. This method should be used whenever a MIME type of a
   * file item that is contained <b>within</b> given context that has MimeRulesSource is about to be guessed.
   *
   * @param path to guess for.
   * @return the most applicable MIME type as String.
   */
  String guessMimeTypeFromPath(MimeRulesSource mimeRulesSource, String path);

  /**
   * Makes a "guess" (usually based on file extension) about the MIME type that is most applicable to the given path.
   * The "guess" is fast, but not so precise as detection, where content is needed.
   *
   * @param path to guess for.
   * @return the most applicable MIME type as String.
   */
  String guessMimeTypeFromPath(String path);

  /**
   * Makes a "guess" (based on file name and/or extension) about all the applicable MIME types for the given path. The
   * "guess" is fast, but not so precise as detection, where content is needed. The list of mime types are in
   * descending order (most specific 1st, least specific last).
   *
   * @param path to guess for.
   * @return the list of applicable mime types.
   * @since 2.8
   */
  List<String> guessMimeTypesListFromPath(final String path);

  /**
   * Performs a real MIME type detection by matching the "magic bytes" of a content to a known database. Is the most
   * precise way for detection but is costly since it does IO (reads several bytes up from content to perform
   * matching).
   *
   * @param fileItem to perform MIME magic matching against.
   * @return all of the applicable MIME types in relevance order (best fit first).
   * @throws IOException in case of IO problems.
   */
  String detectMimeTypesFromContent(StorageFileItem fileItem)
      throws IOException;

  /**
   * Performs a real MIME type detection by matching the "magic bytes" of a content only to a known database. Is the
   * most precise way for detection but is costly since it does IO (reads several bytes up from content to perform
   * matching). The list of mime types are in descending order (most specific 1st, least specific last). Depending
   * on your use case, you might take a peek at similar method {@link #detectMimeTypesFromContent(StorageFileItem)}
   * where content and file name is matched.
   *
   * @param content to perform MIME magic matching against.
   * @return all of the applicable MIME types in relevance order (best fit first).
   * @throws IOException in case of IO problems.
   * @since 2.8
   */
  List<String> detectMimeTypesListFromContent(ContentLocator content)
      throws IOException;

  /**
   * Performs a real MIME type detection by matching the "magic bytes" of a content and file name to a known database.
   * Is the most precise way for detection but is costly since it does IO (reads several bytes up from content to
   * perform matching). The list of mime types are in descending order (most specific 1st, least specific last). A word
   * of warning: the file name will be taken into account, that might lead to more "specific" hits (usually good), but
   * in certain cases might mislead too. For example, passing in a JAR file item will detect the content of
   * "application/zip" from content (as JAR file is basically a ZIP container file), and based on file name will figure
   * out a more specific MIME type of "application/java-archive", and this is good. But, on the other side, cases like
   * empty XML files (file content is several spaces, not an XML file actually) will be detected as "text/plain" file,
   * and file name will lead to more specific "text/xml" (that is specialization of "text/plain"). In short, if you are
   * interested in content matching only, use the other method {@link #detectMimeTypesListFromContent(ContentLocator)}.
   *
   * @param fileItem to perform MIME magic matching against.
   * @return all of the applicable MIME types in relevance order (best fit first).
   * @throws IOException in case of IO problems.
   * @since 2.8
   */
  List<String> detectMimeTypesListFromContent(StorageFileItem fileItem)
      throws IOException;
}
