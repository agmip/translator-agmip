package org.agmip.translators.agmip;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
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
    public void runTranslation() {
        AgmipInput in = new AgmipInput();
        Map results = in.readFile(resource.getPath());
        LOG.info("Translation Results: "+results);
    }
    
    
}
