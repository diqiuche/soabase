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
package io.soabase.client;

import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.client.HttpClientConfiguration;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Environment;
import io.soabase.client.apache.WrappedHttpClient;
import io.soabase.client.jersey.JerseyRetryConnectorProvider;
import io.soabase.core.SoaBundle;
import io.soabase.core.SoaFeatures;
import io.soabase.core.features.client.RetryComponents;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.protocol.HttpContext;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import javax.ws.rs.client.Client;
import java.io.IOException;

public class ClientBuilder
{
    private final RetryComponents retryComponents;
    private final Environment environment;

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final boolean DEFAULT_RETRY_500s = true;

    public ClientBuilder(Environment environment)
    {
        this(environment, DEFAULT_MAX_RETRIES, DEFAULT_RETRY_500s);
    }

    public ClientBuilder(Environment environment, int maxRetries)
    {
        this(environment, maxRetries, DEFAULT_RETRY_500s);
    }

    public ClientBuilder(Environment environment, int maxRetries, boolean retry500s)
    {
        this.environment = environment;
        final SoaFeatures features = SoaBundle.getFeatures(environment);
        retryComponents = new RetryComponents(features.getDiscovery(), maxRetries, retry500s, features.getExecutorBuilder());

        if ( environment.getApplicationContext().getAttributes().getAttribute(ClientBuilder.class.getName()) == null )
        {
            AbstractBinder binder = new AbstractBinder()
            {
                @Override
                protected void configure()
                {
                    for ( String name : features.getNames(Client.class) )
                    {
                        bind(features.getNamed(Client.class, name)).named(name).to(Client.class);
                    }
                    for ( String name : features.getNames(HttpClient.class) )
                    {
                        bind(features.getNamed(HttpClient.class, name)).named(name).to(HttpClient.class);
                    }
                }
            };
            environment.jersey().register(binder);
            JerseyEnvironment adminJerseyEnvironment = features.getNamed(JerseyEnvironment.class, SoaFeatures.ADMIN_NAME);
            if ( adminJerseyEnvironment != null )
            {
                adminJerseyEnvironment.register(binder);
            }
            environment.getApplicationContext().getAttributes().setAttribute(ClientBuilder.class.getName(), "");
        }
    }

    public RetryComponents getRetryComponents()
    {
        return retryComponents;
    }

    public Client buildJerseyClient(JerseyClientConfiguration configuration, String clientName)
    {
        Client client = new JerseyClientBuilder(environment)
            .using(configuration)
            .using(new JerseyRetryConnectorProvider(retryComponents))
            .build(clientName);

        SoaBundle.getFeatures(environment).putNamed(client, Client.class, clientName);

        return client;
    }

    public HttpClient buildHttpClient(HttpClientConfiguration configuration, String clientName)
    {
        HttpRequestRetryHandler nullRetry = new HttpRequestRetryHandler()
        {
            @Override
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context)
            {
                return false;
            }
        };

        HttpClient httpClient = new HttpClientBuilder(environment)
            .using(configuration)
            .using(nullRetry)  // Apache's retry mechanism does not allow changing hosts. Do retries manually
            .build(clientName);
        HttpClient client = new WrappedHttpClient(httpClient, retryComponents);

        SoaBundle.getFeatures(environment).putNamed(client, HttpClient.class, clientName);

        return client;
    }
}
