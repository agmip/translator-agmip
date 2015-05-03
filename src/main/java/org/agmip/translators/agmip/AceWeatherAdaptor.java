package org.agmip.translators.agmip;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import org.agmip.ace.AceRecord;
import org.agmip.ace.AceWeather;
import org.agmip.common.Functions;
import org.agmip.util.MapUtil;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Meng Zhang
 */
public class AceWeatherAdaptor {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(AceWeatherAdaptor.class);
    private final AceWeather wth;
    private final ArrayList<AceDailyWeatherAdaptor> dailyWeather;
    private final static String defVal = "-99";
    private final static HashMap<String, Integer> varLenDef = new HashMap();
    private final static int defVarLen = 6;

    public AceWeatherAdaptor(AceWeather wth) {
        this.wth = wth;
        this.dailyWeather = new ArrayList();

        try {
            Iterator<AceRecord> it = wth.getDailyWeather().iterator();
            while (it.hasNext()) {
                dailyWeather.add(new AceDailyWeatherAdaptor(it.next()));
            }
        } catch (IOException ex) {
            LOG.error(ex.getMessage());
        }
        
        if (varLenDef.isEmpty()) {
            varLenDef.put("wst_long", 9);
            varLenDef.put("wst_lat", 9);
        }

    }
    
    public ArrayList<AceDailyWeatherAdaptor> getDailyWeather() {
        return this.dailyWeather;
    }

    public String getInsi() {
        String ret;
        try {
            ret = wth.getValueOr("insi", wth.getValueOr("wst_id", defVal));
        } catch (Exception ex) {
            LOG.error(ex.getMessage());
            ret = defVal;
        }
        return formatNumStr(6, ret);
    }

    public String get(String var) {
        String ret;
        try {
            ret = wth.getValueOr(var, "");
        } catch (Exception ex) {
            LOG.error(ex.getMessage());
            ret = defVal;
        }
        return formatNumStr(MapUtil.getObjectOr(varLenDef, var, defVarLen), ret);
    }

    public class AceDailyWeatherAdaptor {

        private final AceRecord dailyWeather;

        public AceDailyWeatherAdaptor(AceRecord dailyWeather) {
            this.dailyWeather = dailyWeather;
        }

        public String getDate() {
            String ret;
            try {
                Date d = Functions.convertFromAgmipDateString(dailyWeather.getValueOr("w_date", ""));
                ret = new SimpleDateFormat("yyyyDDD").format(d);
            } catch (Exception ex) {
                LOG.error(ex.getMessage());
                ret = defVal;
            }
            return String.format("%7s", ret);
        }

        public String getYear() {
            String ret;
            try {
                ret = dailyWeather.getValueOr("w_date", "").substring(0, 4);
            } catch (Exception ex) {
                LOG.error(ex.getMessage());
                ret = defVal;
            }
            return String.format("%6d", Integer.parseInt(ret));
        }

        public String getMonth() {
            String ret;
            try {
                ret = dailyWeather.getValueOr("w_date", "").substring(4, 6);
            } catch (Exception ex) {
                LOG.error(ex.getMessage());
                ret = defVal;
            }
            return String.format("%4d", Integer.parseInt(ret));
        }

        public String getDay() {
            String ret;
            try {
                ret = dailyWeather.getValueOr("w_date", "").substring(6, 8);
            } catch (Exception ex) {
                LOG.error(ex.getMessage());
                ret = defVal;
            }
            return String.format("%4d", Integer.parseInt(ret));
        }

        public String getWind() {
            String ret;
            try {
                ret = Functions.divide(dailyWeather.getValueOr("wind", ""), "86.4");
//                ret = Functions.round(ret, 1);
            } catch (Exception ex) {
                LOG.error(ex.getMessage());
                ret = "";
            }
            return formatNumStr(6, ret);
        }

        public String getVprsd() {
            String ret;
            try {
                ret = Functions.multiply(dailyWeather.getValueOr("vprsd", ""), "10");
//                ret = Functions.round(ret, 1);
            } catch (Exception ex) {
                LOG.error(ex.getMessage());
                ret = "";
            }
            return formatNumStr(6, ret);
        }

//        public String getRhum() {
//            String ret;
//            try {
//                ret = dailyWeather.getValueOr("rhum", "");
//            } catch (Exception ex) {
//                LOG.error(ex.getMessage());
//                ret = "";
//            }
//            return String.format("%6s", formatNumStr(5, ret));
//        }

        public String get(String var) {
            String ret;
            try {
                ret = dailyWeather.getValueOr(var, "");
            } catch (Exception ex) {
                LOG.error(ex.getMessage());
                ret = "";
            }
            return formatNumStr(6, ret);
        }
    }

    /**
     * Format the number with maximum length and type
     *
     * @param bits Maximum length of the output string
     * @param str String for number value
     * @return formated string of number
     */
    private String formatNumStr(int bits, String str) {

        String ret;
        String[] inputStr = str.split("\\.");
        if (str.trim().equals("")) {
            ret = String.format("%" + bits + "s", "");
        } else if (str.length() <= bits) {
            ret = String.format("%1$" + bits + "s", str);
        } else if (inputStr[0].length() > bits) {
            ret = String.format("%" + bits + "s", str);
        } else {
            int decimalLength = bits - inputStr[0].length() - 1;
            decimalLength = decimalLength < 0 ? 0 : decimalLength;
            ret = org.agmip.common.Functions.round(str, decimalLength);
        }

        return ret;
    }
}
