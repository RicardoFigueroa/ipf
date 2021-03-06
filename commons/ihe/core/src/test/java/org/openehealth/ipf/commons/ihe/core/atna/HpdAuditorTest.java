/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openehealth.ipf.commons.ihe.core.atna;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openehealth.ipf.commons.ihe.core.atna.custom.HpdAuditor;
import org.openhealthtools.ihe.atna.auditor.codes.rfc3881.RFC3881EventCodes;
import org.openhealthtools.ihe.atna.auditor.codes.rfc3881.RFC3881EventCodes.RFC3881EventOutcomeCodes;
import org.openhealthtools.ihe.atna.auditor.context.AuditorModuleContext;
import org.openhealthtools.ihe.atna.auditor.models.rfc3881.CodedValueType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Dmytro Rud
 */
public class HpdAuditorTest extends Assert {

    private static final String REPLY_TO_URI        = "reply-to-uri";
    private static final String USER_NAME           = "alias<user@issuer>";
    private static final String SERVER_URI          = "server-uri";
    private static final String CLIENT_IP_ADDRESS   = "141.44.162.126";
    private static final List<String> PROVIDER_IDS = Arrays.asList(
            "2.16.10.89.200:UPIN:800-800-8000:Active",
            "2.16.10.98.123:NPI:666789-800:Active",
            "1.89.11.00.123:HospId:786868:Active");

    private static final List<CodedValueType> PURPOSES_OF_USE;
    static {
        PURPOSES_OF_USE = new ArrayList<>();

        CodedValueType cvt1 = new CodedValueType();
        cvt1.setCode("12");
        cvt1.setCodeSystemName("1.0.14265.1");
        cvt1.setOriginalText("Law Enforcement");
        PURPOSES_OF_USE.add(cvt1);

        CodedValueType cvt2 = new CodedValueType();
        cvt2.setCode("13");
        cvt2.setCodeSystemName("1.0.14265.1");
        cvt2.setOriginalText("Something Else");
        PURPOSES_OF_USE.add(cvt2);
    }

    private MockedSender sender;

    @Before
    public void setUp() throws Exception {
        sender = new MockedSender();
        AuditorModuleContext.getContext().setSender(sender);
        AuditorModuleContext.getContext().getConfig().setAuditRepositoryHost("localhost");
        AuditorModuleContext.getContext().getConfig().setAuditRepositoryPort(514);
    }

    @Test
    public void testAuditors() {
        final HpdAuditor auditor = AuditorManager.getHpdAuditor();

        auditor.auditIti59(true,
                RFC3881EventCodes.RFC3881EventActionCodes.CREATE,
                RFC3881EventOutcomeCodes.SUCCESS, REPLY_TO_URI, USER_NAME, SERVER_URI, CLIENT_IP_ADDRESS,
                PROVIDER_IDS,
                PURPOSES_OF_USE);

        auditor.auditIti59(false,
                RFC3881EventCodes.RFC3881EventActionCodes.CREATE,
                RFC3881EventOutcomeCodes.SUCCESS, REPLY_TO_URI, USER_NAME, SERVER_URI, CLIENT_IP_ADDRESS,
                PROVIDER_IDS,
                PURPOSES_OF_USE);

        assertEquals(2, sender.getMessages().size());
    }

}