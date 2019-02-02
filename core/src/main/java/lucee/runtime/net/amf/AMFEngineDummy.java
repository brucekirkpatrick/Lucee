package lucee.runtime.net.amf;

import java.io.IOException;
import java.util.Map;


import coreLoad.RequestResponseImpl;


import lucee.runtime.config.ConfigWeb;
import lucee.runtime.exp.ApplicationException;
import lucee.runtime.exp.PageException;
import lucee.runtime.exp.PageRuntimeException;

public class AMFEngineDummy implements AMFEngine {

    private static AMFEngine instance;

    private AMFEngineDummy() {

    }

    public static AMFEngine getInstance() {
	if (instance == null) instance = new AMFEngineDummy();
	return instance;
    }

    @Override
    public void init(ConfigWeb config, Map<String, String> arguments) throws IOException {
	// do nothing
    }

    @Override
    public void service(RequestResponse req) throws IOException {
	throw notInstalledEL();
    }

    public static PageException notInstalled() {
	return new ApplicationException("No AMF Engine (Flex) installed!", "Check out the Extension Store in the Lucee Administrator for \"Flex\".");
    }

    public static PageRuntimeException notInstalledEL() {
	return new PageRuntimeException(notInstalled());
    }
}
