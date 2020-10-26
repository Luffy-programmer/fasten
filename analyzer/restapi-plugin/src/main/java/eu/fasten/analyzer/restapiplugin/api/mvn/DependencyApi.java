package eu.fasten.analyzer.restapiplugin.api.mvn;

import eu.fasten.analyzer.restapiplugin.api.RestApplication;
import eu.fasten.analyzer.restapiplugin.api.mvn.impl.DependencyApiServiceImpl;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/mvn/packages")
@Produces(MediaType.TEXT_PLAIN)
public class DependencyApi {

    DependencyApiService service = new DependencyApiServiceImpl();

    @GET
    @Path("/{pkg}/{pkg_ver}/deps")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPackageDependencies(@PathParam("pkg") String package_name,
                                           @PathParam("pkg_ver") String package_version,
                                           @DefaultValue("0")
                                               @QueryParam("offset") short offset,
                                           @DefaultValue(RestApplication.DEFAULT_PAGE_SIZE)
                                               @QueryParam("limit") short limit) {
        return service.getPackageDependencies(package_name, package_version, offset, limit);
    }
}