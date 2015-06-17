package hu.qualysoft.tefgin.daxclient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

public class SoapLogger implements SOAPHandler<SOAPMessageContext> {
    final static Logger LOG = Logger.getLogger(SoapLogger.class.getName());

    @Override
    public Set<QName> getHeaders() {
        return null;
    }

    @Override
    public void close(MessageContext arg0) {
    }

    @Override
    public boolean handleFault(SOAPMessageContext arg0) {
        final SOAPMessage message = arg0.getMessage();
        try {
            message.writeTo(System.out);
        } catch (final SOAPException | IOException e) {
            LOG.log(Level.WARNING, "soap exception:" + e.getMessage(), e);
        }
        return true;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext arg0) {
        final SOAPMessage message = arg0.getMessage();
        final boolean isOutboundMessage = (Boolean) arg0.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        if (isOutboundMessage) {
            LOG.info("outbound message");
        } else {
            LOG.info("inbound message");
        }
        try {
            final ByteArrayOutputStream b = new ByteArrayOutputStream();
            message.writeTo(b);
            final String msg = b.toString("UTF-8").replaceAll("><", ">\n<");
            LOG.info("Message : " + msg);
        } catch (final SOAPException | IOException e) {
            LOG.log(Level.WARNING, "soap exception:" + e.getMessage(), e);
        }
        return true;
    }

}
