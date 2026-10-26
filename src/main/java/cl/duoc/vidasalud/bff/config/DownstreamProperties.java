package cl.duoc.vidasalud.bff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** URLs internas de los microservicios de dominio. */
@ConfigurationProperties(prefix = "vidasalud.downstream")
public class DownstreamProperties {

    private String appointments = "http://localhost:8081";
    private String catalog = "http://localhost:8082";
    private String report = "http://localhost:8083";
    private String audit = "http://localhost:8084";
    private String notifications = "http://localhost:8091";
    private String mq = "http://localhost:8090";

    public String getAppointments() {
        return appointments;
    }

    public void setAppointments(String appointments) {
        this.appointments = appointments;
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public String getReport() {
        return report;
    }

    public void setReport(String report) {
        this.report = report;
    }

    public String getAudit() {
        return audit;
    }

    public void setAudit(String audit) {
        this.audit = audit;
    }

    public String getNotifications() {
        return notifications;
    }

    public void setNotifications(String notifications) {
        this.notifications = notifications;
    }

    public String getMq() {
        return mq;
    }

    public void setMq(String mq) {
        this.mq = mq;
    }
}
