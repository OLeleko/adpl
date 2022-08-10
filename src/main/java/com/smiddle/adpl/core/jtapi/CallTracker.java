package com.smiddle.adpl.core.jtapi;

import com.cisco.jtapi.extensions.CiscoCall;
import com.smiddle.adpl.core.service.RequestSendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.telephony.Address;
import javax.telephony.Provider;
import javax.telephony.ResourceUnavailableException;
import javax.telephony.callcontrol.CallControlCallObserver;
import javax.telephony.callcontrol.events.CallCtlConnDisconnectedEv;
import javax.telephony.callcontrol.events.CallCtlConnEstablishedEv;
import javax.telephony.callcontrol.events.CallCtlConnNetworkReachedEv;
import javax.telephony.events.CallEv;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
@Component
public class CallTracker implements CallControlCallObserver {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String newCall = "NewCall";
    private final String endCall = "EndCall";
    private final String newCallUrl = "/newcall";
    private final String endCallUrl = "/endcall";
    private final ProviderUtil providerUtil;
    private final Integer firstConnectCallEvent = 1;
    private final Integer secondConnectCallEvent = 2;
    private List<Address> addresses;
    private List<Integer> connectId;
    private List<Integer> disconnectId;

    private Map<Integer, Integer> callRegister;

    @Autowired
    private RequestSendService requestSendService;

    public CallTracker(ProviderUtil providerUtil) {
        this.providerUtil = providerUtil;
    }

    @PostConstruct
    public void start() {
        addresses = new ArrayList<>();
        callRegister = new HashMap<>();
        connectId = new ArrayList<>();
        disconnectId = new ArrayList<>();
        initProvider();
        addObservers();
        logger.info("INITIALIZED");
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting the provider down...");
        clearProvider();
    }

    private void addObservers() {
        logger.debug("Adding observers to the addresses...");
        for (Address address : addresses) {
            try {
                if (address != null) {
                    logger.debug("Observer added to the address: {}", address);
                    address.addCallObserver(this);
                }
            } catch (Exception e) {
                logger.error("Unable to assign route observer to address: {}. Exception message: {}", address, e.toString());
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void callChangedEvent(CallEv[] callEvs) {
        for (CallEv callEv : callEvs) {
            int globalCallID = ((CiscoCall) callEv.getCall()).getCallID().getGlobalCallID();
            logger.debug("Event: " + callEv.toString() + ".  GlobalCallID: " + globalCallID);
            if (callEv instanceof CallCtlConnDisconnectedEv) {
                disconnectCallEvent(callEv);
            }
            if (callEv instanceof CallCtlConnEstablishedEv || callEv instanceof CallCtlConnNetworkReachedEv) {
                connectCallEvent(callEv);
            }
        }
    }

    private void initProvider() {
        logger.info("Initializing provider...");
        try {
            Provider provider = providerUtil.getProvider();
            if (provider.getAddresses() != null && provider.getAddresses().length != 0) {
                addresses.addAll(Arrays.asList(provider.getAddresses()));
            }
        } catch (ResourceUnavailableException e) {
            throw new RuntimeException(e);
        }
        logger.debug("Provider initiated.");
    }

    private void clearProvider() {
        try {
            Provider provider = providerUtil.getProvider();
            if (provider.getAddresses() != null && provider.getAddresses().length != 0) {
                addresses.forEach(e -> e.removeCallObserver(this));
            }
            logger.debug("Call observer removed from provider");
            provider.shutdown();
        } catch (ResourceUnavailableException e) {
            logger.error("Unable to clear provider. Exception:", e.toString());
        }
        addresses.clear();
    }

    private void connectCallEvent(CallEv callEv) {
        String callingAddress = ((CiscoCall) callEv.getCall()).getCallingAddress().toString();
        String calledAddress = ((CiscoCall) callEv.getCall()).getCalledAddress().toString();
        int globalCallID = ((CiscoCall) callEv.getCall()).getCallID().getGlobalCallID();
        if (callingAddress.length() > calledAddress.length()) {
            if (!callRegister.containsKey(globalCallID)) {
                callRegister.put(globalCallID, firstConnectCallEvent);
                logger.debug("First ConnectCallEvent, calling:" + callingAddress + " called:" + calledAddress + " globalcallId:" + globalCallID);
            } else {
                callRegister.put(globalCallID, secondConnectCallEvent);
                logger.debug("Second ConnectCallEvent, calling:" + callingAddress + " called:" + calledAddress + " globalcallId:" + globalCallID);
                Map<String, String> mapContent = toMapContent(callingAddress, calledAddress, newCall);
                requestSendService.sendRequest(mapContent, newCallUrl);
                logger.debug("ConnectCallEvent, request content: " + mapContent);
            }
        }
    }

    private void disconnectCallEvent(CallEv callEv) {
        String callingAddress = ((CiscoCall) callEv.getCall()).getCallingAddress().toString();
        String calledAddress = ((CiscoCall) callEv.getCall()).getCalledAddress().toString();
        int globalCallID = ((CiscoCall) callEv.getCall()).getCallID().getGlobalCallID();
        if (callRegister.containsKey(globalCallID)) {
            Integer eventOrder = callRegister.get(globalCallID);
            logger.debug("First DisconnectCallEvent, globalCallID=" + globalCallID);
            if (eventOrder == secondConnectCallEvent) {
                Map<String, String> mapContent = toMapContent(callingAddress, calledAddress, endCall);
                requestSendService.sendRequest(mapContent, endCallUrl);
                logger.debug("DisconnectCallEvent, request content: " + mapContent);
                callRegister.remove(globalCallID);
            } else {
                callRegister.remove(globalCallID);
                logger.debug("Remove from callRegister globalCallID= " + globalCallID);
            }
        }
    }

    private Map<String, String> toMapContent(String callingAddress, String calledAddress, String callPhase){
        Map<String, String> mapContent = new HashMap<>();
        mapContent.put("OperatorID", calledAddress);
        mapContent.put("CallerID", callingAddress);
        mapContent.put("CallStatus", callPhase);
        return mapContent;
    }

    @Scheduled(initialDelay = 60 * 60 * 1000, fixedDelay = 60 * 60 * 1000)
    private void timeoutUpdate() {
        logger.debug("Scheduled provider update.");
        clearProvider();
        initProvider();
        addObservers();
    }

    @Scheduled(cron = "0 0 1 * * *")
    private void listsClear() {
        connectId.clear();
        disconnectId.clear();
        callRegister.clear();
    }
}
