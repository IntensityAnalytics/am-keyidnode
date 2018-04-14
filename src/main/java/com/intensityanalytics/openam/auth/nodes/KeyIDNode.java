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

package com.intensityanalytics.openam.auth.nodes;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.http.util.Json;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.openam.core.CoreWrapper;
import javax.inject.Inject;
import com.intensityanalytics.keyid.*;
import com.google.gson.JsonObject;
import static com.intensityanalytics.openam.auth.nodes.Constants.TSDATA;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;


/**
 * A node that validates a user's typing behavior using TickStream.KeyID
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = KeyIDNode.Config.class)
public class KeyIDNode extends AbstractDecisionNode
{
    private KeyIDSettings settings;
    private KeyIDClient client;
    private final Config config;
    private final CoreWrapper coreWrapper;
    private final static String DEBUG_FILE = "KeyIDNode";
    protected Debug debug = Debug.getInstance(DEBUG_FILE);

    /**
     * Configuration for the node.
     */
    public interface Config
    {
        @Attribute(order = 100)
        default String url()
        {
            return "";
        }

        @Attribute(order = 200)
        default String authKey()
        {
            return "";
        }

        @Attribute(order = 300)
        default Integer timeout()
        {
            return 0;
        }

        @Attribute(order = 400)
        default Boolean strictSSL()
        {
            return true;
        }

        @Attribute(order = 500)
        default Boolean passiveValidation()
        {
            return false;
        }

        @Attribute(order = 600)
        default Boolean passiveEnrollment()
        {
            return false;
        }

        @Attribute(order = 700)
        default Boolean customThreshold()
        {
            return false;
        }

        @Attribute(order = 800)
        default Integer thresholdConfidence()
        {
            return 70;
        }

        @Attribute(order = 900)
        default Integer thresholdFidelity()
        {
            return 50;
        }
    }

    /**
     * Create the node.
     *
     * @param config The service config.
     * @throws NodeProcessException If the configuration was not valid.
     */
    @Inject
    public KeyIDNode(@Assisted Config config, CoreWrapper coreWrapper) throws NodeProcessException
    {
        this.config = config;
        this.coreWrapper = coreWrapper;
        settings = createSettings();
        client = new KeyIDClient(settings);
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException
    {
        JsonValue sharedState = context.sharedState.copy();

        if (sharedState.get(USERNAME).isNull())
            throw new NodeProcessException("USERNAME expected in shared state.");

        if (sharedState.get(TSDATA).isNull())
            throw new NodeProcessException("TSDATA expected in shared state.");

        String username = sharedState.get(USERNAME).asString();
        String tsData = sharedState.get(TSDATA).asString();

        if (settings.isPassiveEnrollment())
        {
            JsonObject result;
            try
            {
                result = client.LoginPassiveEnrollment(username, tsData, "").get();

                if (result.get("Match").getAsBoolean())
                    return goTo(true).build();
                else
                    return goTo(false).build();
            }
            catch (Exception e)
            {
                throw new NodeProcessException(e);
            }
        }
        else
        {
            JsonObject result;
            try
            {
                result = client.EvaluateProfile(username, tsData, "").get();

                if (result.get("Match").getAsBoolean())
                    return goTo(true).build();
                else
                    return goTo(false).build();
            }
            catch (Exception e)
            {
                throw new NodeProcessException(e);
            }
        }
    }

    private KeyIDSettings createSettings()
    {
        KeyIDSettings settings = new KeyIDSettings();
        settings.setUrl(config.url());
        settings.setLicense(config.authKey());
        settings.setCustomThreshold(config.customThreshold());
        settings.setPassiveEnrollment(config.passiveEnrollment());
        settings.setPassiveValidation(config.passiveValidation());
        settings.setThresholdConfidence(config.thresholdConfidence());
        settings.setThresholdFidelity(config.thresholdFidelity());
        settings.setStrictSSL(config.strictSSL());
        settings.setTimeout(config.timeout());

        return settings;
    }
}