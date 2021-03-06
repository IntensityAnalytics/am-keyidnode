/*
 * Copyright 2018 Intensity Analytics Corporation
 */

package com.intensityanalytics.openam.auth.nodes;

public final class Constants
{
    public static final String TSDATA = "tsData";
    public static final String KEYIDSCRIPT = "var script = document.createElement('script');\n" +
                                             "script.onload = function () {\n" +
                                             "  document.querySelector(\"input[name='callback_1']\").id = 'idToken1';\n" +
                                             "  tsBindControl('idToken1', bandType.KeyID, true);\n" +
                                             "  document.querySelector('#loginButton_0').addEventListener('click', function (e) {\n" +
                                             "    populateControlWithKeyDataConcatenated('tsData');\n" +
                                             "  });" +
                                             "};\n" +
                                             "script.src = '%s'\n" +
                                             "\n" +
                                             "document.body.appendChild(script);";
}
