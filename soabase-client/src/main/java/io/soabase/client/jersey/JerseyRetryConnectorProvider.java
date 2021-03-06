/**
 * Copyright 2014 Jordan Zimmerman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.soabase.client.jersey;

import io.dropwizard.client.DropwizardApacheConnector;
import io.soabase.core.features.client.RetryComponents;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.client.spi.ConnectorProvider;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Configuration;

public class JerseyRetryConnectorProvider implements ConnectorProvider
{
    private final RetryComponents retryComponents;
    private final CloseableHttpClient apacheHttpClient;
    private final boolean isChunkedEncodingEnabled;

    public JerseyRetryConnectorProvider(RetryComponents retryComponents, CloseableHttpClient closeableHttpClient, boolean isChunkedEncodingEnabled)
    {
        this.retryComponents = retryComponents;
        this.apacheHttpClient = closeableHttpClient;
        this.isChunkedEncodingEnabled = isChunkedEncodingEnabled;
    }

    @Override
    public Connector getConnector(Client client, Configuration runtimeConfig)
    {
        DropwizardApacheConnector dropwizardApacheConnector = new DropwizardApacheConnector(apacheHttpClient, RequestConfig.custom().build(), isChunkedEncodingEnabled);
        return new JerseyRetryConnector(dropwizardApacheConnector, retryComponents);
    }
}
