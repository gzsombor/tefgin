package hu.qualysoft.projtime.ui;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 * Inspired heavily from DefaultListCellRenderer
 *
 * @author zsombor
 *
 * @param <X>
 */
abstract class BasicListRenderer<X> implements ListCellRenderer<X> {
    DefaultListCellRenderer component = new DefaultListCellRenderer();

    @Override
    public Component getListCellRendererComponent(JList<? extends X> list, X value, int index, boolean isSelected, boolean cellHasFocus) {
        return component.getListCellRendererComponent(list, value != null ? getLabelFor(value) : "", index, isSelected, cellHasFocus);
    }

    protected abstract String getLabelFor(X value);
}