package it.eng.dome.invoicing.engine.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.eng.dome.invoicing.engine.tmf.TmfApiFactory;
import it.eng.dome.invoicing.observability.AbstractHealthService;
import it.eng.dome.invoicing.observability.health.Check;
import it.eng.dome.invoicing.observability.health.Health;
import it.eng.dome.invoicing.observability.health.HealthStatus;
import it.eng.dome.invoicing.tedb.TEDBClient;
import it.eng.dome.tmforum.tmf632.v4.api.OrganizationApi;

@Service
public class HealthService extends AbstractHealthService {

    private final Logger logger = LoggerFactory.getLogger(HealthService.class);

    @Autowired
    private TmfApiFactory tmfApiFactory;

    @Autowired
	private TEDBClient tedbClient;

    private OrganizationApi orgApi;

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        // create an API for each of the used TMF APIs ... or just one?
        this.orgApi = new OrganizationApi(tmfApiFactory.getTMF632PartyManagementApiClient());
    }

    public Health getHealth() {
        Health h = new Health();
        h.setDescription("Health for the Revenue Sharing Service");

        h.elevateStatus(HealthStatus.PASS);

        // check the invoicing service
        for(Check c: this.getTedbServiceCheck()) {
            h.addCheck(c);
        }

        // check the TMF APIs
        for(Check c: this.getTMFChecks())
            h.addCheck(c);

        // Status of its dependencies. In case of FAIL or WARN set it to WARN
        for(Check c: h.getAllChecks()) {
            h.elevateStatus(c.getStatus());
        }
        // ... but not to FAIL (which means a failure in the Revenue itself)
        if(HealthStatus.FAIL.equals(h.getStatus()))
            h.setStatus(HealthStatus.WARN);

        // now look at the internal status (may lead to PASS, WARN or FAIL)
        for(Check c: this.getChecksOnSelf()) {
            h.addCheck(c);
            h.elevateStatus(c.getStatus());
        }

        // add some notes
        h.setNotes(this.buildNotes(h));

        return h;
    }

    private List<Check> getChecksOnSelf() {
        List<Check> out = new ArrayList<>();
        Check self = new Check("self", "");
        // FIXME... do a proper check, if applicable.
        self.setStatus(HealthStatus.PASS);
        self.setTime(OffsetDateTime.now());
        self.setComponentType("component");
        out.add(self);
        return out;
    }

    private List<Check> getTedbServiceCheck() {

        List<Check> out = new ArrayList<>();

        Check connectivity = new Check("tedb-service", "connectivity");
        connectivity.setComponentType("external");
        connectivity.setTime(OffsetDateTime.now());

        try {
            tedbClient.getConfigurations();
            connectivity.setStatus(HealthStatus.PASS);
        }
        catch(Exception e) {
			logger.error(e.getMessage());
			logger.error(e.getStackTrace().toString());
            connectivity.setStatus(HealthStatus.FAIL);
            connectivity.setOutput(e.toString());
        }
        out.add(connectivity);

        // TODO: return more/better checks

        return out;
    }

    private List<Check> getTMFChecks() {

        List<Check> out = new ArrayList<>();

        Check connectivity = new Check("tmf-api", "connectivity");
        connectivity.setComponentType("external");
        connectivity.setTime(OffsetDateTime.now());

        try {
            orgApi.listOrganization(null, null, 5, null);
            connectivity.setStatus(HealthStatus.PASS);
        } catch(Exception e) {
            connectivity.setStatus(HealthStatus.FAIL);
            connectivity.setOutput(e.toString());
        }

        out.add(connectivity);

        // TODO: return more/better checks

        return out;
    }

    private List<String> buildNotes(Health hlt) {
        List<String> notes = new ArrayList<>();

        // first, some notes about the Service itself
        for(Check c: hlt.getChecks("self", null)) {
            switch(c.getStatus()) {
                case UNKNOWN:
                    notes.add("Revenue Invoicing Service is UNKNOWN. It might not behave as expected.");
                    break;
                case PASS:
                    break;
                case WARN:
                    notes.add("Revenue Invoicing Service has some internal troubles degrading its behaviour/performance");
                    break;
                case FAIL:
                    notes.add("Revenue Invoicing Service has some major internal troubles");
                    break;
                default:
                    notes.add("The Invoicing Service reported an unknown status: " + c.getStatus());
            }
        }

        // Then, some notes on its dependencies
        for(Check c: hlt.getChecks("tedb-service", null))
            if(c.getStatus().getSeverity()>HealthStatus.PASS.getSeverity())
                notes.add("Revenue Invoicing Service can't behave correctly because the TEDB Service is degraded or failing");
        for(Check c: hlt.getChecks("tmf-api", null))
            if(c.getStatus().getSeverity()>HealthStatus.PASS.getSeverity())
                notes.add("Revenue Sharing Service can't behave correctly because the TMF APIs are degraded or failing");

        return notes;
    }

}


	
