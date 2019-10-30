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
package org.sonatype.nexus.repository.conda.internal.hosted;

import com.google.common.collect.ImmutableList;
import org.sonatype.nexus.common.hash.HashAlgorithm;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.conda.internal.util.CondaPathUtils.REPODATA_JSON;

public class CondaPath {

    public enum HashType {
        SHA1("sha1", HashAlgorithm.SHA1),
        MD5("md5", HashAlgorithm.MD5);

        /**
         * {@link HashAlgorithm}s corresponding to {@link HashType}s.
         */
        public static final List<HashAlgorithm> ALGORITHMS = ImmutableList.of(SHA1.getHashAlgorithm(), MD5.getHashAlgorithm());

        private final String ext;

        private final HashAlgorithm hashAlgorithm;

        HashType(final String ext, final HashAlgorithm hashAlgorithm) {
            this.ext = ext;
            this.hashAlgorithm = hashAlgorithm;
        }

        public String getExt() {
            return ext;
        }

        public HashAlgorithm getHashAlgorithm() {
            return hashAlgorithm;
        }
    }


    public static class Coordinates {
        private final String packageName;

        private final String version;

        private final String buildString;

        private final String extension;


        public Coordinates(final String packageName,
                           final String version,
                           final String buildString,
                           final String extension) {
            this.packageName = checkNotNull(packageName);
            this.version = checkNotNull(version);
            this.buildString = checkNotNull(buildString);
            this.extension = checkNotNull(extension);
        }

        @Nonnull
        public String getPackageName() {
            return packageName;
        }

        @Nonnull
        public String getVersion() {
            return version;
        }

        @Nonnull
        public String getBuildString() {
            return buildString;
        }

        @Nonnull
        public String getExtension() {
            return extension;
        }
    }


    private final String path;

    private final String fileName;

    private final Optional<HashType> hashType;

    private final Optional<Coordinates> coordinates;

    public static CondaPath build(String path) {
        Optional<Coordinates> coordinates = parseCoordinates(path);
        checkNotNull(path);
        return new CondaPath(path, coordinates);
    }

    private CondaPath(final String path, final Optional<Coordinates> coordinates) {
        String currPath = path;
        if (path.startsWith("/")) {
            currPath = currPath.substring(1);
        }
        this.path = currPath;
        this.fileName = currPath.substring(currPath.lastIndexOf('/') + 1);

        this.hashType = Arrays.stream(CondaPath.HashType.values())
                .filter(hash -> this.fileName.endsWith("." + hash.getExt()))
                .findFirst();
        this.coordinates = coordinates;
    }

    @Nonnull
    public String getPath() {
        return path;
    }

    @Nonnull
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns hash type if this path points at Maven hash file, otherwise {@code null}.
     */
    @Nullable
    public Optional<HashType> getHashType() {
        return hashType;
    }

    /**
     * Returns the Maven coordinates if this path is an artifact path, otherwise {@code null}.
     */
    @Nullable
    public Optional<Coordinates> getCoordinates() {
        return coordinates;
    }

    /**
     * Returns {@code true} if this path is subordinate (is hash or signature) of another path.
     *
     * @see {@link #subordinateOf()}
     */
    public boolean isSubordinate() {
        return isHash();
    }

    /**
     * Returns {@code true} if this path represents a hash.
     */
    public boolean isHash() {
        return hashType.isPresent();
    }


    /**
     * Returns the "main", non-subordinate path of this path. The "main" path is never a hash nor a signature.
     */
    @Nonnull
    public CondaPath main() {
        CondaPath condaPath = this;
        while (condaPath.isSubordinate()) {
            condaPath = condaPath.subordinateOf();
        }
        return condaPath;
    }

    /**
     * Returns the "parent" path, that this path is subordinate of, or this instance if it is not a subordinate.
     */
    @Nonnull
    public CondaPath subordinateOf() {
        return hashType.map(
                currHash -> {
                    int hashSuffixLen = currHash.getExt().length() + 1; // the dot
                    Optional<Coordinates> mainCoordinates = coordinates.map(currCoordinates -> new Coordinates(
                            currCoordinates.getPackageName(),
                            currCoordinates.getVersion(),
                            currCoordinates.getBuildString(),
                            currCoordinates.getExtension().substring(0, currCoordinates.getExtension().length() - hashSuffixLen)
                    ));
                    String subPath = path.substring(0, path.length() - hashSuffixLen);
                    return new CondaPath(subPath, mainCoordinates);
                }
        ).orElse(this);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CondaPath)) {
            return false;
        }
        CondaPath that = (CondaPath) o;
        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "path='" + path + '\'' +
                ", fileName='" + fileName + '\'' +
                ", hashType=" + hashType +
                '}';
    }


    /**
     * Returns path of passed in hash type that is subordinate of this path. This path cannot be hash.
     */
    @Nonnull
    public CondaPath hash(final HashType hashType) {
        checkNotNull(hashType);
        checkArgument(this.hashType.isPresent(), "This path is already a hash: %s", this);
        Optional<Coordinates> hashCoordinates = coordinates.map(currCoordinates -> new Coordinates(
                currCoordinates.getPackageName(),
                currCoordinates.getVersion(),
                currCoordinates.getBuildString(),
                currCoordinates.getExtension() + "." + hashType.getExt()
        ));
        return new CondaPath(path + "." + hashType.getExt(), hashCoordinates);
    }

    private static Optional<Coordinates> parseCoordinates(String path) {

        if (path.endsWith(REPODATA_JSON)) {
            return Optional.empty();
        }
        int vEndPos = path.lastIndexOf('/');
        if (vEndPos == -1) {
            return Optional.empty();
        }

        final String fileName = path.substring(vEndPos + 1);
        String[] parts = fileName.split("-");

        String packageName = parts[0];
        String version = "";
        String build = "";
        String extension = "";
        if (parts.length > 1) {
            version = parts[1];
            build = parts[2].replace(".tar.bz2", "");
        }

        int nExtPos = fileName.lastIndexOf('.');
        if (fileName.endsWith(".tar.bz2")) {
            nExtPos -= 4;
        }

        extension = fileName.substring(nExtPos + 1);

        return Optional.of(new CondaPath.Coordinates(packageName, version, build, extension));
    }
}
