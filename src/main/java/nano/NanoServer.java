package nano;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.ServerRunner;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;
import java.util.Random;

/**
 * Created by jscheller on 12/5/14.
 */
public class NanoServer extends NanoHTTPD {




    private static Logger log = LoggerFactory.getLogger(NanoServer.class);

    private static final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();



    private static final HttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();

    private static final CloseableHttpClient client=HttpClients.createDefault();
    private static final RequestConfig requestConfig = RequestConfig.custom()
            .setSocketTimeout(1000)
            .setConnectTimeout(150)
            .build();

    public NanoServer() {
        super(15120);
    }

    private final Random rand=new Random();

    private HttpGet createRequest(String endpoint)
    {
        HttpGet httpget=null;
        if(rand.nextBoolean())
        {
            System.out.print("Relay to #1");
            httpget = new HttpGet("http://ec2-54-173-151-187.compute-1.amazonaws.com:15120"+endpoint);
        }
        else
        {
            System.out.print("Relay to #2");
            httpget = new HttpGet("http://ec2-54-173-25-92.compute-1.amazonaws.com:15120"+endpoint);
        }
        httpget.setConfig(requestConfig);
        return httpget;
    }

    private final static InputStream str=null;

    @Override public Response serve(IHTTPSession session)  {

        try {
            String uri = session.getUri();
            System.out.println("Received: "+uri);

            HttpGet httpget = createRequest(session.getUri());
            CloseableHttpResponse response1 = null;
            try {
                response1 = client.execute(httpget);
                //HttpResponse response1 = httpClient.execute(httpget);
            } catch (ConnectTimeoutException timoutException) {
                System.out.println("Timeout exception");
                return new NanoHTTPD.Response(Response.Status.NO_CONTENT,MIME_HTML, "");

            }

            try {
                if(response1.getStatusLine().getStatusCode()!=200)
                {
                    System.out.println("Bidder returned non-success code: " + response1.getStatusLine().toString());
                }
                StatusAdapter adap=new StatusAdapter(response1.getStatusLine());

                System.out.println("Returning code: "+response1.getStatusLine());
                Response resp=new NanoHTTPD.Response(Response.Status.OK,"text/plain",str);
                for(Map.Entry<String,String> header:session.getHeaders().entrySet())
                {
                    resp.addHeader(header.getKey(),header.getValue());
                }

                return new NanoHTTPD.Response(new StatusAdapter(response1.getStatusLine()), MIME_HTML, "GOOD");
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

    private static final String[] hosts = new String[]{"ec2-54-173-151-187.compute-1.amazonaws.com", "ec2-54-173-25-92.compute-1.amazonaws.com"};
    private static final String[] endpoints = new String[]{"/adap","/altitude","/spotx"};

    private static HttpGet[][] requests;

    static{
        requests=new HttpGet[endpoints.length][hosts.length];
    }


    public static void main(String[] args) {
        ServerRunner.run(NanoServer.class);
    }
}