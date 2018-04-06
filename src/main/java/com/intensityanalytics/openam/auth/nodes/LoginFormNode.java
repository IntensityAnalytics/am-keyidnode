package com.intensityanalytics.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static com.intensityanalytics.openam.auth.nodes.Constants.TSDATA;

import java.util.List;
import java.util.ArrayList;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;

import org.forgerock.guava.common.base.Strings;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.openam.core.CoreWrapper;

/**
 * A node which collects a username from the user via a name callback.
 *
 * <p>Places the result in the shared state as 'username'.</p>
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = LoginFormNode.Config.class)
public class LoginFormNode extends SingleOutcomeNode
{
    private final Config config;
    private final CoreWrapper coreWrapper;
    private static final String BUNDLE = "com/intensityanalytics/openam/auth/nodes/LoginFormNode";
    private final static String DEBUG_FILE = "LoginFormNode";
    protected Debug debug = Debug.getInstance(DEBUG_FILE);

    interface Config
    {
        @Attribute(order = 100)
        default String library()
        {
            return "//keyidservices.tickstream.com/library/keyid-min";
        }
    }

    /**
     * Create the node.
     *
     * @param config The service config.
     * @throws NodeProcessException If the configuration was not valid.
     */
    @Inject
    public LoginFormNode(@Assisted LoginFormNode.Config config, CoreWrapper coreWrapper) throws NodeProcessException
    {
        this.config = config;
        this.coreWrapper = coreWrapper;
    }

    @Override
    public Action process(TreeContext context)
    {
        JsonValue sharedState = context.sharedState.copy();

        context.getCallback(NameCallback.class)
                .map(NameCallback::getName)
                .filter(name -> !Strings.isNullOrEmpty(name))
                .map(name -> sharedState.put(USERNAME, name));

        context.getCallback(PasswordCallback.class)
                .map(PasswordCallback::getPassword)
                .map(String::new)
                .filter(password -> !Strings.isNullOrEmpty(password))
                .map(password -> sharedState.put(PASSWORD, password));

        context.getCallback(HiddenValueCallback.class)
                .map(HiddenValueCallback::getValue)
                .filter(tsData -> !Strings.isNullOrEmpty(tsData))
                .map(tsData -> sharedState.put(TSDATA, tsData));

        if (sharedState.get(PASSWORD).isNotNull() &&
            sharedState.get(USERNAME).isNotNull() &&
            sharedState.get(TSDATA).isNotNull())
        {
            debug.message("username: " + sharedState.get(USERNAME));
            debug.message("password: " + sharedState.get(PASSWORD));
            debug.message("tsData: " + sharedState.get(TSDATA));
            return goToNext().replaceSharedState(sharedState).build();
        }
        else
        {
            return collectUsernamePasswordData(context);
        }

    }

    private Action collectUsernamePasswordData(TreeContext context)
    {
        ResourceBundle bundle = context.request.locales.getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());
        List<Callback> callBackList = new ArrayList<>();
        callBackList.add(new NameCallback(bundle.getString("callback.username")));
        callBackList.add(new PasswordCallback(bundle.getString("callback.password"), false));
        callBackList.add(new ScriptTextOutputCallback(String.format(KEYIDSCRIPT, config.library())));
        callBackList.add(new HiddenValueCallback(TSDATA));

        return send(callBackList).build();
    }

    private static final String KEYIDSCRIPT = "var script = document.createElement('script');\n" +
                                              "script.onload = function () {\n" +
                                              "    $('[name=\"callback_1\"]').attr('id', 'idToken1');\n" +
                                              "    tsBindControl('idToken1', bandType.KeyID, true);\n" +
                                              "    $('#loginButton_0').click(function(){populateControlWithKeyDataConcatenated('tsData');});\n" +
                                              "};\n" +
                                              "script.src = '%s'\n" +
                                              "\n" +
                                              "document.body.appendChild(script);";
}
