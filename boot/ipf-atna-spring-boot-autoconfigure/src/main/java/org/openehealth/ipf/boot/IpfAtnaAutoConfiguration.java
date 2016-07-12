package org.openehealth.ipf.boot;

import org.openhealthtools.ihe.atna.auditor.AuditorTLSConfig;
import org.openhealthtools.ihe.atna.auditor.IHEAuditor;
import org.openhealthtools.ihe.atna.auditor.context.AuditorModuleConfig;
import org.openhealthtools.ihe.atna.auditor.context.AuditorModuleContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 *
 */
@Configuration
@EnableConfigurationProperties(IpfAtnaConfigurationProperties.class)
public class IpfAtnaAutoConfiguration {

    public static final String ATNA_ITI20_SERVICE_NAME = "atna-iti20-service";

    @Autowired(required = false)
    private DiscoveryClient discoveryClient;

    @Value("${spring.application.name}")
    private String appName;

    @Bean
    @ConditionalOnMissingBean
    public AuditorModuleContext atnaAuditorModuleContext(IpfAtnaConfigurationProperties config)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        AuditorModuleContext auditorModuleContext = AuditorModuleContext.getContext();
        auditorModuleContext.setQueue(config.getAuditQueueClass().newInstance());
        return auditorModuleContext;
    }

    @Bean
    @ConfigurationProperties(prefix = "ipf.atna")
    @NotNull
    public AuditorModuleConfig atnaAuditorModuleConfig(AuditorModuleContext auditorModuleContext) {
        AuditorModuleConfig auditorModuleConfig = auditorModuleContext.getConfig();
        // Use app name as auditor source if not configured otherwise
        if (auditorModuleConfig.getAuditSourceId() == null) {
            auditorModuleConfig.setAuditSourceId(appName);
        }
        if (discoveryClient != null) {
            List<ServiceInstance> auditRepositories = discoveryClient.getInstances(ATNA_ITI20_SERVICE_NAME);
            if (!auditRepositories.isEmpty()) {
                ServiceInstance auditRepository = auditRepositories.get(0);
                auditorModuleConfig.setAuditRepositoryHost(auditRepository.getHost());
                auditorModuleConfig.setAuditRepositoryPort(auditRepository.getPort());
            }
        }
        return auditorModuleConfig;
    }

    @Bean
    @ConditionalOnProperty(value = "ipf.atna.auditor.enabled")
    @ConditionalOnMissingBean
    AuditorTLSConfig atnaAuditorTLSConfig(AuditorModuleConfig auditorModuleConfig, IpfAtnaConfigurationProperties config) {
        AuditorTLSConfig auditorTLSConfig = new AuditorTLSConfig(auditorModuleConfig);
        auditorTLSConfig.setSecurityDomainName(config.getSecurityDomainName());
        return auditorTLSConfig;
    }

    @Bean
    @ConditionalOnProperty(value = "ipf.atna.auditor.enabled")
    public IHEAuditor startStopAuditor(AuditorModuleConfig config) {
        IHEAuditor auditor = IHEAuditor.getAuditor();
        auditor.setConfig(config);
        return auditor;
    }

    @Bean
    @ConditionalOnProperty(value = "ipf.atna.auditor.enabled")
    @ConditionalOnMissingBean
    ApplicationStartEventListener applicationStartEventListener(@Qualifier("startStopAuditor") IHEAuditor auditor) {
        return new ApplicationStartEventListener(auditor);
    }

    @Bean
    @ConditionalOnProperty(value = "ipf.atna.auditor.enabled")
    @ConditionalOnMissingBean
    ApplicationStopEventListener applicationStopEventListener(@Qualifier("startStopAuditor") IHEAuditor auditor) {
        return new ApplicationStopEventListener(auditor);
    }

}
