package com.intensityanalytics.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import java.util.List;
import java.util.ArrayList;
import javax.security.auth.callback.Callback;
import java.util.ResourceBundle;

import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import com.sun.identity.shared.debug.Debug;
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
public class LoginFormNode extends SingleOutcomeNode {
    private final static String DEBUG_FILE = "LoginFormNode";
    protected Debug debug = Debug.getInstance(DEBUG_FILE);

    interface Config {
    }

    private static final String BUNDLE = "com/intensityanalytics/openam/auth/nodes/LoginFormNode";

    @Override
    public Action process(TreeContext context) {
        JsonValue sharedState = context.sharedState;
        return context.getCallback(NameCallback.class)
                .map(NameCallback::getName)
                .filter(password -> !Strings.isNullOrEmpty(password))
                .map(name -> goToNext().replaceSharedState(sharedState.copy().put(USERNAME, name)).build())
                .orElseGet(() -> collectUsernamePassword(context));
    }

    private Action collectUsernamePassword(TreeContext context) {
        ResourceBundle bundle = context.request.locales.getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());
        List<Callback> callBackList = new ArrayList<>();
        callBackList.add(new NameCallback(bundle.getString("callback.username")));
        callBackList.add(new PasswordCallback(bundle.getString("callback.password"), false));
        return send(callBackList).build();
    }
}
