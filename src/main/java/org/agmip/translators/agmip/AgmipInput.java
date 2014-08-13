package org.agmip.translators.agmip;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.agmip.ace.util.AcePathfinderUtil;
import org.agmip.common.Functions;
import org.agmip.core.types.TranslatorInput;
import org.agmip.util.MapUtil;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgmipInput implements TranslatorInput {

    private static final Logger LOG = LoggerFactory.getLogger(AgmipInput.class);
    private HashMap<String, Object> finalMap = new HashMap<String, Object>();

    @Override
    public Map readFile(String fileName) {
        ArrayList<HashMap<String, Object>> container = new ArrayList<HashMap<String, Object>>();
        // Since this is a weather only input translator, we can simplify
        // the creation by putting the resulting map directly in the ArrayList.

        try {
            if (fileName.toUpperCase().endsWith("AGMIP")) {
                container.add(readAgMIPFile(new FileInputStream(fileName), fileName));
            } else if (fileName.toUpperCase().endsWith("ZIP")) {
                //Handle a ZipInputStream instead
                LOG.debug("Launching zip file handler");
                ZipFile zf = new ZipFile(fileName);
                Enumeration<? extends ZipEntry> e = zf.entries();
                while (e.hasMoreElements()) {
                    ZipEntry ze = (ZipEntry) e.nextElement();
                    LOG.debug("Entering file: " + ze);
                    if (ze.getName().toUpperCase().endsWith("AGMIP")) {
                        container.add(readAgMIPFile(zf.getInputStream(ze), ze.getName()));
                    }
                }
                zf.close();
            }
        } catch (FileNotFoundException ex) {
            LOG.error("File not found error: {}", ex.getMessage());
            return new HashMap<String, Object>();
        } catch (IOException ex) {
            LOG.error(ex.getMessage());
            return new HashMap<String, Object>();
        }
        finalMap.put("weathers", container);
        return finalMap;
    }

    private HashMap<String, Object> readAgMIPFile(InputStream fileStream, String fileName) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        String baseFile = FilenameUtils.getBaseName(fileName);
        String climid   = "0XXX";
//        String defValue = "-99";

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(fileStream));
            String[] extraData = reader.readLine().split("[:]");
            LinkedHashMap<String, Integer> titles = extractTitle(reader);
            HashMap<String, String> locationData = extractData(reader.readLine(), titles);

            AcePathfinderUtil.insertValue(map, "wst_name", baseFile);
            AcePathfinderUtil.insertValue(map, "wst_notes", ".AgMIP File");
            AcePathfinderUtil.insertValue(map, "wst_source", extraData[1]);
            AcePathfinderUtil.insertValue(map, "wst_id", baseFile.substring(0, Math.min(baseFile.length(), 4)));
//            AcePathfinderUtil.insertValue(map, "wst_lat", locationData.get("wst_lat"));
//            AcePathfinderUtil.insertValue(map, "wst_long", locationData.get("wst_long"));

            if (baseFile.length() == 8) {
                climid = baseFile.substring(4);
                if (isValidClimateID(climid)) {
                    LOG.debug("Valid ClimateID: {}", climid);
                    AcePathfinderUtil.insertValue(map, "clim_id", climid);
                } else {
                    LOG.warn("Invalid ClimateID: {}", climid);
                    AcePathfinderUtil.insertValue(map, "clim_id", climid);
                }
            } else {
                LOG.warn("Filename incorrect length: {}", climid);
                AcePathfinderUtil.insertValue(map, "clim_id", climid);
            }


            // Handle the rest of the values with help for -99 values
            String[] locKeys = {"wst_lat", "wst_long", "wst_elev", "tav", "tamp", "refht", "wndht"};
            int l = locKeys.length;
            for (int i = 0; i < l; i++) {
                String val = locationData.get(locKeys[i]);
                if (val != null) {
                    AcePathfinderUtil.insertValue(map, locKeys[i], val);
                }
            }
            titles = extractTitle(reader);
            String dataLine;
            String[] dataKeys = {"w_date", "srad", "tmax", "tmin", "rain", "wind", "tdew", "vprsd", "rhumd"};
            l = dataKeys.length;
            while ((dataLine = reader.readLine()) != null) {
                HashMap<String, String> data = extractData(dataLine, titles);

                for (int i = 0; i < l; i++) {
                    String val = data.get(dataKeys[i]);
                    if (val != null || i == 0) {
                        if (i == 0) {
                            //Handle the date
                            String year = MapUtil.getValueOr(data, "yyyy", "");
                            String mm = MapUtil.getValueOr(data, "mm", "");
                            String dd = MapUtil.getValueOr(data, "dd", "");
                            if (year.equals("") || mm.equals("") || dd.equals("")) {
                                val = null;
                            } else {
                                val = year;
                                if (mm.length() == 1) {
                                    val += "0";
                                }
                                val += mm;
                                if (dd.length() == 1) {
                                    val += "0";
                                }
                                val += dd;
                            }
                        } else if (i == 5) {
                            //Handle wind conversion from m/s to km/d
                            double wind = Double.parseDouble(val);
                            wind = wind * 86.4;
                            val = String.format("%.1f", wind);
                        } else if (i == 7) {
                            val = Functions.divide(val, "10");
                        }
                        AcePathfinderUtil.insertValue(map, dataKeys[i], val);
                    }
                }
            }
            reader.close();
        } catch (IOException ex) {
            LOG.error(Functions.getStackTrace(ex));
        }
        // Remove extraneous wst_id
        map.remove("wst_id");
        return (HashMap<String, Object>) map.get("weather");
    }

    private HashMap<String, String> extractData(String line, LinkedHashMap<String, Integer> titles) {
        HashMap<String, String> ret = new HashMap<String, String>();
        int start = 0;
        int l = line.length();
        for (String key : titles.keySet()) {
            if (start >= l) {
                break;
            } else {
                int end = Math.min(start + titles.get(key), l);
                String value = line.substring(start, end).trim();
                if (!value.equals("") && (!value.matches("-99(.0*)?") || key.equals("wst_lat") || key.equals("wst_long"))) {
                    ret.put(key, value);
                }
                start = end;
            }
        }
        return ret;
    }

    // Currently, hard code this information to validate. Should be
    // handled by the ace-lookup eventually.
    private boolean isValidClimateID(String climid) {
        String pattern = "[0-9A-Z]{4}"; //[0-6A-F][0A-LN-PW-Z][0-7A-EX][A-KX]";
        return climid.matches(pattern);
    }

    private LinkedHashMap<String, Integer> extractTitle(BufferedReader reader) throws IOException {

        String line = reader.readLine();
        while (line != null && !line.startsWith("@")) {
            line = reader.readLine();
        }
        if (line == null) {
            LOG.error("Can not find a title line in the .AGMIP file, failed to load data.");
            return new LinkedHashMap();
        } else {
            line = line.substring(1).trim();
        }

        int cnt = 1;
        while (line.contains("       ")) {
            line = line.replaceFirst("      ", String.format(" XXX%02d ", cnt));
            cnt++;
        }
        String[] titles = line.split("\\s+");

        // Mapping the title with ICASA varibale name and recored the position in the line
        LinkedHashMap<String, Integer> titleLenMap = new LinkedHashMap<String, Integer>();
        if (line.toUpperCase().contains("INSI")) {
            for (int i = 0; i < titles.length; i++) {
                String title = titles[i].toLowerCase();
                int len = 6;
                if ("lat".equals(title)) {
                    title = "wst_lat";
                    len = 9;
                } else if ("long".equals(title)) {
                    title = "wst_long";
                    len = 9;
                } else if ("elev".equals(title)) {
                    title = "wst_elev";
                } else if ("amp".equals(title)) {
                    title = "tamp";
                } else if ("amp".equals(title)) {
                    title = "tamp";
                }
//                else if ("cco2".equals(title) || "co2".equals(title)) {
//                    title = "co2y";
//                }
                titleLenMap.put(title, len);
            }
        } // Daily data mapping
        else {
            for (int i = 0; i < titles.length; i++) {
                String title = titles[i].toLowerCase();
                int len = 6;
                if ("dewp".equals(title)) {
                    title = "tdew";
                } else if ("vprs".equals(title)) {
                    title = "vprsd";
                } else if ("rhum".equals(title)) {
                    title = "rhumd";
                } else if ("date".equals(title)) {
                    len = 8;
                } else if ("yyyy".equals(title)) {
                    len = 5;
                } else if ("mm".equals(title)) {
                    len = 4;
                } else if ("dd".equals(title)) {
                    len = 4;
                }
                titleLenMap.put(title, len);
            }
        }
        LOG.debug("Find titles as {}", titleLenMap);
        return titleLenMap;
    }
}
