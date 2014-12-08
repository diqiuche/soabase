package io.soabase.client.apache;

import com.google.common.base.Preconditions;
import io.soabase.client.Common;
import io.soabase.core.features.discovery.SoaDiscoveryInstance;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import java.io.IOException;

abstract class RetryLoop<T>
{
    HttpResponse run(WrappedHttpClient client, T original, HttpContext context, String originalHost) throws IOException
    {
        originalHost = Preconditions.checkNotNull(originalHost, "Request URI's host cannot be null");

        if ( context == null )
        {
            context = new BasicHttpContext();
        }

        HttpResponse response = null;
        int count = 0;
        boolean done = false;
        while ( !done )
        {
            SoaDiscoveryInstance instance = Common.hostToInstance(client.getDiscovery(), originalHost);
            try
            {
                ++count;
                response = execute(original, context, instance);
                if ( client.isRetry500s() )
                {
                    int status = response.getStatusLine().getStatusCode();
                    if ( (status >= 500) && (status <= 599) )
                    {
                        // TODO logging
                        throw new IOException("Bad status: " + status);
                    }
                }
                done = true;
            }
            catch ( IOException e )
            {
                client.getDiscovery().noteError(null, null);    // TODO
                if ( !client.getRetryHandler().retryRequest(e, count, context) )
                {
                    throw e;
                }
            }
        }
        return response;
    }

    protected RetryLoop()
    {
    }

    protected abstract HttpResponse execute(T original, HttpContext context, SoaDiscoveryInstance instance) throws IOException;
}