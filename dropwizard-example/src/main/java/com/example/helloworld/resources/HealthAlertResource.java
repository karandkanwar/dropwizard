package com.example.helloworld.resources;

import com.codahale.metrics.MetricRegistry;
import com.example.helloworld.db.HealthAlertDAO;
import com.example.helloworld.param.SleepAlertParam;
import com.google.common.collect.ImmutableList;
import io.dropwizard.hibernate.UnitOfWork;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;

@Path("/health-alerts")
@Produces(MediaType.APPLICATION_JSON)
public class HealthAlertResource {

    private final HealthAlertDAO healthAlertDAO;
    private final ZendeskTicketService zendeskTicketService;
    private int concurrentUsers;
    private int maxConcurrents;

    public HealthAlertResource(final HealthAlertDAO healthAlertDAO, ZendeskTicketService zendeskTicketService) {
        this.healthAlertDAO = healthAlertDAO;
        this.zendeskTicketService = zendeskTicketService;
        this.concurrentUsers = 0;
        this.maxConcurrents = 0;
    }

    @POST
    @Path("/sleep")
    @UnitOfWork
    public Response createSleepAlert(final SleepAlertParam sleepAlertParam) {
        // Log to concurrent accesses (so we can tell when this spikes)
        ++this.concurrentUsers;
        if (this.concurrentUsers > this.maxConcurrents){
            this.maxConcurrents = this.concurrentUsers;
            MetricRegistry.updateMetric(MAX_CONCURRENTS, this.maxConcurrents);
        }

        HealthAlert sleepAlert = new HealthAlert(
          sleepAlertParam.getCustomerID(),
          sleepAlertParam.getAlertDate(),
          sleepAlertParam.getConcernLevel(),
          HealthAlertType.SLEEP
        );

        healthAlertDAO.create(sleepAlert);

        // Check the concern level, and if it is immediate create ZD ticket
        if (sleepAlertParam.getConcernLevel() > ConcernLevel.IMMEDIATE) {
            zendeskTicketService.createHealthAlertTicketAsync(sleepAlert);
        } else{
            // Check other meal alerts from the past 30 days
            ImmutableList<Object> alerts = healthAlertDAO.findByTypeAndDate(HealthAlertType.SLEEP, LocalDate.now().minusDays(30));

            // Count alerts with MODERATE or higher (using ordinal for comparison) in that list
            final int moderateAlerts = alerts.stream()
              .filter(alert -> alert.getConcernLevel().oridinal() >= ConcernLevel.MODERATE.ordinal())
              .count();

            // If that exceeds the threshold, create ticket
            if (moderateAlerts > HealthAlert.MODERATE_ALERT_THRESHOLD) {
                zendeskTicketService.createHealthAlertTicketAsync(sleepAlert);
            } else {
                // if the AVERAGE concern level for these alerts is moderate or higher, also create an alert
                final int totalConcernLevel = alerts.stream()
                  .map(alert -> alert.getConcernLevel().ordinal())
                  .reduce(0, Integer::sum);

                final float avergageConcernLevel = (float) totalConcernLevel / alerts.size();
                
                if (avergageConcernLevel > (float) ConcernLevel.MODERATE.ordinal()) {
                    zendeskTicketService.createHealthAlertTicketAsync(sleepAlert);
                }
            }
        }

        // Subtract from concurrent accesses right before returning
        --this.concurrentUsers;
        return Response.ok("Sleep Alert successfully logged").build();
    }


}
