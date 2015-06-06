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
package io.soabase.sql.attributes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import io.soabase.core.SoaBundle;
import io.soabase.core.SoaFeatures;
import io.soabase.core.features.attributes.DynamicAttributes;
import io.soabase.core.features.attributes.DynamicAttributesFactory;
import org.apache.ibatis.session.SqlSession;
import org.hibernate.validator.constraints.NotEmpty;
import javax.validation.constraints.Min;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@JsonTypeName("sql")
public class SqlDynamicAttributesFactory implements DynamicAttributesFactory
{
    @Min(0)
    private int refreshPeriodSeconds = 30;

    @NotEmpty
    private String sessionName = SoaFeatures.DEFAULT_NAME;

    @JsonProperty("refreshPeriodSeconds")
    public int getRefreshPeriodSeconds()
    {
        return refreshPeriodSeconds;
    }

    @JsonProperty("refreshPeriodSeconds")
    public void setRefreshPeriodSeconds(int refreshPeriodSeconds)
    {
        this.refreshPeriodSeconds = refreshPeriodSeconds;
    }

    @JsonProperty("name")
    public String getSessionName()
    {
        return sessionName;
    }

    @JsonProperty("name")
    public void setSessionName(String sessionName)
    {
        this.sessionName = sessionName;
    }

    @Override
    public DynamicAttributes build(Configuration configuration, Environment environment, List<String> scopes)
    {
        SqlSession sqlSession = SoaBundle.getFeatures(environment).getNamedRequired(SqlSession.class, sessionName);

        final SqlDynamicAttributes dynamicAttributes = new SqlDynamicAttributes(sqlSession, scopes);
        ScheduledExecutorService service = environment.lifecycle().scheduledExecutorService("SqlDynamicAttributes-%d", true).build();
        Runnable command = new Runnable()
        {
            @Override
            public void run()
            {
                dynamicAttributes.update();
            }
        };
        service.scheduleAtFixedRate(command, refreshPeriodSeconds, refreshPeriodSeconds, TimeUnit.SECONDS);
        return dynamicAttributes;
    }
}
