package org.agmip.translators.agmip;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import org.agmip.ace.AceDataset;
import org.agmip.ace.io.AceParser;
import org.agmip.util.JSONAdapter;
import org.junit.Test;
import org.junit.Before;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit test for simple App.
 */
public class TranslationTest {
    private static final Logger LOG = LoggerFactory.getLogger(TranslationTest.class);
    private URL resource;
    
    @Before
    public void setupTests() {
        resource = this.getClass().getResource("/BDJE0XXX.AgMIP");
    }
    
    @Test
    public void runTranslation() throws IOException {
        AgmipInput in = new AgmipInput();
        Map results = in.readFile(resource.getPath());
        AceDataset ace = AceParser.parse(JSONAdapter.toJSON(results));
        AgmipOutput out = new AgmipOutput();
        File outputDir = new File("output");
        List<File> files = out.write(outputDir, ace);
        for (File f : files) {
            LOG.info("Generate " + f.getName());
            f.delete();
        }
        outputDir.delete();
        //LOG.info("Translation Results: "+results);
    }
}
