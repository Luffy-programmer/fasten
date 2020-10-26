package eu.fasten.analyzer.restapiplugin.api.mvn.impl;

import eu.fasten.analyzer.restapiplugin.RestAPIPlugin;
import eu.fasten.analyzer.restapiplugin.api.mvn.EdgeApiService;

import javax.ws.rs.core.Response;

public class EdgeApiServiceImpl implements EdgeApiService {

    @Override
    public Response getPackageEdges(String package_name,
                                    String package_version,
                                    short offset,
                                    short limit) {
        String result = RestAPIPlugin.RestAPIExtension.kbDao.getPackageEdges(
                package_name, package_version, offset, limit);
        return Response.status(200).entity(result).build();
    }
}