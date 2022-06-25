package com.smiddle.adpl.core.jtapi;

import com.cisco.jtapi.extensions.CiscoJtapiPeer;
import com.cisco.jtapi.extensions.CiscoJtapiProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.telephony.JtapiPeer;
import javax.telephony.JtapiPeerFactory;
import javax.telephony.JtapiPeerUnavailableException;
import javax.telephony.ProviderUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class ProviderUtil {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final static String BEAN = ProviderUtil.class.getSimpleName();
    private JtapiPeer peer;
    @Value("${provider.address}")
    private String ipAddress;
    @Value("${provider.login}")
    private String login;
    @Value("${provider.password}")
    private String password;

    @PostConstruct
    private void setUp(){
        logger.debug(BEAN, "Getting JTAPI peer...");
        try {
            this.peer = JtapiPeerFactory.getJtapiPeer("");
            CiscoJtapiProperties properties = ((CiscoJtapiPeer) peer).getJtapiProperties();
            properties.setUseFileTrace(false);
            properties.setUseJavaConsoleTrace(false);
        } catch (JtapiPeerUnavailableException e) {
            logger.error(BEAN, "Unable to get JTAPI peer. Exception message: {} ", e.toString());
            throw new RuntimeException(e);
        }
        logger.info(BEAN + "INITIALIZED");
    }

    public javax.telephony.Provider getProvider() throws ProviderUnavailableException{
        String connectionString = String.format("%s;login=%s;passwd=%s", ipAddress, login, password);
        return peer.getProvider(connectionString);
    }
}
