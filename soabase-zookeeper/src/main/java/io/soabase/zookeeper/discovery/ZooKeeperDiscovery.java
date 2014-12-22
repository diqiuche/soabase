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
package io.soabase.zookeeper.discovery;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.dropwizard.lifecycle.Managed;
import io.soabase.core.SoaInfo;
import io.soabase.core.features.discovery.ForcedState;
import io.soabase.core.features.discovery.HealthyState;
import io.soabase.core.features.discovery.SoaDiscovery;
import io.soabase.core.features.discovery.SoaDiscoveryInstance;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.x.discovery.InstanceFilter;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceInstanceBuilder;
import org.apache.curator.x.discovery.ServiceProvider;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

// TODO
public class ZooKeeperDiscovery extends CacheLoader<String, ServiceProvider<Payload>> implements SoaDiscovery, Managed, RemovalListener<String, ServiceProvider<Payload>>
{
    private final ServiceDiscovery<Payload> discovery;
    private final LoadingCache<String, ServiceProvider<Payload>> providers;
    private final AtomicReference<ServiceInstance<Payload>> us = new AtomicReference<>();
    private final AtomicReference<HealthyState> healthyState = new AtomicReference<>(HealthyState.HEALTHY);
    private final AtomicReference<ForcedState> forcedState = new AtomicReference<>(ForcedState.CLEARED);
    private final AtomicBoolean isRegistered = new AtomicBoolean(false);
    private final String bindAddress;
    private final SoaInfo soaInfo;

    public ZooKeeperDiscovery(CuratorFramework curator, ZooKeeperDiscoveryFactory factory, SoaInfo soaInfo)
    {
        this.soaInfo = soaInfo;
        bindAddress = factory.getBindAddress();
        providers = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)  // TODO config
            .removalListener(this)
            .build(this);

        try
        {
            Payload payload = new Payload(soaInfo.getAdminPort(), Maps.<String, String>newHashMap(), ForcedState.CLEARED, HealthyState.UNHEALTHY);  // initially unhealthy

            us.set(buildInstance(payload));

            discovery = ServiceDiscoveryBuilder
                .builder(Payload.class)
                .basePath(factory.getZookeeperPath())
                .client(curator)
                .build();
        }
        catch ( Exception e )
        {
            // TODO logging
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<String> getCurrentServiceNames()
    {
        try
        {
            // TODO - possibly cache this
            return discovery.queryForNames();
        }
        catch ( Exception e )
        {
            // TODO logging
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setHealthyState(HealthyState healthyState)
    {
        this.healthyState.set(healthyState);
        updateRegistration();
    }

    @Override
    public HealthyState getHealthyState()
    {
        return healthyState.get();
    }

    @Override
    public void setForcedState(ForcedState forcedState)
    {
        this.forcedState.set(forcedState);
        updateRegistration();
    }

    @Override
    public ForcedState getForcedState()
    {
        return forcedState.get();
    }

    @Override
    public ServiceProvider<Payload> load(String serviceName) throws Exception
    {
        InstanceFilter<Payload> filter = new InstanceFilter<Payload>()
        {
            @Override
            public boolean apply(ServiceInstance<Payload> instance)
            {
                Payload payload = instance.getPayload();
                if ( payload.getForcedState() == ForcedState.CLEARED )
                {
                    return (payload.getHealthyState() == HealthyState.HEALTHY);
                }
                return (payload.getForcedState() == ForcedState.REGISTER);
            }
        };
        ServiceProvider<Payload> provider = discovery
            .serviceProviderBuilder()
            .serviceName(serviceName)
            .additionalFilter(filter)
            .build();
        provider.start();
        return provider;
    }

    @Override
    public void onRemoval(RemovalNotification<String, ServiceProvider<Payload>> notification)
    {
        CloseableUtils.closeQuietly(notification.getValue());
    }

    @Override
    public Collection<SoaDiscoveryInstance> getAllInstances(String serviceName)
    {
        try
        {
            // TODO - validate service name
            ServiceProvider<Payload> provider = providers.get(serviceName);
            Collection<ServiceInstance<Payload>> allInstances = provider.getAllInstances();
            Iterable<SoaDiscoveryInstance> transformed = Iterables.transform(allInstances, new Function<ServiceInstance<Payload>, SoaDiscoveryInstance>()
            {
                @Nullable
                @Override
                public SoaDiscoveryInstance apply(ServiceInstance<Payload> instance)
                {
                    return toSoaInstance(instance);
                }
            });
            return Lists.newArrayList(transformed);
        }
        catch ( Exception e )
        {
            // TODO logging
            throw new RuntimeException(e);
        }
    }

    @Override
    public SoaDiscoveryInstance getInstance(String serviceName)
    {
        try
        {
            // TODO - validate service name
            ServiceProvider<Payload> provider = providers.get(serviceName);
            ServiceInstance<Payload> instance = provider.getInstance();
            return toSoaInstance(instance);
        }
        catch ( Exception e )
        {
            // TODO logging
            throw new RuntimeException(e);
        }
    }

    @Override
    public void noteError(String serviceName, final SoaDiscoveryInstance errorInstance)
    {
        ServiceProvider<Payload> provider = providers.getUnchecked(serviceName);
        if ( provider != null )
        {
            try
            {
                ServiceInstance<Payload> foundInstance = Iterables.find
                    (
                        provider.getAllInstances(),
                        new Predicate<ServiceInstance<Payload>>()
                        {
                            @Override
                            public boolean apply(ServiceInstance<Payload> instance)
                            {
                                if ( instance.getAddress().equals(errorInstance.getHost()) )
                                {
                                    //noinspection SimplifiableIfStatement
                                    if ( errorInstance.getPort() != 0 )
                                    {
                                        return errorInstance.isForceSsl() ? (errorInstance.getPort() == instance.getSslPort()) : (errorInstance.getPort() == instance.getPort());
                                    }
                                    return true;
                                }
                                return false;
                            }
                        },
                        null
                    );
                if ( foundInstance != null )
                {
                    provider.noteError(foundInstance);
                }
            }
            catch ( Exception e )
            {
                // TODO logging
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void start() throws Exception
    {
        discovery.start();
        if ( soaInfo.isRegisterInDiscovery() )
        {
            discovery.registerService(us.get());
        }
    }

    @Override
    public void stop() throws Exception
    {
        providers.invalidateAll();
        CloseableUtils.closeQuietly(discovery);
    }

    private SoaDiscoveryInstance toSoaInstance(ServiceInstance<Payload> instance)
    {
        Payload payload = instance.getPayload();
        int port = Objects.firstNonNull(instance.getPort(), Objects.firstNonNull(instance.getSslPort(), 0));
        return new SoaDiscoveryInstanceImpl(instance.getAddress(), port, instance.getSslPort() != null, payload);
    }

    private void updateRegistration()
    {
        if ( !soaInfo.isRegisterInDiscovery() )
        {
            return;
        }

        Payload currentPayload = us.get().getPayload();
        Payload newPayload = new Payload(currentPayload.getAdminPort(), currentPayload.getMetaData(), forcedState.get(), healthyState.get());
        if ( !newPayload.equals(currentPayload) )
        {
            try
            {
                us.set(buildInstance(newPayload));
                discovery.updateService(us.get());
            }
            catch ( Exception e )
            {
                // TODO logging
                throw new RuntimeException(e);
            }
        }
    }

    private ServiceInstance<Payload> buildInstance(Payload payload) throws Exception
    {
        ServiceInstanceBuilder<Payload> builder = ServiceInstance.<Payload>builder()
            .name(soaInfo.getServiceName())
            .payload(payload)
            .port(soaInfo.getMainPort());
        if ( bindAddress != null )
        {
            builder = builder.address(bindAddress);
        }
        return builder.build();
    }
}
