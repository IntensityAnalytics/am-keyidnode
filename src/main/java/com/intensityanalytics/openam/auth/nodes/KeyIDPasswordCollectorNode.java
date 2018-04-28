/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2017 ForgeRock AS.
 */
/*
 * Portions copyright 2018 Intensity Analytics Corporation
 */

package com.intensityanalytics.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static com.intensityanalytics.openam.auth.nodes.Constants.*;

import java.util.List;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;

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
 * A node which collects the username, password and KeyID typing data from the user via callbacks.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
configClass = KeyIDPasswordCollectorNode.Config.class)
public class KeyIDPasswordCollectorNode extends SingleOutcomeNode
{
    private final Config config;
    private final CoreWrapper coreWrapper;
    private static final String BUNDLE = "com/intensityanalytics/openam/auth/nodes/KeyIDPasswordCollectorNode";
    private final static String DEBUG_FILE = "KeyIDLoginFormNode";
    protected Debug debug = Debug.getInstance(DEBUG_FILE);

    interface Config
    {
        @Attribute(order = 100)
        default String library()
        {
            return "//keyidservices.tickstream.com/library/keyid-verbose";
        }
    }

    /**
     * Create the node.
     *
     * @param config The service config.
     * @throws NodeProcessException If the configuration was not valid.
     */
    @Inject
    public KeyIDPasswordCollectorNode(@Assisted KeyIDPasswordCollectorNode.Config config, CoreWrapper coreWrapper) throws NodeProcessException
    {
        debug.message( "KeyIDPasswordCollectorNode() called");
        this.config = config;
        this.coreWrapper = coreWrapper;
    }

    @Override
    public Action process(TreeContext context)
    {
        debug.message("KeyIDPasswordCollectorNode.process() called");
        JsonValue sharedState = context.sharedState.copy();

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
            debug.warning(String.format("Password submitted for user %s", sharedState.get(USERNAME)));
            debug.message(String.format("KeyID tsData: %s", sharedState.get(TSDATA)));
            return goToNext().replaceSharedState(sharedState).build();
        }
        else
        {
            return collectPasswordData(context);
        }
    }

    private Action collectPasswordData(TreeContext context)
    {
        debug.message("KeyIDPasswordCollectorNode.collectUsernamePasswordData() called");
        ResourceBundle bundle = context.request.locales.getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());
        List<Callback> callBackList = new ArrayList<>();
        callBackList.add(new TextOutputCallback(TextOutputCallback.INFORMATION, "Please enter your password again to continue enrollment."));
        callBackList.add(new PasswordCallback(bundle.getString("callback.password"), false));
        callBackList.add(new ScriptTextOutputCallback(String.format(KEYIDSCRIPT, config.library())));
        callBackList.add(new HiddenValueCallback(TSDATA));

        return send(callBackList).build();
    }
}

