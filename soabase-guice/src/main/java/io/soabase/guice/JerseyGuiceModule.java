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
package io.soabase.guice;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.internal.UniqueAnnotations;
import javax.servlet.Filter;
import javax.servlet.http.HttpServlet;
import java.util.List;

// heavily copied from Guice Servlet
public class JerseyGuiceModule extends AbstractModule
{
    private final List<FilterDefinition> filterDefinitions = Lists.newArrayList();
    private final List<ServletDefinition> servletDefinitions = Lists.newArrayList();

    protected final FilterKeyBindingBuilder filter(String... urlPatterns)
    {
        return new FilterKeyBindingBuilderImpl(this, Lists.newArrayList(urlPatterns));
    }

    protected final ServletKeyBindingBuilder serve(String... urlPatterns)
    {
        return new ServletKeyBindingBuilderImpl(this, Lists.newArrayList(urlPatterns));
    }

    @Override
    protected final void configure()
    {
        configureServlets();

        for ( FilterDefinition filterDefinition : filterDefinitions )
        {
            bind(FilterDefinition.class).annotatedWith(UniqueAnnotations.create()).toInstance(filterDefinition);
        }
        for ( ServletDefinition servletDefinition : servletDefinitions )
        {
            bind(ServletDefinition.class).annotatedWith(UniqueAnnotations.create()).toInstance(servletDefinition);
        }
    }

    /**
     * <h3>Servlet Mapping EDSL</h3>
     * <p/>
     * <p> Part of the EDSL builder language for configuring servlets
     * and filters with guice-servlet. Think of this as an in-code replacement for web.xml.
     * Filters and servlets are configured here using simple java method calls. Here is a typical
     * example of registering a filter when creating your Guice injector:
     * <p/>
     * <pre>
     *   Guice.createInjector(..., new ServletModule() {
     *
     *     {@literal @}Override
     *     protected void configureServlets() {
     *       <b>serve("*.html").with(MyServlet.class)</b>
     *     }
     *   }
     * </pre>
     * <p/>
     * This registers a servlet (subclass of {@code HttpServlet}) called {@code MyServlet} to service
     * any web pages ending in {@code .html}. You can also use a path-style syntax to register
     * servlets:
     * <p/>
     * <pre>
     *       <b>serve("/my/*").with(MyServlet.class)</b>
     * </pre>
     * <p/>
     * Every servlet (or filter) is required to be a singleton. If you cannot annotate the class
     * directly, you should add a separate {@code bind(..).in(Singleton.class)} rule elsewhere in
     * your module. Mapping a servlet that is bound under any other scope is an error.
     * <p/>
     * <p/>
     * <h3>Initialization Parameters</h3>
     * <p/>
     * Servlets (and filters) allow you to pass in init params
     * using the {@code <init-param>} tag in web.xml. You can similarly pass in parameters to
     * Servlets and filters registered in Guice-servlet using a {@link java.util.Map} of parameter
     * name/value pairs. For example, to initialize {@code MyServlet} with two parameters
     * ({@code name="Dhanji", site="google.com"}) you could write:
     * <p/>
     * <pre>
     *  Map&lt;String, String&gt; params = new HashMap&lt;String, String&gt;();
     *  params.put("name", "Dhanji");
     *  params.put("site", "google.com");
     *
     *  ...
     *      serve("/*").with(MyServlet.class, <b>params</b>)
     * </pre>
     * <p/>
     * <p/>
     * <h3>Binding Keys</h3>
     * <p/>
     * You can also bind keys rather than classes. This lets you hide
     * implementations with package-local visbility and expose them using
     * only a Guice module and an annotation:
     * <p/>
     * <pre>
     *  ...
     *      filter("/*").through(<b>Key.get(Filter.class, Fave.class)</b>);
     * </pre>
     * <p/>
     * Where {@code Filter.class} refers to the Servlet API interface and {@code Fave.class} is a
     * custom binding annotation. Elsewhere (in one of your own modules) you can bind this
     * filter's implementation:
     * <p/>
     * <pre>
     *   bind(Filter.class)<b>.annotatedWith(Fave.class)</b>.to(MyFilterImpl.class);
     * </pre>
     * <p/>
     * See {@link com.google.inject.Binder} for more information on binding syntax.
     */
    protected void configureServlets()
    {
    }

    void add(FilterDefinition filterDefinition)
    {
        filterDefinitions.add(filterDefinition);
    }

    void add(Key<Filter> key, Filter filter)
    {
        bind(key).toInstance(filter);
    }

    void add(ServletDefinition servletDefinition)
    {
        servletDefinitions.add(servletDefinition);
    }

    void add(Key<HttpServlet> key, HttpServlet servlet)
    {
        bind(key).toInstance(servlet);
    }
}
