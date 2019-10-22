package org.sonatype.nexus.repository.conda.internal.hosted.metadata
/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2018-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import groovy.json.JsonSlurper
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream

import java.util.stream.Collectors

class PackageInfo {
    //String activate.d
    boolean binary_prefix
    //deactivate.d
    String description
    String dev_url
    String doc_url
    String home
    String license
    boolean post_link
    boolean pre_link
    boolean post_unlink
    Object run_exports
    String source_url
    Set<String> subdirs
    String summary
    boolean text_prefix
    String version
}

class ChannelData {
    int channeldata_version
    Map<String, PackageInfo> packages
    Set<String> subdirs
}

class Info {
    String subdir
}

class PackageDesc {
    String arch
    String build
    int build_number
    List<String> depends
    String license
    String license_family
    String md5
    String name
    String sha256
    String noarch
    String platform
    long size
    String subdir
    long timestamp
    String version
}

class RepoData {
    Info info = new Info()
    Map<String, PackageDesc> packages = new HashMap<>()
    int repodata_version =1
    Object packages_conda
    List<String> removed
}

class MetaData {
    static String readIndexJson(InputStream input) throws IOException {
        TarArchiveInputStream tarInputStream = null
        try {
            tarInputStream = (TarArchiveInputStream) new ArchiveStreamFactory().createArchiveInputStream("tar", new BZip2CompressorInputStream(input))
            TarArchiveEntry entry
            while ((entry = (TarArchiveEntry) tarInputStream.getNextEntry()) != null) {
                if (entry.getName().equalsIgnoreCase("info/index.json")) {
                    break
                }
            }
            return entry != null ? readAsString(tarInputStream) : null
        } finally {
            tarInputStream?.close()
        }
    }

    static String readAsString(InputStream inputStream) throws IOException {
        BufferedReader br = null
        try {
            new BufferedReader(new InputStreamReader(inputStream, "utf-8"))
                    .lines()
                    .collect(Collectors.joining(System.lineSeparator()))
        } finally {
            br?.close()
        }
    }

    static PackageDesc asIndex(String json) {
        new JsonSlurper().parseText(json) as PackageDesc
    }

    static RepoData asRepoData(String json) {
        def cleanJson = json.replaceAll('packages.conda', 'packages_conda')
        new JsonSlurper().parseText(cleanJson) as RepoData
    }

    static ChannelData asChannelData(String json) {
        new JsonSlurper().parseText(json) as ChannelData
    }
}
