package org.agmip.translators.agmip;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;


import org.agmip.core.types.TranslatorInput;
import org.agmip.ace.util.AcePathfinderUtil;

import org.apache.commons.io.FilenameUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgmipInput implements TranslatorInput {
    private static final Logger LOG = LoggerFactory.getLogger(AgmipInput.class);
    private HashMap<String, Object> finalMap = new HashMap<String, Object>();
    
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
        String defValue = "-99";
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(fileStream));
            String[] extraData = reader.readLine().split("[:]");
            reader.readLine();
            reader.readLine();
            String[] locationData = extractData(reader.readLine());
            
            AcePathfinderUtil.insertValue(map, "wst_name", FilenameUtils.getBaseName(fileName));
            AcePathfinderUtil.insertValue(map, "wst_notes", ".AgMIP File");
            AcePathfinderUtil.insertValue(map, "wst_source", extraData[1]);
            AcePathfinderUtil.insertValue(map, "wst_id", locationData[0]);
            AcePathfinderUtil.insertValue(map, "wst_lat", locationData[1]);
            AcePathfinderUtil.insertValue(map, "wst_long", locationData[2]);
            
            // Handle the rest of the values with help for -99 values
            String[] locKeys = {"wst_elev", "tav", "tamp", "refht", "wndht"};
            int l = locKeys.length;
            for (int i=0; i < l; i++) {
                String val = locationData[i+3];
                if (! val.startsWith(defValue)) {
                    AcePathfinderUtil.insertValue(map, locKeys[i], val);
                }
            }
            reader.readLine();
            String dataLine;
            String[] dataKeys = {"w_date", "srad", "tmax", "tmin", "rain", "wind", "dewp", "vprsd", "rhumd"};
            l = dataKeys.length;
            while((dataLine = reader.readLine()) != null) {
                String[] data = extractData(dataLine);
                
                for (int i=0; i < l; i++) {
                    int v = 1;
                    String val;
                    if (i > 0) v = i+3;
                    if (! data[v].contains(defValue)) { 
                        if (i == 0) {
                            //Handle the date
                            val = data[1];
                            if (data[2].length() == 1)
                                val += "0";
                            val += data[2];
                            if (data[2].length() == 1)
                                val += "0";
                            val += data[3];
                        } else if (i == 5) {
                            //Handle wind conversion from m/s to km/d
                            double wind = Double.parseDouble(data[v]);
                            wind = wind*86.4;
                            val = String.format("%.1f", wind);
                        } else {
                            val = data[v];
                        }
                        AcePathfinderUtil.insertValue(map, dataKeys[i], val);
                    }
                }
            }
            reader.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        // Remove extraneous wst_id
        map.remove("wst_id");
        return (HashMap<String, Object>) map.get("weather");
    }
    
    private String[] extractData(String line) {
        line = line.trim().replaceAll("\\s+", "|");
        return line.split("[|]");
    }
}
