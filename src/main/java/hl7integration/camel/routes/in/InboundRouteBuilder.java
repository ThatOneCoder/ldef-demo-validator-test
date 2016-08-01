package hl7integration.camel.routes.in;

import org.apache.camel.spring.SpringRouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InboundRouteBuilder extends SpringRouteBuilder {
    private static final Logger log = LoggerFactory.getLogger(InboundRouteBuilder.class);


    Processor processor = new Processor();
    String vHost = (System.getenv("VALIDATOR_SERVICE_HOST")== null) ? processor.getPropValues("endpoint-server") : System.getenv("VALIDATOR_SERVICE_HOST");
    String vPort = (System.getenv("VALIDATOR_SERVICE_PORT")== null) ? processor.getPropValues("endpoint-port") : System.getenv("VALIDATOR_SERVICE_PORT");

    @Override
    public void configure() throws Exception {

        from("mina:tcp://" + vHost + ":" + vPort + "?sync=true&codec=#hl7codec").routeId("Validator-Camel-Route")
                .to("bean:processor?method=processMessage")
                .to("bean:respondACK?method=process")
                .end();
    }
}
