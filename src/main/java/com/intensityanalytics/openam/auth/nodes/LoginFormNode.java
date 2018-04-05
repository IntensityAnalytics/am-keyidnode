package com.intensityanalytics.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static com.intensityanalytics.openam.auth.nodes.Constants.TSDATA;

import java.util.List;
import java.util.ArrayList;
import java.util.ResourceBundle;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;

import org.forgerock.guava.common.base.Strings;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;

/**
 * A node which collects a username from the user via a name callback.
 *
 * <p>Places the result in the shared state as 'username'.</p>
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = LoginFormNode.Config.class)
public class LoginFormNode extends SingleOutcomeNode
{
    private final static String DEBUG_FILE = "LoginFormNode";
    protected Debug debug = Debug.getInstance(DEBUG_FILE);

    interface Config
    {
    }

    private static final String BUNDLE = "com/intensityanalytics/openam/auth/nodes/LoginFormNode";

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
        callBackList.add(new ScriptTextOutputCallback("console.log('hello world');"));
        callBackList.add(new HiddenValueCallback(TSDATA));

        return send(callBackList).build();
    }
}
