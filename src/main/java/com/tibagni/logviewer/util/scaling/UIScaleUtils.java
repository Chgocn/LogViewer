package com.tibagni.logviewer.util.scaling;

import com.tibagni.logviewer.logger.Logger;
import com.tibagni.logviewer.rc.UIScaleConfig;
import com.tibagni.logviewer.util.StringUtils;
import com.tibagni.logviewer.view.CustomCheckboxIcon;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.Collections;

public class UIScaleUtils {
    private static int SCALE_FACTOR;

    public static void initialize(UIScaleConfig config) {
        int configValue = config == null ? 1 : config.getConfigValue();
        SCALE_FACTOR = configValue == UIScaleConfig.SCALE_OFF ? 1 : configValue;
        Logger.debug("Initializing UIScaleUtils. Scale Factor is " + SCALE_FACTOR);

        // Always update the checkbox icon so it does not have any scaling issues and look consistent across
        // all themes and resolutions
        UIManager.put("CheckBox.icon", new CustomCheckboxIcon());
        UIManager.put("CheckBoxMenuItem.checkIcon", new CustomCheckboxIcon());
    }

    public static void updateDefaultSizes() {
        if (!shouldUpdateDefaultSizes()) {
            return;
        }

        UIDefaults defaults = UIManager.getDefaults();
        for (Object obj: Collections.list(defaults.keys())) {
            if (!(obj instanceof String)) continue;

            String key = (String) obj;
            Object value = UIManager.get(key);
            System.out.println(key + " : " + value);

            Object modified = null;
            if (value instanceof Integer) modified = updateInteger(key, (Integer) value);
            if (value instanceof Icon) modified = updateIcon((Icon) value);
            if (value instanceof FontUIResource) modified = updateFont((FontUIResource) value);
            if (value instanceof Dimension) modified = updateDimension((Dimension) value);

            if (modified != null && modified != value) {
                Logger.verbose("Updating " + key + " from " + value + " to " + modified);
                defaults.put(key, modified);
            }
        }
    }

    private static boolean shouldUpdateDefaultSizes() {
        if (SCALE_FACTOR == 1) {
            // When scale factor is 1, don't bother changing the default sizes as it will be the same
            return false;
        }

        String currentLnF = UIManager.getLookAndFeel().getClass().getName();
        return !currentLnF.contains("GTKLookAndFeel");
    }

    private static FontUIResource updateFont(FontUIResource value) {
        return new FontUIResource(value.getFamily(), value.getStyle(), scaleFont(value.getSize()));
    }

    private static Icon updateIcon(Icon value) {
        if (value instanceof ImageIcon) {
            ImageIcon imageIcon = (ImageIcon) value;
            Image img = imageIcon.getImage();
            Image newImg = img.getScaledInstance(dip(imageIcon.getIconWidth()), dip(imageIcon.getIconHeight()),
                    java.awt.Image.SCALE_SMOOTH);
            return new ImageIcon(newImg);
        }

        return value;
    }

    private static Integer updateInteger(String key, Integer value) {
        final String[] scalableSuffixes = new String[] { "width", "height", "indent", "size", "gap" };
        if (!StringUtils.endsWithOneOf(key.toLowerCase(), scalableSuffixes)) {
            return value;
        }

        return dip(value);
    }

    private static Dimension updateDimension(Dimension value) {
        return scaleDimension(value);
    }

    public static int dip(int value) {
        return value * SCALE_FACTOR;
    }

    public static float dipf(float value) {
        return value * SCALE_FACTOR;
    }

    public static int scaleFont(int value) {
        return value * SCALE_FACTOR;
    }

    public static Dimension scaleDimension(Dimension d) {
        return new Dimension(dip(d.width), dip(d.height));
    }
}