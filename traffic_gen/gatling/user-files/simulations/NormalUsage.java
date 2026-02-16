package simulations;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class NormalUsage extends Simulation {

    HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://localhost:8080")
            .acceptHeader("application/json, text/xml");

    ScenarioBuilder scn = scenario("Standard User Workflow")
            .exec(
                    http("1. Lister les fichiers (Root)")
                            // CORRECTION URL : admin -> admin_cyber
                            .httpRequest("PROPFIND", "/remote.php/dav/files/admin_cyber/")
                            .basicAuth("admin_cyber", "admin_pwd")
                            .check(status().is(207))
            )
            .pause(2)

            .exec(
                    http("2. Uploader un document de travail")
                            // CORRECTION URL : admin -> admin_cyber
                            .put("/remote.php/dav/files/admin_cyber/rapport_projet.txt")
                            .body(StringBody("Contenu confidentiel du projet Cyber..."))
                            .basicAuth("admin_cyber", "admin_pwd")
                            .check(status().in(201, 204))
            )
            .pause(1)

            .exec(
                    http("3. Relire le document (Download)")
                            // CORRECTION URL : admin -> admin_cyber
                            .get("/remote.php/dav/files/admin_cyber/rapport_projet.txt")
                            .basicAuth("admin_cyber", "admin_pwd")
                            .check(status().is(200))
            )
            .pause(2)

            .exec(
                    http("4. Supprimer le document")
                            // CORRECTION URL : admin -> admin_cyber
                            .delete("/remote.php/dav/files/admin_cyber/rapport_projet.txt")
                            .basicAuth("admin_cyber", "admin_pwd")
                            .check(status().is(204))
            );

    {
        setUp(
                scn.injectOpen(atOnceUsers(1))
        ).protocols(httpProtocol);
    }
}