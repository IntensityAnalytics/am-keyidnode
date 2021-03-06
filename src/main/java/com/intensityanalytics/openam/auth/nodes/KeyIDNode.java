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
import org.forgerock.guava.common.collect.ImmutableList;
import javax.inject.Inject;
import com.intensityanalytics.keyid.*;
import com.google.gson.JsonObject;
import org.forgerock.util.i18n.PreferredLocales;
import java.util.List;
import java.util.ResourceBundle;
import static com.intensityanalytics.openam.auth.nodes.Constants.TSDATA;
import static com.intensityanalytics.openam.auth.nodes.Utility.*;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

/**
 * A node that validates a user's typing behavior using TickStream.KeyID
 */
@Node.Metadata(outcomeProvider = KeyIDNode.OutcomeProvider.class,
configClass = KeyIDNode.Config.class)
public class KeyIDNode extends AbstractDecisionNode
{
    private KeyIDSettings settings;
    private KeyIDClient client;
    private final Config config;
    private final CoreWrapper coreWrapper;
    private final static String DEBUG_FILE = "KeyIDNode";
    private Debug debug = Debug.getInstance(DEBUG_FILE);
    private final static String TRUE_OUTCOME = "true";
    private final static String FALSE_OUTCOME = "false";
    private final static String ENROLL_OUTCOME = "enroll";

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
            return 5000;
        }

        @Attribute(order = 400)
        default Boolean resetProfile()
        {
            return false;
        }

        @Attribute(order = 500)
        default ValidationEnrollmentMode validationEnrollmentMode()
        {
            return ValidationEnrollmentMode.ACTIVE_ACTIVE;
        }

        @Attribute(order = 800)
        default Boolean customThreshold()
        {
            return false;
        }

        @Attribute(order = 900)
        default Integer thresholdConfidence()
        {
            return 70;
        }

        @Attribute(order = 1000)
        default Integer thresholdFidelity()
        {
            return 50;
        }

        @Attribute(order = 1100)
        default Boolean grantOnError()
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
        JsonValue transientState = context.transientState.copy();
        transientState.remove(PASSWORD);

        try
        {
            String username = sharedState.get(USERNAME).asString();
            String tsData = sharedState.get(TSDATA).asString();
            JsonObject loginResult = keyIDLogin(username, tsData);

            // handle active enrollment
            if (config.validationEnrollmentMode() == ValidationEnrollmentMode.ACTIVE_ACTIVE &&
                !loginResult.get("IsReady").getAsBoolean())
            {
                return Action.goTo(ENROLL_OUTCOME).replaceSharedState(sharedState).replaceTransientState
                (transientState).build();
            }

            // handle successful match and whether passive validation is enabled
            if (loginResult.get("Match").getAsBoolean() ||
                config.validationEnrollmentMode() == ValidationEnrollmentMode.PASSIVE_NONE ||
                config.validationEnrollmentMode() == ValidationEnrollmentMode.PASSIVE_PASSIVE)
            {
                String msg = String.format("KeyID behavior match %b, validation / enrollment mode %s",
                                           loginResult.get("Match").getAsBoolean(),
                                           config.validationEnrollmentMode().toString());
                debug.warning(msg);

                if (config.resetProfile())
                    keyIDResetProfile(username, tsData);

                return Action.goTo(TRUE_OUTCOME).replaceSharedState(sharedState).replaceTransientState
                (transientState).build();
            }
        }
        catch (Exception e)
        {
            debug.error("Exception: " + exceptionAsString(e));

            if(config.grantOnError())
            {
                debug.error("Access grant on error");
                return Action.goTo(TRUE_OUTCOME).replaceSharedState(sharedState).replaceTransientState
                (transientState).build();
            }
            else
                throw new NodeProcessException("An error occured, please try again.");
        }

        // default case is to return failure, rely on default login failed error message
        debug.warning("KeyID behavior match failure");
        return Action.goTo(FALSE_OUTCOME).replaceSharedState(sharedState).replaceTransientState(transientState)
        .build();
    }

    /**
     * Executes the KeyID login process and checks the result for errors.
     * @param username  username
     * @param tsData    tsData
     * @return          KeyID login result.
     * @throws Exception
     */
    private JsonObject keyIDLogin(String username, String tsData) throws Exception
    {
        debug.warning(String.format("KeyID evaluation started for user %s", username));
        debug.message(String.format("KeyID tsData: %s", tsData));

        JsonObject result = client.Login(username, tsData, "").get();

        if (!result.get("Error").getAsString().isEmpty())
            throw new NodeProcessException("KeyID Error: " + result.get("Error").getAsString());

        debug.message(String.format("KeyID behavior statistics: Match=%b, Confidence=%f, Fidelity=%f, Profiles=%d, IsReady=%b",
                                    result.get("Match").getAsBoolean(),
                                    result.get("Confidence").getAsDouble(),
                                    result.get("Fidelity").getAsDouble(),
                                    result.get("Profiles").getAsInt(),
                                    result.get("IsReady").getAsBoolean()));
        return result;
    }

    /**
     * Resets a KeyID profile
     * @param username  username
     * @param tsData    tsData
     * @throws Exception
     */
    private void keyIDResetProfile(String username, String tsData) throws Exception
    {
        debug.warning(String.format("Resetting KeyID profile for %s", username));
        client.RemoveProfile(username, tsData, "").get();
        //todo need error checking here when rest webservice bug is fixed.
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
        settings.setThresholdConfidence(config.thresholdConfidence());
        settings.setThresholdFidelity(config.thresholdFidelity());
        settings.setTimeout(config.timeout());

        if (config.validationEnrollmentMode() == ValidationEnrollmentMode.PASSIVE_PASSIVE ||
            config.validationEnrollmentMode() == ValidationEnrollmentMode.ACTIVE_PASSIVE ||
            config.validationEnrollmentMode() == ValidationEnrollmentMode.ACTIVE_ACTIVE)
        {
            settings.setLoginEnrollment(true);
        }
        else
            settings.setLoginEnrollment(false);

        return settings;
    }

    public enum ValidationEnrollmentMode
    {
        PASSIVE_NONE,
        PASSIVE_PASSIVE,
        ACTIVE_NONE,
        ACTIVE_PASSIVE,
        ACTIVE_ACTIVE
    }

    /**
     * KeyIDNode custom outcome provider.
     */
    static class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        private static final String BUNDLE = KeyIDNode.class.getName().replace(".", "/");

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, OutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
            new Outcome(TRUE_OUTCOME, bundle.getString("trueOutcome")),
            new Outcome(FALSE_OUTCOME, bundle.getString("falseOutcome")),
            new Outcome(ENROLL_OUTCOME, bundle.getString("enrollOutcome")));
        }
    }
}