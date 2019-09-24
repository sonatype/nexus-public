package org.sonatype.nexus.repository.pypi.internal;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Container for URL PEP 503 compatible urls.
 */
final class PyPiLink {
    private final String file;

    private final String link;

    private final String dataPythonRequires;

    PyPiLink(@Nonnull final String file, @Nonnull final String link, @Nonnull final String dataPythonRequires) {
        this.file = checkNotNull(file);
        this.link = checkNotNull(link);
        this.dataPythonRequires = dataPythonRequires;
    }

    PyPiLink(@Nonnull final String file, @Nonnull final String link) {
        this(file, link, "");
    }

    public String getLink() {
        return link;
    }

    public String getFile() {
        return file;
    }

    public String getDataPythonRequires() {
        return dataPythonRequires;
    }
}
