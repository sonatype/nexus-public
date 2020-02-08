package org.sonatype.nexus.repository.rest.api;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.rest.api.model.AbstractApiRepository;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertThat;

public class RepositoriesApiResourceTest extends TestSupport {
    private static final String REPOSITORY_1_NAME = "docker-local";
    private static final Format REPOSITORY_1_FORMAT = new Format("docker") {};
    private static final HostedType REPOSITORY_1_TYPE = new HostedType();
    private static final String REPOSITORY_1_URL = "dockerUrl";
    private Repository REPOSITORY_1;
    private AbstractApiRepository ABSTRACT_API_REPOSITORY_1;

    @Mock
    private AuthorizingRepositoryManager authorizingRepositoryManager;

    @Mock
    private ApiRepositoryAdapter defaultAdapter;

    private Map<String, ApiRepositoryAdapter> convertersByFormat = Collections.emptyMap();

    private RepositoriesApiResource underTest;

    @Before
    public void setup() throws RepositoryNotFoundException {
        REPOSITORY_1 = createMockRepository(REPOSITORY_1_NAME, REPOSITORY_1_FORMAT, REPOSITORY_1_TYPE, REPOSITORY_1_URL, null);
        ABSTRACT_API_REPOSITORY_1 = createMockAbstractApiRepository();

        when(authorizingRepositoryManager.getRepository("docker-local")).thenReturn(REPOSITORY_1);
        when(defaultAdapter.adapt(REPOSITORY_1)).thenReturn(ABSTRACT_API_REPOSITORY_1);

        underTest = new RepositoriesApiResource(authorizingRepositoryManager, defaultAdapter, convertersByFormat);
    }

    @Test
    public void testGetRepository() {
        assertThat(underTest.getRepository(REPOSITORY_1_NAME), is(ABSTRACT_API_REPOSITORY_1));
    }

    private static Repository createMockRepository(final String name, final Format format, final Type type,
                                                   final String url, final String remoteUrl) {
        Repository repository = mock(Repository.class);
        when(repository.getName()).thenReturn(name);
        when(repository.getFormat()).thenReturn(format);
        when(repository.getType()).thenReturn(type);
        when(repository.getUrl()).thenReturn(url);
        Configuration configuration = mock(Configuration.class);
        if (type instanceof ProxyType) {
            when(configuration.attributes("proxy")).thenReturn(new NestedAttributesMap(
                    "proxy",
                    Collections.singletonMap(remoteUrl, remoteUrl)
            ));
        }
        when(repository.getConfiguration()).thenReturn(configuration);
        return repository;
    }

    private static AbstractApiRepository createMockAbstractApiRepository() {
        AbstractApiRepository repository = mock(AbstractApiRepository.class);
        return repository;
    }
}
