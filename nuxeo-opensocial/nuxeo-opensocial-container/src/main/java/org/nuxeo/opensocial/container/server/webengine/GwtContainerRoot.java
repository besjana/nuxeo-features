package org.nuxeo.opensocial.container.server.webengine;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.webengine.gwt.GwtResource;
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * @author Stéphane Fourrier
 */
@WebObject(type="GwtContainerRoot")
public class GwtContainerRoot extends GwtResource {
    @GET @Produces("text/html")
    public Object getIndex() {
        return Response.status(404).build();
    }

    @GET
    @Path("browser")
    public Object doBrowse() throws ClientException {
//        log.debug(logPrefix + id);
        return newObject("browser");
    }
}