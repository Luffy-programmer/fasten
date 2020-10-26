package eu.fasten.analyzer.restapiplugin.api.mvn;

import javax.ws.rs.core.Response;

public interface CallableApiService {

    Response getPackageCallables(String package_name,
                                 String package_version,
                                 short offset,
                                 short limit);

    Response getCallableMetadata(String package_name,
                                 String package_version,
                                 String fasten_uri,
                                 short offset,
                                 short limit);
}