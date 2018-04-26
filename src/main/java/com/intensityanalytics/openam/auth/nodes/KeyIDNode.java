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

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.openam.core.CoreWrapper;
import javax.inject.Inject;
import com.intensityanalytics.keyid.*;
import com.google.gson.JsonObject;

import java.io.PrintWriter;
import java.io.StringWriter;

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
            return true;
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

        @Attribute(order = 1000)
        default Boolean grantOnError()
        {
            return false;
        }

        @Attribute(order = 1100)
        default Boolean resetProfile()
        {
            return false;
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
        debug.message("KeyIDNode() called");
        this.config = config;
        this.coreWrapper = coreWrapper;
        settings = createSettings();
        client = new KeyIDClient(settings);
    }

    /**
     * Process a login using TickStream.KeyID.
     */
    @Override
    public Action process(TreeContext context) throws NodeProcessException
    {
        debug.message("KeyIDNode.process() called");
        JsonValue sharedState = context.sharedState.copy();
        JsonObject result;

        try
        {
            String username = sharedState.get(USERNAME).asString();
            String tsData = sharedState.get(TSDATA).asString();
            debug.warning(String.format("KeyID evaluation started for user %s", username));
            debug.message(String.format("KeyID tsData: %s", tsData));

            result = client.Login(username, tsData, "").get();

            if (!result.get("Error").getAsString().isEmpty())
                throw new NodeProcessException("KeyID Error: " + result.get("Error").getAsString());

            debug.message(String.format("KeyID behavior statistics: Match=%b, Confidence=%f, Fidelity=%f, Profiles=%d, IsReady=%b",
                                        result.get("Match").getAsBoolean(),
                                        result.get("Confidence").getAsDouble(),
                                        result.get("Fidelity").getAsDouble(),
                                        result.get("Profiles").getAsInt(),
                                        result.get("IsReady").getAsBoolean()));

            // handle successful match and whether passive validation is enabled
            if (result.get("Match").getAsBoolean() || config.passiveValidation())
            {
                String msg = String.format("KeyID behavior match %b, passive validation %b",
                                           result.get("Match").getAsBoolean(),
                                           config.passiveEnrollment());
                debug.warning(msg);

                if (config.resetProfile())
                {
                    debug.warning(String.format("Resetting KeyID profile for %s", username));
                    client.RemoveProfile(username, tsData, "").get();
                }

                return goTo(true).build();
            }
        }
        catch (Exception e)
        {
            // write full error detail to log
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            debug.error("NodeProcessException: " + exceptionAsString);

            if(config.grantOnError())
            {
                debug.error("Access grant on error");
                return goTo(true).build();
            }
            else
                throw new NodeProcessException("An error occured, please try again.");
        }

        // default case is to return failure, tricky because we're using exceptions to return information to the user and as actual exceptions
        debug.warning("KeyID behavior match failure");
        throw new NodeProcessException("The typing effort does not match the profile.");
    }

    /**
     * Create a KeyIDSettings object using settings configured for authentication tree node.
     * @return KeyIDSettings object
     */
    private KeyIDSettings createSettings()
    {
        KeyIDSettings settings = new KeyIDSettings();
        settings.setUrl(config.url());
        settings.setLicense(config.authKey());
        settings.setCustomThreshold(config.customThreshold());
        settings.setPassiveEnrollment(config.passiveEnrollment());
        settings.setThresholdConfidence(config.thresholdConfidence());
        settings.setThresholdFidelity(config.thresholdFidelity());
        settings.setStrictSSL(config.strictSSL());
        settings.setTimeout(config.timeout());
        return settings;
    }
}