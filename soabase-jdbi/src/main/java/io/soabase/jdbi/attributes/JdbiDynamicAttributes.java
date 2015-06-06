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
package io.soabase.jdbi.attributes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import io.dropwizard.lifecycle.Managed;
import io.soabase.core.features.attributes.AttributeKey;
import io.soabase.core.features.attributes.DynamicAttributeListener;
import io.soabase.core.features.attributes.StandardAttributesContainer;
import io.soabase.core.features.attributes.WritableDynamicAttributes;
import io.soabase.core.listening.Listenable;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class JdbiDynamicAttributes implements WritableDynamicAttributes, Managed
{
    private final StandardAttributesContainer container;
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final AttributeEntityDao dao;

    public JdbiDynamicAttributes(DBI jdbi, List<String> scopes)
    {
        container = new StandardAttributesContainer(scopes);
        dao = jdbi.onDemand(AttributeEntityDao.class);
    }

    @Override
    public Map<AttributeKey, Object> getAll()
    {
        return container.getAll();
    }

    @Override
    public void remove(AttributeKey key)
    {
        AttributeEntity attribute = new AttributeEntity(key.getKey(), key.getScope(), "");  // value isn't used
        dao.delete(attribute.getfKEY(), attribute.getfSCOPE());
        update();
    }

    @Override
    public void put(AttributeKey key, Object value)
    {
        AttributeEntity attribute = new AttributeEntity(key.getKey(), key.getScope(), String.valueOf(value));
        if ( container.hasKey(key) )
        {
            dao.update(attribute.getfKEY(), attribute.getfSCOPE(), attribute.getfVALUE());
        }
        else
        {
            dao.insert(attribute.getfKEY(), attribute.getfSCOPE(), attribute.getfVALUE(), attribute.getfTIMESTAMP());
        }
        update();
    }

    @Override
    public String getAttribute(String key)
    {
        return container.getAttribute(key, null);
    }

    @Override
    public String getAttribute(String key, String defaultValue)
    {
        return container.getAttribute(key, defaultValue);
    }

    @Override
    public boolean getAttributeBoolean(String key)
    {
        return container.getAttributeBoolean(key, false);
    }

    @Override
    public boolean getAttributeBoolean(String key, boolean defaultValue)
    {
        return container.getAttributeBoolean(key, defaultValue);
    }

    @Override
    public int getAttributeInt(String key)
    {
        return container.getAttributeInt(key, 0);
    }

    @Override
    public int getAttributeInt(String key, int defaultValue)
    {
        return container.getAttributeInt(key, defaultValue);
    }

    @Override
    public long getAttributeLong(String key)
    {
        return container.getAttributeLong(key, 0);
    }

    @Override
    public long getAttributeLong(String key, long defaultValue)
    {
        return container.getAttributeLong(key, defaultValue);
    }

    @Override
    public double getAttributeDouble(String key, double defaultValue)
    {
        return container.getAttributeDouble(key, defaultValue);
    }

    @Override
    public double getAttributeDouble(String key)
    {
        return container.getAttributeDouble(key, 0.0);
    }

    @Override
    public void temporaryOverride(String key, boolean value)
    {
        container.temporaryOverride(key, value);
    }

    @Override
    public void temporaryOverride(String key, int value)
    {
        container.temporaryOverride(key, value);
    }

    @Override
    public void temporaryOverride(String key, long value)
    {
        container.temporaryOverride(key, value);
    }

    @Override
    public void temporaryOverride(String key, String value)
    {
        container.temporaryOverride(key, value);
    }

    @Override
    public void temporaryOverride(String key, double value)
    {
        container.temporaryOverride(key, value);
    }

    @Override
    public boolean removeOverride(String key)
    {
        return container.removeOverride(key);
    }

    @Override
    public Collection<String> getKeys()
    {
        return container.getKeys();
    }

    @Override
    public Listenable<DynamicAttributeListener> getListenable()
    {
        return container.getListenable();
    }

    @Override
    public void start() throws Exception
    {
        update();
    }

    @Override
    public void stop() throws Exception
    {
        // NOP
    }

    @VisibleForTesting
    AttributeEntityDao getDao()
    {
        return dao;
    }

    synchronized void update()
    {
        Map<AttributeKey, Object> newAttributes = Maps.newHashMap();
        try
        {
            for ( AttributeEntity entity : dao.selectAll() )
            {
                newAttributes.put(new AttributeKey(entity.getfKEY(), entity.getfSCOPE()), entity.getfVALUE());
            }
            container.reset(newAttributes);
        }
        catch ( Exception e )
        {
            log.error("Could not set new attributes", e);
        }
    }
}
