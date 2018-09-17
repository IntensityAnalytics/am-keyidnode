<!--
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
 * Copyright ${data.get('yyyy')} ForgeRock AS.
-->
# TickStream.KeyID Authentication Node

**PREVIEW RELEASE** of an authentication node for ForgeRock's [Identity Platform][forgerock_platform] that protects the login process with TickStream.KeyID. With TickStream.KeyID a user's typing behavior can be passively or actively enrolled and evaluated to provide an advanced second factor behavioral biometric. For more information visit <http://www.intensityanalytics.com>

For evaluation licenses please contact <sales@intensityanalytics.com>

## BUILD ##

The code in this repository has binary dependencies that live in the ForgeRock maven repository. Maven can be configured to authenticate to this repository by following the following [ForgeRock Knowledge Base Article](https://backstage.forgerock.com/knowledge/kb/article/a74096897).

## Installation ##
Copy the .jar file from the ../target directory into the ../webapps/openam/WEB-INF/lib directory where AM is deployed. Restart the AM service to load the TickStream.KeyID authentication tree node. The TickStream.KeyID components will then be available for use in the Authenticaton Tree designer.

## USAGE ##

### AUTHENTICATING ###
![](./images/authtree.png)

To protect logins with TickStream.KeyID you must configure the Authentication Tree to use the TickStream.KeyID Login Form and TickStream.KeyID nodes.

![](./images/loginform.png)

The TickStream.KeyID Login Form node captures typing behavior metrics using JavaScript and stores it in a shared state variable. You may customize the path to the TickStream.KeyID JavaScript library.

The TickStream.KeyID node evaluates the login data captured by the login form. Typically the node is placed after the password has been authenticated. You must provide the webservice URL and authentication key for your TickStream.KeyID server. There are several additional configuration operations that let you customize the login process.

Option | Description
-- | --
**Connection Timeout** | TickStream.KeyID web service connection timeout in milliseconds
**Reset Profile** | Reset TickStream.KeyID profile after verification
**Validation / Enrollment** |
Passive / None | Always allow the user access, do not enroll the profile
Passive / Passive | Always allow the user access, passively enroll the profile
Active / None | Gate user access, do not enroll the profile
Active / Passive | Gate user access, passively enroll profile with each subsequent login
Active / Active | Gate user access, actively enroll profile until it is complete
**Custom Threshold** | Provide a custom threshold different than the TickStream.KeyID server setting
**Threshold Confidence** | Custom threshold confidence value (integer)
**Threshold Fidelity** | Custom threshold fidelity value (integer)
**Grant On Error** | Allow access if there is an error communicating with the TickStream.KeyID web service

### ENROLLMENT ###

![](./images/activeauthtree.png)

Passwords used for enrollment should be at least 10 characters. With the TickStream.KeyID Auth Tree Node you may configure enrollment to be 'active' or 'passive'. In the active scenario shown above, a user will be prompted to enter their password repeatedly until the behavior profile is complete. In a passive scenario, the user profile will be built over subsequent logins.

### PASSWORD RESETS ###

![](./images/resetauthtree.png)

When a user's password is reset, the user's TickStream.KeyID profile must also be reset using the TickStream.KeyID web service. Because the password reset process is environment and deployment specific, we only provide a simple scenario using authentication trees for demonstration purposes. 

You can construct an password reset authentication tree using TickStream.KeyID components and the ForgeRock [Set Profile Property Authentication Node.](https://github.com/ForgeRock/set-profile-property-auth-tree-node) Construct an authentication tree as shown in the above diagram. Configure the TickStream.KeyID node normally and enable the `Reset Profile` option and disable Profile and Passive enrollment. Configure the Set Profile Property node to have a key of `userPassword` and a value of `password`.

When accessing the authentication tree, the user will be prompted to provide their username and password, the password and typing behavior will be validated, the user prompted for a new password and the KeyID profile and user password changed.

## TROUBLESHOOTING ##

Errors, warnings and messages are logged in the `openam/openam/debug/KeyIDNode` file. You may configure the logging level in AM by going to the `openam/Debug.jsp` page. Only errors are logged when the AM service is started by default.

## DISCLAIMER ##

The sample code described herein is provided on an "as is" basis, without warranty of any kind, to the fullest extent permitted by law. Intensity Analytics does not warrant or guarantee the individual success developers may have in implementing the sample code on their development platforms or in production configurations.

Intensity Analytics does not warrant, guarantee or make any representations regarding the use, results of use, accuracy, timeliness or completeness of any data or information relating to the sample code. Intensity Analytics disclaims all warranties, expressed or implied, and in particular, disclaims all warranties of merchantability, and warranties related to the code, or any service or software related thereto.

Intensity Analytics shall not be liable for any direct, indirect or consequential damages or costs of any type arising out of any action taken by you or others related to the sample code.

[forgerock_platform]: https://www.forgerock.com/platform/  
