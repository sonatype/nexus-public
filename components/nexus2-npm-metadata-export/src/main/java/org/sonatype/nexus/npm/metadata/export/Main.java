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
package org.sonatype.nexus.npm.metadata.export;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.engine.local.OEngineLocalPaginated;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ORecordBytes;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main
{
  private static final Logger log = LoggerFactory.getLogger(Main.class);

  private static final String LONG_OPT_ARG = "D";

  private static final String ORIENTDB_URL_ARG = "db.url";

  private static final String EXPORT_DIRECTORY_ARG = "export.directory";

  private static final String REPOSITORY_NAME_ARG = "repository.name";

  private static final String ABBREVIATED_PACKAGEROOT_ARG = "abbreviated.packageroot";

  private static final String NAME = "name";

  private static final String DIST_TAGS = "dist-tags";

  private static final String ORIENT_USERNAME = "admin";

  private static final String ORIENT_USER_PASSWORD = "admin";

  private static final ObjectMapper READ_MAPPER = new ObjectMapper(new SmileFactory());

  private static final ObjectMapper WRITE_MAPPER = new ObjectMapper();

  public static void main(String[] args) throws Exception {
    Properties arguments = parseArgs(args, LONG_OPT_ARG);
    printArgs(LONG_OPT_ARG, arguments);
    validateNotNullArgs(LONG_OPT_ARG, arguments,
        ORIENTDB_URL_ARG,
        EXPORT_DIRECTORY_ARG,
        REPOSITORY_NAME_ARG
    );
    validateArgs(arguments);
    orientConnectionPreparation(arguments);
    Instant start = Instant.now();

    String connectionURL = arguments.getProperty(ORIENTDB_URL_ARG);
    String repositoryName = arguments.getProperty(REPOSITORY_NAME_ARG);
    String exportDirectory = arguments.getProperty(EXPORT_DIRECTORY_ARG);
    boolean abbreviatedPackageroot = extractBooleanArgument(arguments, ABBREVIATED_PACKAGEROOT_ARG);

    try (ODatabaseDocumentTx db = new ODatabaseDocumentTx(connectionURL)
        .open(ORIENT_USERNAME, ORIENT_USER_PASSWORD)) {
      List<ODocument> packageRootResponse = db
          .query(new OSQLSynchQuery<>("select * from packageroot where repositoryId = ?"), repositoryName);
      for (ODocument oDocument : packageRootResponse) {
        safelyProcessPackageRoot(oDocument, exportDirectory, abbreviatedPackageroot);
      }
    }

    Instant end = Instant.now();
    log.info("Process has been finished. Time ~{} seconds.",
        TimeUnit.MILLISECONDS.toSeconds(end.toEpochMilli() - start.toEpochMilli()));
  }

  private static boolean extractBooleanArgument(Properties arguments, String argumentKey) {
    String argument = arguments.getProperty(argumentKey);
    if (arguments.containsKey(argumentKey) && StringUtils.isEmpty(argument)) {
      return true;
    }
    return Boolean.parseBoolean(argument);
  }

  private static Properties parseArgs(String[] args, String longOption) throws ParseException {
    Options options = new Options();
    Option propertyOption = Option.builder()
        .longOpt(longOption)
        .argName("property=value")
        .hasArgs()
        .valueSeparator()
        .numberOfArgs(2)
        .build();

    options.addOption(propertyOption);
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);

    return cmd.getOptionProperties(longOption);
  }

  private static void validateNotNullArgs(String longOption, Properties properties, String... argumentKeys) {
    for (String argumentKey : argumentKeys) {
      if (!properties.containsKey(argumentKey)) {
        throw new IllegalArgumentException(
            String.format("Missing argument. -%s", longOption + argumentKey)
        );
      }

      String argumentValue = properties.getProperty(argumentKey);
      if (StringUtils.isBlank(argumentValue)) {
        throw new IllegalArgumentException(
            String.format("Argument value is required. Actual -%s=%s", longOption + argumentKey, argumentValue)
        );
      }
    }
  }

  private static void validateArgs(Properties properties)
      throws FileNotFoundException
  {
    String exportDir = properties.getProperty(EXPORT_DIRECTORY_ARG);
    if (!Files.exists(Paths.get(exportDir))) {
      throw new FileNotFoundException(String.format("Export directory does not exist %s", exportDir));
    }
    if (!Files.isDirectory(Paths.get(exportDir))) {
      throw new FileNotFoundException(String.format("Is not a directory %s", exportDir));
    }
  }

  private static void printArgs(String longOption, Properties properties) {
    for (String argumentKey : properties.stringPropertyNames()) {
      String argumentValue = properties.getProperty(argumentKey);
      log.info("Running with argument -{}={}", longOption + argumentKey, argumentValue);
    }
  }

  private static void orientConnectionPreparation(Properties arguments) {
    String dbUrl = arguments.getProperty(ORIENTDB_URL_ARG);

    int index = dbUrl.indexOf(":");
    if (index <= 0) {
      log.info("No found OrientDB engine. Building OrientDB 'plocal' mode.");
      dbUrl = "plocal:" + dbUrl;
      arguments.setProperty(ORIENTDB_URL_ARG, dbUrl);
    }

    if (dbUrl.startsWith(OEngineLocalPaginated.NAME)) {
      System.err.println("\nRunning on Orient 'plocal' mode. Orient 'plocal' mode is a single connection mode.");
      System.err.println(String.format("URL = %s", dbUrl));
      System.err.println(
          "\nRequirements:\n1. Nexus Repository has to shutdown.\n2. OrientDB connection has to have no connections.");
      System.err.println("\nEnter:\n'YES' in case you've done the requirements\n'NO' to exist");
      try (Scanner scanner = new Scanner(System.in)) {
        while (true) {
          String line = scanner.nextLine();
          if (line.equalsIgnoreCase("yes")) {
            return;
          }
          if (line.equalsIgnoreCase("no")) {
            System.exit(0);
          }
        }
      }
    }
  }

  private static void safelyProcessPackageRoot(
      ODocument oDocument,
      String exportDirectory,
      boolean isAbbreviatedPackageRoot)
  {
    String rid = oDocument.field("@rid").toString();
    log.info("Processing {}", rid);
    try {
      JsonNode packageRoot = extractPackageRoot(oDocument, isAbbreviatedPackageRoot);
      String npmPackageId = oDocument.field("name");
      exportPackageRoot(npmPackageId, packageRoot, exportDirectory);
      log.info("Processed {}", rid);
    }
    catch (Exception e) {
      log.warn("Unable to populate metadata " + rid + ". Continuing.", e.getMessage(), e);
    }
  }

  private static JsonNode extractPackageRoot(ODocument oDocument, boolean isAbbreviatedPackageRoot) throws IOException {
    if (isAbbreviatedPackageRoot) {
      return buildAbbreviatedPackageRoot(oDocument);
    }
    return extractFullPackageRoot(oDocument);
  }

  private static ObjectNode extractFullPackageRoot(ODocument oDocument) throws IOException {
    ORecordBytes rawBytes = oDocument.field("raw");

    byte[] rawBytesArray = rawBytes.toStream();

    return READ_MAPPER.readValue(rawBytesArray, ObjectNode.class);
  }

  private static JsonNode buildAbbreviatedPackageRoot(ODocument oDocument) throws IOException {
    ObjectNode packageRoot = extractFullPackageRoot(oDocument);

    JsonNode nodeName = requireNonNullJsonNode(packageRoot, NAME);
    JsonNode nodeDistTags = requireNonNullJsonNode(packageRoot, DIST_TAGS);

    ObjectNode result = WRITE_MAPPER.createObjectNode();
    result.set(NAME, nodeName);
    result.set(DIST_TAGS, nodeDistTags);

    return result;
  }

  private static JsonNode requireNonNullJsonNode(ObjectNode source, String jsonNodeKey) {
    JsonNode jsonNode = source.get(jsonNodeKey);
    if (jsonNode == null) {
      throw new IllegalArgumentException(String.format("Json node '%s' is required.", jsonNodeKey));
    }
    return jsonNode;
  }

  private static void exportPackageRoot(
      String npmPackageId,
      JsonNode packageRoot,
      String exportDirectory) throws IOException
  {
    String json = WRITE_MAPPER.writeValueAsString(packageRoot);
    NpmPackageRootExporter.export(npmPackageId, json, exportDirectory);
  }
}
