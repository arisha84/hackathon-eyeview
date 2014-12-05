package nano;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.graphite.GraphiteSender;
import com.codahale.metrics.graphite.GraphiteUDP;
import com.codahale.metrics.graphite.PickledGraphite;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * Created by jscheller on 12/5/14.
 */
public class NanoServer extends NanoHTTPD {

    private static final String[] hosts = new String[]{"ec2-54-173-151-187.compute-1.amazonaws.com", "ec2-54-173-25-92.compute-1.amazonaws.com"};
    private static final String[] endpoints = new String[]{"/adap","/altitude","/spotx"};

    private static HttpGet[][] requests;

    static{
        requests=new HttpGet[endpoints.length][hosts.length];
    }


    private static final MetricRegistry metrics = new MetricRegistry();
    private static final GraphiteSender sender=new GraphiteUDP("ec2-54-174-161-66.compute-1.amazonaws.com",8125);
    private static GraphiteReporter reporter = GraphiteReporter.forRegistry(metrics).convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).build(sender);

    private static final Counter totalRequests = metrics.counter("rlb-hack.gingis.requests.total");
    private static Counter[] outboundRequests;
    private static final Counter bidderTimeouts = metrics.counter("rlb-hack.gingis.bidder.timeouts");
    private static final Timer bidderResponse = metrics.timer("rlb-hack.gingis.bidder.response_time");
    private static final Timer response = metrics.timer("rlb-hack.gingis.response_time");


    static{
        for(int ii=0;ii<hosts.length;ii++)
        {
            outboundRequests[ii]=metrics.counter("rlb-hack.gingis.outbound."+ii);
        }
    }

    private static Logger log = LoggerFactory.getLogger(NanoServer.class);

    private static final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();

    private static final CloseableHttpClient client=HttpClients.createDefault();
    private static final RequestConfig requestConfig = RequestConfig.custom()
            .setSocketTimeout(1000)
            .setConnectTimeout(1000)
            .build();

    public NanoServer() {
        super(15120);
        reporter.start(1, TimeUnit.SECONDS);
    }

    private final Random rand=new Random();

    private HttpGet createRequest(String endpoint)
    {
        HttpGet httpget=null;
        if(rand.nextBoolean())
        {
            System.out.print("Relay to #1");
            outboundRequests[0].inc();
            httpget = new HttpGet("http://ec2-54-173-151-187.compute-1.amazonaws.com:15120"+endpoint);
        }
        else
        {
            System.out.print("Relay to #2");
            outboundRequests[1].inc();
            httpget = new HttpGet("http://ec2-54-173-25-92.compute-1.amazonaws.com:15120"+endpoint);
        }
        httpget.setConfig(requestConfig);
        return httpget;
    }

    private final static InputStream str=null;

    @Override public Response serve(IHTTPSession session)  {
        totalRequests.inc();
        final Timer.Context reqCtx=response.time();

        try {
            String uri = session.getUri();
            System.out.println("Received: "+uri);

            HttpGet httpget = createRequest(session.getUri());
            final Timer.Context bidCtx = bidderResponse.time();
            CloseableHttpResponse response1 = client.execute(httpget);
            bidCtx.stop();

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

                reqCtx.close();
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

        reqCtx.stop();
        return new NanoHTTPD.Response("ERROR");
    }



    public static void main(String[] args) {
        ServerRunner.run(NanoServer.class);
    }
}