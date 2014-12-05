package nano;

import fi.iki.elonen.NanoHTTPD;
import org.apache.http.StatusLine;

/**
 * Created by jscheller on 12/5/14.
 */
public class StatusAdapter implements NanoHTTPD.Response.IStatus {

    private final StatusLine status;

    public StatusAdapter(StatusLine status)
    {
        this.status=status;
    }

    @Override
    public int getRequestStatus() {
        return status.getStatusCode();
    }

    @Override
    public String getDescription() {
        return status.getStatusCode()+" "+status.getReasonPhrase();
    }
}
