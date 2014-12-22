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
package io.soabase.admin;

import com.google.common.collect.Lists;
import io.dropwizard.Application;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.soabase.admin.components.ComponentManager;
import io.soabase.admin.components.TabComponent;
import io.soabase.admin.rest.PreferencesResource;
import io.soabase.config.ComposedConfiguration;
import io.soabase.core.config.FlexibleConfigurationSourceProvider;
import io.soabase.config.service.FromServices;
import io.soabase.core.SoaBundle;
import io.soabase.core.SoaFeatures;
import io.soabase.core.rest.DiscoveryApis;
import io.soabase.zookeeper.discovery.CuratorBundle;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import java.util.prefs.Preferences;

public class SoaAdminApp extends Application<SoaAdminConfiguration>
{
    @SuppressWarnings("ParameterCanBeLocal")
    public static void main(String[] args) throws Exception
    {
        System.setProperty("dw.soa.serviceName", "soabaseadmin");
        System.setProperty("dw.soa.addCorsFilter", "true");
        System.setProperty("dw.soa.registerInDiscovery", "false");
        System.setProperty("dw.server.rootPath", "/api/*");

        new SoaAdminApp().run(args);
    }

    @Override
    public void initialize(Bootstrap<SoaAdminConfiguration> bootstrap)
    {
        bootstrap.setConfigurationSourceProvider(new FlexibleConfigurationSourceProvider());

        ConfiguredBundle<SoaAdminConfiguration> bundle = new ConfiguredBundle<SoaAdminConfiguration>()
        {
            @Override
            public void run(SoaAdminConfiguration configuration, Environment environment) throws Exception
            {
                DefaultServerFactory factory = new DefaultServerFactory();
                factory.setAdminConnectors(Lists.<ConnectorFactory>newArrayList());
                configuration.setServerFactory(factory);
            }

            @Override
            public void initialize(Bootstrap<?> bootstrap)
            {
                // NOP
            }
        };
        bootstrap.addBundle(bundle);
        bootstrap.addBundle(new CuratorBundle<>());
        bootstrap.addBundle(new SoaBundle<>());
        bootstrap.addBundle(new AssetsBundle("/assets", "/assets"));
    }

    @Override
    public void run(SoaAdminConfiguration configuration, Environment environment) throws Exception
    {
        final ComponentManager componentManager = new ComponentManager(configuration.getAppName(), configuration.getCompany(), configuration.getFooterMessage());
        final Preferences preferences = Preferences.userRoot();
        AbstractBinder binder = new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bind(preferences).to(Preferences.class);
                bind(componentManager).to(ComponentManager.class);
            }
        };
        SoaBundle.getFeatures(environment).putNamed(componentManager, ComponentManager.class, SoaFeatures.DEFAULT_NAME);
        SoaBundle.getFeatures(environment).putNamed(preferences, Preferences.class, SoaFeatures.DEFAULT_NAME);

        componentManager.addTab(new TabComponent("", "Services", "assets/main.html", Lists.newArrayList("assets/js/main.js"), Lists.newArrayList("assets/css/main.css")));
        componentManager.addTab(new TabComponent("soa-attributes", "Attributes", "assets/attributes.html"));

        environment.servlets().addServlet("index", new IndexServlet(componentManager)).addMapping("");

        environment.jersey().register(binder);
        environment.jersey().register(DiscoveryApis.class);
        environment.jersey().register(PreferencesResource.class);
    }
}
