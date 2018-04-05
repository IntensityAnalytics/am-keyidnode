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
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.openam.core.CoreWrapper;

import javax.inject.Inject;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

/**
 * A node that checks to see if zero-page login headers have specified username and shared key
 * for this request.
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = KeyIDNode.Config.class)
public class KeyIDNode extends AbstractDecisionNode
{

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
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException
    {
        return goTo(true).build();
    }
}