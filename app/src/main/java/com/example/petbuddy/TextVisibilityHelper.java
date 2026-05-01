package com.example.petbuddy;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

/**
 * Helper class to ensure text visibility across different themes
 */
public class TextVisibilityHelper {
    
    /**
     * Check if the current theme is dark mode
     */
    public static boolean isDarkMode(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
    
    /**
     * Set text color based on current theme for better visibility
     */
    public static void setThemeAwareTextColor(Context context, TextView textView, int lightColorRes, int darkColorRes) {
        if (isDarkMode(context)) {
            textView.setTextColor(ContextCompat.getColor(context, darkColorRes));
        } else {
            textView.setTextColor(ContextCompat.getColor(context, lightColorRes));
        }
    }
    
    /**
     * Set primary text color based on theme
     */
    public static void setPrimaryTextColor(Context context, TextView textView) {
        textView.setTextColor(ContextCompat.getColor(context, R.color.primary_text));
    }
    
    /**
     * Set secondary text color based on theme
     */
    public static void setSecondaryTextColor(Context context, TextView textView) {
        textView.setTextColor(ContextCompat.getColor(context, R.color.secondary_text));
    }
    
    /**
     * Set hint text color based on theme
     */
    public static void setHintTextColor(Context context, TextView textView) {
        textView.setTextColor(ContextCompat.getColor(context, R.color.hint_text));
    }
    
    /**
     * Calculate contrast ratio between two colors
     */
    public static double calculateContrastRatio(int color1, int color2) {
        double luminance1 = calculateLuminance(color1);
        double luminance2 = calculateLuminance(color2);
        
        double lighter = Math.max(luminance1, luminance2);
        double darker = Math.min(luminance1, luminance2);
        
        return (lighter + 0.05) / (darker + 0.05);
    }
    
    /**
     * Calculate relative luminance of a color
     */
    private static double calculateLuminance(int color) {
        double red = Color.red(color) / 255.0;
        double green = Color.green(color) / 255.0;
        double blue = Color.blue(color) / 255.0;
        
        red = (red <= 0.03928) ? red / 12.92 : Math.pow((red + 0.055) / 1.055, 2.4);
        green = (green <= 0.03928) ? green / 12.92 : Math.pow((green + 0.055) / 1.055, 2.4);
        blue = (blue <= 0.03928) ? blue / 12.92 : Math.pow((blue + 0.055) / 1.055, 2.4);
        
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
    }
    
    /**
     * Check if text has sufficient contrast (WCAG AA standard: 4.5:1)
     */
    public static boolean hasSufficientContrast(int textColor, int backgroundColor) {
        return calculateContrastRatio(textColor, backgroundColor) >= 4.5;
    }
    
    /**
     * Get appropriate text color for a given background
     */
    public static int getContrastingTextColor(Context context, int backgroundColor) {
        int whiteColor = ContextCompat.getColor(context, R.color.white);
        int blackColor = ContextCompat.getColor(context, R.color.black);
        
        double whiteContrast = calculateContrastRatio(whiteColor, backgroundColor);
        double blackContrast = calculateContrastRatio(blackColor, backgroundColor);
        
        return whiteContrast > blackContrast ? whiteColor : blackColor;
    }
}