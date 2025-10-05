package it.eng.dome.invoicing.observability;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

import it.eng.dome.invoicing.observability.health.Health;
import it.eng.dome.invoicing.observability.info.Info;

@Service
public abstract class AbstractHealthService implements InitializingBean {

    @Autowired
    private BuildProperties buildProperties;

    @Override
    public void afterPropertiesSet() throws Exception {
    }

    public Info getInfo() {
        Info info = new Info();
        info.setName(buildProperties.getName());        
        info.setVersion(buildProperties.getVersion());
    	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        ZonedDateTime zonedDateTime = buildProperties.getTime().atZone(ZoneId.of("Europe/Rome"));
        info.setReleaseTime(zonedDateTime.format(formatter));
        return info;
    }

    /**
     * Fully implement this methodby adding Checks on internal status and dependencies, as well as notes and output.
     * For more information, see: https://datatracker.ietf.org/doc/html/draft-inadarei-api-health-check-06
     * @return
     */
    public abstract Health getHealth();
    /*
        Health h = new Health();
        h.setDescription("Health for " + buildProperties.getName());
        h.setStatus(HealthStatus.UNKNOWN);
        h.setReleaseId(buildProperties.getVersion());

        // TODO: fully overwrite this by adding CHecks on internal status and dependencies.
        // For more information, see: https://datatracker.ietf.org/doc/html/draft-inadarei-api-health-check-06

        return h;
    */

}


	
