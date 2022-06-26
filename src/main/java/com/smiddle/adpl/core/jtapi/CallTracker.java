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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
@Component
public class CallTracker implements CallControlCallObserver {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String newCall = "NewCall";
    private final String endCall = "EndCall";
    private final ProviderUtil providerUtil;
    private List<Address> addresses;
    private List<Integer> connectId;
    private List<Integer> disconnectId;

    @Autowired
    private RequestSendService requestSendService;

    public CallTracker(ProviderUtil providerUtil) {
        this.providerUtil = providerUtil;
    }

    @PostConstruct
    public void start(){
        addresses = new ArrayList<>();
        /*Provider provider = providerUtil.getProvider();
        try {
            addresses.addAll(Arrays.asList(provider.getAddresses()));
        } catch (ResourceUnavailableException e) {
            throw new RuntimeException(e);
        }*/
        connectId = new ArrayList<>();
        disconnectId = new ArrayList<>();
        initProvider();
        addObservers();
        logger.info("INITIALIZED");
    }

    @PreDestroy
    public void shutdown(){
        logger.info("Shutting the provider down...");
        clearProvider();
    }

    private void addObservers(){
        logger.debug("Adding observers to the addresses...");
        for (Address address : addresses){
            try {
                if(address != null){
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
        for (CallEv callEv : callEvs){
            System.out.println(callEv.toString());
            if(callEv instanceof CallCtlConnDisconnectedEv){
                disconnectCallEvent(callEv);
            }
            if(callEv instanceof CallCtlConnEstablishedEv || callEv instanceof CallCtlConnNetworkReachedEv){
                connectCallEvent(callEv);
            }
        }

    }

    private void initProvider(){
        logger.info("Initializing provider...");
        try {
            Provider provider = providerUtil.getProvider();
            if(provider.getAddresses() != null && provider.getAddresses().length != 0) {
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


    private void connectCallEvent(CallEv callEv){
        //Address callingAddress = ((CiscoCall) callEv.getCall()).getCallingAddress();
        //Address calledAddress = ((CiscoCall) callEv.getCall()).getCalledAddress();
        String callingAddress = ((CiscoCall) callEv.getCall()).getCallingAddress().toString();
        String calledAddress = ((CiscoCall) callEv.getCall()).getCalledAddress().toString();
        int globalCallID = ((CiscoCall)callEv.getCall()).getCallID().getGlobalCallID();
        if(!connectId.contains(globalCallID)){
            connectId.add(globalCallID);
            logger.debug("ConnectCallEvent, calling:" + callingAddress +" called:" + calledAddress + " globalcallId:" + globalCallID);
            System.out.println("NotContain ConnectCall Calling address = " + callingAddress + "; GlobalCallId = " + globalCallID);
            System.out.println("NotContain ConnectCall Called address = " + calledAddress + "; GlobalCallId = " + globalCallID);
            //String content = "{\"OperatorID\": \"4736\", \"CallerID”: \"0112334455\", \"CallStatus\": \"NewCall\"}";

        }else {
            if(callingAddress.length() > calledAddress.length()){
                String content = toStringContent(callingAddress, calledAddress, newCall);
                requestSendService.sendRequest(content);
                logger.debug("ConnectCallEvent, request content: " + content );
            }
            connectId.remove(Integer.valueOf(globalCallID));
            System.out.println("Contain ConnectCall Calling address = " + callingAddress + "; GlobalCallId = " + globalCallID);
            System.out.println("Contain ConnectCall Called address = " + calledAddress + "; GlobalCallId = " + globalCallID);
        }
    }

    private void disconnectCallEvent(CallEv callEv){
        //Address callingAddress = ((CiscoCall) callEv.getCall()).getCallingAddress();
        //Address calledAddress = ((CiscoCall) callEv.getCall()).getCalledAddress();
        String callingAddress = ((CiscoCall) callEv.getCall()).getCallingAddress().toString();
        String calledAddress = ((CiscoCall) callEv.getCall()).getCalledAddress().toString();
        int globalCallID = ((CiscoCall)callEv.getCall()).getCallID().getGlobalCallID();
        if(!disconnectId.contains(globalCallID)){
            disconnectId.add(globalCallID);
            logger.debug("DisconnectCallEvent, globalCallID=" + globalCallID);
            System.out.println("Not containe DisconnectCall Calling address = " + callingAddress + "; GlobalCallId = " + globalCallID);
            System.out.println("Not containe DisconnectCall Called address = " + calledAddress + "; GlobalCallId = " + globalCallID);
        }else{
            if(callingAddress.length() > calledAddress.length()){
                String content = toStringContent(callingAddress, calledAddress, endCall);
                requestSendService.sendRequest(content);
                logger.debug("DisconnectCallEvent, request content: " + content );
            }
            disconnectId.remove(Integer.valueOf(globalCallID));
            System.out.println("Containe DisconnectCall Calling address = " + callingAddress + "; GlobalCallId = " + globalCallID);
            System.out.println("Containe DisconnectCall Called address = " + calledAddress + "; GlobalCallId = " + globalCallID);
        }
    }

    private String toStringContent(String callingAddress, String calledAddress, String callPhase){
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"OperatorID\":").append("\"").append(calledAddress).append("\"").append(",");
        sb.append("\"CallerID\":").append("\"").append(callingAddress).append("\"").append(",");
        sb.append("\"CallStatus\":").append("\"").append(callPhase).append("\"").append("}");
        return sb.toString();
    }
    @Scheduled(initialDelay = 60 * 60 * 1000, fixedDelay = 60 * 60 * 1000)
    private void timeoutUpdate() {
        clearProvider();
        initProvider();
        addObservers();
    }
    @Scheduled(cron = "0 0 1 * * *")
    private void listsClear(){
        connectId.clear();
        disconnectId.clear();
    }

}
