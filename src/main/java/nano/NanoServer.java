package nano;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.ServerRunner;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.io.InputStream;

/**
 * Created by jscheller on 12/5/14.
 */
public class NanoServer extends NanoHTTPD {

    private static final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();

    private static final CloseableHttpClient client=HttpClients.createDefault();
    private static final RequestConfig requestConfig = RequestConfig.custom()
            .setSocketTimeout(1000)
            .setConnectTimeout(1000)
            .build();

    public NanoServer() {
        super(15120);
    }


    @Override public Response serve(IHTTPSession session)  {

        try {
            String uri = session.getUri();
            System.out.println( " '" + uri + "' ");

            HttpGet httpget = new HttpGet("http://ec2-54-173-151-187.compute-1.amazonaws.com:15120"+session.getUri());
            httpget.setConfig(requestConfig);
            CloseableHttpResponse response1 = client.execute(httpget);
            try {
                StatusAdapter adap=new StatusAdapter(response1.getStatusLine());
//                String type=response1.getEntity().getContentType().getValue();
//                HttpEntity ent=response1.getEntity().getContent();
                InputStream str=null;
                return new NanoHTTPD.Response(adap,"text/plain",str);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            finally {
                response1.close();
            }

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        return new NanoHTTPD.Response("ERROR");
    }


    public static void main(String[] args) {
        ServerRunner.run(NanoServer.class);
    }
}