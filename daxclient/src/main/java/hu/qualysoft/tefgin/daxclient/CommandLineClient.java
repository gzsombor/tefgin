package hu.qualysoft.tefgin.daxclient;

import hu.qualysoft.tefgin.common.Utils;

import java.net.PasswordAuthentication;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;

import com.qualysoft.timesheet.ws.service._200.ArrayOfDateTime;
import com.qualysoft.timesheet.ws.service._200.ArrayOfProjectListVO;
import com.qualysoft.timesheet.ws.service._200.TSWebService;
import com.qualysoft.timesheet.ws.service._200.TSWebServiceSoap;

public class CommandLineClient {

    private final TSWebService t = new TSWebService();
    private final TSWebServiceSoap service;

    private PasswordAuthentication auth;

    private String username;
    private final NTLMAuthenticator authenticator;

    public CommandLineClient(NTLMAuthenticator authenticator) {
        this.authenticator = authenticator;
        this.service = t.getTSWebServiceSoap12();

        final BindingProvider bp = (BindingProvider) service;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, Utils.getEndpointUrl());

        if (Utils.LOGGING) {
            final List<Handler> handlers = bp.getBinding().getHandlerChain();
            handlers.add(new SoapLogger());
            bp.getBinding().setHandlerChain(handlers);
        }
    }

    public void setLogin(String username, String password) {
        this.username = username;
        this.auth = new PasswordAuthentication(Utils.DOMAIN + '\\' + username, password.toCharArray());

        final BindingProvider bp = (BindingProvider) service;
        bp.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, username);
        bp.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, password);
    }

    public ArrayOfProjectListVO projectActivities(int year, int month, int day) throws Exception {
        return projectActivities(toLocalDate(year, month, day));
    }

    public ArrayOfProjectListVO projectActivities(LocalDate date) throws Exception {
        authCheck();
        return authenticator.secureCall(auth, () -> service.getProjectsWithActivitiesByUser(getUserName(), toLocalDateTime(date)));
    }

    private void authCheck() {
        if (auth == null) {
            throw new IllegalStateException("Credentials not yet provided, empty authentication object!");
        }
    }

    public ArrayOfDateTime getNotFilledDays(LocalDate from, LocalDate to) throws Exception {
        authCheck();
        return authenticator.secureCall(auth, () -> service.getNotFilledDays(getUserName(), toLocalDateTime(from), toLocalDateTime(to)));
    }

    public ArrayOfDateTime getNotFilledDays(int fromYear, int fromMonth, int fromDay, int toYear, int toMonth, int toDay) throws Exception {
        return getNotFilledDays(toLocalDate(fromYear, fromMonth, fromDay), toLocalDate(toYear, toMonth, toDay));
    }

    /**
     * Return the id of the record
     *
     * @param projRecId
     * @param actRecId
     * @param time1
     * @param time2
     * @param note
     * @param pauseStart
     * @param pauseEnd
     * @return
     * @throws Exception
     */
    public String createWorkTime(long projRecId, long actRecId, LocalDateTime time1, LocalDateTime time2, String note, LocalDateTime pauseStart,
            LocalDateTime pauseEnd) throws Exception {
        authCheck();
        return authenticator.secureCall(auth, () -> {
            return service.createWorkTime(getUserName(), projRecId, actRecId, time1, time2, note, pauseStart, pauseEnd);
        });
    }

    private String getUserName() {
        return username;
    }

    private LocalDate toLocalDate(int year, int month, int day) {
        return LocalDate.of(year, month, day);
    }

    private LocalDateTime toLocalDateTime(LocalDate date) {
        return date.atTime(0, 0, 0);
    }

    public void logout() {
        this.auth = null;
    }

    public static void main(String[] args) throws Exception {
        Utils.setupProxyServerFromEnvironment();
        NTLMAuthenticator.getInstance().install();

        final CommandLineClient c = new CommandLineClient(NTLMAuthenticator.getInstance());
        c.setLogin("zgegesy", "EZNEMJELSZO");
        final ArrayOfProjectListVO activities = c.projectActivities(LocalDate.now());
        System.out.println("Activities : " + activities);
        activities
                .getProjectListVO()
                .stream()
                .forEach(
                        t -> System.out.println("id:" + t.getIdField() + ", " + t.getNameField() + ", " + t.getProjectStartField() + ","
                                + t.getProjectEndField()));

    }

}
