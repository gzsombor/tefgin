package hu.qualysoft.projtime.ui;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ButtonTask implements Runnable {

    final static Logger LOG = LoggerFactory.getLogger(ButtonTask.class);

    final JButton button;

    public ButtonTask(JButton button) {
        this.button = button;
    }

    @Override
    public void run() {
        try {
            executeTask();
        } catch (final Exception e) {
            LOG.warn("Failed to execute task:" + e.getMessage(), e);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(null, "Hiba:" + getMessage(e));
                }
            });

        } finally {
            button.setEnabled(true);
        }

    }

    String getMessage(Exception e) {
        final StringBuilder s = new StringBuilder();
        Throwable current = e;
        while (current != null) {
            if (current.getMessage() != null) {
                if (s.length() > 0) {
                    s.append(", caused by: ");
                }
                s.append(current.getMessage());
            }
            current = current.getCause();
        }
        return s.toString();
    }

    protected abstract void executeTask() throws Exception;

}
