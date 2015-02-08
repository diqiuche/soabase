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

import com.google.common.collect.ImmutableList;
import com.google.inject.Key;
import com.google.inject.internal.UniqueAnnotations;
import javax.servlet.http.HttpServlet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// heavily copied from Guice Servlet
class ServletKeyBindingBuilderImpl implements ServletKeyBindingBuilder
{
    private final List<String> uriPatterns;
    private final JerseyGuiceModule module;

    ServletKeyBindingBuilderImpl(JerseyGuiceModule module, List<String> uriPatterns)
    {
        this.module = module;
        this.uriPatterns = ImmutableList.copyOf(uriPatterns);
    }

    public void with(Class<? extends HttpServlet> servletKey)
    {
        with(Key.get(servletKey));
    }

    public void with(Key<? extends HttpServlet> servletKey)
    {
        with(servletKey, new HashMap<String, String>());
    }

    public void with(HttpServlet servlet)
    {
        with(servlet, new HashMap<String, String>());
    }

    public void with(Class<? extends HttpServlet> servletKey, Map<String, String> initParams)
    {
        with(Key.get(servletKey), initParams);
    }

    public void with(Key<? extends HttpServlet> servletKey, Map<String, String> initParams)
    {
        with(servletKey, initParams, null);
    }

    public void with(HttpServlet servlet, Map<String, String> initParams)
    {
        Key<HttpServlet> servletKey = Key.get(HttpServlet.class, UniqueAnnotations.create());
        module.add(servletKey, servlet);
        with(servletKey, initParams, servlet);
    }

    private void with(Key<? extends HttpServlet> servletKey, Map<String, String> initParams, HttpServlet servletInstance)
    {
        module.add(new ServletDefinition(uriPatterns, servletKey, initParams, servletInstance));
    }
}