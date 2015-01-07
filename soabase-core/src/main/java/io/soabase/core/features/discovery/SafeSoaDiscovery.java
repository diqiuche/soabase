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
package io.soabase.core.features.discovery;

import java.util.Collection;
import java.util.Map;

public class SafeSoaDiscovery implements SoaDiscovery
{
    private final SoaDiscovery implementation;

    public SafeSoaDiscovery(SoaDiscovery implementation)
    {
        this.implementation = implementation;
    }

    @Override
    public Collection<String> getServiceNames()
    {
        return implementation.getServiceNames();
    }

    @Override
    public SoaDiscoveryInstance getInstance(String serviceName)
    {
        return implementation.getInstance(serviceName);
    }

    @Override
    public Collection<SoaDiscoveryInstance> getAllInstances(String serviceName)
    {
        return implementation.getAllInstances(serviceName);
    }

    @Override
    public void noteError(String serviceName, SoaDiscoveryInstance errorInstance)
    {
        implementation.noteError(serviceName, errorInstance);
    }

    @Override
    public void setHealthyState(HealthyState healthyState)
    {
        implementation.setHealthyState(healthyState);
    }

    @Override
    public void setMetaData(Map<String, String> newMetaData)
    {
        implementation.setMetaData(newMetaData);
    }
}
