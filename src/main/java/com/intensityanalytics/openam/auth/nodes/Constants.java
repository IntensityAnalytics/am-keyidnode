/*
 * Copyright 2018 Intensity Analytics Corporation
 */

package com.intensityanalytics.openam.auth.nodes;

public final class Constants
{
    public static final String TSDATA = "tsData";
    public static final String KEYIDSCRIPT = "var script = document.createElement('script');\n" +
                                              "script.onload = function () {\n" +
                                              "    $('[name=\"callback_1\"]').attr('id', 'idToken1');\n" +
                                              "    tsBindControl('idToken1', bandType.KeyID, true);\n" +
                                              "    $('#loginButton_0').click(function(){populateControlWithKeyDataConcatenated('tsData');});\n" +
                                              "};\n" +
                                              "script.src = '%s'\n" +
                                              "\n" +
                                              "document.body.appendChild(script);";
}
