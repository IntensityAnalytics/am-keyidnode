package com.intensityanalytics.openam.auth.nodes;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Utility
{
    public static String exceptionAsString(Exception e)
    {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
