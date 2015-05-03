package org.agmip.translators.agmip;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import org.agmip.ace.AceBaseComponentType;
import org.agmip.ace.AceDataset;
import org.agmip.ace.AceWeather;
import org.agmip.ace.translators.io.TranslatorOutput;
import org.agmip.common.Functions;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgmipOutput implements TranslatorOutput {

    private static final Logger LOG = LoggerFactory.getLogger(AgmipOutput.class);

    @Override
    public List<File> write(File outputDirectory, AceDataset ace, AceBaseComponentType... components) throws IOException {
        List<File> ret = new ArrayList();
        if (outputDirectory.exists() && outputDirectory.isFile()) {
            LOG.warn(outputDirectory.getPath() + " is a file rather than a directory, will use its parent's directory instead");
            outputDirectory = outputDirectory.getParentFile();
        }
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        for (AceWeather wth : ace.getWeathers()) {
            File f = writeFile(outputDirectory, wth);
            if (f != null) {
                ret.add(f);
            }
        }
        return ret;
    }
    
    public File writeFile(File outputDirectory, AceWeather wth) throws IOException {
        
        String wstId = wth.getValueOr("wst_id", "");
        String climId = wth.getValueOr("clim_id", "0XXX");
        
        if (wstId.equals("")) {
            LOG.warn("Missing weather station ID, will skip this weather data");
            return null;
        }
        
        File ret = new File(outputDirectory.getPath() + File.separator + wstId + climId + ".AgMIP");
        AceWeatherAdaptor wthAdp = new AceWeatherAdaptor(wth);
        Velocity.init();
        VelocityContext context = new VelocityContext();
        FileWriter writer;
        try {
            context.put("weather", wthAdp);
            writer = new FileWriter(ret);
            Reader r = new InputStreamReader(AgmipOutput.class.getClassLoader().getResourceAsStream("template.AgMIP"));
            Velocity.evaluate(context, writer, "Generate " + ret.getName(), r);
            writer.close();
        } catch (IOException ex) {
            LOG.error(Functions.getStackTrace(ex));
        }
        return ret;
    }
}
