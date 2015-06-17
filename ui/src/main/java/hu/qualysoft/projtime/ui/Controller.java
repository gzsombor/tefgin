package hu.qualysoft.projtime.ui;

import hu.qualysoft.tefgin.base.HourMinute;
import hu.qualysoft.tefgin.base.json.Booking;
import hu.qualysoft.tefgin.base.json.PositionList;
import hu.qualysoft.tefgin.common.Utils;
import hu.qualysoft.tefgin.daxclient.CommandLineClient;
import hu.qualysoft.tefgin.daxclient.NTLMAuthenticator;
import hu.qualysoft.tefgin.tefclient.BaseClient;
import hu.qualysoft.tefgin.tefclient.TEFClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.stream.Collectors;

import javax.swing.DefaultComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qualysoft.timesheet.ws.service._200.ActivityListVO;
import com.qualysoft.timesheet.ws.service._200.ArrayOfDateTime;
import com.qualysoft.timesheet.ws.service._200.ArrayOfProjectListVO;
import com.qualysoft.timesheet.ws.service._200.ProjectListVO;

public class Controller {

    private final static Logger LOG = LoggerFactory.getLogger(Controller.class);

    class TEFContext extends LoginHolder {
        final BaseClient baseClient;
        final TEFClient tefClient;

        public TEFContext() {
            this.baseClient = new BaseClient();
            this.baseClient.setAuthentication(Utils.getNetworkConfiguration());
            this.tefClient = new TEFClient(baseClient);
        }

        @Override
        protected void logout() {
            tefClient.logout();
        }

        @Override
        protected boolean login(String username, String password) {
            return tefClient.login(username, password);
        }

        public Booking getTimingsAt(LocalDate date) {
            return tefClient.getTimingsAt(date);
        }

        public PositionList getPositionListAt(LocalDate date) {
            return tefClient.getPositionListAt(date);
        }
    }

    class DAXContext extends LoginHolder {
        private final CommandLineClient daxClient = new CommandLineClient(NTLMAuthenticator.getInstance());

        @Override
        protected boolean login(String username, String password) {
            daxClient.setLogin(username, password);
            return true;
        }

        @Override
        protected void logout() {
            daxClient.logout();
        }

    }

    private final TefBookingTableModel model;
    private final DaxTableModel daxModel;
    private final PlainDocument summaryDocumentModel;
    private final TEFContext tef;
    private final DAXContext dax;
    private final DefaultComboBoxModel<ProjectListVO> projects;
    private final DefaultComboBoxModel<ActivityListVO> activities;

    private LocalTime defaultPauseStart = LocalTime.of(11, 30);

    ProjectListVO selectedProject;

    public Controller(TefBookingTableModel model, DaxTableModel daxModel) {
        this.daxModel = daxModel;
        this.summaryDocumentModel = new PlainDocument();
        this.projects = new DefaultComboBoxModel<>();
        this.activities = new DefaultComboBoxModel<>();
        this.tef = new TEFContext();
        this.dax = new DAXContext();
        this.model = model;
        this.projects.addListDataListener(new ListDataListener() {
            @Override
            public void contentsChanged(ListDataEvent e) {
                check();
            }

            void check() {
                if (projects.getSelectedItem() != selectedProject) {
                    regenerateActivityModel((ProjectListVO) projects.getSelectedItem());
                }
            }

            @Override
            public void intervalAdded(ListDataEvent e) {
                check();
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                check();
            }

        });
    }

    public PlainDocument getSummaryDocumentModel() {
        return summaryDocumentModel;
    }

    public DefaultComboBoxModel<ProjectListVO> getProjects() {
        return projects;
    }

    public DefaultComboBoxModel<ActivityListVO> getActivities() {
        return activities;
    }

    public DAXContext getDax() {
        return dax;
    }

    public TEFContext getTef() {
        return tef;
    }

    public void loadBookings(Date from) {
        LocalDate cal = convert(from);
        try {
            daxModel.setBlocking(true);
            final LocalDate now = LocalDate.now();
            while (cal.isBefore(now) || cal.isEqual(now)) {
                final Booking booking = tef.getTimingsAt(cal);
                DayBookingModel wrapper = null;
                if (booking.getComingTime(0) == null || booking.getGoingTime(0) == null) {
                    wrapper = new PositionListBookingModel(cal, tef.getPositionListAt(cal));
                } else {
                    wrapper = new SimpleDayBookingModel(cal, booking);
                }
                model.add(wrapper);
                daxModel.addBooking(cal, wrapper);
                cal = cal.plusDays(1);
            }
        } finally {
            daxModel.setBlocking(false);
            final HourMinute sum = model.getSummary();
            final String sumAsText = String.format("%s nap %s óra %s perc", sum.getHour() / 8, sum.getHour() % 8, sum.getMinute());
            try {
                summaryDocumentModel.replace(0, summaryDocumentModel.getLength(), sumAsText, null);
            } catch (final BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean changeDateRanges(Date start, Date end) {
        if (start == null || end == null) {
            return false;
        }
        final LocalDate fromDate = convert(start);
        final LocalDate endDate = convert(end);
        if (fromDate.isBefore(endDate)) {
            daxModel.setRange(fromDate, endDate);
            LOG.info("Number of rows : " + daxModel.getRowCount());
            return true;
        }
        return false;
    }

    public void setupDaxDateRanges(Date start, Date end) throws Exception {
        final LocalDate fromDate = convert(start);
        final LocalDate endDate = convert(end);

        if (endDate.isBefore(fromDate)) {
            throw new RuntimeException("A befejező dátum a kezdő dátum előtt van!");
        }
        daxModel.setRange(fromDate, endDate);

        daxModel.setBlocking(true);

        final ArrayOfProjectListVO projectActivities = dax.daxClient.projectActivities(fromDate);
        projects.removeAllElements();
        for (final ProjectListVO p : projectActivities.getProjectListVO()) {
            projects.addElement(p);
        }
        if (!projectActivities.getProjectListVO().isEmpty()) {
            projects.setSelectedItem(projectActivities.getProjectListVO().get(0));
        }

        final ArrayOfDateTime notFilledDays = dax.daxClient.getNotFilledDays(fromDate, endDate);
        daxModel.setNotFilledDates(notFilledDays.getDateTime().stream().map(dt -> dt.toLocalDate()).collect(Collectors.toList()));
        daxModel.setBlocking(false);
    }

    LocalDate convert(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    void regenerateActivityModel(ProjectListVO projectListVO) {
        activities.removeAllElements();
        if (projectListVO != null) {
            for (final ActivityListVO activity : projectListVO.getActivitiesField().getActivityListVO()) {
                activities.addElement(activity);
            }
            if (activities.getSize() > 0) {
                activities.setSelectedItem(activities.getElementAt(0));
            }
        }
        this.selectedProject = projectListVO;
    }

    public LocalTime getDefaultPauseStart() {
        return defaultPauseStart;
    }

    public void setDefaultPauseStart(LocalTime defaultPauseStart) {
        if (defaultPauseStart == null) {
            throw new NullPointerException("defaultPauseStart");
        }
        this.defaultPauseStart = defaultPauseStart;
    }

    public void bookTasks(ProjectListVO project, ActivityListVO activity, String note) {
        if (project == null) {
            throw new RuntimeException("Nincs projekt kiválasztva!");
        }
        if (activity == null) {
            throw new RuntimeException("Nincs aktivitás kiválasztva!");
        }
        daxModel.getSelectedBookings().forEach(booking -> {
            final LocalDate day = booking.getAskedTime();
            LocalTime pauseTime = booking.getPauseTime();
            final LocalDateTime pauseStart = day.atTime(0, 0, 0);
            final LocalDateTime pauseEnd = pauseStart;
            // if (pauseTime != null) {
            // pauseStart = day.atTime(defaultPauseStart);
            // pauseEnd = pauseStart.plus(pauseTime.getHour(),
            // ChronoUnit.HOURS).plus(pauseTime.getMinute(),
            // ChronoUnit.MINUTES);
            // } else {
            // pauseStart = day.atTime(0, 0, 0);
            // pauseEnd = pauseStart;
            // }
                try {
                    final Long projId = project.getIdField();
                    final Long activId = activity != null ? activity.getIdField() : null;

                    for (int i = 0; i < booking.getNumberOfTimeRanges(); i++) {
                        final LocalTime comingTime = booking.getComingTime(i);
                        final LocalTime goingTime = booking.getGoingTime(i);
                        if (comingTime != null && goingTime != null) {
                            final LocalDateTime startTime = day.atTime(comingTime);
                            LocalDateTime endTime = day.atTime(goingTime);
                            if (pauseTime != null) {
                                endTime = endTime.minusMinutes(pauseTime.get(java.time.temporal.ChronoField.MINUTE_OF_DAY));
                                pauseTime = null;
                            }

                            LOG.info("createWorkTime ( " + projId + "," + activId + "," + startTime + "," + endTime + "," + note + "," + pauseStart + ","
                                    + pauseEnd + ")");
                            dax.daxClient.createWorkTime(projId, activId, startTime, endTime, note, pauseStart, pauseEnd);
                        } else {
                            LOG.info("creating work time skipped : " + comingTime + " => " + goingTime + " at " + day);
                        }
                        // reset pause times, as we wont send pause anymore.
                        // Yes, it's not too correct.
                        // pauseStart = day.atTime(0, 0, 0);
                        // pauseEnd = pauseStart;

                    }
                } catch (final RuntimeException e) {
                    throw e;
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            });
    }

}
